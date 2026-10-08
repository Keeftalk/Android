package tn.keeftalk.chat.ui.browser

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import tn.keeftalk.chat.domain.model.UserBrowserSettings
import tn.keeftalk.chat.ui.components.FabActionType

@Composable
fun BrowserScreen(
    browserSettings: UserBrowserSettings = UserBrowserSettings(),
    initialUrl: String? = null,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit = {},
    fabActionFlow: SharedFlow<FabActionType>? = null
) {
    val viewModel = remember { BrowserViewModel() }

    LaunchedEffect(browserSettings) {
        viewModel.updateSettings(browserSettings)
    }

    LaunchedEffect(initialUrl) {
        initialUrl?.takeIf { it.isNotBlank() }?.let { url ->
            viewModel.navigateToUrl(url)
        }
    }

    val period by viewModel.period.collectAsState()
    val weatherCondition by viewModel.weatherCondition.collectAsState()
    val cloudDensity by viewModel.cloudDensity.collectAsState()
    val windSpeed by viewModel.windSpeed.collectAsState()
    val simulatedDate by viewModel.simulatedDate.collectAsState()
    val currentTabIndex by viewModel.currentTabIndex.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()

    val isTabsOverlayOpen by viewModel.isTabsOverlayOpen.collectAsState()
    val isHistoryOpen by viewModel.isHistorySheetOpen.collectAsState()
    val isMenuOpen by viewModel.isMenuOpen.collectAsState()

    var showSiteInfoUrl by remember { mutableStateOf<String?>(null) }

    val currentTab = tabs.getOrNull(currentTabIndex)
    val isHome = viewModel.isHome

    // Back Handler
    BackHandler(enabled = true) {
        when {
            showSiteInfoUrl != null -> showSiteInfoUrl = null
            isHistoryOpen -> viewModel.isHistorySheetOpen.value = false
            isTabsOverlayOpen -> viewModel.isTabsOverlayOpen.value = false
            isMenuOpen -> viewModel.isMenuOpen.value = false
            !isHome -> viewModel.openHome()
            else -> onBack()
        }
    }

    // Auto-clear Toast
    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(1800)
            viewModel.clearToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Dynamic Sky & Weather Background
        SkyBackground(
            period = period,
            condition = weatherCondition,
            cloudDensity = cloudDensity,
            windSpeed = windSpeed,
            effectsEnabled = browserSettings.fx,
            soundEnabled = browserSettings.sound,
            hapticsEnabled = browserSettings.haptic,
            simulatedDate = simulatedDate
        )

        // Main Screen View Switcher (Home View vs Page View)
        AnimatedContent(
            targetState = isHome,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                }
            },
            label = "browserScreenTransition",
            modifier = Modifier.fillMaxSize()
        ) { homeState ->
            if (homeState) {
                BrowserHomeContent(
                    viewModel = viewModel,
                    browserSettings = browserSettings,
                    onClose = onBack,
                    onSettingsClick = onSettingsClick
                )
            } else {
                BrowserPageView(
                    viewModel = viewModel,
                    browserSettings = browserSettings,
                    onOpenSiteInfo = {
                        currentTab?.currentUrl?.let { url ->
                            showSiteInfoUrl = url
                        }
                    }
                )
            }
        }

        // Overlay Dismiss Scrim for Popup Menu
        if (isMenuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewModel.isMenuOpen.value = false }
            )
        }

        // Contextual Menu Popup anchored to Bottom Right
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp, end = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            BrowserMenuPopup(
                viewModel = viewModel,
                onOpenSettings = {
                    viewModel.isMenuOpen.value = false
                    onSettingsClick()
                },
                onOpenSiteInfo = {
                    viewModel.isMenuOpen.value = false
                    currentTab?.currentUrl?.let { url ->
                        showSiteInfoUrl = url
                    }
                }
            )
        }

        // Fullscreen Open Tabs Overlay
        BrowserTabsOverlay(viewModel = viewModel)

        // Site Information Bottom Sheet
        showSiteInfoUrl?.let { siteUrl ->
            SiteInfoBottomSheet(
                url = siteUrl,
                viewModel = viewModel,
                onDismiss = { showSiteInfoUrl = null }
            )
        }

        // History Bottom Sheet
        if (isHistoryOpen) {
            HistoryBottomSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.isHistorySheetOpen.value = false }
            )
        }

        // Toast Message Notification Overlay
        AnimatedVisibility(
            visible = toastMsg != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 104.dp)
        ) {
            toastMsg?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color(0xFF16181d),
                    contentColor = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = msg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}
