package com.keeftalk.chat.feature.email.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.keeftalk.chat.data.local.entities.MailAccountEntity
import com.keeftalk.chat.feature.email.model.*
import com.keeftalk.chat.feature.email.repository.EmailRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class EmailViewModel(
    private val repository: EmailRepository,
    private val authManager: com.keeftalk.chat.feature.email.auth.EmailAuthManager
) : ViewModel() {

    private val _onboardingStep = MutableStateFlow<OnboardingStep>(OnboardingStep.ProviderSelection)
    val onboardingStep = _onboardingStep.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId = _selectedAccountId.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId = _selectedFolderId.asStateFlow()

    private val _selectedFolderType = MutableStateFlow(FolderType.INBOX)
    val selectedFolderType = _selectedFolderType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMessageIds = _selectedMessageIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    // Settings State
    private val _signature = MutableStateFlow("Sent from Keeftalk")
    val signature = _signature.asStateFlow()

    private val _syncFrequency = MutableStateFlow("15 minutes")
    val syncFrequency = _syncFrequency.asStateFlow()

    private val _theme = MutableStateFlow("System default")
    val theme = _theme.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    val accounts = repository.getAccounts()
        .onEach { android.util.Log.d("EmailViewModel", "Accounts updated: ${it.size} - ${it.map { a -> a.emailAddress }}") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _forceOnboarding = MutableStateFlow(false)
    
    val showOnboarding = combine(accounts, _forceOnboarding) { acc, force ->
        acc.isEmpty() || force
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun forceShowOnboarding(show: Boolean) {
        _forceOnboarding.value = show
    }

    val messages: Flow<PagingData<EmailMessage>> = combine(
        _selectedAccountId,
        _selectedFolderId,
        _selectedFolderType,
        _searchQuery,
        accounts
    ) { accountId, folderId, folderType, query, _ ->
        if (query.isNotBlank()) {
            repository.searchMessages(query)
        } else {
            when {
                folderType == FolderType.STARRED -> repository.getStarredMessages(accountId)
                folderType == FolderType.IMPORTANT -> repository.getImportantMessages(accountId)
                folderType == FolderType.SHARED -> repository.getSharedEmails()
                accountId == null -> repository.getMergedInbox()
                folderId != null -> repository.getMessages(accountId, folderId)
                else -> repository.getMessagesByType(accountId, folderType)
            }
        }
    }.flatMapLatest { it }
        .cachedIn(viewModelScope)

    fun setOnboardingStep(step: OnboardingStep) {
        _onboardingStep.value = step
    }

    fun getGoogleSignInIntent(): android.content.Intent {
        _errorMessage.value = null
        return authManager.getGoogleSignInIntent()
    }

    fun handleGoogleSignInResult(data: android.content.Intent?) {
        android.util.Log.d("EmailViewModel", "handleGoogleSignInResult START. Data null: ${data == null}")
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account == null) {
                _errorMessage.value = "Google account is null"
                android.util.Log.e("EmailViewModel", "Account is null despite success result")
                return
            }
            val email = account.email ?: run {
                _errorMessage.value = "Google email is missing"
                android.util.Log.e("EmailViewModel", "Email is missing from account")
                return
            }
            
            android.util.Log.i("EmailViewModel", "Successfully retrieved account for: $email")
            
            val existingAccount = accounts.value.find { it.emailAddress.lowercase() == email.lowercase() }

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val accountId = existingAccount?.id ?: java.util.UUID.randomUUID().toString()
                    val emailAccount = EmailAccount(
                        id = accountId,
                        emailAddress = email,
                        provider = EmailProvider.GMAIL,
                        displayName = account.displayName ?: email,
                        profilePicUrl = account.photoUrl?.toString()
                    )
                    
                    setOnboardingStep(OnboardingStep.Syncing(EmailProvider.GMAIL))
                    repository.addAccount(
                        emailAccount, 
                        account.serverAuthCode ?: "", 
                        account.idToken ?: "", 
                        0
                    )
                    _selectedAccountId.value = accountId
                    _forceOnboarding.value = false
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to add account: ${e.message}"
                    android.util.Log.e("EmailViewModel", "Error adding account to repo", e)
                }
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            val statusMsg = when (e.statusCode) {
                10 -> "Developer Error: SHA-1 mismatch or Client ID not registered"
                7 -> "Network Error: Check your connection"
                12500 -> "Sign-in failed: Internal Error"
                12501 -> "User cancelled sign-in"
                else -> "Sign-in failed (Error Code: ${e.statusCode})"
            }
            _errorMessage.value = statusMsg
            android.util.Log.e("EmailViewModel", "Google Sign-In API Exception: code=${e.statusCode}, message=${e.message}")
        }
    }

    fun connectMicrosoft(activity: android.app.Activity) {
        authManager.loginMicrosoft(
            activity,
            onSuccess = { _ ->
                // Success logic
                setOnboardingStep(OnboardingStep.Syncing(EmailProvider.OUTLOOK))
            },
            onError = { _ ->
                // Error logic
            }
        )
    }

    fun testImapConnection(email: String, pass: String, imapH: String, imapP: Int, smtpH: String, smtpP: Int) {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            
            val config = MailAccountEntity(
                id = java.util.UUID.randomUUID().toString(),
                emailAddress = email,
                provider = EmailProvider.CUSTOM_IMAP,
                displayName = email.substringBefore("@"),
                imapHost = imapH,
                imapPort = imapP,
                smtpHost = smtpH,
                smtpPort = smtpP,
                securityType = "SSL_TLS"
            )

            val result = repository.validateImapConnection(config, pass)
            
            if (result.isSuccess) {
                repository.addImapAccount(
                    account = EmailAccount(config.id, email, EmailProvider.CUSTOM_IMAP, config.displayName),
                    password = pass,
                    imapHost = imapH,
                    imapPort = imapP,
                    smtpHost = smtpH,
                    smtpPort = smtpP,
                    securityType = "SSL_TLS"
                )
                setOnboardingStep(OnboardingStep.Syncing(EmailProvider.CUSTOM_IMAP))
                _forceOnboarding.value = false
            } else {
                _errorMessage.value = "Connection failed: ${result.exceptionOrNull()?.message}"
            }
            _isSyncing.value = false
        }
    }

    fun selectAccount(accountId: String?) {
        _selectedAccountId.value = accountId
        _selectedFolderId.value = null
        _selectedFolderType.value = FolderType.INBOX
    }

    fun selectFolder(folderId: String) {
        _selectedFolderId.value = folderId
        _selectedFolderType.value = FolderType.CUSTOM
    }

    fun selectFolderType(type: FolderType) {
        _selectedFolderType.value = type
        _selectedFolderId.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleMessageSelection(messageId: String) {
        val current = _selectedMessageIds.value
        if (current.contains(messageId)) {
            _selectedMessageIds.value = current - messageId
        } else {
            _selectedMessageIds.value = current + messageId
        }
        _isSelectionMode.value = _selectedMessageIds.value.isNotEmpty()
    }

    fun clearSelection() {
        _selectedMessageIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _selectedMessageIds.value.toList()
            ids.forEach { repository.deleteMessage(it) }
            clearSelection()
        }
    }

    fun archiveSelected() {
        viewModelScope.launch {
            val ids = _selectedMessageIds.value.toList()
            ids.forEach { repository.archiveMessage(it) }
            clearSelection()
        }
    }

    fun markSelectedAsRead(isRead: Boolean) {
        viewModelScope.launch {
            val ids = _selectedMessageIds.value.toList()
            ids.forEach { repository.markAsRead(it, isRead) }
            clearSelection()
        }
    }

    fun starSelected(isStarred: Boolean) {
        viewModelScope.launch {
            val ids = _selectedMessageIds.value.toList()
            ids.forEach { repository.toggleStar(it, isStarred) }
            clearSelection()
        }
    }

    fun refresh() {
        if (_isSyncing.value) return
        
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                android.util.Log.d("EmailViewModel", "Manual refresh started")
                val currentAccounts = accounts.value
                if (currentAccounts.isEmpty()) {
                    android.util.Log.w("EmailViewModel", "No accounts to refresh")
                    return@launch
                }
                
                // Sync all accounts in parallel
                currentAccounts.map { account ->
                    async {
                        try {
                            repository.syncAccount(account.id)
                        } catch (e: Exception) {
                            android.util.Log.e("EmailViewModel", "Error syncing account ${account.emailAddress}", e)
                        }
                    }
                }.awaitAll()
                
                android.util.Log.d("EmailViewModel", "Manual refresh completed")
            } catch (e: Exception) {
                android.util.Log.e("EmailViewModel", "Refresh failed", e)
                _errorMessage.value = "Refresh failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun getMessage(messageId: String): Flow<EmailMessage?> = repository.getMessage(messageId)

    fun syncAccount(accountId: String) {
        viewModelScope.launch {
            repository.syncAccount(accountId)
        }
    }

    fun toggleStar(messageId: String, isStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStar(messageId, isStarred)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun archiveMessage(messageId: String) {
        viewModelScope.launch {
            repository.archiveMessage(messageId)
        }
    }

    fun removeAccount(accountId: String) {
        viewModelScope.launch {
            repository.removeAccount(accountId)
        }
    }

    fun markAsRead(messageId: String, isRead: Boolean) {
        viewModelScope.launch {
            repository.markAsRead(messageId, isRead)
        }
    }

    fun downloadAttachment(attachment: EmailAttachment) {
        viewModelScope.launch {
            repository.downloadAttachment(attachment)
        }
    }

    fun sendMessage(accountId: String, subject: String, content: String, to: String) {
        viewModelScope.launch {
            // Simplified recipient parsing for now
            val recipients = to.split(",").map { EmailRecipient(null, it.trim()) }
            repository.sendMessage(accountId, subject, content, recipients, emptyList(), emptyList(), emptyList())
        }
    }

    fun setSignature(newSignature: String) {
        _signature.value = newSignature
    }

    fun setSyncFrequency(frequency: String) {
        _syncFrequency.value = frequency
    }

    fun setTheme(newTheme: String) {
        _theme.value = newTheme
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearAllCache()
        }
    }
}

sealed class OnboardingStep {
    data object ProviderSelection : OnboardingStep()
    data object ImapSetup : OnboardingStep()
    data class Syncing(val provider: EmailProvider) : OnboardingStep()
}
