package tn.keeftalk.chat.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import tn.keeftalk.chat.domain.model.UserBrowserSettings
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun BrowserPageView(
    viewModel: BrowserViewModel,
    browserSettings: UserBrowserSettings,
    onOpenSiteInfo: () -> Unit
) {
    val tabs by viewModel.tabs.collectAsState()
    val curIdx by viewModel.currentTabIndex.collectAsState()
    val isFindOpen by viewModel.isFindInPageOpen.collectAsState()
    val findQuery by viewModel.findQuery.collectAsState()
    val isReader by viewModel.isReaderMode.collectAsState()

    val currentTab = tabs.getOrNull(curIdx)
    val currentUrl = currentTab?.currentUrl ?: "https://www.google.com"

    var webProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val isSecure = !currentUrl.startsWith("http://")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    if (currentTab?.canGoBack == true) {
                        viewModel.goBackInTab()
                    } else {
                        viewModel.openHome()
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF16181d))
            }

            IconButton(
                onClick = { viewModel.goForwardInTab() },
                enabled = currentTab?.canGoForward == true,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (currentTab?.canGoForward == true) Color(0xFF16181d) else Color(0xFF16181d).copy(alpha = 0.3f)
                )
            }

            // Site URL button & Address Editor
            var isEditingUrl by remember { mutableStateOf(false) }
            var editingUrlText by remember(currentUrl) { mutableStateOf(currentUrl) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFf0f1f4))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Tapping the lock/warning icon specifically opens Site Information
                    IconButton(
                        onClick = { onOpenSiteInfo() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Warning,
                            contentDescription = if (isSecure) "Connection is secure" else "Not secure",
                            tint = if (isSecure) Color(0xFF168a4a) else Color(0xFFd12e2e),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (!isEditingUrl) {
                                    isEditingUrl = true
                                    editingUrlText = currentUrl
                                }
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (isEditingUrl) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = editingUrlText,
                                onValueChange = { editingUrlText = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color(0xFF16181d),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF0a7cff)),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onGo = {
                                        if (editingUrlText.isNotBlank()) {
                                            viewModel.navigateToUrl(editingUrlText)
                                            isEditingUrl = false
                                        }
                                    }
                                )
                            )
                        } else {
                            Text(
                                text = if (currentUrl.startsWith("search.keeftalk.app?q=")) {
                                    "Search: " + viewModel.titleOf(currentUrl).removePrefix("Search: ")
                                } else {
                                    currentUrl
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF16181d),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = { webViewRef?.reload() ?: viewModel.selectTab(curIdx) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color(0xFF16181d))
            }
        }

        // Find In Page Bar
        if (isFindOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFf6f7f9))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = findQuery,
                    onValueChange = {
                        viewModel.findQuery.value = it
                        if (it.isNotBlank()) {
                            webViewRef?.findAllAsync(it)
                        } else {
                            webViewRef?.clearMatches()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    placeholder = { Text("Find in page", fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFd9dbe1),
                        unfocusedBorderColor = Color(0xFFd9dbe1)
                    )
                )

                IconButton(
                    onClick = { webViewRef?.findNext(false) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
                }
                IconButton(
                    onClick = { webViewRef?.findNext(true) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
                }
                IconButton(
                    onClick = {
                        viewModel.isFindInPageOpen.value = false
                        viewModel.findQuery.value = ""
                        webViewRef?.clearMatches()
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close find")
                }
            }
        }

        val FabCyan = Color(0xFF00CCCC)
        val FabPurple = Color(0xFF7D5CFF)
        val FabGradient = Brush.linearGradient(listOf(FabCyan, FabPurple))

        // Progress Bar under topbar / url field
        if (isLoading || webProgress < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Transparent)
            ) {
                val animProgress by animateFloatAsState(
                    targetValue = if (isLoading) webProgress.coerceAtLeast(0.15f) else 1f,
                    animationSpec = tween(250),
                    label = "webProgressAnim"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animProgress)
                        .background(FabGradient)
                )
            }
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }

        HorizontalDivider(color = Color(0xFFe6e7ea), thickness = 1.dp)

        // Page Content Body with smooth fade-in transition
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 60.dp)
        ) {
            AnimatedContent(
                targetState = currentUrl,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing))
                        .togetherWith(fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)))
                },
                label = "webpageFadeTransition",
                modifier = Modifier.fillMaxSize()
            ) { url ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        url.contains("weather.keeftalk.app") -> {
                            WeatherForecastPageView(
                                viewModel = viewModel,
                                browserSettings = browserSettings
                            )
                        }

                        url.startsWith("search.keeftalk.app?q=") -> {
                            // Search Results Native Listing
                            val queryEncoded = url.substringAfter("q=")
                            val query = URLDecoder.decode(queryEncoded, "UTF-8")
                            SearchResultsView(
                                query = query,
                                engine = browserSettings.searchEngine,
                                onResultClick = { targetUrl -> viewModel.navigateToUrl(targetUrl) }
                            )
                        }

                        url.endsWith(".keeftalk.app") || url.contains(".example") -> {
                            // Mock Preview Page View
                            MockPagePreviewView(
                                url = url,
                                viewModel = viewModel,
                                isReader = isReader
                            )
                        }

                        else -> {
                            // Actual Android WebView
                            @SuppressLint("SetJavaScriptEnabled")
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                        settings.javaScriptEnabled = browserSettings.javaScriptEnabled
                                        settings.domStorageEnabled = true
                                        settings.useWideViewPort = true
                                        settings.loadWithOverviewMode = true
                                        settings.userAgentString = if (browserSettings.desktopMode) {
                                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
                                        } else {
                                            settings.userAgentString
                                        }

                                        webViewClient = object : WebViewClient() {
                                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                super.onPageStarted(view, url, favicon)
                                                isLoading = true
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                isLoading = false
                                                webProgress = 1f
                                            }

                                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                request?.url?.toString()?.let { newUrl ->
                                                    viewModel.navigateToUrl(newUrl)
                                                    return true
                                                }
                                                return false
                                            }
                                        }

                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                webProgress = newProgress / 100f
                                            }
                                        }

                                        setDownloadListener { downloadUrl, _, contentDisposition, mimeType, _ ->
                                            viewModel.startDownload(downloadUrl, contentDisposition, mimeType, ctx)
                                        }

                                        loadUrl(if (url.startsWith("http")) url else "https://$url")
                                        webViewRef = this
                                    }
                                },
                                update = { webView ->
                                    val target = if (url.startsWith("http")) url else "https://$url"
                                    if (webView.url != target) {
                                        webView.loadUrl(target)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Smooth appearance / loading veil to completely prevent any white or black flash
                    var isAppearing by remember { mutableStateOf(true) }
                    androidx.compose.runtime.LaunchedEffect(url) {
                        isAppearing = true
                        kotlinx.coroutines.delay(280L)
                        isAppearing = false
                    }

                    val veilAlpha by animateFloatAsState(
                        targetValue = if (isLoading || isAppearing) 1f else 0f,
                        animationSpec = tween(300),
                        label = "veilAlpha"
                    )

                    if (veilAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(alpha = veilAlpha)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF070c24),
                                            Color(0xFF12183c)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsView(
    query: String,
    engine: String,
    onResultClick: (String) -> Unit
) {
    val slug = query.lowercase().replace(Regex("\\W+"), "-").take(24)
    val domains = listOf("wiki", "guide", "news", "forum", "shop")
    val titles = listOf("the basics explained", "a practical guide", "latest updates", "what people are saying", "where to buy")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Results from $engine", fontSize = 13.sp, color = Color(0xFF6b6e76))
        Text("Results for “$query”", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16181d))

        domains.forEachIndexed { i, domain ->
            val link = "${if (domain == "forum") "http://" else "https://"}$domain.example/$slug"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFf3f4f7))
                    .clickable { onResultClick(link) }
                    .padding(12.dp, 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(link, fontSize = 12.sp, color = Color(0xFF0a7cff))
                Text("$query — ${titles[i]}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF16181d))
                Text("Everything you need to know about $query, collected from trusted sources.", fontSize = 13.sp, color = Color(0xFF555a63))
            }
        }
    }
}

@Composable
private fun MockPagePreviewView(
    url: String,
    viewModel: BrowserViewModel,
    isReader: Boolean
) {
    val title = viewModel.titleOf(url)
    val host = viewModel.hostOf(url)
    val hash = viewModel.hash(url)
    val colors = listOf(
        Pair(Color(0xFFff9a3c), Color(0xFFff4f7b)),
        Pair(Color(0xFF2d9bf0), Color(0xFF18a999)),
        Pair(Color(0xFF7a5cff), Color(0xFFe0447a))
    )
    val gradColors = colors[hash % colors.size]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (!isReader) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Brush.linearGradient(listOf(gradColors.first, gradColors.second)))
                    .padding(18.dp, 24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = title,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        Column(
            modifier = Modifier.padding(if (isReader) 28.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (hash % 3 == 0) "A calm, well-organised place to find what you came for."
                else if (hash % 3 == 1) "Fresh stories, useful guides and simple tools, updated every day."
                else "Built for people who like clear answers and quick reads.",
                fontSize = if (isReader) 22.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16181d)
            )

            Text(
                text = "Welcome to $title. This is a preview page, so links stay inside this tab and build your back and forward history.",
                fontSize = if (isReader) 18.sp else 14.sp,
                fontFamily = if (isReader) FontFamily.Serif else FontFamily.Default,
                color = Color(0xFF40434b),
                lineHeight = if (isReader) 28.sp else 22.sp
            )

            Text(
                text = "Save it to your quick links with the star, or add it to your reading list from the menu.",
                fontSize = if (isReader) 18.sp else 14.sp,
                fontFamily = if (isReader) FontFamily.Serif else FontFamily.Default,
                color = Color(0xFF40434b),
                lineHeight = if (isReader) 28.sp else 22.sp
            )

            if (!isReader) {
                Text("Keep exploring", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16181d), modifier = Modifier.padding(top = 8.dp))

                listOf("featured", "popular", "new").forEach { section ->
                    val sectionUrl = "https://$host/$section"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFf3f4f7))
                            .clickable { viewModel.navigateToUrl(sectionUrl) }
                            .padding(12.dp, 14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("$host/$section", fontSize = 12.sp, color = Color(0xFF0a7cff))
                        Text("$title · $section", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF16181d))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherForecastPageView(
    viewModel: BrowserViewModel,
    browserSettings: UserBrowserSettings
) {
    val weatherInfo by viewModel.weatherInfo.collectAsState()
    val isWeatherExpanded by viewModel.isWeatherExpanded.collectAsState()
    val weatherDayIndex by viewModel.weatherDayIndex.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("weather.keeftalk.app", fontSize = 12.sp, color = Color(0xFF0a7cff))
        Text("Live Weather & Forecast", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16181d))

        tn.keeftalk.chat.ui.feed.components.WeatherCardExact(
            weather = weatherInfo,
            isExpanded = isWeatherExpanded,
            selectedDayIndex = weatherDayIndex,
            onToggleExpand = { viewModel.toggleWeatherExpand() },
            onSelectDay = { viewModel.setWeatherDayIndex(it) },
            onOpenDetail = { viewModel.showToast("Weather details (${browserSettings.unit})") },
            onOpenLocationSettings = { viewModel.showToast("Opening location settings") }
        )
    }
}
