package com.keeftalk.chat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.domain.model.AppNotification
import com.keeftalk.chat.domain.model.NotificationData
import com.keeftalk.chat.domain.model.NotificationType
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.util.PerformanceProfiler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class StartupState {
    data object Loading : StartupState()
    data class Ready(val isLogged: Boolean, val isEmailVerified: Boolean = true) : StartupState()
}

sealed class NotificationNavigationEvent {
    data class NavigateToChat(val chatId: String) : NotificationNavigationEvent()
    data class NavigateToNote(val noteId: String) : NotificationNavigationEvent()
    data object NavigateToCalls : NotificationNavigationEvent()
    data class ShowSystemDialog(
        val title: String, 
        val message: String,
        val type: com.keeftalk.chat.domain.model.NotificationType = com.keeftalk.chat.domain.model.NotificationType.SYSTEM,
        val sourceId: String? = null
    ) : NotificationNavigationEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    val userPreferencesRepository: UserPreferencesRepository
        get() = AppModule.provideUserPreferencesRepository(getApplication())
    
    private val _startupState = MutableStateFlow<StartupState>(StartupState.Ready(userPreferencesRepository.isLoggedFast()))
    val startupState: StateFlow<StartupState> = _startupState.asStateFlow()

    private val _isEmailVerified = MutableStateFlow(true)
    val isEmailVerified = _isEmailVerified.asStateFlow()

    val userPreferences = userPreferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),        com.keeftalk.chat.data.prefs.UserPreferences(
            userId = userPreferencesRepository.getUserIdFast() ?: "",
            themeMode = userPreferencesRepository.getThemeFast(),
            fontSize = userPreferencesRepository.getFontSizeFast(),
            uiScale = userPreferencesRepository.getUiScaleFast(),
            extraNightModeEnabled = userPreferencesRepository.isExtraNightModeEnabledFast(),
            nightModeOpacity = userPreferencesRepository.getNightModeOpacityFast(),
            isDynamicColorEnabled = userPreferencesRepository.isDynamicColorFast(),
            isLogged = userPreferencesRepository.isLoggedFast(),
            biometricLockEnabled = userPreferencesRepository.isAppLockEnabledFast(),
            biometricTimeout = userPreferencesRepository.getAppLockTimeoutFast()
        ))

    private val _isHydrated = MutableStateFlow(false)
    val isHydrated: StateFlow<Boolean> = _isHydrated.asStateFlow()

    // Use AppModule directly to ensure they are always available (singleton handled by AppModule)
    val authRepository: AuthRepository
        get() = AppModule.provideAuthRepository(getApplication())

    val chatRepository: ChatRepository
        get() = AppModule.provideChatRepository(getApplication())

    val securityRepository: com.keeftalk.chat.domain.repository.SecurityRepository
        get() = AppModule.provideSecurityRepository(getApplication())

    val chatSettingsRepository: com.keeftalk.chat.domain.repository.ChatSettingsRepository
        get() = AppModule.provideChatSettingsRepository(getApplication())

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // FAST PATH: Mirror the singleton cache for instant rendering
    val fastChats: StateFlow<List<com.keeftalk.chat.domain.model.ChatListItemUiModel>> = com.keeftalk.chat.util.FastPathInbox.chats

    init {
        initialize()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            chatRepository.getAppNotifications().collect {
                _notifications.value = it
            }
        }
        viewModelScope.launch {
            chatRepository.getUnreadNotificationCount().collect {
                _unreadCount.value = it
            }
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            chatRepository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            chatRepository.markAllNotificationsAsRead()
        }
    }

    private val _navigationEvent = MutableSharedFlow<NotificationNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val _isFabMenuOpen = MutableStateFlow(false)
    val isFabMenuOpen = _isFabMenuOpen.asStateFlow()

    fun setFabMenuOpen(open: Boolean) {
        _isFabMenuOpen.value = open
    }

    fun toggleExtraNightMode() {
        viewModelScope.launch {
            val current = userPreferences.value.extraNightModeEnabled
            userPreferencesRepository.updateChatSettings(extraNightMode = !current)
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val current = userPreferences.value.themeMode
            val next = if (current == "DARK") "LIGHT" else "DARK"
            userPreferencesRepository.updateThemeMode(next)
        }
    }

    fun increaseUiScale() {
        viewModelScope.launch {
            val current = userPreferences.value.uiScale
            if (current < 1.30f) {
                val next = (current + 0.05f).coerceAtMost(1.30f)
                userPreferencesRepository.updateChatSettings(uiScale = next)
            }
        }
    }

    fun decreaseUiScale() {
        viewModelScope.launch {
            val current = userPreferences.value.uiScale
            if (current > 0.80f) {
                val next = (current - 0.05f).coerceAtMost(1.30f).coerceAtLeast(0.80f)
                userPreferencesRepository.updateChatSettings(uiScale = next)
            }
        }
    }

    fun resetUiScale() {
        viewModelScope.launch {
            userPreferencesRepository.updateChatSettings(uiScale = 1.0f)
        }
    }

    fun updateNightModeOpacity(opacity: Float) {
        viewModelScope.launch {
            userPreferencesRepository.updateChatSettings(nightModeOpacity = opacity)
        }
    }

    private fun observeBillingEvents() {
        val billingManager = AppModule.provideBillingManager(getApplication())
        viewModelScope.launch {
            billingManager.purchaseEvents.collect { result ->
                when (result) {
                    is com.keeftalk.chat.data.billing.PurchaseResult.Success -> {
                        authRepository.verifySubscriptionPurchase(
                            result.purchase.purchaseToken,
                            result.purchase.products.firstOrNull() ?: ""
                        )
                    }
                    is com.keeftalk.chat.data.billing.PurchaseResult.Error -> {
                        _navigationEvent.emit(NotificationNavigationEvent.ShowSystemDialog(
                            title = "Purchase Failed",
                            message = result.message
                        ))
                    }
                    else -> {}
                }
            }
        }
    }

    fun handleNotificationClick(notification: AppNotification) {
        viewModelScope.launch {
            chatRepository.markNotificationAsRead(notification.id)
            
            when (notification.type) {
                NotificationType.MESSAGE -> {
                    notification.data?.chatId?.let { 
                        _navigationEvent.emit(NotificationNavigationEvent.NavigateToChat(it))
                    }
                }
                NotificationType.CALL -> {
                    _navigationEvent.emit(NotificationNavigationEvent.NavigateToCalls)
                }
                NotificationType.NOTE_SHARE -> {
                    notification.data?.noteId?.let {
                        _navigationEvent.emit(NotificationNavigationEvent.NavigateToNote(it))
                    }
                }
                NotificationType.SYSTEM,
                NotificationType.PROFILE_VIEW,
                NotificationType.CONTACT_JOINED,
                NotificationType.BROADCAST,
                NotificationType.REMINDER,
                NotificationType.NUDGE -> {
                    _navigationEvent.emit(NotificationNavigationEvent.ShowSystemDialog(
                        title = notification.title,
                        message = notification.data?.fullMessage ?: notification.message,
                        type = notification.type,
                        sourceId = notification.sourceId
                    ))
                }
            }
        }
    }

    fun addTestNotification(type: NotificationType) {
        viewModelScope.launch {
            val notification = when (type) {
                NotificationType.MESSAGE -> AppNotification(
                    id = java.util.UUID.randomUUID().toString(),
                    type = type,
                    title = "New Message",
                    message = "Ahmed: Hey, are you available?",
                    timestamp = System.currentTimeMillis(),
                    data = NotificationData(chatId = "demo-chat-id")
                )
                NotificationType.CALL -> AppNotification(
                    id = java.util.UUID.randomUUID().toString(),
                    type = type,
                    title = "Missed Call",
                    message = "Sarah called you",
                    timestamp = System.currentTimeMillis() - 600000,
                    data = NotificationData(contactName = "Sarah")
                )
                NotificationType.SYSTEM -> AppNotification(
                    id = java.util.UUID.randomUUID().toString(),
                    type = type,
                    title = "AI Notification",
                    message = "AI sent you a compliment!",
                    timestamp = System.currentTimeMillis(),
                    data = NotificationData(fullMessage = "You are one of our most active users. Keep going! 🌟")
                )
                else -> AppNotification(
                    id = java.util.UUID.randomUUID().toString(),
                    type = type,
                    title = "Notification: ${type.name}",
                    message = "This is a ${type.name.lowercase().replace('_', ' ')} notification",
                    timestamp = System.currentTimeMillis()
                )
            }
            chatRepository.insertNotification(notification)
            
            // Also show system notification for "Generate a local notification"
            showLocalTestNotification(notification.title, notification.message)
        }
    }

    private fun showLocalTestNotification(title: String, message: String) {
        val context = getApplication<Application>()
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "test_notifications"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Test Notifications", android.app.NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        
        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.keeftalk.chat.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            
        notificationManager.notify(999, builder.build())
    }


    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            // Reset internal state
            _isHydrated.value = false
            _startupState.value = StartupState.Ready(false)
            // The activity will handle final cleanup/restart if needed
        }
    }

    fun onStartupComplete() {
        com.keeftalk.chat.util.PerformanceProfiler.logEvent("MainViewModel.onStartupComplete() called")
        _isHydrated.value = true
    }

    fun refreshLoginStatus() {
        viewModelScope.launch {
            val logged = authRepository.isLogged.first()
            if (logged) {
                authRepository.refreshSession()
            }
            val verified = if (logged) authRepository.isEmailVerified.first() else true
            if (_startupState.value != StartupState.Ready(logged, verified)) {
                _startupState.value = StartupState.Ready(logged, verified)
            }
        }
    }

    private fun initialize() {
        com.keeftalk.chat.util.PerformanceProfiler.logEvent("MainViewModel initialize started")
        
        com.keeftalk.chat.util.StartupOrchestrator.enqueue(com.keeftalk.chat.util.StartupOrchestrator.Tier.TIER_3_POST_RENDER) {
            com.keeftalk.chat.util.PerformanceProfiler.startStage("Deferred Repo Init (Tier 3)")
            
            val authRepo = authRepository
            val chatRepo = chatRepository
            
            // Initialize Repositories in parallel without blocking Tier 3 completion
            viewModelScope.launch(Dispatchers.IO) {
                PerformanceProfiler.logEvent("Tier 3: Starting Session Observation")
                authRepo.startSessionObservation()
                PerformanceProfiler.logEvent("Tier 3: Starting Background Tasks")
                chatRepo.startBackgroundTasks()
                PerformanceProfiler.logEvent("Tier 3: Observing Notifications")
                observeNotifications()
                
                PerformanceProfiler.logEvent("Tier 3: Starting Settings Observation")
                val settingsRepo = AppModule.provideChatSettingsRepository(getApplication())
                settingsRepo.startSettingsObservation()
                
                PerformanceProfiler.logEvent("Tier 3: Starting Customization Observation")
                val customizationRepo = AppModule.provideAppCustomizationRepository(getApplication())
                customizationRepo.startCustomizationObservation()
                
                observeBillingEvents()

                com.keeftalk.chat.util.PerformanceProfiler.endStage("Deferred Repo Init (Tier 3)")
            }

            viewModelScope.launch(Dispatchers.IO) {
                combine(authRepo.isLogged, authRepo.isEmailVerified) { logged, verified ->
                    logged to verified
                }.collect { (logged, verified) ->
                    PerformanceProfiler.logEvent("Tier 3: Auth Status Changed: logged=$logged, verified=$verified")
                    withContext(Dispatchers.Main) {
                        _isEmailVerified.value = verified
                        if (_startupState.value != StartupState.Ready(logged, verified)) {
                            _startupState.value = StartupState.Ready(logged, verified)
                        }
                    }
                    if (logged && verified) {
                        PerformanceProfiler.logEvent("Tier 3: Syncing Profiles (Verified)")
                        chatRepo.syncAllProfiles()
                        AppModule.provideCalendarRepository(getApplication()).syncCalendar()
                    }
                }
            }
        }

        com.keeftalk.chat.util.StartupOrchestrator.enqueue(com.keeftalk.chat.util.StartupOrchestrator.Tier.TIER_3_POST_RENDER) {
            com.keeftalk.chat.util.PerformanceProfiler.logEvent("Tier 3 Tasks Started (Non-Critical)")
            // Defer non-critical repos
            AppModule.provideSecurityRepository(getApplication())
            AppModule.provideVaultRepository(getApplication())
            AppModule.provideCalendarRepository(getApplication())
            AppModule.provideNoteRepository(getApplication())
        }
    }
}
