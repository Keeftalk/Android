package tn.keeftalk.chat.ui.browser

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val viewModel = remember { BrowserViewModel(context.applicationContext as Application) }

    LaunchedEffect(browserSettings) {
        viewModel.updateSettings(browserSettings)
    }

    LaunchedEffect(initialUrl) {
        initialUrl?.takeIf { it.isNotBlank() }?.let { url ->
            viewModel.navigateToUrl(url)
        }
    }

    val period by viewModel.period.collectAsState()
    val isNight = period == "night"
    val rootBg = if (isNight) Color.Black else Color.White
    val weatherCondition by viewModel.weatherCondition.collectAsState()
    val cloudDensity by viewModel.cloudDensity.collectAsState()
    val windSpeed by viewModel.windSpeed.collectAsState()
    val simulatedDate by viewModel.simulatedDate.collectAsState()
    val currentTabIndex by viewModel.currentTabIndex.collectAsState()
    val lastTabIndex by viewModel.lastTabIndex.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()

    val isTabsOverlayOpen by viewModel.isTabsOverlayOpen.collectAsState()
    val isHistoryOpen by viewModel.isHistorySheetOpen.collectAsState()
    val isDownloadsOpen by viewModel.isDownloadsSheetOpen.collectAsState()
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
            .background(rootBg)
            .threeButtonNavigationPadding()
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
                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))).togetherWith(
                    fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
                )
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

        // Downloads Bottom Sheet
        if (isDownloadsOpen) {
            DownloadsBottomSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.isDownloadsSheetOpen.value = false }
            )
        }

        // Persistent Bottom Bar across both Home and Webpages (sustained without disappearing/reappearing)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .threeButtonNavigationPadding()
                .padding(bottom = 6.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val barBg = if (isNight) Color(0xFF0e1330).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.88f)
            val cardBorder = if (isNight) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.7f)
            val inkColor = if (isNight) Color(0xFFeef2ff) else Color(0xFF16181d)

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(56.dp), // Increased by 25% (45dp * 1.25 = ~56dp)
                backgroundColor = barBg,
                borderColor = cardBorder,
                cornerRadius = 20.dp,
                enableTouchScale = false
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isHome) {
                                viewModel.goBackInTab()
                            } else {
                                if (currentTab?.canGoBack == true) viewModel.goBackInTab() else viewModel.openHome()
                            }
                        },
                        enabled = !isHome || (currentTab?.canGoBack == true),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (!isHome || currentTab?.canGoBack == true) inkColor else inkColor.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.goForwardInTab() },
                        enabled = currentTab?.canGoForward == true || viewModel.lastTabIndex.value >= 0,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (currentTab?.canGoForward == true || viewModel.lastTabIndex.value >= 0) inkColor else inkColor.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isHome) Color(0xFF0a6fe0) else Color.Transparent)
                            .clickable { viewModel.openHome() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (isHome) Color.White else inkColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.isTabsOverlayOpen.value = true },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Tab, contentDescription = "Tabs", tint = inkColor, modifier = Modifier.size(20.dp))
                            Text(
                                text = "${tabs.size}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkColor
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.isMenuOpen.value = !viewModel.isMenuOpen.value },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = inkColor, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Toast Message Notification Overlay
        AnimatedVisibility(
            visible = toastMsg != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
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

@Composable
fun Modifier.threeButtonNavigationPadding(): Modifier {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return if (bottomInset > 32.dp) {
        this.navigationBarsPadding()
    } else {
        this
    }
}

