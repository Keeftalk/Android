package tn.keeftalk.chat.ui.profile.media

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.keeftalk.chat.domain.model.MediaLiker
import tn.keeftalk.chat.domain.model.MediaStatus
import tn.keeftalk.chat.domain.model.MediaType
import tn.keeftalk.chat.domain.model.ProfileMedia
import tn.keeftalk.chat.domain.repository.AuthRepository
import tn.keeftalk.chat.domain.repository.ProfileMediaRepository

data class ProfileMediaViewerUiState(
    val userId: String = "",
    val mediaType: MediaType = MediaType.AVATAR,
    val isOwnProfile: Boolean = false,
    val mediaList: List<ProfileMedia> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val showLikersSheet: Boolean = false,
    val likersList: List<MediaLiker> = emptyList(),
    val isLikersLoading: Boolean = false
) {
    val currentMedia: ProfileMedia?
        get() = mediaList.getOrNull(currentIndex)
}

class ProfileMediaViewerViewModel(
    private val profileMediaRepository: ProfileMediaRepository,
    private val authRepository: AuthRepository,
    private val supabaseProvider: suspend () -> SupabaseClient
) : ViewModel() {

    private val TAG = "ProfileMediaViewerVM"

    private val _uiState = MutableStateFlow(ProfileMediaViewerUiState())
    val uiState: StateFlow<ProfileMediaViewerUiState> = _uiState.asStateFlow()

    private var isInitialIndexResolved = false

    fun init(userId: String, mediaType: MediaType, initialMediaId: String? = null, initialUrl: String? = null) {
        viewModelScope.launch {
            isInitialIndexResolved = false
            _uiState.update { it.copy(isLoading = true, userId = userId, mediaType = mediaType) }

            val currentUserId = try {
                supabaseProvider().auth.currentUserOrNull()?.id
            } catch (_: Exception) {
                null
            }

            val isOwn = currentUserId != null && currentUserId == userId
            _uiState.update { it.copy(isOwnProfile = isOwn) }

            // Observe local Room media history
            launch {
                profileMediaRepository.getMediaHistory(userId, mediaType).collect { list ->
                    var activeList = list
                    if (activeList.isEmpty() && !initialUrl.isNullOrBlank()) {
                        val fallbackMedia = ProfileMedia(
                            id = "fallback_${System.currentTimeMillis()}",
                            userId = userId,
                            mediaType = mediaType,
                            originalObjectKey = initialUrl,
                            originalCdnUrl = initialUrl,
                            compressedCdnUrl = initialUrl,
                            status = MediaStatus.ACTIVE,
                            isActive = true,
                            activatedAt = System.currentTimeMillis()
                        )
                        activeList = listOf(fallbackMedia)
                    }

                    var idx = _uiState.value.currentIndex
                    if (!isInitialIndexResolved) {
                        if (initialMediaId != null) {
                            val matchIdx = activeList.indexOfFirst { it.id == initialMediaId }
                            if (matchIdx >= 0) {
                                idx = matchIdx
                                isInitialIndexResolved = true
                            }
                        } else if (initialUrl != null && activeList.isNotEmpty()) {
                            val cleanTarget = initialUrl.substringBeforeLast("/original").substringBeforeLast("/compressed")
                            val matchIdx = activeList.indexOfFirst { 
                                it.originalCdnUrl.contains(cleanTarget) || 
                                (it.compressedCdnUrl != null && it.compressedCdnUrl.contains(cleanTarget)) ||
                                it.originalCdnUrl == initialUrl
                            }
                            if (matchIdx >= 0) {
                                idx = matchIdx
                                isInitialIndexResolved = true
                            }
                        }
                    }

                    if (idx >= activeList.size) idx = (activeList.size - 1).coerceAtLeast(0)

                    _uiState.update { 
                        it.copy(
                            mediaList = activeList,
                            currentIndex = idx,
                            isLoading = false
                        ) 
                    }
                }
            }

            // Fetch latest history from Supabase remote
            profileMediaRepository.fetchMediaHistoryFromRemote(userId, mediaType).onSuccess { remoteList ->
                if (remoteList.isEmpty() && !initialUrl.isNullOrBlank() && isOwn) {
                    profileMediaRepository.registerProfileMedia(
                        userId = userId,
                        mediaType = mediaType,
                        originalObjectKey = initialUrl,
                        compressedObjectKey = initialUrl,
                        originalCdnUrl = initialUrl,
                        compressedCdnUrl = initialUrl
                    )
                }
            }.onFailure { e ->
                Log.w(TAG, "Failed to fetch remote media history: ${e.message}")
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onSwipeTo(index: Int) {
        val size = _uiState.value.mediaList.size
        if (index in 0 until size) {
            _uiState.update { 
                it.copy(
                    currentIndex = index,
                    showLikersSheet = false,
                    likersList = emptyList()
                ) 
            }
        }
    }

    fun toggleLike() {
        val media = _uiState.value.currentMedia ?: return
        viewModelScope.launch {
            if (media.isLikedByMe) {
                profileMediaRepository.unlikeMedia(media.id).onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            } else {
                profileMediaRepository.likeMedia(media.id).onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }
    }

    fun requestDeleteConfirmation() {
        if (_uiState.value.isOwnProfile && _uiState.value.currentMedia != null) {
            _uiState.update { it.copy(showDeleteDialog = true) }
        }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun confirmDeleteCurrentMedia() {
        val media = _uiState.value.currentMedia ?: return
        if (!_uiState.value.isOwnProfile) return

        _uiState.update { it.copy(showDeleteDialog = false, isLoading = true) }

        viewModelScope.launch {
            profileMediaRepository.deleteMediaToDetention(media.id).onSuccess {
                authRepository.getAuthenticatedUser()?.let { user ->
                    authRepository.refreshProfile(user.id)
                }

                val remainingSize = _uiState.value.mediaList.size - 1
                val newIndex = (_uiState.value.currentIndex).coerceAtMost((remainingSize - 1).coerceAtLeast(0))
                _uiState.update { it.copy(currentIndex = newIndex, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun restoreCurrentMedia() {
        val media = _uiState.value.currentMedia ?: return
        if (!_uiState.value.isOwnProfile) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            profileMediaRepository.restoreMediaFromDetention(media.id).onSuccess {
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun setAsActiveMedia() {
        val media = _uiState.value.currentMedia ?: return
        if (!_uiState.value.isOwnProfile) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            profileMediaRepository.setActiveMedia(media.userId, media.id, media.mediaType).onSuccess {
                authRepository.getAuthenticatedUser()?.let { user ->
                    authRepository.refreshProfile(user.id)
                }
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun openLikersSheet() {
        val media = _uiState.value.currentMedia ?: return
        _uiState.update { it.copy(showLikersSheet = true, isLikersLoading = true) }

        viewModelScope.launch {
            profileMediaRepository.getMediaLikers(media.id).onSuccess { likers ->
                _uiState.update { it.copy(likersList = likers, isLikersLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLikersLoading = false) }
            }
        }
    }

    fun closeLikersSheet() {
        _uiState.update { it.copy(showLikersSheet = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
