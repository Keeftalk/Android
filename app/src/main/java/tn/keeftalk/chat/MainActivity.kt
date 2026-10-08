package tn.keeftalk.chat

import tn.keeftalk.chat.ui.theme.AppTheme
import tn.keeftalk.chat.ui.theme.keef.KeefThemeBackground
import tn.keeftalk.chat.ui.theme.keef.KeefHomeView
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import tn.keeftalk.chat.util.BiometricAuthManager
import tn.keeftalk.chat.ui.screens.chatdetail.AttachmentType
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Trace
import java.util.Locale
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.edit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.ui.components.KeeftalkAvatar
import tn.keeftalk.chat.util.AvatarUtils
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.messaging.FirebaseMessaging
import tn.keeftalk.chat.domain.repository.AuthRepository
import tn.keeftalk.chat.domain.repository.ChatRepository
import tn.keeftalk.chat.ui.StartupState
import tn.keeftalk.chat.ui.auth.AuthScreen
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.ui.auth.AuthViewModel
import tn.keeftalk.chat.ui.discovery.DiscoveryScreen
import tn.keeftalk.chat.ui.discovery.DiscoveryViewModel
import tn.keeftalk.chat.ui.profile.ProfileScreen
import tn.keeftalk.chat.ui.profile.ProfileViewModel
import tn.keeftalk.chat.ui.profile.ConnectionPathScreen
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import tn.keeftalk.chat.ui.screens.*
import tn.keeftalk.chat.feature.auth.forgotpassword.ui.screens.PasswordResetSuccessScreen
import tn.keeftalk.chat.ui.components.KeeftalkSearchTopBar
import tn.keeftalk.chat.ui.components.KeeftalkTopBar
import tn.keeftalk.chat.ui.components.KeeftalkFeatureTopBar
import tn.keeftalk.chat.ui.components.NotificationsDropdown
import tn.keeftalk.chat.ui.components.LocalNotifications
import tn.keeftalk.chat.ui.components.LocalNotificationActions
import tn.keeftalk.chat.ui.components.NotificationActions
import tn.keeftalk.chat.ui.settings.*
import tn.keeftalk.chat.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.zIndex
import tn.keeftalk.chat.util.ConnectivityObserver
import tn.keeftalk.chat.util.NetworkConnectivityObserver
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu

import androidx.paging.compose.collectAsLazyPagingItems
import tn.keeftalk.chat.feature.email.model.EmailMessage
import tn.keeftalk.chat.domain.model.VaultItem
import tn.keeftalk.chat.domain.model.calendar.CalendarItem

import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import tn.keeftalk.chat.util.profileLayout
import tn.keeftalk.chat.util.PerformanceProfiler
import tn.keeftalk.chat.util.FrameProfiler
import tn.keeftalk.chat.util.StartupOrchestrator
import tn.keeftalk.chat.ui.vault.VaultScreen
import tn.keeftalk.chat.ui.vault.VaultViewModel
import tn.keeftalk.chat.feature.secrets.ui.SecretsScreen
import tn.keeftalk.chat.feature.secrets.ui.SecretsSettingsScreen
import tn.keeftalk.chat.feature.secrets.viewmodel.SecretsViewModel
import tn.keeftalk.chat.ui.settings.AutomaticBackupSettingsScreen
import tn.keeftalk.chat.ui.screens.calendar.CalendarScreen
import tn.keeftalk.chat.ui.screens.calendar.CalendarViewModel
import tn.keeftalk.chat.ui.screens.calendar.CalendarEditorScreen
import tn.keeftalk.chat.feature.wallet.ui.screens.WalletScreen
import tn.keeftalk.chat.feature.wallet.viewmodel.WalletViewModel
import tn.keeftalk.chat.feature.finances.ui.screens.FinancesScreen
import tn.keeftalk.chat.feature.finances.ui.FinancesSettingsScreen
import tn.keeftalk.chat.feature.finances.viewmodel.FinancesViewModel
import tn.keeftalk.chat.feature.email.ui.screens.EmailScreen
import tn.keeftalk.chat.feature.email.ui.screens.ComposeEmailScreen
import tn.keeftalk.chat.feature.email.viewmodel.EmailViewModel
import tn.keeftalk.chat.feature.auth.forgotpassword.ui.screens.ForgotPasswordScreen
import tn.keeftalk.chat.feature.auth.forgotpassword.ui.screens.CreateNewPasswordScreen
import tn.keeftalk.chat.feature.auth.forgotpassword.viewmodel.ForgotPasswordViewModel
import tn.keeftalk.chat.domain.model.calendar.CalendarItemType
import tn.keeftalk.chat.domain.model.*
import kotlinx.serialization.json.Json
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

private const val TAG = "KEEFTALK_NAV"

class MainActivity : FragmentActivity(), ChatListFragment.OnChatListReadyListener {
    private var isDataReady by mutableStateOf(false)
    private val pendingChatId = mutableStateOf<String?>(null)

    override fun onChatListReady() {
        if (!isDataReady) {
            PerformanceProfiler.logEvent("MainActivity: onChatListReady reached", category = PerformanceProfiler.Category.UI)
            isDataReady = true
        }
    }

    override fun onStop() {
        super.onStop()
        getSharedPreferences("keeftalk_prefs", MODE_PRIVATE)
            .edit { putBoolean("is_app_in_foreground", false) }
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences("keeftalk_prefs", MODE_PRIVATE)
            .edit { putBoolean("is_app_in_foreground", true) }
        
        // Refresh auth status on resume to catch background verification
        try {
            val mainViewModel = androidx.lifecycle.ViewModelProvider(this)[tn.keeftalk.chat.ui.MainViewModel::class.java]
            mainViewModel.refreshLoginStatus()
        } catch (e: Exception) {
            // ViewModel not ready yet or another error
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "onNewIntent received: ${intent.action}")

        // Handle Dropbox Auth Result
        val dropboxCredential = com.dropbox.core.android.Auth.getDbxCredential()
        if (dropboxCredential != null) {
            Log.d(TAG, "Dropbox authentication successful")
            AppModule.cloudImportResultFlow.tryEmit(listOf("DROPBOX_AUTH_SUCCESS"))
        }

        intent.getStringExtra("chatId")?.let {
            pendingChatId.value = it
        }
        StartupOrchestrator.enqueue(StartupOrchestrator.Tier.TIER_3_POST_RENDER) {
            AppModule.provideAuthRepository(this@MainActivity).handleDeepLink(intent)
        }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Trace.beginSection("MainActivity.onCreate")
        PerformanceProfiler.startStage("MainActivity.onCreate()")

        PerformanceProfiler.startStage("SplashScreen Installation")
        val splashScreen = installSplashScreen()
        PerformanceProfiler.endStage("SplashScreen Installation", category = PerformanceProfiler.Category.ANDROID)

        super.onCreate(savedInstanceState)
        FrameProfiler.attachToWindow(window)

        PerformanceProfiler.startStage("EdgeToEdge Enabling")
        enableEdgeToEdge()
        PerformanceProfiler.endStage("EdgeToEdge Enabling", category = PerformanceProfiler.Category.ANDROID)

        Trace.beginSection("MainActivity.setContentView")
        PerformanceProfiler.startStage("UI Layout Inflation")
        setContentView(R.layout.activity_main)
        PerformanceProfiler.endStage("UI Layout Inflation", category = PerformanceProfiler.Category.UI)
        Trace.endSection()

        val composeOverlay = findViewById<androidx.compose.ui.platform.ComposeView>(R.id.compose_overlay)
        val nativeShell = findViewById<android.view.ViewGroup>(R.id.native_fragment_shell)

        // Removed blocking OnPreDrawListener to allow instant first frame draw with placeholder

        val prefsRepo = AppModule.provideUserPreferencesRepository(this)
        splashScreen.setKeepOnScreenCondition { !isDataReady }

        // Failsafe: Ensure splash screen is dismissed after a timeout
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            {
                if (!isDataReady) {
                    Log.w(TAG, "Failsafe: Forcing splash screen dismissal after timeout")
                    onChatListReady()
                }
            },
            3000 
        )

        PerformanceProfiler.logEvent("MainActivity: attachToWindow deferred", category = PerformanceProfiler.Category.UI)

        // FAST PATH: Initial Fragment Transaction
        val isLogged = prefsRepo.isLoggedFast()
        val isLocked = prefsRepo.isAppLockEnabledFast()

        if (isLogged && !isLocked) {
            Trace.beginSection("MainActivity.nativeShellInit")
            PerformanceProfiler.startStage("Native Shell Fragment Init")
            val fragment = ChatListFragment()
            fragment.setOnChatClickListener { chatId ->
                pendingChatId.value = chatId
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.native_fragment_shell, fragment, "chat_list")
                .commit()
            PerformanceProfiler.endStage("Native Shell Fragment Init", category = PerformanceProfiler.Category.UI)
            Trace.endSection()
        } else {
            nativeShell.visibility = android.view.View.GONE
        }

        PerformanceProfiler.startStage("Fast Path Sync Prefs Read")
        val syncTheme = prefsRepo.getThemeFast()
        
        // Schedule Email Sync
        AppModule.provideEmailRepository(this).scheduleBackgroundSync()

        val syncFontSize = prefsRepo.getFontSizeFast()
        val syncFontScale = prefsRepo.getFontScaleFast()
        val syncIsBold = prefsRepo.isBoldFast()
        val syncDynamicColor = prefsRepo.isDynamicColorFast()
        PerformanceProfiler.endStage("Fast Path Sync Prefs Read", category = PerformanceProfiler.Category.STORAGE)

        PerformanceProfiler.startStage("Compose content set")
        Trace.beginSection("MainActivity.setContent")
        composeOverlay.setContent {
            val mainViewModel: tn.keeftalk.chat.ui.MainViewModel = viewModel()
            val fastChats by mainViewModel.fastChats.collectAsState()

            // Trigger Tier 2 after first composition to avoid stalling the very first frame
            LaunchedEffect(Unit) {
                // Ready to render for performance tracking
                StartupOrchestrator.onCriticalRenderEventStart()

                // Defer Tier 2 launch to post-draw
                android.view.Choreographer.getInstance().postFrameCallback {
                    StartupOrchestrator.startTier2()
                }
            }

            // Hydration and state readiness logic
            LaunchedEffect(fastChats) {
                if (fastChats.isNotEmpty() || !isLogged) {
                    onChatListReady()
                }
            }

            val userPrefs by mainViewModel.userPreferences.collectAsState()
            val environmentalState by mainViewModel.environmentalState.collectAsState()

            val appTheme = remember(userPrefs.themeMode, userPrefs.appCustomization.isMinimalMode) {
                if (userPrefs.appCustomization.isMinimalMode) {
                    AppTheme.SYSTEM
                } else {
                    when (userPrefs.themeMode) {
                        "LIGHT" -> AppTheme.LIGHT
                        "DARK" -> AppTheme.DARK
                        "KEEF" -> AppTheme.KEEF
                        else -> AppTheme.SYSTEM
                    }
                }
            }

            KeeftalkTheme(
                appTheme = appTheme,
                environmentalState = environmentalState,
                dynamicColor = syncDynamicColor,
                fontSize = syncFontSize.toInt(),
                fontScale = syncFontScale,
                uiScale = userPrefs.uiScale,
                isBold = syncIsBold,
                cardBlurPercent = userPrefs.keefCardBlurPercent,
                buttonBlurPercent = userPrefs.keefButtonBlurPercent,
                surfaceBlurPercent = userPrefs.keefSurfaceBlurPercent,
                cardOpacityPercent = userPrefs.keefCardOpacityPercent,
                buttonOpacityPercent = userPrefs.keefButtonOpacityPercent,
                surfaceOpacityPercent = userPrefs.keefSurfaceOpacityPercent,
                borderOpacityPercent = userPrefs.keefBorderOpacityPercent,
                cornerRadiusDp = userPrefs.keefCornerRadiusDp,
                dialogBlurPercent = userPrefs.keefDialogBlurPercent,
                dialogOpacityPercent = userPrefs.keefDialogOpacityPercent,
                fabBlurPercent = userPrefs.keefFabBlurPercent,
                fabOpacityPercent = userPrefs.keefFabOpacityPercent,
                containerBlurPercent = userPrefs.keefContainerBlurPercent,
                containerOpacityPercent = userPrefs.keefContainerOpacityPercent,
                isSkeleton = !isDataReady
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (appTheme == AppTheme.KEEF) {
                        KeefThemeBackground(
                            environmentalState = environmentalState
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (appTheme == AppTheme.KEEF || isDataReady) Color.Transparent else MaterialTheme.colorScheme.background
                    ) {
                        if (!isDataReady) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            val isHydrated by mainViewModel.isHydrated.collectAsState()

                            FullAppContent(
                                mainViewModel = mainViewModel,
                                userPrefs = userPrefs,
                                isHydrated = isHydrated,
                                pendingChatId = pendingChatId,
                                intent = intent,
                                onReady = { onChatListReady() }
                            )
                        }
                    }
                }

                LaunchedEffect(isDataReady) {
                    if (isDataReady) {
                        mainViewModel.onStartupComplete()
                        StartupOrchestrator.onCriticalRenderEventEnd()
                    }
                }
            }
        }
        PerformanceProfiler.endStage("Compose content set", category = PerformanceProfiler.Category.COMPOSE)
        PerformanceProfiler.endStage("MainActivity.onCreate()", category = PerformanceProfiler.Category.UI)
        Trace.endSection() 
        Trace.endSection() 
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FullAppContent(
    mainViewModel: tn.keeftalk.chat.ui.MainViewModel,
    userPrefs: tn.keeftalk.chat.data.prefs.UserPreferences,
    isHydrated: Boolean,
    pendingChatId: MutableState<String?>,
    intent: android.content.Intent,
    onReady: () -> Unit
) {
    val context = LocalContext.current
    val startupState by mainViewModel.startupState.collectAsState()
    var currentScreen by androidx.compose.runtime.saveable.rememberSaveable { 
        mutableStateOf<AppScreen>(AppScreen.ChatList) 
    }
    val screenBackStack = remember { mutableStateListOf<AppScreen>() }
    val onScreenChange: (AppScreen) -> Unit = { target ->
        if (target != currentScreen) {
            screenBackStack.add(currentScreen)
            currentScreen = target
        }
    }
    val onNavigateBack: () -> Unit = {
        if (screenBackStack.isNotEmpty()) {
            currentScreen = screenBackStack.removeAt(screenBackStack.lastIndex)
        } else {
            currentScreen = AppScreen.ChatList
        }
    }
    var showAttachmentsForChatId by androidx.compose.runtime.saveable.rememberSaveable { 
        mutableStateOf<String?>(null) 
    }

    val timeTracker = remember { tn.keeftalk.chat.data.repository.TimeTrackingManager.getInstance(context) }
    LaunchedEffect(currentScreen) {
        val featureKey = when (currentScreen) {
            is AppScreen.ChatList, is AppScreen.ArchivedChats -> "CHATS"
            is AppScreen.Notes, is AppScreen.NoteEditor -> "NOTES"
            is AppScreen.Vault -> "VAULT"
            is AppScreen.Secrets -> "SECRETS"
            is AppScreen.Email, is AppScreen.ComposeEmail, is AppScreen.EmailDetail -> "EMAIL"
            is AppScreen.Calendar, is AppScreen.CalendarEditor -> "CALENDAR"
            is AppScreen.Wallet -> "WALLET"
            is AppScreen.Finances, is AppScreen.FinancesSettings -> "FINANCES"
            is AppScreen.Feed -> "FEED"
            is AppScreen.Books -> "BOOKS"
            is AppScreen.Market -> "MARKET"
            is AppScreen.Habits -> "HABITS"
            is AppScreen.Todo -> "TODO"
            is AppScreen.Todo -> "TODO"
            is AppScreen.Browser -> "BROWSER"
            else -> "SETTINGS"
        }
        timeTracker.setCurrentFeature(featureKey)
    }
    
    when (val state = startupState) {
        is StartupState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is StartupState.Ready -> {
            LaunchedEffect(intent) {
                if (intent.action == android.content.Intent.ACTION_VIEW) {
                    val data = intent.data
                    if (data?.scheme == "keeftalk" && data.host == "reset-password") {
                        onScreenChange(AppScreen.CreateNewPassword)
                        intent.action = null
                    } else if (data?.scheme == "keeftalk" && data.host == "confirm-signup") {
                        // Supabase handleDeeplinks will update the session,
                        // and StartupState.Ready will eventually reflect the new isLogged status.
                        intent.action = null
                    }
                }
            }

            if (!state.isLogged) {
                val currentContext = LocalContext.current
                
                when (currentScreen) {
                    is AppScreen.ForgotPassword -> {
                        val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel { ForgotPasswordViewModel(mainViewModel.authRepository) }
                        ForgotPasswordScreen(viewModel = forgotPasswordViewModel, onBack = { onScreenChange(AppScreen.ChatList) })
                    }
                    is AppScreen.CreateNewPassword -> {
                        val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel { ForgotPasswordViewModel(mainViewModel.authRepository) }
                        CreateNewPasswordScreen(viewModel = forgotPasswordViewModel, onSuccess = { onScreenChange(AppScreen.ResetSuccess) })
                    }
                    is AppScreen.ResetSuccess -> {
                        PasswordResetSuccessScreen(onSignInNow = { onScreenChange(AppScreen.ChatList) })
                    }
                    else -> {
                        AuthScreen(
                            viewModel = viewModel {
                                AuthViewModel(
                                    mainViewModel.authRepository,
                                    AppModule.provideCountryService(currentContext),
                                    AppModule.providePhoneNumberService(currentContext),
                                    AppModule.provideTwoFactorRepository(currentContext)
                                ).apply {
                                    if (state.isLogged && !state.isEmailVerified) {
                                        setFormType(tn.keeftalk.chat.ui.auth.AuthFormType.VERIFY_EMAIL)
                                    }
                                }
                            },
                            onForgotPassword = { onScreenChange(AppScreen.ForgotPassword) },
                            onAuthSuccess = {
                                mainViewModel.refreshLoginStatus()
                            }
                        )
                    }
                }

                // Notify ready when Auth or ForgotPassword screens are shown to dismiss splash
                LaunchedEffect(Unit) { 
                    StartupOrchestrator.onCriticalRenderEventEnd()
                    onReady() 
                }
            } else {
                // Defer security settings until hydrated
                val securitySettings by produceState<tn.keeftalk.chat.domain.model.UserSecuritySettings?>(initialValue = null, key1 = isHydrated) {
                    if (isHydrated) {
                        mainViewModel.securityRepository.securitySettings.collect { value = it }
                    }
                }

                val chatSettingsState = produceState(initialValue = tn.keeftalk.chat.domain.model.UserChatSettings(userPrefs.userId)) {
                    mainViewModel.chatSettingsRepository.chatSettings.collect { value = it }
                }
                val chatSettings = chatSettingsState.value
                
                val notifications by mainViewModel.notifications.collectAsState()

                var isAppUnlocked by remember { 
                    mutableStateOf(!mainViewModel.userPreferencesRepository.isAppLockEnabledFast()) 
                }

                if ((!isAppUnlocked) && (securitySettings?.appLockEnabled == true)) {
                    AppLockScreen(context as FragmentActivity, securitySettings, onUnlocked = { isAppUnlocked = true })
                    // Also notify ready if lock screen is showing
                    LaunchedEffect(Unit) { 
                        StartupOrchestrator.onCriticalRenderEventEnd()
                        onReady() 
                    }
                } else {
                        val securityState by mainViewModel.securityState.collectAsState()
                        if (securityState == tn.keeftalk.chat.security.crypto.SecurityState.RECOVERY_REQUIRED) {
                            tn.keeftalk.chat.ui.security.SecurityRecoveryDialog(
                                onDismiss = { /* Stay in restricted state */ }
                            )
                        }

                        MainScaffold(
                            mainViewModel = mainViewModel,
                            userPrefs = userPrefs,
                            chatSettings = chatSettings,
                            isHydrated = isHydrated,
                            currentScreen = currentScreen,
                            showAttachmentsForChatId = showAttachmentsForChatId,
                            onShowAttachmentsChange = { showAttachmentsForChatId = it },
                            notifications = notifications,
                            onScreenChange = onScreenChange,
                            onNavigateBack = onNavigateBack,
                            canNavigateBackScreen = screenBackStack.isNotEmpty(),
                            pendingChatId = pendingChatId,
                            intent = intent,
                            onReady = onReady
                        )
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(
    activity: FragmentActivity,
    securitySettings: tn.keeftalk.chat.domain.model.UserSecuritySettings?,
    onUnlocked: () -> Unit
) {
    val biometricManager = remember { BiometricAuthManager(activity) }
    LaunchedEffect(Unit) {
        biometricManager.showBiometricPrompt(
            activity = activity,
            title = "Keeftalk Locked",
            subtitle = "Authenticate to continue",
            allowBiometrics = securitySettings?.biometricUnlockEnabled ?: true,
            onSuccess = { onUnlocked() },
            onError = { Log.e("AppLock", it) },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Keeftalk is locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    biometricManager.showBiometricPrompt(
                        activity = activity,
                        allowBiometrics = securitySettings?.biometricUnlockEnabled ?: true,
                        onSuccess = { onUnlocked() },
                        onError = {}
                    )
                }
            ) {
                Text("Unlock")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    mainViewModel: tn.keeftalk.chat.ui.MainViewModel,
    userPrefs: tn.keeftalk.chat.data.prefs.UserPreferences,
    chatSettings: tn.keeftalk.chat.domain.model.UserChatSettings,
    isHydrated: Boolean,
    currentScreen: AppScreen,
    showAttachmentsForChatId: String?,
    onShowAttachmentsChange: (String?) -> Unit,
    notifications: List<tn.keeftalk.chat.domain.model.AppNotification>,
    onScreenChange: (AppScreen) -> Unit,
    onNavigateBack: () -> Unit = {},
    canNavigateBackScreen: Boolean = false,
    pendingChatId: MutableState<String?>,
    intent: android.content.Intent,
    onReady: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
        mainViewModel.weatherRepository.refreshWeather()
    }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            mainViewModel.weatherRepository.refreshWeather()
        }
    }
    
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    var isDetailShown by remember { mutableStateOf(false) }
    
    val customization = userPrefs.appCustomization
    var bottomTab by remember(customization.bottomNavTabs) { 
        mutableStateOf(customization.bottomNavTabs.firstOrNull() ?: tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS) 
    }
    
    var chatTab by remember { mutableIntStateOf(0) }
    val fabActionFlow = remember { MutableSharedFlow<tn.keeftalk.chat.ui.components.FabActionType>(extraBufferCapacity = 1) }
    var showFabSearchDialog by remember { mutableStateOf(false) }
    var fabSearchQuery by remember { mutableStateOf("") }

    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val networkStatus by connectivityObserver.observe().collectAsState(initial = ConnectivityObserver.Status.Available)
    var isOfflineFlash by remember { mutableStateOf(false) }
    var connectionRestoredFlash by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(networkStatus) {
        if (networkStatus == ConnectivityObserver.Status.Unavailable || networkStatus == ConnectivityObserver.Status.Lost) {
            isOfflineFlash = true
            delay(3.seconds)
            isOfflineFlash = false
        } else if (networkStatus == ConnectivityObserver.Status.Available) {
            // Check if we were previously offline or if this is a restoration
            if (isOfflineFlash) {
                isOfflineFlash = false
                connectionRestoredFlash = true
                scope.launch {
                    snackbarHostState.showSnackbar("Connection restored")
                }
                delay(2.seconds)
                connectionRestoredFlash = false
            }
        }
    }

    var selectedContainerId by remember { mutableStateOf("all") }
    var manualRevealContainers by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }
    var selectedChatForMenu by remember { mutableStateOf<tn.keeftalk.chat.domain.model.ChatListItemUiModel?>(null) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val icons = LocalAppIcons.current
    val myId = userPrefs.userId

    // Progressive Data Fetching
    val chatListViewModel: tn.keeftalk.chat.ui.screens.ChatListViewModel? = if (isHydrated) {
        viewModel { tn.keeftalk.chat.ui.screens.ChatListViewModel(mainViewModel.chatRepository) }
    } else null
    
    // Sync Filtering with Native Shell
    LaunchedEffect(selectedContainerId, chatListViewModel) {
        val fragment = (context as FragmentActivity).supportFragmentManager.findFragmentByTag("chat_list") as? ChatListFragment
        fragment?.setFilter(selectedContainerId)
        chatListViewModel?.setContainerId(selectedContainerId)
    }
    
    val fullChatsForBadges = chatListViewModel?.allChatsForBadges?.collectAsState()?.value ?: emptyList()

    val anyBadgeCount = remember(fullChatsForBadges) {
        fullChatsForBadges.any { !it.isArchived && it.unreadCount > 0 }
    }
    val showContainersActual = (anyBadgeCount || manualRevealContainers || selectedContainerId != "all")

    LaunchedEffect(manualRevealContainers, lastInteractionTime) {
        if (manualRevealContainers && selectedContainerId == "all" && !anyBadgeCount) {
            delay(5.seconds)
            manualRevealContainers = false
        }
    }

    val containers = remember(fullChatsForBadges, selectedContainerId, chatTab) {
        listOf(
            ChatContainer("all", "All", if (chatTab == 0) icons.messageCircle else icons.messageSquare),
            ChatContainer("unread", "Unread", icons.email, fullChatsForBadges.count { !it.isArchived && it.unreadCount > 0 }),
            ChatContainer("groups", "Groups", icons.user, fullChatsForBadges.count { !it.isArchived && it.type == tn.keeftalk.chat.domain.model.ChatType.GROUP && it.unreadCount > 0 }),
            ChatContainer("archived", "Archived", icons.archive, fullChatsForBadges.count { it.isArchived && it.unreadCount > 0 }),
            ChatContainer("favorites", "Favorites", icons.heart, fullChatsForBadges.count { !it.isArchived && it.isFavorite })
        )
    }

    LaunchedEffect(intent) {
        mainViewModel.handleNotificationIntent(intent)
        val screen = intent.getStringExtra("screen")

        when (screen) {
            "profile_view_history" -> {
                onScreenChange(AppScreen.ProfileViewHistory)
                intent.removeExtra("screen")
            }
            "security_activity" -> {
                onScreenChange(AppScreen.AccountSettings)
                intent.removeExtra("screen")
            }
            "sms_detail" -> {
                val address = intent.getStringExtra("address") ?: ""
                scope.launch {
                    val threadId = android.provider.Telephony.Threads.getOrCreateThreadId(context, address)
                    onScreenChange(AppScreen.SmsDetail(threadId, address))
                }
                intent.removeExtra("screen")
            }
        }

        if (intent.action == "SHARE_TO_CHAT") {
            val chatId = intent.getStringExtra("chatId")
            @Suppress("DEPRECATION")
            val mediaItems = intent.getParcelableArrayListExtra<tn.keeftalk.chat.domain.model.MediaItem>("mediaItems") ?: emptyList()
            @Suppress("DEPRECATION")
            val docModels = intent.getParcelableArrayListExtra<tn.keeftalk.chat.domain.model.DocumentModel>("documentModels") ?: emptyList()
            
            if (chatId != null) {
                onScreenChange(AppScreen.FileReview(chatId, mediaItems, docModels))
            }
            intent.action = null // Clear to avoid re-triggering
        }
    }

    LaunchedEffect(Unit) {
        fabActionFlow.collectLatest { action ->
            when (action) {
                tn.keeftalk.chat.ui.components.FabActionType.NEW_CHAT -> onScreenChange(AppScreen.Discovery)
                tn.keeftalk.chat.ui.components.FabActionType.NEW_GROUP -> onScreenChange(AppScreen.NewGroup)
                tn.keeftalk.chat.ui.components.FabActionType.AI -> {
                    scope.launch {
                        mainViewModel.chatRepository.getOrCreateOneToOneChat("keeftalk-ai-bot").onSuccess { chatId ->
                            onScreenChange(AppScreen.ChatList)
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, chatId)
                        }
                    }
                }
                tn.keeftalk.chat.ui.components.FabActionType.NEW_SMS -> {
                    onScreenChange(AppScreen.NewSms())
                }
                tn.keeftalk.chat.ui.components.FabActionType.ADD_CONTACT -> {
                    onScreenChange(AppScreen.AddContact)
                }
                tn.keeftalk.chat.ui.components.FabActionType.QR -> {
                    if (cameraPermissionState.status.isGranted) onScreenChange(AppScreen.QrCode)
                    else cameraPermissionState.launchPermissionRequest()
                }
                tn.keeftalk.chat.ui.components.FabActionType.COMPOSE_EMAIL -> onScreenChange(AppScreen.ComposeEmail())
                tn.keeftalk.chat.ui.components.FabActionType.TOGGLE_NIGHT_MODE -> mainViewModel.toggleExtraNightMode()
                tn.keeftalk.chat.ui.components.FabActionType.INCREASE_SCALE -> mainViewModel.increaseUiScale()
                tn.keeftalk.chat.ui.components.FabActionType.DECREASE_SCALE -> mainViewModel.decreaseUiScale()
                tn.keeftalk.chat.ui.components.FabActionType.SEARCH -> {
                    showFabSearchDialog = true
                }
                else -> {}
            }
        }
    }

    var highlightedChatId by remember { mutableStateOf<String?>(null) }
    var systemDialogData by remember { mutableStateOf<tn.keeftalk.chat.ui.NotificationNavigationEvent.ShowSystemDialog?>(null) }

    LaunchedEffect(Unit) {
        mainViewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToChat -> {
                    onScreenChange(AppScreen.ChatList)
                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, event.chatId) }
                    highlightedChatId = event.chatId
                    scope.launch {
                        delay(2.seconds)
                        highlightedChatId = null
                    }
                }
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToCalls -> {
                    onScreenChange(AppScreen.ChatList)
                    bottomTab = tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS
                }
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToSms -> onScreenChange(AppScreen.SmsList)
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToNote -> onScreenChange(AppScreen.Notes(event.noteId))
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToVault -> onScreenChange(AppScreen.Vault)
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToSecrets -> onScreenChange(AppScreen.Secrets)
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToCalendar -> onScreenChange(AppScreen.Calendar)
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToWallet -> onScreenChange(AppScreen.Wallet)
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToFinances -> onScreenChange(AppScreen.Finances)
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToEmail -> onScreenChange(AppScreen.Email)
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToFeed -> onScreenChange(AppScreen.Feed)
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToBooks -> onScreenChange(AppScreen.Books)
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToMarket -> onScreenChange(AppScreen.Market)
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToHabits -> onScreenChange(AppScreen.Habits)
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.NavigateToBrowser -> onScreenChange(AppScreen.BrowserSettings)
                is tn.keeftalk.chat.ui.NotificationNavigationEvent.ShowSystemDialog -> systemDialogData = event
            }
        }
    }


    if (systemDialogData != null) {
        val data = systemDialogData!!
        AlertDialog(
            onDismissRequest = { systemDialogData = null },
            title = { Text(data.title) },
            text = { Text(data.message) },
            confirmButton = {
                if (data.type == tn.keeftalk.chat.domain.model.NotificationType.MESSAGE_REQUEST && data.sourceId != null) {
                    val sourceId = data.sourceId
                    TextButton(onClick = {
                        systemDialogData = null
                        scope.launch {
                            mainViewModel.chatRepository.getOrCreateOneToOneChat(sourceId).onSuccess { chatId ->
                                onScreenChange(AppScreen.ChatList)
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, chatId)
                            }
                        }
                    }) {
                        Text("View Request", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    TextButton(onClick = { systemDialogData = null }) { Text("OK") }
                }
            },
            dismissButton = {
                if (data.type == tn.keeftalk.chat.domain.model.NotificationType.MESSAGE_REQUEST) {
                    TextButton(onClick = { systemDialogData = null }) {
                        Text("Ignore", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        )
    }


    var activeChatIdForMedia by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(myId, isHydrated) {
        if (myId.isNotEmpty() && isHydrated) {
            mainViewModel.chatRepository.getIncomingCalls().collectLatest { incoming ->
                tn.keeftalk.chat.ui.call.IncomingCallActivity.start(context as FragmentActivity, incoming.id)
            }
        }
    }

    BackHandler(enabled = (navigator.canNavigateBack() || canNavigateBackScreen || (currentScreen !is AppScreen.ChatList))) {
        scope.launch {
            if (navigator.canNavigateBack()) {
                navigator.navigateBack()
            } else {
                onNavigateBack()
            }
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { fileUri ->
            try {
                context.contentResolver.takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to take_persistable_uri_permission for $fileUri", e)
            }
            activeChatIdForMedia?.let { chatId ->
                scope.launch {
                    val mimeType = context.contentResolver.getType(fileUri) ?: ""
                    val messageType = when {
                        mimeType.startsWith("image/") -> tn.keeftalk.chat.domain.model.MessageType.IMAGE
                        mimeType.startsWith("video/") -> tn.keeftalk.chat.domain.model.MessageType.VIDEO
                        mimeType.startsWith("audio/") -> tn.keeftalk.chat.domain.model.MessageType.VOICE
                        mimeType == "application/pdf" -> tn.keeftalk.chat.domain.model.MessageType.PDF
                        else -> tn.keeftalk.chat.domain.model.MessageType.FILE
                    }
                    val fileName = fileUri.lastPathSegment ?: "file"
                    mainViewModel.chatRepository.sendMessage(chatId, if (messageType == tn.keeftalk.chat.domain.model.MessageType.FILE) fileName else "", messageType, fileUri.toString())
                }
            }
        }
    }

    val pickContactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        uri?.let { activeChatIdForMedia?.let { chatId -> scope.launch { mainViewModel.chatRepository.sendMessage(chatId, "Contact Shared", tn.keeftalk.chat.domain.model.MessageType.CONTACT, it.toString()) } } }
    }

    val unlockFullAccessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        // On resume, DocumentPickerViewModel will check isExternalStorageManager again
    }

    val pickVisualMediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { fileUri ->
            activeChatIdForMedia?.let { chatId ->
                scope.launch {
                    val mimeType = context.contentResolver.getType(fileUri) ?: ""
                    val messageType = when {
                        mimeType.startsWith("image/") -> tn.keeftalk.chat.domain.model.MessageType.IMAGE
                        mimeType.startsWith("video/") -> tn.keeftalk.chat.domain.model.MessageType.VIDEO
                        else -> tn.keeftalk.chat.domain.model.MessageType.IMAGE 
                    }
                    mainViewModel.chatRepository.sendMessage(chatId, "", messageType, fileUri.toString())
                }
            }
        }
    }

    val googlePhotosAppLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uris = mutableListOf<Uri>()
            result.data?.data?.let { uris.add(it) }
            result.data?.clipData?.let { cd ->
                for (i in 0 until cd.itemCount) {
                    uris.add(cd.getItemAt(i).uri)
                }
            }
            uris.forEach { fileUri ->
                activeChatIdForMedia?.let { chatId ->
                    scope.launch {
                        val mimeType = context.contentResolver.getType(fileUri) ?: ""
                        val messageType = when {
                            mimeType.startsWith("image/") -> tn.keeftalk.chat.domain.model.MessageType.IMAGE
                            mimeType.startsWith("video/") -> tn.keeftalk.chat.domain.model.MessageType.VIDEO
                            else -> tn.keeftalk.chat.domain.model.MessageType.IMAGE
                        }
                        mainViewModel.chatRepository.sendMessage(chatId, "", messageType, fileUri.toString())
                    }
                }
            }
        }
    }

    LaunchedEffect(pendingChatId.value) {
        pendingChatId.value?.let { chatId ->
            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, chatId) }
            pendingChatId.value = null
            intent.removeExtra("chatId")
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                delay(10.seconds)
                @Suppress("DEPRECATION")
                val token = FirebaseMessaging.getInstance().token.await()
                mainViewModel.authRepository.updateFcmToken(token)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e(TAG, "Failed to get FCM token", e)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameMillis { }
        androidx.compose.runtime.withFrameMillis { }
        PerformanceProfiler.logEvent("Stage 1: UI Hydrated", category = PerformanceProfiler.Category.UI)
        StartupOrchestrator.onCriticalRenderEventEnd()
        onReady()
    }

    CompositionLocalProvider(
        LocalNotifications provides notifications,
        LocalNotificationActions provides NotificationActions(
            onNotificationClick = { mainViewModel.handleNotificationClick(it) },
            onMarkAllAsRead = { mainViewModel.markAllNotificationsAsRead() }
        )
    ) {
        val isListVisible = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
        val isDetailVisible = navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (currentScreen is AppScreen.ChatList && !isDetailVisible) Color.Transparent else MaterialTheme.colorScheme.background
        ) {
            val nightDimAlpha = if (userPrefs.extraNightModeEnabled) userPrefs.nightModeOpacity else 0f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        if (nightDimAlpha > 0f) {
                            drawRect(Color.Black.copy(alpha = nightDimAlpha))
                        }
                    }
            ) {
                // isListVisible moved up to Surface for transparency logic
                var isSearching by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }
                var isReaderOpen by remember { mutableStateOf(false) }
                var isBookDetailStatsOpen by remember { mutableStateOf(false) }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        val isMainModule = (currentScreen is AppScreen.Notes && currentScreen.noteId == null) ||
                                          currentScreen is AppScreen.Vault ||
                                          currentScreen is AppScreen.Secrets ||
                                          currentScreen is AppScreen.Email ||
                                          currentScreen is AppScreen.Calendar ||
                                          currentScreen is AppScreen.Wallet ||
                                          currentScreen is AppScreen.Finances ||
                                          currentScreen is AppScreen.Feed ||
                                          currentScreen is AppScreen.Books ||
                                          currentScreen is AppScreen.Market ||
                                          currentScreen is AppScreen.Habits ||
                                          currentScreen is AppScreen.Todo

                        val showGlobalTopBar = if (currentScreen is AppScreen.ChatList || (currentScreen is AppScreen.Books && (isReaderOpen || isBookDetailStatsOpen))) {
                            false // Handled locally or hidden in full screen reader / book detail stats
                        } else {
                            isMainModule
                        }

                        if (showGlobalTopBar) {
                            if (isSearching && isHydrated) {
                                KeeftalkSearchTopBar(
                                    query = searchQuery, 
                                    onQueryChange = { searchQuery = it }, 
                                    onCancel = { isSearching = false; searchQuery = "" }
                                )
                            } else {
                                val title = when (currentScreen) {
                                    is AppScreen.Notes -> "Notes"
                                    is AppScreen.Vault -> ""
                                    is AppScreen.Secrets -> "Secrets"
                                    is AppScreen.SecretsSettings -> "Secrets Settings"
                                    is AppScreen.Email -> "Emails"
                                    is AppScreen.Calendar -> "Calendar"
                                    is AppScreen.Wallet -> "Wallet"
                                    is AppScreen.Finances -> "My Finances"
                                    is AppScreen.Feed -> "Feed"
                                    is AppScreen.Books -> "My Books"
                                    is AppScreen.Market -> "My Market"
                                    is AppScreen.Habits -> "My Habits"
                                    is AppScreen.Todo -> "My Todo"
                                    is AppScreen.Browser -> "My Browser"
                                    else -> ""
                                }
                                
                                KeeftalkFeatureTopBar(
                                    title = title,
                                    onBack = null,
                                    onSearch = { isSearching = true },
                                    onSettings = {
                                        when (currentScreen) {
                                            is AppScreen.Notes -> onScreenChange(AppScreen.NoteSettings)
                                            is AppScreen.Vault -> onScreenChange(AppScreen.VaultSettings)
                                            is AppScreen.Secrets -> onScreenChange(AppScreen.SecretsSettings)
                                            is AppScreen.Email -> onScreenChange(AppScreen.EmailSettings)
                                            is AppScreen.Calendar -> onScreenChange(AppScreen.CalendarSettings)
                                            is AppScreen.Finances -> onScreenChange(AppScreen.FinancesSettings)
                                            is AppScreen.Browser -> onScreenChange(AppScreen.BrowserSettings)
                                            is AppScreen.Feed -> onScreenChange(AppScreen.ManageFeeds)
                                            else -> onScreenChange(AppScreen.Settings)
                                        }
                                    },
                                    notifications = notifications,
                                    onNotificationClick = { mainViewModel.handleNotificationClick(it) },
                                    onMarkAllNotificationsAsRead = { mainViewModel.markAllNotificationsAsRead() },
                                    actions = {
                                        if (currentScreen is AppScreen.Feed) {
                                            IconButton(onClick = { 
                                                // Trigger refresh via some global mechanism or just let PullToRefresh handle it
                                                // For now, let's keep the management icon here
                                                onScreenChange(AppScreen.ManageFeeds)
                                            }) {
                                                Icon(icons.edit, contentDescription = "Manage Feeds")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    },
                    bottomBar = {
                        val activeTabs = if (customization.isEnabled) {
                            customization.bottomNavTabs
                        } else {
                            listOf(
                                tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS,
                                tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS,
                                tn.keeftalk.chat.domain.model.KeeftalkModule.SMS,
                                tn.keeftalk.chat.domain.model.KeeftalkModule.BOOKS,
                                tn.keeftalk.chat.domain.model.KeeftalkModule.MARKET,
                                tn.keeftalk.chat.domain.model.KeeftalkModule.VAULT
                            )
                        }
                        val isBottomBarVisible = isListVisible && activeTabs.size > 1 && !(currentScreen is AppScreen.Books && isReaderOpen) && (
                            currentScreen is AppScreen.ChatList || activeTabs.any { it.toAppScreen()::class == currentScreen::class }
                        ) && !customization.isMinimalMode
                        if (isBottomBarVisible) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                            ) {
                                val itemContent: @Composable (tn.keeftalk.chat.domain.model.KeeftalkModule) -> Unit = { module ->
                                    Box(modifier = Modifier.size(24.dp)) {
                                        BadgedBox(badge = {
                                            if (isHydrated && module == tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS) {
                                                val unreadCount = fullChatsForBadges.asSequence().filter { !it.isArchived }.sumOf { it.unreadCount }
                                                if (unreadCount > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .background(tn.keeftalk.chat.ui.theme.FabGradient, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(unreadCount.toString(), fontSize = 8.sp, color = Color.White)
                                                    }
                                                }
                                            }
                                        }) {
                                            Icon(
                                                module.toIcon(icons, chatTab),
                                                contentDescription = module.name,
                                                modifier = Modifier.fabGradientIcon(),
                                                tint = Color.Unspecified
                                            )
                                        }
                                    }
                                }

                                if (activeTabs.size <= 5) {
                                    activeTabs.forEach { module ->
                                        val isSelected = when (module) {
                                            tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS -> currentScreen is AppScreen.ChatList && bottomTab == module
                                            tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS -> currentScreen is AppScreen.ChatList && bottomTab == module
                                            tn.keeftalk.chat.domain.model.KeeftalkModule.SMS -> currentScreen is AppScreen.ChatList && bottomTab == module
                                            else -> currentScreen::class == module.toAppScreen()::class
                                        }

                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = {
                                                if (module == tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS || 
                                                    module == tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS || 
                                                    module == tn.keeftalk.chat.domain.model.KeeftalkModule.SMS) {
                                                    onScreenChange(AppScreen.ChatList)
                                                    bottomTab = module
                                                } else {
                                                    onScreenChange(module.toAppScreen())
                                                }
                                            },
                                            icon = { itemContent(module) },
                                            label = { Text(module.toLabel()) }
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .horizontalScroll(rememberScrollState())
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        activeTabs.forEach { module ->
                                            val isSelected = when (module) {
                                                tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS -> currentScreen is AppScreen.ChatList && bottomTab == module
                                                tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS -> currentScreen is AppScreen.ChatList && bottomTab == module
                                                tn.keeftalk.chat.domain.model.KeeftalkModule.SMS -> currentScreen is AppScreen.ChatList && bottomTab == module
                                                else -> currentScreen::class == module.toAppScreen()::class
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .width(76.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable {
                                                        if (module == tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS || 
                                                            module == tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS || 
                                                            module == tn.keeftalk.chat.domain.model.KeeftalkModule.SMS) {
                                                            onScreenChange(AppScreen.ChatList)
                                                            bottomTab = module
                                                        } else {
                                                            onScreenChange(module.toAppScreen())
                                                        }
                                                    }
                                                    .padding(vertical = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .height(32.dp)
                                                        .width(56.dp)
                                                        .background(
                                                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                                            shape = RoundedCornerShape(16.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    itemContent(module)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = module.toLabel(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }
                ) { innerPadding ->
                    val topPadding = if (currentScreen is AppScreen.ChatList) 0.dp else innerPadding.calculateTopPadding()
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding)
                    ) {
                        when (currentScreen) {
                            is AppScreen.ChatList -> {
                                if (bottomTab == tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS && isHydrated) {
                                    val callListViewModel: CallListViewModel = viewModel { CallListViewModel(context.applicationContext as android.app.Application, mainViewModel.chatRepository, AppModule.provideCountryService(context), AppModule.providePhoneNumberService(context), AppModule.provideCallLogManager(context), AppModule.providePhoneContactManager(context)) }
                                    CallListScreen(
                                        viewModel = callListViewModel,
                                        modifier = Modifier.fillMaxSize(),
                                        onChatStarted = { chatId -> scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, chatId); bottomTab = tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS } },
                                        onSmsStarted = { phone ->
                                            scope.launch {
                                                val threadId = android.provider.Telephony.Threads.getOrCreateThreadId(context, phone)
                                                onScreenChange(AppScreen.SmsDetail(threadId, phone))
                                            }
                                        },
                                        onContactDetail = { onScreenChange(AppScreen.ContactDetail(it)) },
                                        onMenuAction = { action ->
                                            when (action) {
                                                MenuAction.MY_PROFILE -> onScreenChange(AppScreen.MyProfile)
                                                MenuAction.ABOUT -> onScreenChange(AppScreen.About)
                                                MenuAction.LOGOUT -> mainViewModel.logout()
                                                else -> onScreenChange(AppScreen.Settings)
                                            }
                                        },
                                        showTopBar = true,
                                        isSearching = isSearching,
                                        onIsSearchingChange = { isSearching = it },
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { searchQuery = it },
                                        fabActionFlow = fabActionFlow
                                    )
                                } else {
                                    ListDetailPaneScaffold(
                                        modifier = Modifier.fillMaxSize().profileLayout("ChatListScaffold"),
                                        directive = navigator.scaffoldDirective,
                                        value = navigator.scaffoldValue,
                                        listPane = {
                                            val isSyncing by (chatListViewModel?.isSyncing?.collectAsState() ?: remember { mutableStateOf(false) })
                                            val pullToRefreshState = rememberPullToRefreshState()
                                            
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                if (customization.isMinimalMode) {
                                                    Text(
                                                        text = "Keeftalk",
                                                        style = MaterialTheme.typography.headlineLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 32.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.onBackground,
                                                        modifier = Modifier.statusBarsPadding().padding(start = 24.dp, top = 20.dp, bottom = 8.dp)
                                                    )
                                                } else {
                                                    // MOVE TOP BARS HERE
                                                    if (isSearching && isHydrated) {
                                                        KeeftalkSearchTopBar(
                                                            query = searchQuery,
                                                            onQueryChange = { searchQuery = it },
                                                            onCancel = { isSearching = false; searchQuery = "" }
                                                        )
                                                    } else {
                                                        val title = when (bottomTab) {
                                                            tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS -> "Calls"
                                                            tn.keeftalk.chat.domain.model.KeeftalkModule.SMS -> "SMS"
                                                            else -> "Chats"
                                                        }
                                                        KeeftalkFeatureTopBar(
                                                            title = title,
                                                            onBack = null,
                                                            onSearch = { isSearching = true },
                                                            onSettings = {
                                                                when (bottomTab) {
                                                                    tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS -> onScreenChange(AppScreen.CallSettings)
                                                                    else -> onScreenChange(AppScreen.ChatSettings)
                                                                }
                                                            },
                                                            notifications = notifications,
                                                            onNotificationClick = { mainViewModel.handleNotificationClick(it) },
                                                            onMarkAllNotificationsAsRead = { mainViewModel.markAllNotificationsAsRead() }
                                                        )
                                                    }

                                                    if (bottomTab == tn.keeftalk.chat.domain.model.KeeftalkModule.SMS) {
                                                        // SMS SECTION
                                                        val smsViewModel: tn.keeftalk.chat.ui.screens.SmsViewModel = viewModel {
                                                            tn.keeftalk.chat.ui.screens.SmsViewModel(
                                                                context.applicationContext as android.app.Application,
                                                                tn.keeftalk.chat.di.AppModule.provideSmsRepository(context),
                                                                tn.keeftalk.chat.di.AppModule.provideChatRepository(context)
                                                            )
                                                        }
                                                        tn.keeftalk.chat.ui.screens.SmsListScreen(
                                                            viewModel = smsViewModel,
                                                            onConversationClick = { threadId, address ->
                                                                onScreenChange(AppScreen.SmsDetail(threadId, address))
                                                            },
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    } else {
                                                        if (!isSearching && showContainersActual) {
                                                            ChatContainersRow(
                                                                containers = containers,
                                                                selectedId = selectedContainerId,
                                                                onContainerClick = {
                                                                    selectedContainerId = it
                                                                    lastInteractionTime = System.currentTimeMillis()
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                if (bottomTab != tn.keeftalk.chat.domain.model.KeeftalkModule.SMS || customization.isMinimalMode) {
                                                    PullToRefreshBox(
                                                        isRefreshing = isSyncing,
                                                        onRefresh = {
                                                            if (!showContainersActual) {
                                                                manualRevealContainers = true
                                                                lastInteractionTime = System.currentTimeMillis()
                                                            } else {
                                                                chatListViewModel?.refresh()
                                                            }
                                                        },
                                                        state = pullToRefreshState,
                                                        modifier = Modifier.weight(1f).clip(androidx.compose.ui.graphics.RectangleShape)
                                                    ) {
                                                        if (isHydrated && chatListViewModel != null) {
                                                            ChatListRecyclerView(
                                                                chatsPager = chatListViewModel.activeChatsPager,
                                                                typingStatuses = chatListViewModel.typingStatuses,
                                                                currentUserId = myId,
                                                                onChatClick = { chatId ->
                                                                    pendingChatId.value = chatId
                                                                },
                                                                onChatLongClick = { chat ->
                                                                    selectedChatForMenu = chat
                                                                }
                                                            )
                                                        } else {
                                                            Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        detailPane = {
                                                    val content = navigator.currentDestination?.contentKey
                                                    if (content != null) {
                                                        val detailViewModel: tn.keeftalk.chat.ui.screens.ChatDetailViewModel = viewModel(key = content) { tn.keeftalk.chat.ui.screens.ChatDetailViewModel(context.applicationContext as android.app.Application, mainViewModel.chatRepository, tn.keeftalk.chat.di.AppModule.provideMediaRepository(context.applicationContext), tn.keeftalk.chat.di.AppModule.provideUserPreferencesRepository(context.applicationContext), content) }
                                                        DisposableEffect(content) {
                                                            val sp = context.getSharedPreferences("keeftalk_prefs", Context.MODE_PRIVATE)
                                                            sp.edit { putString("current_open_chat_id", content) }
                                                            onDispose { sp.edit { remove("current_open_chat_id") } }
                                                        }
                                                        val chat by detailViewModel.chat.collectAsState()
                                                        val messagesPagingData = detailViewModel.messagesPagingData
                                                        val typingUsers by detailViewModel.typingUsers.collectAsState()
                                                        val reactionProfiles by detailViewModel.userProfiles.collectAsState()
                                                        val peerUser by remember(chat?.peerId) { if (chat?.peerId != null) mainViewModel.chatRepository.getContact(chat!!.peerId!!) else flowOf(null) }.collectAsState(initial = null)
                                                        val detailUserPrefs by detailViewModel.userPreferences.collectAsState()
                                                        val currentChatTheme = remember(chat?.themeId) { tn.keeftalk.chat.ui.theme.ChatThemes.getById(chat?.themeId) }
                                                        val recordingState by detailViewModel.recordingState.collectAsState()
                                                        val recordingDuration by detailViewModel.recordingDuration.collectAsState()
                                                        val amplitudeHistory by detailViewModel.amplitudeHistory.collectAsState()
                                                        val transferProgress by detailViewModel.transferProgress.collectAsState()
                                                        val failedMessage by detailViewModel.failedMessage.collectAsState()
                                                        val expandedMessageId by detailViewModel.expandedMessageId.collectAsState()
                                                        
                                                        val albums by detailViewModel.availableAlbums.collectAsState()
                                                        val selectedAlbum by detailViewModel.selectedAlbum.collectAsState()
                                                        val selectedMediaList by detailViewModel.selectedMediaList.collectAsState()
                                                        val mediaQualityMap by detailViewModel.mediaQualityMap.collectAsState()
                                                        val readReceiptsMap by detailViewModel.readReceipts.collectAsState()

                                                        CompositionLocalProvider(tn.keeftalk.chat.ui.theme.LocalChatTheme provides currentChatTheme) {
                                                            var replyingToMessage by remember { mutableStateOf<tn.keeftalk.chat.domain.model.Message?>(null) }
                                                            ChatDetailContent(
                                                                chatName = chat?.name,
                                                                modifier = Modifier.fillMaxSize(),
                                                                avatarUrl = chat?.avatarUrl,
                                                                peerId = chat?.peerId,
                                                                chatId = chat?.id,
                                                                currentUserId = myId,
                                                                peerLastSeen = peerUser?.lastSeen ?: 0L,
                                                                isMuted = chat?.isMuted ?: false,
                                                                isPeerBlocked = peerUser?.isBlocked ?: false,
                                                                isGroup = chat?.type == tn.keeftalk.chat.domain.model.ChatType.GROUP,
                                                                screenshotProtectionEnabled = detailUserPrefs.screenshotProtectionEnabled,
                                                                autoDeleteTimer = chat?.autoDeleteTimer,
                                                                autoTranslateEnabled = chat?.autoTranslateEnabled ?: false,
                                                                messagesFlow = messagesPagingData,
                                                                readReceipts = readReceiptsMap,
                                                                typingUsers = typingUsers.asSequence().filter { it != myId }.toSet(),
                                                                onSendMessage = { detailViewModel.sendMessage(it, replyingToMessage?.id); replyingToMessage = null },
                                                                onTypingStatusChange = { detailViewModel.setTyping(it) },
                                                                onMessageVisible = { detailViewModel.onMessageVisible(it) },
                                                                onImageCaptured = { detailViewModel.sendMedia(tn.keeftalk.chat.domain.model.MessageType.IMAGE, it.toString()) },
                                                                onVideoCaptureCaptured = { detailViewModel.sendMedia(tn.keeftalk.chat.domain.model.MessageType.VIDEO, it.toString()) },
                                                                onBackClick = { scope.launch { navigator.navigateBack() } },
                                                                onMarkAsRead = { detailViewModel.markAsRead() },
                                                                initialShowAttachmentSheet = showAttachmentsForChatId == content,
                                                                onProfileClick = { chat?.peerId?.let { onScreenChange(AppScreen.OtherProfile(it)) } },
                                                                onAutoDeleteChange = { detailViewModel.setAutoDelete(it) },
                                                                onThemeChange = { detailViewModel.updateTheme(it) },
                                                                onAutoTranslateToggle = { detailViewModel.toggleAutoTranslate(it) },
                                                                onTranslateMessage = { detailViewModel.translateMessage(it) },
                                                                onMuteToggle = { detailViewModel.toggleMute(it) },
                                                                onBlockToggle = { b -> chat?.peerId?.let { scope.launch { mainViewModel.chatRepository.blockUser(it, b) } } },
                                                                onClearChat = { detailViewModel.clearChat() },
                                                                onDeleteChat = { detailViewModel.deleteChat(); scope.launch { navigator.navigateBack() } },
                                                                onVoiceCallClick = { val callId = java.util.UUID.randomUUID().toString(); scope.launch { mainViewModel.chatRepository.startCall(content, "VOICE", callId); tn.keeftalk.chat.ui.call.CallActivity.start(context as FragmentActivity, callId, true) } },
                                                                onVideoCallClick = { val callId = java.util.UUID.randomUUID().toString(); scope.launch { mainViewModel.chatRepository.startCall(content, "VIDEO", callId); tn.keeftalk.chat.ui.call.CallActivity.start(context as FragmentActivity, callId, true) } },
                                                                onAttachmentClick = { type ->
                                                                    activeChatIdForMedia = content
                                                                    handleAttachmentAction(
                                                                        type = type,
                                                                        chatId = content,
                                                                        pickFileLauncher = pickFileLauncher,
                                                                        pickContactLauncher = pickContactLauncher,
                                                                        onScreenChange = onScreenChange
                                                                    )
                                                                },
                                                                onBrowseSystemDocuments = { activeChatIdForMedia = content; pickFileLauncher.launch(arrayOf("*/*")) },
                                                                onReviewDocuments = { docs -> onScreenChange(AppScreen.FileReview(content, selectedDocs = docs)) },
                                                                onSendDocuments = { uris ->
                                                                    uris.forEach { fileUri ->
                                                                        activeChatIdForMedia?.let { chatId ->
                                                                            scope.launch {
                                                                                val mimeType = context.contentResolver.getType(fileUri) ?: ""
                                                                                val messageType = when {
                                                                                    mimeType.startsWith("image/") -> tn.keeftalk.chat.domain.model.MessageType.IMAGE
                                                                                    mimeType.startsWith("video/") -> tn.keeftalk.chat.domain.model.MessageType.VIDEO
                                                                                    mimeType.startsWith("audio/") -> tn.keeftalk.chat.domain.model.MessageType.VOICE
                                                                                    mimeType == "application/pdf" -> tn.keeftalk.chat.domain.model.MessageType.PDF
                                                                                    else -> tn.keeftalk.chat.domain.model.MessageType.FILE
                                                                                }
                                                                                val fileName = fileUri.lastPathSegment ?: "file"
                                                                                mainViewModel.chatRepository.sendMessage(chatId, if (messageType == tn.keeftalk.chat.domain.model.MessageType.FILE) fileName else "", messageType, fileUri.toString())
                                                                            }
                                                                        }
                                                                    }
                                                                },
                                                                onMicStart = { detailViewModel.startRecording() },
                                                                onMicStop = { autoSend -> detailViewModel.stopRecording(autoSend) },
                                                                onMicCancel = { detailViewModel.cancelRecording() },
                                                                onGooglePhotosClick = { 
                                                                    activeChatIdForMedia = content
                                                                    pickVisualMediaLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) 
                                                                },
                                                                onGooglePhotosAppClick = {
                                                                    activeChatIdForMedia = content
                                                                    val intentPhotos = Intent(Intent.ACTION_GET_CONTENT).apply {
                                                                        type = "image/* video/*"
                                                                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                                                                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                                                        setPackage("com.google.android.apps.photos")
                                                                    }
                                                                    try {
                                                                        googlePhotosAppLauncher.launch(intentPhotos)
                                                                    } catch (e: Exception) {
                                                                        // If specifically targeting Google Photos fails, just use generic picker
                                                                        intentPhotos.setPackage(null)
                                                                        googlePhotosAppLauncher.launch(intentPhotos)
                                                                    }
                                                                },
                                                                onAddReaction = { msgId, emoji -> detailViewModel.addReaction(msgId, emoji) },
                                                                onRemoveReaction = { msgId -> detailViewModel.removeReaction(msgId) },
                                                                onBumpChat = { detailViewModel.bumpChat() },
                                                                onForwardMessage = { _, _ -> android.widget.Toast.makeText(context, "Forwarding not yet implemented in UI picker", android.widget.Toast.LENGTH_SHORT).show() },
                                                                onReportMessage = { msgId, reason -> detailViewModel.reportMessage(msgId, reason) },
                                                                onDeleteMessage = { msgId -> detailViewModel.deleteMessage(msgId) },
                                                                onReactionBubbleClick = { reactions -> detailViewModel.loadReactionProfiles(reactions) },
                                                                getUserProfile = { detailViewModel.getProfile(it) },
                                                                chatSettings = chatSettings,
                                                                onNoteClick = { noteId -> onScreenChange(AppScreen.Notes(noteId)) },
                                                                checkNoteAvailability = { noteId -> 
                                                                    val repo = AppModule.provideNoteRepository(context)
                                                                    repo.getNote(noteId).first() != null 
                                                                },
                                                                onMediaClick = { messageId, pos -> 
                                                                    scope.launch {
                                                                        detailViewModel.getMessage(messageId).firstOrNull()?.let { msg ->
                                                                            if (msg.type == tn.keeftalk.chat.domain.model.MessageType.LOCATION) {
                                                                                val geoUri = msg.content // Use content for geo URI
                                                                                try {
                                                                                    val coords = geoUri.removePrefix("geo:").substringBefore("?").split(",")
                                                                                    val lat = coords[0].toDouble()
                                                                                    val lon = coords[1].toDouble()
                                                                                    val isLive = geoUri.contains("live=true")
                                                                                    onScreenChange(AppScreen.LocationDetail(lat, lon, "Location", isLive))
                                                                                } catch (e: Exception) {
                                                                                    onScreenChange(AppScreen.MediaViewer(messageId, content, pos))
                                                                                }
                                                                            } else if (msg.fileName?.lowercase(Locale.getDefault())?.endsWith(".pdf") == true) {
                                                                                onScreenChange(AppScreen.PdfViewer(messageId, content))
                                                                            } else {
                                                                                onScreenChange(AppScreen.MediaViewer(messageId, content, pos))
                                                                            }
                                                                        } ?: run {
                                                                            onScreenChange(AppScreen.MediaViewer(messageId, content, pos))
                                                                        }
                                                                    }
                                                                },
                                                                onPdfClick = { messageId ->
                                                                    onScreenChange(AppScreen.PdfViewer(messageId, content))
                                                                },
                                                                onEmailClick = { emailId ->
                                                                    onScreenChange(AppScreen.EmailDetail(emailId))
                                                                },
                                                                onVaultClick = { _ ->
                                                                    onScreenChange(AppScreen.Vault)
                                                                },
                                                                onAgendaClick = { _ ->
                                                                    onScreenChange(AppScreen.Calendar)
                                                                },
                                                                onCodeClick = { messageId ->
                                                                    onScreenChange(AppScreen.CodeViewer(messageId, content))
                                                                },
                                                                onToggleMediaLock = { messageId, locked ->
                                                                    detailViewModel.toggleMediaLock(messageId, locked)
                                                                },
                                                                onDownloadMessage = { message ->
                                                                    detailViewModel.downloadMedia(message)
                                                                },
                                                                recordingState = recordingState,
                                                                recordingDuration = recordingDuration,
                                                                amplitudeHistory = amplitudeHistory,
                                                                onSendVoice = { detailViewModel.sendVoiceMessage() },
                                                                initialScrollPosition = detailViewModel.savedScrollPosition,
                                                                initialScrollOffset = detailViewModel.savedScrollOffset,
                                                                onScrollStateChange = { pos, offset -> 
                                                                    detailViewModel.savedScrollPosition = pos
                                                                    detailViewModel.savedScrollOffset = offset
                                                                },
                                                                transferProgress = transferProgress,
                                                                failedMessage = failedMessage,
                                                                expandedMessageId = expandedMessageId,
                                                                onToggleTimestamp = { detailViewModel.toggleTimestamp(it) },
                                                                onRetryMessage = { detailViewModel.retryMessage(it) },
                                                                onCancelMessage = { detailViewModel.cancelMessage(it) },
                                                                onDismissFailure = { detailViewModel.dismissFailure() },
                                                                userProfiles = reactionProfiles,
                                                                mediaItemsFlow = detailViewModel.mediaItems,
                                                                availableAlbums = albums,
                                                                selectedAlbum = selectedAlbum,
                                                                onAlbumClick = { detailViewModel.setAlbum(it) },
                                                                selectedMediaList = selectedMediaList,
                                                                onToggleMediaSelection = { detailViewModel.toggleMediaSelection(it) },
                                                                mediaQualityMap = mediaQualityMap,
                                                                onQualityChange = { id, quality -> detailViewModel.setMediaQuality(id, quality) },
                                                                onClearMediaSelection = { detailViewModel.clearMediaSelection() },
                                                                onSendMediaWithCaption = { media, caption -> detailViewModel.sendMediaWithCaption(media, caption) },
                                                                onSendEditedMedia = { items -> 
                                                                    scope.launch {
                                                                        @androidx.media3.common.util.UnstableApi
                                                                        AppModule.provideMediaExportPipeline(context)
                                                                            .exportAndSend(content, items)
                                                                    }
                                                                    detailViewModel.clearMediaSelection()
                                                                },
                                                                onUnlockFullAccess = { 
                                                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                                                        val intentAccess = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                                            data = "package:${context.packageName}".toUri()
                                                                        }
                                                                        unlockFullAccessLauncher.launch(intentAccess)
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    } else {
                                                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) { Text("Select a chat to start messaging", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge) }
                                                    }
                                                }
                                            )
                                        }
                                    }
                            is AppScreen.NewGroup -> NewGroupScreen(
                                viewModel = viewModel { NewGroupViewModel(mainViewModel.chatRepository) },
                                onBack = { onScreenChange(AppScreen.ChatList) },
                                onGroupCreated = { chatId ->
                                    onScreenChange(AppScreen.ChatList)
                                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, chatId) }
                                }
                            )
                            is AppScreen.Discovery -> DiscoveryScreen(
                                viewModel = viewModel { DiscoveryViewModel(mainViewModel.chatRepository) },
                                onBack = { onScreenChange(AppScreen.ChatList) },
                                onChatStarted = { chatId ->
                                    onScreenChange(AppScreen.ChatList)
                                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, chatId) }
                                },
                                onProfileClick = { userId -> onScreenChange(AppScreen.OtherProfile(userId)) }
                            )
                            is AppScreen.MyProfile -> ProfileScreen(
                                viewModel = viewModel {
                                    ProfileViewModel(mainViewModel.authRepository, mainViewModel.chatRepository, AppModule.provideCountryService(context), AppModule.providePhoneNumberService(context), AppModule.provideCallLogManager(context), AppModule.provideSmsRepository(context), AppModule.provideRelationshipRepository(context), null)
                                },
                                onBack = { onScreenChange(AppScreen.ChatList) },
                                isCurrentUser = true
                            )
                            is AppScreen.OtherProfile -> ProfileScreen(
                                viewModel = viewModel(key = currentScreen.userId) {
                                    ProfileViewModel(mainViewModel.authRepository, mainViewModel.chatRepository, AppModule.provideCountryService(context), AppModule.providePhoneNumberService(context), AppModule.provideCallLogManager(context), AppModule.provideSmsRepository(context), AppModule.provideRelationshipRepository(context), currentScreen.userId)
                                },
                                onBack = { onScreenChange(AppScreen.ChatList) },
                                onChatStarted = { chatId ->
                                    onScreenChange(AppScreen.ChatList)
                                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, chatId) }
                                },
                                onConnectionPathClick = { onScreenChange(AppScreen.ConnectionPath(currentScreen.userId)) },
                                isCurrentUser = false
                            )
                            is AppScreen.ConnectionPath -> ConnectionPathScreen(
                                viewModel = viewModel(key = currentScreen.userId) {
                                    tn.keeftalk.chat.ui.profile.ConnectionPathViewModel(
                                        AppModule.provideRelationshipRepository(context),
                                        mainViewModel.chatRepository,
                                        mainViewModel.authRepository,
                                        currentScreen.userId
                                    )
                                },
                                onBack = { onScreenChange(AppScreen.OtherProfile(currentScreen.userId)) },
                                onProfileClick = { userId -> onScreenChange(AppScreen.OtherProfile(userId)) }
                            )
                            is AppScreen.Settings -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { onScreenChange(AppScreen.ChatList) },
                                    onViewProfile = { onScreenChange(AppScreen.MyProfile) },
                                    onAccountSettingsClick = { onScreenChange(AppScreen.AccountSettings) },
                                    onNotificationsClick = { onScreenChange(AppScreen.NotificationSettings) },
                                    onChatSettingsClick = { onScreenChange(AppScreen.ChatSettings) },
                                    onCallSettingsClick = { onScreenChange(AppScreen.CallSettings) },
                                    onNoteSettingsClick = { onScreenChange(AppScreen.NoteSettings) },
                                    onVaultSettingsClick = { onScreenChange(AppScreen.VaultSettings) },
                                    onCalendarSettingsClick = { onScreenChange(AppScreen.CalendarSettings) },
                                    onHabitSettingsClick = { onScreenChange(AppScreen.HabitSettings) },
                                    onFinancesSettingsClick = { onScreenChange(AppScreen.FinancesSettings) },
                                    onEmailSettingsClick = { onScreenChange(AppScreen.EmailSettings) },
                                    onBrowserSettingsClick = { onScreenChange(AppScreen.BrowserSettings) },
                                    onParentalControlsClick = { onScreenChange(AppScreen.ParentalControls) },
                                    onAppCustomizationClick = { onScreenChange(AppScreen.AppCustomization) },
                                    onDisplaySettingsClick = { onScreenChange(AppScreen.DisplaySettings) },
                                    onAboutClick = { onScreenChange(AppScreen.About) },
                                    onHelpClick = { onScreenChange(AppScreen.Help) },
                                    onTestScreensPreviewClick = { onScreenChange(AppScreen.TestScreensPreview) }
                                )
                            }
                            is AppScreen.TestScreensPreview -> {
                                tn.keeftalk.chat.ui.settings.TestScreensPreviewScreen(
                                    onBack = { onScreenChange(AppScreen.Settings) }
                                )
                            }
                            is AppScreen.SubscriptionPlans -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                tn.keeftalk.chat.ui.settings.SubscriptionPlansScreen(
                                    viewModel = settingsViewModel,
                                    billingManager = AppModule.provideBillingManager(context),
                                    onBack = { onScreenChange(AppScreen.AccountSettings) }
                                )
                            }
                            is AppScreen.AccountSettings -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                tn.keeftalk.chat.ui.settings.AccountSettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { onScreenChange(AppScreen.Settings) },
                                    onSubscriptionPlansClick = { onScreenChange(AppScreen.SubscriptionPlans) }
                                )
                            }
                            is AppScreen.AppCustomization -> {
                                val customizationViewModel: tn.keeftalk.chat.ui.settings.AppCustomizationViewModel = viewModel {
                                    tn.keeftalk.chat.ui.settings.AppCustomizationViewModel(AppModule.provideAppCustomizationRepository(context))
                                }
                                tn.keeftalk.chat.ui.settings.AppCustomizationScreen(
                                    viewModel = customizationViewModel,
                                    onBack = { onScreenChange(AppScreen.Settings) }
                                )
                            }
                            is AppScreen.NotificationSettings -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                NotificationsSettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { onScreenChange(AppScreen.Settings) },
                                    onSendTestNotification = { settingsViewModel.sendTestNotification(context) }
                                )

                            }
                            is AppScreen.BlockedUsers -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                BlockedUsersScreen(viewModel = settingsViewModel, onBack = { onScreenChange(AppScreen.Settings) })
                            }
                            is AppScreen.CallSettings -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                CallSettingsScreen(viewModel = settingsViewModel, onBack = { onScreenChange(AppScreen.Settings) })
                            }
                            is AppScreen.NoteSettings -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                NoteSettingsScreen(viewModel = settingsViewModel, onBack = { onScreenChange(AppScreen.Settings) })
                            }
                            is AppScreen.VaultSettings -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                VaultSettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { onScreenChange(AppScreen.Settings) },
                                    onAutomaticBackupClick = { onScreenChange(AppScreen.AutomaticBackupSettings) }
                                )
                            }
                            is AppScreen.SecretsSettings -> {
                                val secretsViewModel: SecretsViewModel = viewModel {
                                    SecretsViewModel(
                                        myId,
                                        AppModule.provideSecretsRepository(context)
                                    )
                                }
                                SecretsSettingsScreen(
                                    settings = secretsViewModel.uiState.collectAsState().value.settings,
                                    onSettingsChanged = { secretsViewModel.updateSettings(it) },
                                    onBack = { onScreenChange(AppScreen.Secrets) }
                                )
                            }
                            is AppScreen.AutomaticBackupSettings -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                AutomaticBackupSettingsScreen(
                                    viewModel = settingsViewModel,
                                    backupManager = AppModule.provideVaultBackupManager(context),
                                    onBack = { onScreenChange(AppScreen.VaultSettings) }
                                )
                            }
                            is AppScreen.CalendarSettings -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                CalendarSettingsScreen(viewModel = settingsViewModel, onBack = { onScreenChange(AppScreen.Settings) })
                            }
                            is AppScreen.HabitSettings -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                HabitSettingsScreen(viewModel = settingsViewModel, onBack = { onScreenChange(AppScreen.Settings) })
                            }
                            is AppScreen.ParentalControls -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                ParentalControlsScreen(viewModel = settingsViewModel, onBack = { onScreenChange(AppScreen.Settings) })
                            }
                            is AppScreen.EmailSettings -> {
                                val emailViewModel: EmailViewModel = viewModel {
                                    EmailViewModel(
                                        AppModule.provideEmailRepository(context),
                                        AppModule.provideEmailAuthManager(context)
                                    )
                                }
                                tn.keeftalk.chat.feature.email.ui.screens.EmailSettingsScreen(
                                    viewModel = emailViewModel,
                                    onBack = { onScreenChange(AppScreen.Email) },
                                    onAddAccount = { 
                                        emailViewModel.setOnboardingStep(tn.keeftalk.chat.feature.email.viewmodel.OnboardingStep.ProviderSelection)
                                        emailViewModel.forceShowOnboarding(true)
                                        onScreenChange(AppScreen.Email)
                                    }
                                )
                            }
                            is AppScreen.ProfileViewHistory -> {
                                val profileViewHistoryViewModel: ProfileViewHistoryViewModel = viewModel {
                                    ProfileViewHistoryViewModel(AppModule.providePrivacyRepository(context))
                                }
                                ProfileViewHistoryScreen(
                                    viewModel = profileViewHistoryViewModel,
                                    onBack = { onScreenChange(AppScreen.Settings) },
                                    onProfileClick = { onScreenChange(AppScreen.OtherProfile(it)) }
                                )
                            }
                            is AppScreen.ChatSettings -> {
                                val chatSettingsViewModel: ChatSettingsViewModel = viewModel {
                                    ChatSettingsViewModel(
                                        AppModule.provideSettingsRepository(context),
                                        AppModule.provideUserPreferencesRepository(context)
                                    )
                                }
                                ChatSettingsScreen(
                                    viewModel = chatSettingsViewModel,
                                    onBack = { onScreenChange(AppScreen.Settings) },
                                    onAppearanceClick = { onScreenChange(AppScreen.ChatAppearanceSettings) },
                                    onTextAccessibilityClick = { onScreenChange(AppScreen.ChatTextAccessibilitySettings) },
                                    onMediaDownloadsClick = { onScreenChange(AppScreen.ChatMediaDownloadsSettings) },
                                    onChatBehaviorClick = { onScreenChange(AppScreen.ChatBehaviorSettings) },
                                    onStorageCacheClick = { onScreenChange(AppScreen.ChatStorageCacheSettings) },
                                    onChatCleanupClick = { onScreenChange(AppScreen.ChatCleanupSettings) }
                                )
                            }
                            is AppScreen.ChatAppearanceSettings -> {
                                val chatSettingsViewModel: ChatSettingsViewModel = viewModel {
                                    ChatSettingsViewModel(
                                        AppModule.provideSettingsRepository(context),
                                        AppModule.provideUserPreferencesRepository(context)
                                    )
                                }
                                ChatAppearanceSettingsScreen(viewModel = chatSettingsViewModel, onBack = { onScreenChange(AppScreen.ChatSettings) })
                            }
                            is AppScreen.ChatTextAccessibilitySettings -> {
                                val chatSettingsViewModel: ChatSettingsViewModel = viewModel {
                                    ChatSettingsViewModel(
                                        AppModule.provideSettingsRepository(context),
                                        AppModule.provideUserPreferencesRepository(context)
                                    )
                                }
                                TextAccessibilitySettingsScreen(viewModel = chatSettingsViewModel, onBack = { onScreenChange(AppScreen.Settings) })
                            }
                            is AppScreen.ChatMediaDownloadsSettings -> {
                                val chatSettingsViewModel: ChatSettingsViewModel = viewModel {
                                    ChatSettingsViewModel(
                                        AppModule.provideSettingsRepository(context),
                                        AppModule.provideUserPreferencesRepository(context)
                                    )
                                }
                                MediaDownloadsSettingsScreen(viewModel = chatSettingsViewModel, onBack = { onScreenChange(AppScreen.ChatSettings) })
                            }
                            is AppScreen.ChatBehaviorSettings -> {
                                val chatSettingsViewModel: ChatSettingsViewModel = viewModel {
                                    ChatSettingsViewModel(
                                        AppModule.provideSettingsRepository(context),
                                        AppModule.provideUserPreferencesRepository(context)
                                    )
                                }
                                ChatBehaviorSettingsScreen(viewModel = chatSettingsViewModel, onBack = { onScreenChange(AppScreen.ChatSettings) })
                            }
                            is AppScreen.ChatStorageCacheSettings -> {
                                val chatSettingsViewModel: ChatSettingsViewModel = viewModel {
                                    ChatSettingsViewModel(
                                        AppModule.provideSettingsRepository(context),
                                        AppModule.provideUserPreferencesRepository(context)
                                    )
                                }
                                StorageCacheSettingsScreen(viewModel = chatSettingsViewModel, onBack = { onScreenChange(AppScreen.Settings) })
                            }
                            is AppScreen.ChatCleanupSettings -> {
                                val chatSettingsViewModel: ChatSettingsViewModel = viewModel {
                                    ChatSettingsViewModel(
                                        AppModule.provideSettingsRepository(context),
                                        AppModule.provideUserPreferencesRepository(context)
                                    )
                                }
                                ChatCleanupSettingsScreen(viewModel = chatSettingsViewModel, onBack = { onScreenChange(AppScreen.ChatStorageCacheSettings) })
                            }
                            is AppScreen.DisplaySettings -> {
                                tn.keeftalk.chat.ui.settings.DisplaySettingsScreen(
                                    viewModel = mainViewModel,
                                    onBack = { onScreenChange(AppScreen.Settings) }
                                )
                            }
                            is AppScreen.ChatNotificationSettings -> {
                                val chat by mainViewModel.chatRepository.getChat(currentScreen.chatId).collectAsState(initial = null)
                                if (chat != null) {
                                    ChatNotificationSettingsScreen(
                                        chat = chat!!,
                                        onBack = { onScreenChange(AppScreen.ChatList) },
                                        onUpdateSettings = { scope.launch { mainViewModel.chatRepository.updateChatNotificationSettings(currentScreen.chatId, it) } }
                                    )
                                }
                            }
                            is AppScreen.MediaViewer -> {
                                val mediaViewModel: MediaViewerViewModel = viewModel(key = currentScreen.initialMessageId) {
                                    MediaViewerViewModel(
                                        context.applicationContext as android.app.Application,
                                        mainViewModel.chatRepository,
                                        currentScreen.chatId,
                                        currentScreen.initialMessageId
                                    )
                                }
                                MediaViewerScreen(
                                    viewModel = mediaViewModel,
                                    onBack = { 
                                        onScreenChange(AppScreen.ChatList)
                                        scope.launch {
                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, currentScreen.chatId)
                                        }
                                    },
                                    currentUserId = myId,
                                    initialPlaybackPosition = currentScreen.initialPlaybackPosition
                                )
                            }
                            is AppScreen.PdfViewer -> {
                                val pdfViewModel: PdfViewerViewModel = viewModel(key = currentScreen.messageId) {
                                    PdfViewerViewModel(
                                        context.applicationContext as android.app.Application,
                                        mainViewModel.chatRepository,
                                        currentScreen.messageId
                                    )
                                }

                                PdfViewerScreen(
                                    viewModel = pdfViewModel,
                                    myId = myId,
                                    onBack = { 
                                        onScreenChange(AppScreen.ChatList)
                                        scope.launch {
                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, currentScreen.chatId)
                                        }
                                    }
                                )
                            }
                            is AppScreen.CodeViewer -> {
                                val codeViewModel: CodeViewerViewModel = viewModel(key = currentScreen.messageId) {
                                    CodeViewerViewModel(
                                        context.applicationContext as android.app.Application,
                                        mainViewModel.chatRepository,
                                        currentScreen.messageId
                                    )
                                }

                                CodeViewerScreen(
                                    viewModel = codeViewModel,
                                    myId = myId,
                                    onToggleLock = {
                                        codeViewModel.message.value?.let { m ->
                                            scope.launch {
                                                mainViewModel.chatRepository.toggleMediaLock(m.id, !m.mediaLocked)
                                            }
                                        }
                                    },
                                    onBack = { 
                                        onScreenChange(AppScreen.ChatList)
                                        scope.launch {
                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, currentScreen.chatId)
                                        }
                                    }
                                )
                            }
                            is AppScreen.MediaPicker -> {
                                val mediaPickerViewModel: tn.keeftalk.chat.ui.screens.picker.MediaPickerViewModel = viewModel {
                                    tn.keeftalk.chat.ui.screens.picker.MediaPickerViewModel(
                                        context.applicationContext as android.app.Application,
                                        AppModule.provideMediaRepository(context)
                                    )
                                }
                                tn.keeftalk.chat.ui.screens.picker.MediaPickerScreen(
                                    viewModel = mediaPickerViewModel,
                                    onBack = { onScreenChange(AppScreen.ChatList) },
                                    onComplete = { selectedItems ->
                                        onScreenChange(AppScreen.FileReview(currentScreen.chatId, selectedMedia = selectedItems))
                                    }
                                )
                            }
                            is AppScreen.FileReview -> {
                                @androidx.media3.common.util.UnstableApi
                                tn.keeftalk.chat.ui.screens.editor.MediaReviewScreen(
                                    initialMedia = currentScreen.selectedMedia,
                                    initialDocs = currentScreen.selectedDocs,
                                    onBack = { 
                                        if (currentScreen.selectedMedia.isNotEmpty()) {
                                            onScreenChange(AppScreen.MediaPicker(currentScreen.chatId))
                                        } else {
                                            onScreenChange(AppScreen.ChatList)
                                        }
                                    },
                                    onSend = { editorModels: List<tn.keeftalk.chat.domain.model.EditorModel> ->
                                        scope.launch {
                                            @androidx.media3.common.util.UnstableApi
                                            AppModule.provideMediaExportPipeline(context)
                                                .exportAndSend(currentScreen.chatId, editorModels)
                                        }
                                        // Navigate back to Chat Detail immediately
                                        onScreenChange(AppScreen.ChatList)
                                        scope.launch {
                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, currentScreen.chatId)
                                        }
                                    }
                                )
                            }
                            is AppScreen.About -> AboutScreen(onBack = { onScreenChange(AppScreen.Settings) })
                            is AppScreen.Notes -> {
                                NotesScreen(
                                    initialNoteId = currentScreen.noteId,
                                    onBack = { onScreenChange(AppScreen.ChatList) },
                                    onNoteClick = { onScreenChange(AppScreen.NoteEditor(it)) },
                                    fabActionFlow = fabActionFlow,
                                    onDetailVisibilityChange = { isDetailShown = it }
                                )
                            }
                            is AppScreen.NoteEditor -> {
                                NativeNoteEditorScreen(
                                    noteId = currentScreen.noteId ?: "",
                                    onBack = { onNavigateBack() }
                                )
                            }
                            is AppScreen.Vault -> {
                                val vaultViewModel: VaultViewModel = viewModel {
                                    VaultViewModel(
                                        AppModule.provideVaultRepository(context), 
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideChatRepository(context),
                                        AppModule.provideCloudImportCoordinator(context),
                                        AppModule.provideGooglePhotosService(context),
                                        AppModule.provideGoogleDriveService(context),
                                        AppModule.provideDropboxService(),
                                        AppModule.provideAuthRepository(context),
                                        AppModule.provideUserPreferencesRepository(context)
                                    )
                                }
                                VaultScreen(
                                    viewModel = vaultViewModel,
                                    onBack = { onScreenChange(AppScreen.ChatList) },
                                    searchQuery = searchQuery,
                                    fabActionFlow = fabActionFlow
                                )
                            }
                            is AppScreen.Secrets -> {
                                val secretsViewModel: SecretsViewModel = viewModel {
                                    SecretsViewModel(
                                        myId,
                                        AppModule.provideSecretsRepository(context)
                                    )
                                }
                                SecretsScreen(
                                    viewModel = secretsViewModel,
                                    onBack = { onScreenChange(AppScreen.ChatList) },
                                    onSettingsClick = { onScreenChange(AppScreen.SecretsSettings) },
                                    fabActionFlow = fabActionFlow
                                )
                            }
                            is AppScreen.QrCode -> QrScreen(userId = myId, onQrScanned = { }, onBack = { onScreenChange(AppScreen.ChatList) })
                            is AppScreen.Calendar -> {
                                val calendarViewModel: CalendarViewModel = viewModel {
                                    CalendarViewModel(
                                        AppModule.provideCalendarRepository(context),
                                        AppModule.provideAuthRepository(context),
                                        AppModule.provideChatRepository(context)
                                    )
                                }
                                CalendarScreen(
                                    viewModel = calendarViewModel,
                                    onBack = { onScreenChange(AppScreen.ChatList) },
                                    onNavigateToEditor = { onScreenChange(AppScreen.CalendarEditor(it)) },
                                    onSettingsClick = { },
                                    fabActionFlow = fabActionFlow
                                )
                            }
                            is AppScreen.CalendarEditor -> CalendarEditorScreen(type = currentScreen.type, onBack = { onScreenChange(AppScreen.Calendar) }, onSave = { onScreenChange(AppScreen.Calendar) })
                            is AppScreen.Wallet -> {
                                val financesViewModel: FinancesViewModel = viewModel {
                                    FinancesViewModel(AppModule.provideFinancesRepository(context))
                                }
                                WalletScreen(
                                    viewModel = financesViewModel,
                                    onBack = { onScreenChange(AppScreen.ChatList) }
                                )
                            }
                            is AppScreen.Finances -> {
                                val financesViewModel: FinancesViewModel = viewModel {
                                    FinancesViewModel(AppModule.provideFinancesRepository(context))
                                }
                                FinancesScreen(
                                    viewModel = financesViewModel,
                                    onBack = { onScreenChange(AppScreen.ChatList) },
                                    onSettingsClick = { onScreenChange(AppScreen.FinancesSettings) },
                                    fabActionFlow = fabActionFlow
                                )
                            }
                            is AppScreen.FinancesSettings -> {
                                val financesViewModel: FinancesViewModel = viewModel {
                                    FinancesViewModel(AppModule.provideFinancesRepository(context))
                                }
                                val state by financesViewModel.uiState.collectAsState()
                                FinancesSettingsScreen(
                                    state = state,
                                    viewModel = financesViewModel,
                                    onBack = { onScreenChange(AppScreen.Finances) }
                                )
                            }
                            is AppScreen.Email -> {
                                val emailViewModel: EmailViewModel = viewModel {
                                    EmailViewModel(
                                        AppModule.provideEmailRepository(context),
                                        AppModule.provideEmailAuthManager(context)
                                    )
                                }
                                EmailScreen(
                                    viewModel = emailViewModel,
                                    onBack = { onScreenChange(AppScreen.ChatList) },
                                    onCompose = { to, sub, body -> onScreenChange(AppScreen.ComposeEmail(to, sub, body)) },
                                    onSettingsClick = { onScreenChange(AppScreen.EmailSettings) },
                                    onEmailClick = { onScreenChange(AppScreen.EmailDetail(it)) },
                                    fabActionFlow = fabActionFlow
                                )
                            }
                            is AppScreen.EmailDetail -> {
                                val emailViewModel: EmailViewModel = viewModel {
                                    EmailViewModel(
                                        AppModule.provideEmailRepository(context),
                                        AppModule.provideEmailAuthManager(context)
                                    )
                                }
                                tn.keeftalk.chat.feature.email.ui.screens.EmailDetailScreen(
                                    messageId = currentScreen.messageId,
                                    viewModel = emailViewModel,
                                    onBack = { onScreenChange(AppScreen.Email) },
                                    onDelete = { onScreenChange(AppScreen.Email) },
                                    onReply = { msg -> 
                                        onScreenChange(AppScreen.ComposeEmail(msg.senderEmail, "Re: ${msg.subject}", "<br><br>---<br>${msg.content}"))
                                    },
                                    onForward = { msg ->
                                        onScreenChange(AppScreen.ComposeEmail("", "Fwd: ${msg.subject}", "<br><br>--- Forwarded message ---<br>From: ${msg.senderName} &lt;${msg.senderEmail}&gt;<br>Subject: ${msg.subject}<br><br>${msg.content}"))
                                    }
                                )
                            }
                            is AppScreen.ComposeEmail -> {
                                val emailViewModel: EmailViewModel = viewModel {
                                    EmailViewModel(
                                        AppModule.provideEmailRepository(context),
                                        AppModule.provideEmailAuthManager(context)
                                    )
                                }
                                ComposeEmailScreen(
                                    viewModel = emailViewModel,
                                    initialTo = currentScreen.to,
                                    initialSubject = currentScreen.subject,
                                    initialBody = currentScreen.initialBody,
                                    onBack = { onScreenChange(AppScreen.Email) }
                                )
                            }
                            is AppScreen.ForgotPassword -> {
                                val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel { ForgotPasswordViewModel(mainViewModel.authRepository) }
                                ForgotPasswordScreen(viewModel = forgotPasswordViewModel, onBack = { onScreenChange(AppScreen.ChatList) })
                            }
                            is AppScreen.CreateNewPassword -> {
                                val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel { ForgotPasswordViewModel(mainViewModel.authRepository) }
                                CreateNewPasswordScreen(viewModel = forgotPasswordViewModel, onSuccess = { onScreenChange(AppScreen.ResetSuccess) })
                            }
                            is AppScreen.ResetSuccess -> {
                                PasswordResetSuccessScreen(onSignInNow = { onScreenChange(AppScreen.ChatList) })
                            }
                            is AppScreen.AddContact -> {
                                tn.keeftalk.chat.ui.screens.AddContactScreen(
                                    onBack = { onScreenChange(AppScreen.ChatList) },
                                    onSave = { name, phone, secPhone, secLabel, tertPhone, tertLabel, email, bday, card, avatar ->
                                        scope.launch {
                                            mainViewModel.chatRepository.saveLocalContact(
                                                name, phone, secPhone, secLabel, tertPhone, tertLabel, email, bday, card, avatar
                                            )
                                            onScreenChange(AppScreen.ChatList)
                                        }
                                    }
                                )
                            }
                            is AppScreen.ContactDetail -> {
                                val user by mainViewModel.chatRepository.getContact(currentScreen.userId).collectAsState(initial = null)
                                user?.let { u ->
                                    tn.keeftalk.chat.ui.screens.ContactDetailScreen(
                                        user = u,
                                        onBack = { onScreenChange(AppScreen.ChatList) },
                                        onChatClick = { scope.launch { mainViewModel.chatRepository.getOrCreateOneToOneChat(u.id).onSuccess { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, it) } } },
                                        onCallClick = { /* Start call */ },
                                        onSmsClick = {
                                            u.phone?.let { phone ->
                                                scope.launch {
                                                    val threadId = android.provider.Telephony.Threads.getOrCreateThreadId(context, phone)
                                                    onScreenChange(AppScreen.SmsDetail(threadId, phone))
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            is AppScreen.ArchivedChats -> {
                                val chatListViewModel: tn.keeftalk.chat.ui.screens.ChatListViewModel = viewModel { tn.keeftalk.chat.ui.screens.ChatListViewModel(mainViewModel.chatRepository) }
                                ArchivedChatsScreen(viewModel = chatListViewModel, myId = myId, onBack = { onScreenChange(AppScreen.ChatList) }, onChatClick = { scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, it) } })
                            }
                            is AppScreen.Help -> {
                                val bugReportViewModel: tn.keeftalk.chat.ui.settings.BugReportViewModel = viewModel {
                                    tn.keeftalk.chat.ui.settings.BugReportViewModel(
                                        AppModule.provideBugReportRepository(context)
                                    )
                                }
                                HelpScreen(
                                    viewModel = bugReportViewModel,
                                    onBack = { onScreenChange(AppScreen.Settings) }
                                )
                            }
                            is AppScreen.SmsList -> { /* Already handled via chatTab */ }
                            is AppScreen.SmsDetail -> {
                                val smsDetailViewModel: tn.keeftalk.chat.ui.screens.SmsDetailViewModel = viewModel(key = currentScreen.threadId.toString()) {
                                    tn.keeftalk.chat.ui.screens.SmsDetailViewModel(
                                        context.applicationContext as android.app.Application,
                                        tn.keeftalk.chat.di.AppModule.provideSmsRepository(context),
                                        tn.keeftalk.chat.di.AppModule.provideChatRepository(context),
                                        currentScreen.threadId,
                                        currentScreen.address
                                    )
                                }
                                tn.keeftalk.chat.ui.screens.SmsDetailScreen(
                                    viewModel = smsDetailViewModel,
                                    onBack = { onScreenChange(AppScreen.ChatList) }
                                )
                            }
                            is AppScreen.NewSms -> {
                                val newSmsViewModel: tn.keeftalk.chat.ui.screens.NewSmsViewModel = viewModel(key = currentScreen.phoneNumber ?: "new_sms") {
                                    tn.keeftalk.chat.ui.screens.NewSmsViewModel(
                                        context.applicationContext as android.app.Application,
                                        tn.keeftalk.chat.di.AppModule.provideChatRepository(context),
                                        currentScreen.phoneNumber
                                    )
                                }
                                tn.keeftalk.chat.ui.screens.NewSmsScreen(
                                    viewModel = newSmsViewModel,
                                    onBack = { onScreenChange(AppScreen.ChatList) },
                                    onSmsThreadSelected = { threadId, address ->
                                        onScreenChange(AppScreen.SmsDetail(threadId, address))
                                    }
                                )
                            }
                            is AppScreen.SharePicker -> {
                                tn.keeftalk.chat.ui.screens.chatdetail.SharePickerScreen(
                                    chatId = currentScreen.chatId,
                                    onBack = { 
            onScreenChange(AppScreen.ChatList)
            onShowAttachmentsChange(currentScreen.chatId)
        },
                                    onShareContent = { category ->
                                        when (category) {
                                            tn.keeftalk.chat.ui.screens.chatdetail.ShareCategory.NOTES -> onScreenChange(AppScreen.ShareNotePicker(currentScreen.chatId))
                                            tn.keeftalk.chat.ui.screens.chatdetail.ShareCategory.EMAILS -> onScreenChange(AppScreen.ShareEmailPicker(currentScreen.chatId))
                                            tn.keeftalk.chat.ui.screens.chatdetail.ShareCategory.VAULT -> onScreenChange(AppScreen.ShareVaultPicker(currentScreen.chatId))
                                            tn.keeftalk.chat.ui.screens.chatdetail.ShareCategory.AGENDA -> onScreenChange(AppScreen.ShareAgendaPicker(currentScreen.chatId))
                                        }
                                    }
                                )
                            }
                            is AppScreen.ShareNotePicker -> {
                                val noteRepo = AppModule.provideNoteRepository(context)
                                val notes by noteRepo.getNotes("All", "").collectAsState(emptyList())
                                tn.keeftalk.chat.ui.screens.chatdetail.NotePicker(
                                    notes = notes,
                                    onBack = { onScreenChange(AppScreen.SharePicker(currentScreen.chatId)) },
                                    onNoteSelected = { note, access, invite ->
                                        scope.launch {
                                            AppModule.provideChatRepository(context).shareNoteToChat(note.id, currentScreen.chatId, access, invite)
                                            onScreenChange(AppScreen.ChatList)
                                        }
                                    }
                                )
                            }
                            is AppScreen.ShareEmailPicker -> {
                                val emailRepo = AppModule.provideEmailRepository(context)
                                val emailsState = emailRepo.getMergedInbox().collectAsLazyPagingItems()
                                
                                tn.keeftalk.chat.ui.screens.chatdetail.EmailPicker(
                                    emails = emailsState,
                                    onBack = { onScreenChange(AppScreen.SharePicker(currentScreen.chatId)) },
                                    onEmailSelected = { email ->
                                        scope.launch {
                                            AppModule.provideChatRepository(context).shareEmailToChat(email.id, currentScreen.chatId)
                                            onScreenChange(AppScreen.ChatList)
                                        }
                                    }
                                )
                            }
                            is AppScreen.ShareVaultPicker -> {
                                val vaultRepo = AppModule.provideVaultRepository(context)
                                val vaultItems by vaultRepo.getItems().collectAsState(emptyList())
                                tn.keeftalk.chat.ui.screens.chatdetail.VaultPicker(
                                    items = vaultItems,
                                    onBack = { onScreenChange(AppScreen.SharePicker(currentScreen.chatId)) },
                                    onItemSelected = { item ->
                                        scope.launch {
                                            AppModule.provideChatRepository(context).shareVaultFileToChat(item.id, currentScreen.chatId)
                                            onScreenChange(AppScreen.ChatList)
                                        }
                                    }
                                )
                            }
                            is AppScreen.ShareAgendaPicker -> {
                                val calendarRepo = AppModule.provideCalendarRepository(context)
                                val agendaItems by calendarRepo.getAllItems().collectAsState(emptyList())
                                tn.keeftalk.chat.ui.screens.chatdetail.AgendaPicker(
                                    items = agendaItems,
                                    onBack = { onScreenChange(AppScreen.SharePicker(currentScreen.chatId)) },
                                    onItemSelected = { item ->
                                        scope.launch {
                                            AppModule.provideChatRepository(context).shareAgendaToChat(item.id, currentScreen.chatId)
                                            onScreenChange(AppScreen.ChatList)
                                        }
                                    }
                                )
                            }
                            is AppScreen.LocationPicker -> {
                                tn.keeftalk.chat.ui.screens.LocationPickerScreen(
                                    onBack = { 
            onScreenChange(AppScreen.ChatList)
            onShowAttachmentsChange(currentScreen.chatId)
        },
                                    onLocationShared = { latitude, longitude, isLive, duration ->
                                        scope.launch {
                                            if (isLive) {
                                                mainViewModel.chatRepository.sendMessage(
                                                    currentScreen.chatId,
                                                    "Live location started ($duration)",
                                                    tn.keeftalk.chat.domain.model.MessageType.LOCATION,
                                                    "geo:$latitude,$longitude?live=true&duration=$duration"
                                                )
                                            } else {
                                                mainViewModel.chatRepository.sendMessage(
                                                    currentScreen.chatId,
                                                    "Shared a location",
                                                    tn.keeftalk.chat.domain.model.MessageType.LOCATION,
                                                    "geo:$latitude,$longitude"
                                                )
                                            }
                                            navigator.navigateBack()
                                        }
                                    }
                                )
                            }
                            is AppScreen.LocationDetail -> {
                                tn.keeftalk.chat.ui.screens.LocationDetailScreen(
                                    latitude = currentScreen.latitude,
                                    longitude = currentScreen.longitude,
                                    title = currentScreen.title,
                                    isLive = currentScreen.isLive,
                                    onBack = { scope.launch { navigator.navigateBack() } }
                                )
                            }
                            is AppScreen.Feed -> {
                                tn.keeftalk.chat.ui.feed.FeedScreen(
                                    onManageFeeds = { onScreenChange(AppScreen.ManageFeeds) },
                                    onOpenUrlInBrowser = { url -> onScreenChange(AppScreen.Browser(url)) }
                                )
                            }
                            is AppScreen.ManageFeeds -> {
                                tn.keeftalk.chat.ui.feed.ManageFeedsScreen(
                                    onBack = { onScreenChange(AppScreen.Feed) },
                                    onExploreCurated = { onScreenChange(AppScreen.ExploreFeeds) }
                                )
                            }
                            is AppScreen.ExploreFeeds -> {
                                tn.keeftalk.chat.ui.feed.ExploreFeedsScreen(
                                    onBack = { onScreenChange(AppScreen.ManageFeeds) }
                                )
                            }
                            is AppScreen.Books -> {
                                tn.keeftalk.chat.ui.books.BooksScreen(
                                    onSettings = { onScreenChange(AppScreen.Settings) },
                                    notifications = notifications,
                                    onNotificationClick = { mainViewModel.handleNotificationClick(it) },
                                    onMarkAllNotificationsAsRead = { mainViewModel.markAllNotificationsAsRead() },
                                    fabActionFlow = fabActionFlow,
                                    onReaderStateChanged = { isReaderOpen = it },
                                    onDetailStatsStateChanged = { isBookDetailStatsOpen = it }
                                )
                            }
                            is AppScreen.Market -> {
                                tn.keeftalk.chat.ui.market.MarketScreen(
                                    onSettings = { onScreenChange(AppScreen.Settings) },
                                    notifications = notifications,
                                    onNotificationClick = { mainViewModel.handleNotificationClick(it) },
                                    onMarkAllNotificationsAsRead = { mainViewModel.markAllNotificationsAsRead() },
                                    searchQuery = searchQuery
                                )
                            }
                            is AppScreen.Habits -> {
                                tn.keeftalk.chat.ui.habits.HabitsScreen(
                                    onSettings = { onScreenChange(AppScreen.Settings) },
                                    notifications = notifications,
                                    onNotificationClick = { mainViewModel.handleNotificationClick(it) },
                                    onMarkAllNotificationsAsRead = { mainViewModel.markAllNotificationsAsRead() },
                                    fabActionFlow = fabActionFlow,
                                    onNavigateToBooks = { _ -> onScreenChange(AppScreen.Books) }
                                )
                            }
                            is AppScreen.Todo -> {
                                tn.keeftalk.chat.ui.screens.todo.TodoScreen(
                                    viewModel = viewModel { tn.keeftalk.chat.ui.screens.todo.TodoViewModel(context.applicationContext as android.app.Application) }
                                )
                            }
                            is AppScreen.Browser -> {
                                tn.keeftalk.chat.ui.browser.BrowserScreen(
                                    browserSettings = userPrefs.browserSettings,
                                    initialUrl = currentScreen.initialUrl,
                                    onBack = { onScreenChange(AppScreen.ChatList) },
                                    onSettingsClick = { onScreenChange(AppScreen.BrowserSettings) },
                                    fabActionFlow = fabActionFlow
                                )
                            }
                            is AppScreen.BrowserSettings -> {
                                val settingsViewModel: SettingsViewModel = viewModel {
                                    SettingsViewModel(
                                        AppModule.provideUserPreferencesRepository(context),
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.providePrivacyRepository(context),
                                        AppModule.provideSecurityRepository(context),
                                        AppModule.provideSettingsRepository(context)
                                    )
                                }
                                tn.keeftalk.chat.ui.settings.BrowserSettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { onScreenChange(AppScreen.Browser()) }
                                )
                            }
                            is AppScreen.KeefHome -> {
                                val envState by mainViewModel.environmentalState.collectAsState()
                                val unreadCount by mainViewModel.unreadCount.collectAsState()
                                val profileViewModel: ProfileViewModel = viewModel {
                                    ProfileViewModel(
                                        mainViewModel.authRepository,
                                        mainViewModel.chatRepository,
                                        AppModule.provideCountryService(context),
                                        AppModule.providePhoneNumberService(context),
                                        AppModule.provideCallLogManager(context),
                                        AppModule.provideSmsRepository(context),
                                        AppModule.provideRelationshipRepository(context),
                                        null
                                    )
                                }
                                val profile by profileViewModel.profile.collectAsState()

                                KeefHomeView(
                                    profile = profile,
                                    environmentalState = envState,
                                    unreadMessagesCount = unreadCount,
                                    activeTasksCount = 3,
                                    streakDays = 12,
                                    onNavigate = { screen -> onScreenChange(screen) },
                                    onOpenSettings = { onScreenChange(AppScreen.Settings) }
                                )
                            }
                        }
                    }
                }

                val activeNavTabs = if (customization.isEnabled) {
                    customization.bottomNavTabs
                } else {
                    listOf(
                        tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS,
                        tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS,
                        tn.keeftalk.chat.domain.model.KeeftalkModule.SMS,
                        tn.keeftalk.chat.domain.model.KeeftalkModule.BOOKS,
                        tn.keeftalk.chat.domain.model.KeeftalkModule.MARKET,
                        tn.keeftalk.chat.domain.model.KeeftalkModule.VAULT
                    )
                }
                val isBottomBarVisible = isListVisible && activeNavTabs.size > 1 && !(currentScreen is AppScreen.Books && (isReaderOpen || isBookDetailStatsOpen)) && (
                    currentScreen is AppScreen.ChatList || activeNavTabs.any { it.toAppScreen()::class == currentScreen::class }
                ) && !customization.isMinimalMode
                
                // PERFORMANCE: Sync native shell visibility
                LaunchedEffect(isDetailVisible, currentScreen, userPrefs.isLogged, isHydrated) {
                    val shell = (context as FragmentActivity).findViewById<android.view.View>(R.id.native_fragment_shell)
                    val isLocked = mainViewModel.userPreferencesRepository.isAppLockEnabledFast()

                    // Hide shell once hydrated to allow Compose layer to handle interactions
                    shell?.visibility = if (userPrefs.isLogged && !isLocked && !isDetailVisible && currentScreen == AppScreen.ChatList && !isHydrated) {
                        android.view.View.VISIBLE
                    } else {
                        android.view.View.GONE
                    }
                }

                val showFab = when (currentScreen) {
                    AppScreen.ChatList -> isListVisible // Show if list is part of the scaffold
                    AppScreen.DisplaySettings -> true
                    AppScreen.Feed -> true
                    AppScreen.Books -> !isReaderOpen && !isBookDetailStatsOpen
                    AppScreen.Market -> true
                    AppScreen.Habits -> true
                    AppScreen.Todo -> true
                    is AppScreen.Browser -> false
                    is AppScreen.BrowserSettings -> false
                    is AppScreen.Notes -> !isDetailShown
                    AppScreen.Calendar -> true
                    is AppScreen.CalendarEditor -> true
                    AppScreen.Vault -> true
                    AppScreen.Secrets -> true
                    AppScreen.SecretsSettings -> false
                    AppScreen.Wallet -> true
                    AppScreen.Finances -> true
                    AppScreen.Email -> true
                    is AppScreen.LocationPicker -> false
                    is AppScreen.LocationDetail -> false
                    is AppScreen.SharePicker -> false
                    is AppScreen.ShareNotePicker -> false
                    is AppScreen.ShareEmailPicker -> false
                    is AppScreen.ShareVaultPicker -> false
                    is AppScreen.ShareAgendaPicker -> false
                    else -> false
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showFab,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            bottom = if (currentScreen is AppScreen.Todo) 78.dp else 16.dp,
                            end = 16.dp
                        )
                ) {
                    val emailRepo = remember { AppModule.provideEmailRepository(context) }
                    val emailAccounts by emailRepo.getAccounts().collectAsState(initial = emptyList())

                    tn.keeftalk.chat.ui.components.AdaptiveFab(
                        currentScreen = currentScreen,
                        bottomTab = bottomTab,
                        chatTab = chatTab,
                        emailAccounts = emailAccounts,
                        isSearching = isSearching,
                        onNavigate = { screen: AppScreen -> onScreenChange(screen) },
                        onBottomTabChange = { tab -> bottomTab = tab },
                        onAction = { action -> 
                            tn.keeftalk.chat.util.NotesLogger.d("UI", "AdaptiveFab: onAction $action")
                            scope.launch { fabActionFlow.emit(action) }
                        },
                        showBottomBar = isBottomBarVisible
                    )
                }

                if (selectedChatForMenu != null) {
                    ChatContextMenu(
                        chat = selectedChatForMenu!!,
                        onAction = { action ->
                            scope.launch {
                                when (action) {
                                    ChatAction.PIN -> mainViewModel.chatRepository.togglePinChat(selectedChatForMenu!!.id, !selectedChatForMenu!!.isPinned)
                                    ChatAction.MUTE -> mainViewModel.chatRepository.toggleMuteChat(selectedChatForMenu!!.id, !selectedChatForMenu!!.isMuted)
                                    ChatAction.ARCHIVE -> mainViewModel.chatRepository.archiveChat(selectedChatForMenu!!.id, !selectedChatForMenu!!.isArchived)
                                    ChatAction.MARK_AS_READ -> mainViewModel.chatRepository.markAsRead(selectedChatForMenu!!.id)
                                    ChatAction.CLEAR_HISTORY -> mainViewModel.chatRepository.clearChat(selectedChatForMenu!!.id)
                                    ChatAction.DELETE -> mainViewModel.chatRepository.deleteChat(selectedChatForMenu!!.id)
                                    ChatAction.FAVORITE -> mainViewModel.chatRepository.toggleFavorite(selectedChatForMenu!!.id, !selectedChatForMenu!!.isFavorite)
                                }
                                selectedChatForMenu = null
                            }
                        },
                        onDismiss = { selectedChatForMenu = null }
                    )
                }

                if (showFabSearchDialog) {
                    AlertDialog(
                        onDismissRequest = { showFabSearchDialog = false; fabSearchQuery = "" },
                        title = { Text("Search") },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = fabSearchQuery,
                                    onValueChange = { fabSearchQuery = it },
                                    placeholder = { Text("Search...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (fabSearchQuery.isEmpty()) "Type to search..." else "Searching for \"$fabSearchQuery\"...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showFabSearchDialog = false
                                fabSearchQuery = ""
                            }) {
                                Text("Search")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showFabSearchDialog = false; fabSearchQuery = "" }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Parcelize
sealed class AppScreen : Parcelable {
    @Parcelize
    data object ChatList : AppScreen()
    @Parcelize
    data object KeefHome : AppScreen()
    @Parcelize
    data object NewGroup : AppScreen()
    @Parcelize
    data object Discovery : AppScreen()
    @Parcelize
    data object MyProfile : AppScreen()
    @Parcelize
    data class OtherProfile(val userId: String) : AppScreen()
    @Parcelize
    data object Settings : AppScreen()
    @Parcelize
    data object AccountSettings : AppScreen()
    @Parcelize
    data object About : AppScreen()
    @Parcelize
    data object Help : AppScreen()
    @Parcelize
    data object ArchivedChats : AppScreen()
    @Parcelize
    data class Notes(val noteId: String? = null) : AppScreen()
    @Parcelize
    data class NoteEditor(val noteId: String? = null) : AppScreen()
    @Parcelize
    data object Vault : AppScreen()
    @Parcelize
    data object Secrets : AppScreen()
    @Parcelize
    data object QrCode : AppScreen()
    @Parcelize
    data object Calendar : AppScreen()
    @Parcelize
    data class CalendarEditor(val type: tn.keeftalk.chat.domain.model.calendar.CalendarItemType) : AppScreen()
    @Parcelize
    data object Wallet : AppScreen()
    @Parcelize
    data object Finances : AppScreen()
    @Parcelize
    data object FinancesSettings : AppScreen()
    @Parcelize
    data object Email : AppScreen()
    @Parcelize
    data object Books : AppScreen()
    @Parcelize
    data object Market : AppScreen()
    @Parcelize
    data object Habits : AppScreen()
    @Parcelize
    data object Todo : AppScreen()
    @Parcelize
    data class Browser(val initialUrl: String? = null) : AppScreen()
    @Parcelize
    data object BrowserSettings : AppScreen()
    @Parcelize
    data class ComposeEmail(
        val to: String? = null,
        val subject: String? = null,
        val initialBody: String? = null
    ) : AppScreen()
    @Parcelize
    data class EmailDetail(val messageId: String) : AppScreen()
    @Parcelize
    data object EmailSettings : AppScreen()
    @Parcelize
    data object NotificationSettings : AppScreen()
    @Parcelize
    data object ProfileViewHistory : AppScreen()
    @Parcelize
    data object BlockedUsers : AppScreen()
    @Parcelize
    data object ChatSettings : AppScreen()
    @Parcelize
    data object CallSettings : AppScreen()
    @Parcelize
    data object NoteSettings : AppScreen()
    @Parcelize
    data object VaultSettings : AppScreen()
    @Parcelize
    data object SecretsSettings : AppScreen()
    @Parcelize
    data object AutomaticBackupSettings : AppScreen()
    @Parcelize
    data object CalendarSettings : AppScreen()
    @Parcelize
    data object HabitSettings : AppScreen()
    @Parcelize
    data object ParentalControls : AppScreen()
    @Parcelize
    data object AppCustomization : AppScreen()
    @Parcelize
    data object ChatAppearanceSettings : AppScreen()
    @Parcelize
    data object ChatTextAccessibilitySettings : AppScreen()
    @Parcelize
    data object ChatMediaDownloadsSettings : AppScreen()
    @Parcelize
    data object ChatBehaviorSettings : AppScreen()
    @Parcelize
    data object ChatStorageCacheSettings : AppScreen()
    @Parcelize
    data object ChatCleanupSettings : AppScreen()
    @Parcelize
    data object DisplaySettings : AppScreen()
    @Parcelize
    data class ChatNotificationSettings(val chatId: String) : AppScreen()
    @Parcelize
    data class MediaViewer(
        val initialMessageId: String,
        val chatId: String,
        val initialPlaybackPosition: Long = 0L
    ) : AppScreen()
    @Parcelize
    data class PdfViewer(val messageId: String, val chatId: String) : AppScreen()
    @Parcelize
    data class CodeViewer(val messageId: String, val chatId: String) : AppScreen()
    @Parcelize
    data class MediaPicker(val chatId: String) : AppScreen()
    @Parcelize
    data class FileReview(
        val chatId: String,
        val selectedMedia: List<tn.keeftalk.chat.domain.model.MediaItem> = emptyList(),
        val selectedDocs: List<tn.keeftalk.chat.domain.model.DocumentModel> = emptyList()
    ) : AppScreen()
    @Parcelize
    data object SmsList : AppScreen()
    @Parcelize
    data class NewSms(val phoneNumber: String? = null) : AppScreen()
    @Parcelize
    data class SmsDetail(val threadId: Long, val address: String) : AppScreen()
    @Parcelize
    data object ForgotPassword : AppScreen()
    @Parcelize
    data object CreateNewPassword : AppScreen()
    @Parcelize
    data object ResetSuccess : AppScreen()
    @Parcelize
    data class ContactDetail(val userId: String) : AppScreen()
    @Parcelize
    data object AddContact : AppScreen()
    @Parcelize
    data class LocationPicker(val chatId: String) : AppScreen()
    @Parcelize
    data class SharePicker(val chatId: String) : AppScreen()
    @Parcelize
    data class ShareNotePicker(val chatId: String) : AppScreen()
    @Parcelize
    data class ShareEmailPicker(val chatId: String) : AppScreen()
    @Parcelize
    data class ShareVaultPicker(val chatId: String) : AppScreen()
    @Parcelize
    data class ShareAgendaPicker(val chatId: String) : AppScreen()
    @Parcelize
    data class LocationDetail(val latitude: Double, val longitude: Double, val title: String? = null, val isLive: Boolean = false) : AppScreen()
    @Parcelize
    data class ConnectionPath(val userId: String) : AppScreen()
    @Parcelize
    data object Feed : AppScreen()
    @Parcelize
    data object ManageFeeds : AppScreen()
    @Parcelize
    data object ExploreFeeds : AppScreen()
    @Parcelize
    data object TestScreensPreview : AppScreen()
    @Parcelize
    data object SubscriptionPlans : AppScreen()
}

@SuppressLint("MissingPermission")
private fun handleAttachmentAction(
    type: AttachmentType,
    chatId: String,
    pickFileLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    pickContactLauncher: androidx.activity.result.ActivityResultLauncher<Void?>,
    onScreenChange: (AppScreen) -> Unit
) {
    when (type) {
        AttachmentType.PICTURE -> {
            onScreenChange(AppScreen.MediaPicker(chatId))
        }
        AttachmentType.SHARE -> {
            onScreenChange(AppScreen.SharePicker(chatId))
        }
        AttachmentType.FILE -> {
            pickFileLauncher.launch(arrayOf("*/*"))
        }
        AttachmentType.APK -> {
            pickFileLauncher.launch(arrayOf("application/vnd.android.package-archive"))
        }
        AttachmentType.VOICE_FILE -> {
            pickFileLauncher.launch(arrayOf("audio/*"))
        }
        AttachmentType.CONTACT -> {
            pickContactLauncher.launch(null)
        }
        AttachmentType.LOCATION -> {
            onScreenChange(AppScreen.LocationPicker(chatId))
        }
        else -> {}
    }
}
