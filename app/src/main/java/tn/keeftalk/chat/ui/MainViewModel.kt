package tn.keeftalk.chat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import tn.keeftalk.chat.data.prefs.UserPreferencesRepository
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.AppNotification
import tn.keeftalk.chat.domain.model.NotificationData
import tn.keeftalk.chat.domain.model.NotificationType
import tn.keeftalk.chat.domain.repository.AuthRepository
import tn.keeftalk.chat.domain.repository.ChatRepository
import tn.keeftalk.chat.util.PerformanceProfiler
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
    data object NavigateToSms : NotificationNavigationEvent()
    data object NavigateToVault : NotificationNavigationEvent()
    data object NavigateToSecrets : NotificationNavigationEvent()
    data object NavigateToCalendar : NotificationNavigationEvent()
    data object NavigateToWallet : NotificationNavigationEvent()
    data object NavigateToFinances : NotificationNavigationEvent()
    data object NavigateToEmail : NotificationNavigationEvent()
    data object NavigateToFeed : NotificationNavigationEvent()
    data object NavigateToBooks : NotificationNavigationEvent()
    data object NavigateToMarket : NotificationNavigationEvent()
    data object NavigateToHabits : NotificationNavigationEvent()
    data object NavigateToBrowser : NotificationNavigationEvent()
    data class ShowSystemDialog(
        val title: String, 
        val message: String,
        val type: tn.keeftalk.chat.domain.model.NotificationType = tn.keeftalk.chat.domain.model.NotificationType.SYSTEM,
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),        tn.keeftalk.chat.data.prefs.UserPreferences(
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

    val securityManager: tn.keeftalk.chat.security.crypto.SecurityManager
        get() = AppModule.provideSecurityManager(getApplication())

    val securityState = securityManager.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), tn.keeftalk.chat.security.crypto.SecurityState.INITIALIZING)

    val chatRepository: ChatRepository
        get() = AppModule.provideChatRepository(getApplication())

    val securityRepository: tn.keeftalk.chat.domain.repository.SecurityRepository
        get() = AppModule.provideSecurityRepository(getApplication())

    val chatSettingsRepository: tn.keeftalk.chat.domain.repository.ChatSettingsRepository
        get() = AppModule.provideChatSettingsRepository(getApplication())

    val weatherRepository: tn.keeftalk.chat.domain.repository.WeatherRepository
        get() = AppModule.provideWeatherRepository(getApplication())

    val environmentalState: StateFlow<tn.keeftalk.chat.domain.model.EnvironmentalState> by lazy {
        weatherRepository.environmentalState
    }

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // FAST PATH: Mirror the singleton cache for instant rendering
    val fastChats: StateFlow<List<tn.keeftalk.chat.domain.model.ChatListItemUiModel>> = tn.keeftalk.chat.util.FastPathInbox.chats

    val spotlightManager by lazy {
        tn.keeftalk.chat.util.spotlight.FeatureSpotlightManager(
            getApplication(),
            userPreferencesRepository,
            AppModule.provideDatabase(getApplication()).timeTrackingDao()
        )
    }

    val spotlightNotificationEngine by lazy {
        tn.keeftalk.chat.util.spotlight.FeatureSpotlightNotificationEngine(
            getApplication(),
            userPreferencesRepository,
            AppModule.provideDatabase(getApplication()).timeTrackingDao()
        )
    }

    init {
        initialize()
        viewModelScope.launch {
            delay(5000)
            spotlightManager.evaluateSpotlightNudges()
            delay(3000)
            spotlightNotificationEngine.evaluateAndDispatchNotificationNudge()
        }
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

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateThemeMode(themeMode)
            AppModule.provideSettingsRepository(getApplication()).updateChatSettings { it.copy(theme = themeMode) }
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val current = userPreferences.value.themeMode
            val next = if (current == "DARK") "LIGHT" else "DARK"
            updateThemeMode(next)
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
                    is tn.keeftalk.chat.data.billing.PurchaseResult.Success -> {
                        authRepository.verifySubscriptionPurchase(
                            result.purchase.purchaseToken,
                            result.purchase.products.firstOrNull() ?: ""
                        )
                    }
                    is tn.keeftalk.chat.data.billing.PurchaseResult.Error -> {
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
            
            val chatId = notification.data?.chatId
            val noteId = notification.data?.noteId

            when (notification.type) {
                NotificationType.MESSAGE -> {
                    if (!chatId.isNullOrBlank()) {
                        _navigationEvent.emit(NotificationNavigationEvent.NavigateToChat(chatId))
                    } else {
                        _navigationEvent.emit(NotificationNavigationEvent.NavigateToChat(""))
                    }
                }
                NotificationType.CALL -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToCalls)
                NotificationType.NOTE_SHARE -> {
                    if (!noteId.isNullOrBlank()) {
                        _navigationEvent.emit(NotificationNavigationEvent.NavigateToNote(noteId))
                    } else {
                        _navigationEvent.emit(NotificationNavigationEvent.NavigateToNote(""))
                    }
                }
                else -> {
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

    fun handleNotificationIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val feature = intent.getStringExtra("feature") ?: return
        viewModelScope.launch {
            when (feature.uppercase()) {
                "CHATS" -> {
                    val chatId = intent.getStringExtra("chatId")
                    if (!chatId.isNullOrBlank()) {
                        _navigationEvent.emit(NotificationNavigationEvent.NavigateToChat(chatId))
                    }
                }
                "CALLS" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToCalls)
                "SMS_MMS" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToSms)
                "NOTES" -> {
                    val noteId = intent.getStringExtra("noteId")
                    if (!noteId.isNullOrBlank()) {
                        _navigationEvent.emit(NotificationNavigationEvent.NavigateToNote(noteId))
                    }
                }
                "VAULT" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToVault)
                "SECRETS" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToSecrets)
                "CALENDAR" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToCalendar)
                "WALLET" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToWallet)
                "FINANCES" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToFinances)
                "EMAIL" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToEmail)
                "FEED" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToFeed)
                "BOOKS" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToBooks)
                "MARKET" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToMarket)
                "HABITS" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToHabits)
                "BROWSER" -> _navigationEvent.emit(NotificationNavigationEvent.NavigateToBrowser)
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
            .setSmallIcon(tn.keeftalk.chat.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            
        notificationManager.notify(999, builder.build())
    }


    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState

    fun logout() {
        showLogoutConfirmation()
    }

    fun showLogoutConfirmation() {
        _logoutState.value = LogoutState.Confirming
    }

    fun confirmLogout() {
        viewModelScope.launch {
            _logoutState.value = LogoutState.Deleting
            val result = authRepository.logout()
            if (result.isSuccess) {
                _logoutState.value = LogoutState.Idle
                // Reset internal state
                _isHydrated.value = false
                _startupState.value = StartupState.Ready(false)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to wipe cached data and tokens completely."
                _logoutState.value = LogoutState.Error(errorMsg)
            }
        }
    }

    fun dismissLogout() {
        _logoutState.value = LogoutState.Idle
    }

    fun dismissLogoutError() {
        _logoutState.value = LogoutState.Idle
    }

    fun onStartupComplete() {
        tn.keeftalk.chat.util.PerformanceProfiler.logEvent("MainViewModel.onStartupComplete() called")
        _isHydrated.value = true
    }

    fun refreshLoginStatus() {
        viewModelScope.launch {
            val logged = authRepository.isLogged.first()
            if (logged) {
                try {
                    authRepository.refreshSession()
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "refreshSession failed during lifecycle resume: ${e.message}")
                }
            }
            val isStillLogged = authRepository.isLogged.first()
            val verified = if (isStillLogged) {
                try {
                    withTimeoutOrNull(2000L) { authRepository.isEmailVerified.first() } ?: true
                } catch (_: Exception) {
                    true
                }
            } else true
            
            if (_startupState.value != StartupState.Ready(isStillLogged, verified)) {
                _startupState.value = StartupState.Ready(isStillLogged, verified)
            }
        }
    }

    private fun initialize() {
        tn.keeftalk.chat.util.PerformanceProfiler.logEvent("MainViewModel initialize started")
        
        tn.keeftalk.chat.util.StartupOrchestrator.enqueue(tn.keeftalk.chat.util.StartupOrchestrator.Tier.TIER_3_POST_RENDER) {
            tn.keeftalk.chat.util.PerformanceProfiler.startStage("Deferred Repo Init (Tier 3)")
            
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
                try {
                    weatherRepository.refreshWeather()
                } catch (_: Exception) {}

                tn.keeftalk.chat.util.PerformanceProfiler.endStage("Deferred Repo Init (Tier 3)")
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

        tn.keeftalk.chat.util.StartupOrchestrator.enqueue(tn.keeftalk.chat.util.StartupOrchestrator.Tier.TIER_3_POST_RENDER) {
            tn.keeftalk.chat.util.PerformanceProfiler.logEvent("Tier 3 Tasks Started (Non-Critical)")
            // Defer non-critical repos
            AppModule.provideSecurityRepository(getApplication())
            AppModule.provideVaultRepository(getApplication())
            AppModule.provideCalendarRepository(getApplication())
            AppModule.provideNoteRepository(getApplication())
        }
    }
}
