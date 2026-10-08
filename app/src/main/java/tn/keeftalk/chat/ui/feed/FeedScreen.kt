package tn.keeftalk.chat.ui.feed

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.FeedArticle
import tn.keeftalk.chat.domain.model.MarketAsset
import tn.keeftalk.chat.ui.feed.components.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun FeedScreen(
    onManageFeeds: () -> Unit = {},
    onOpenUrlInBrowser: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: FeedViewModel = viewModel {
        FeedViewModel(
            AppModule.provideFeedRepository(context),
            AppModule.provideUserPreferencesRepository(context)
        )
    }

    val userPrefs by viewModel.userPrefs.collectAsState()
    val allArticles by viewModel.filteredArticles.collectAsState()
    val pagedArticles by viewModel.pagedArticles.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val isWeatherExpanded by viewModel.isWeatherExpanded.collectAsState()
    val weatherDayIndex by viewModel.weatherDayIndex.collectAsState()
    val prayerDayIndex by viewModel.prayerDayIndex.collectAsState()
    val hijriDateText by viewModel.hijriDateText.collectAsState()
    val prayerTimes by viewModel.prayerTimesFlow.collectAsState()
    val verse by viewModel.verseOfTheDay.collectAsState()
    val marketAssets by viewModel.marketAssets.collectAsState()

    val selectedArticle by viewModel.selectedArticle.collectAsState()
    val selectedMarketAsset by viewModel.selectedMarketAsset.collectAsState()
    val lightboxImages by viewModel.lightboxImages.collectAsState()
    val lightboxIndex by viewModel.lightboxIndex.collectAsState()
    val showSettingsSheet by viewModel.showSettingsSheet.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var showLocationDialog by remember { mutableStateOf(false) }
    var showPrayerLocationDialog by remember { mutableStateOf(false) }
    var showVerseDetail by remember { mutableStateOf(false) }
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)

    // Trigger real GPS/Geocoder coordinate update for Salat times & request permission if needed
    LaunchedEffect(userPrefs.weatherLocationMode, userPrefs.weatherManualLocation, locationPermissionState.status.isGranted) {
        if (userPrefs.weatherLocationMode == "AUTO" && !locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
        viewModel.updateCoordinates(context)
    }

    // Liked and Saved Maps/Sets
    val likedArticlesSet = remember(userPrefs.likedArticleLinksJson) {
        try { Json.decodeFromString<List<String>>(userPrefs.likedArticleLinksJson).toSet() } catch (_: Exception) { emptySet() }
    }
    val savedArticlesSet = remember(userPrefs.savedArticleLinksJson) {
        try { Json.decodeFromString<List<String>>(userPrefs.savedArticleLinksJson).toSet() } catch (_: Exception) { emptySet() }
    }
    val likedSourcesMap = remember(userPrefs.likedSourcesMapJson) {
        try { Json.decodeFromString<Map<String, Int>>(userPrefs.likedSourcesMapJson) } catch (_: Exception) { emptyMap() }
    }
    val mosaicRows = remember(pagedArticles) { buildMosaicRows(pagedArticles) }
    val topLikedSource = remember(likedSourcesMap) { likedSourcesMap.maxByOrNull { it.value } }

    // Reminders Map
    val prayerRemindersMap = remember(userPrefs.prayerRemindersJson) {
        try { Json.decodeFromString<Map<String, Boolean>>(userPrefs.prayerRemindersJson) } catch (_: Exception) {
            mapOf("Fajr" to true, "Dhuhr" to true, "Asr" to true, "Maghrib" to true, "Isha" to true)
        }
    }

    // Adaptive Theme Background Color
    val isSystemDark = isSystemInDarkTheme()
    val isDark = remember(userPrefs.themeMode, isSystemDark) {
        if (userPrefs.themeMode == "DARK") true
        else if (userPrefs.themeMode == "LIGHT") false
        else isSystemDark
    }

    val containerBg = if (userPrefs.themeMode == "KEEF") Color.Transparent else if (isDark) Color(0xFF080A10) else MaterialTheme.colorScheme.background
    val sheetBg = if (userPrefs.themeMode == "KEEF") Color.Transparent else if (isDark) Color(0xFF131722) else Color(0xFFEEF0F7)

    val listState = rememberLazyListState()
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 6 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMoreArticles()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerBg)
    ) {
        // Ambient Radial Background Glows
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1D2336), Color.Transparent),
                    center = Offset(size.width * 0.5f, -size.height * 0.08f),
                    radius = 900.dp.toPx()
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF241B3D), Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 1.1f),
                    radius = 700.dp.toPx()
                )
            )
        }

        // Adaptive Container Sheet Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(sheetBg)
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    if (userPrefs.weatherLocationMode == "AUTO" && !locationPermissionState.status.isGranted) {
                        locationPermissionState.launchPermissionRequest()
                    }
                    viewModel.refresh(context)
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp)
                ) {
                    item {
                        val spotlightManager = remember {
                            tn.keeftalk.chat.util.spotlight.FeatureSpotlightManager(
                                context.applicationContext,
                                tn.keeftalk.chat.di.AppModule.provideUserPreferencesRepository(context.applicationContext),
                                tn.keeftalk.chat.di.AppModule.provideDatabase(context.applicationContext).timeTrackingDao()
                            )
                        }
                        val activeTip by spotlightManager.activeSpotlightTip.collectAsState()

                        activeTip?.let { tip ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tip.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(tip.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = { spotlightManager.dismissActiveTip(tip.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Card Stack (Order & Visibility Driven)
                    item {
                        val cardOrder = userPrefs.feedCardOrder
                        val hiddenCards = userPrefs.feedHiddenCards
                        val layoutMode = userPrefs.feedLayoutMode

                        if (layoutMode == "SWIPE") {
                            // Swipe Cards Sideways
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                cardOrder.filter { !hiddenCards.contains(it) }.forEach { cardId ->
                                    Box(modifier = Modifier.width(320.dp)) {
                                        RenderFeedCard(
                                            cardId = cardId,
                                            weather = weather,
                                            isWeatherExpanded = isWeatherExpanded,
                                            weatherDayIndex = weatherDayIndex,
                                            hijriDateText = hijriDateText,
                                            prayerDayIndex = prayerDayIndex,
                                            prayerTimes = prayerTimes,
                                            verse = verse,
                                            marketAssets = marketAssets,
                                            viewModel = viewModel,
                                            onOpenWeatherLocationSettings = { showLocationDialog = true },
                                            onOpenPrayerLocationSettings = { showPrayerLocationDialog = true },
                                            onOpenVerseDetail = { showVerseDetail = true }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Stacked Mode
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                cardOrder.filter { !hiddenCards.contains(it) }.forEach { cardId ->
                                    RenderFeedCard(
                                        cardId = cardId,
                                        weather = weather,
                                        isWeatherExpanded = isWeatherExpanded,
                                        weatherDayIndex = weatherDayIndex,
                                        hijriDateText = hijriDateText,
                                        prayerDayIndex = prayerDayIndex,
                                        prayerTimes = prayerTimes,
                                        verse = verse,
                                        marketAssets = marketAssets,
                                        viewModel = viewModel,
                                        onOpenWeatherLocationSettings = { showLocationDialog = true },
                                        onOpenPrayerLocationSettings = { showPrayerLocationDialog = true },
                                        onOpenVerseDetail = { showVerseDetail = true }
                                    )
                                }
                            }
                        }
                    }

                    // Top Stories Section Title
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Top stories",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.4).sp
                            )
                            Text(
                                text = "${allArticles.size} live",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Category Chips Row
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                val isSelected = selectedCategory.equals(category, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) Brush.linearGradient(listOf(Color(0xFF5B5BF5), Color(0xFF7C3AED)))
                                            else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                                        )
                                        .clickable { viewModel.setCategory(category) }
                                        .padding(horizontal = 15.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = category,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = (-0.1).sp
                                    )
                                }
                            }
                        }
                    }

                    // Feed Grid Mosaic (Lazy Virtualization)
                    if (isRefreshing && pagedArticles.isEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                FeedMosaicSkeleton()
                            }
                        }
                    } else if (pagedArticles.isEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No stories yet.",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        if (topLikedSource != null && topLikedSource.value > 0) {
                            item(key = "liked_banner_${topLikedSource.key}") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    LikedBannerView(topLikedSource.key, topLikedSource.value)
                                }
                            }
                        }

                        items(
                            items = mosaicRows,
                            key = { row ->
                                when (row) {
                                    is MosaicRowItem.Single -> "single_${row.article.id}"
                                    is MosaicRowItem.Pair -> "pair_${row.left.id}_${row.right.id}"
                                }
                            }
                        ) { row ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                when (row) {
                                    is MosaicRowItem.Single -> {
                                        val isLiked = (likedSourcesMap[row.article.sourceName] ?: 0) > 0
                                        if (row.type == ArticleDisplayType.LIST) {
                                            FeedItemList(article = row.article, isLiked = isLiked, onClick = { viewModel.selectArticle(row.article) })
                                        } else {
                                            FeedItemBig(article = row.article, isLiked = isLiked, onClick = { viewModel.selectArticle(row.article) })
                                        }
                                    }
                                    is MosaicRowItem.Pair -> {
                                        val isLikedLeft = (likedSourcesMap[row.left.sourceName] ?: 0) > 0
                                        val isLikedRight = (likedSourcesMap[row.right.sourceName] ?: 0) > 0
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                FeedItemSquare(article = row.left, isLiked = isLikedLeft, onClick = { viewModel.selectArticle(row.left) })
                                            }
                                            Box(modifier = Modifier.weight(1f)) {
                                                FeedItemSquare(article = row.right, isLiked = isLikedRight, onClick = { viewModel.selectArticle(row.right) })
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (pagedArticles.size < allArticles.size) {
                            item(key = "load_more_footer") {
                                LaunchedEffect(pagedArticles.size) {
                                    viewModel.loadMoreArticles()
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Toast Overlay
            ToastOverlay(
                message = toastMessage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 98.dp)
            )

            // Weather Location Dialog
            if (showLocationDialog) {
                WeatherLocationDialog(
                    currentMode = userPrefs.weatherLocationMode,
                    currentManualLocation = userPrefs.weatherManualLocation ?: "",
                    onDismiss = { showLocationDialog = false },
                    onSave = { mode, loc ->
                        if (mode == "AUTO") locationPermissionState.launchPermissionRequest()
                        viewModel.setWeatherMode(mode)
                        if (mode == "MANUAL") viewModel.setManualLocation(loc)
                        showLocationDialog = false
                    }
                )
            }

            // Prayer Location Dialog
            if (showPrayerLocationDialog) {
                PrayerLocationDialog(
                    currentMode = userPrefs.prayerLocationMode,
                    currentManualLocation = userPrefs.prayerManualLocation ?: "",
                    onDismiss = { showPrayerLocationDialog = false },
                    onSave = { mode, loc ->
                        viewModel.setPrayerLocationSettings(context, mode, loc)
                        showPrayerLocationDialog = false
                    }
                )
            }

            // Market Detail Modal
            selectedMarketAsset?.let { asset ->
                MarketDetailModal(
                    asset = asset,
                    onDismiss = { viewModel.selectMarketAsset(null) }
                )
            }

            // Verse Detail Modal
            if (showVerseDetail) {
                VerseDetailModal(
                    verse = verse,
                    onDismiss = { showVerseDetail = false },
                    onCopy = {
                        val text = "${verse.arabic}\n\n${verse.english}\n— ${verse.reference}"
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Verse", text)
                        clipboard.setPrimaryClip(clip)
                        viewModel.showToast("Verse copied to clipboard")
                    },
                    onShare = {
                        val text = "${verse.arabic}\n\n${verse.english}\n— ${verse.reference}"
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Verse"))
                    }
                )
            }

            // Article Reader Modal
            selectedArticle?.let { article ->
                ArticleReaderModal(
                    article = article,
                    isLiked = likedArticlesSet.contains(article.link),
                    isSaved = savedArticlesSet.contains(article.link),
                    likedSourceCount = likedSourcesMap[article.sourceName] ?: 0,
                    onDismiss = { viewModel.selectArticle(null) },
                    onToggleLike = { viewModel.toggleLikeArticle(article) },
                    onToggleSave = { viewModel.toggleSaveArticle(article) },
                    onOpenImageInLightbox = { imgUrl, list ->
                        viewModel.openLightbox(list, list.indexOf(imgUrl))
                    },
                    onDownloadImage = { imgUrl ->
                        viewModel.showToast("Image download queued")
                    },
                    onOpenUrlInBrowser = { url ->
                        viewModel.selectArticle(null)
                        onOpenUrlInBrowser(url)
                    }
                )
            }

            // Image Lightbox Modal
            if (lightboxImages.isNotEmpty()) {
                ImageLightboxModal(
                    images = lightboxImages,
                    currentIndex = lightboxIndex,
                    onDismiss = { viewModel.closeLightbox() },
                    onNavigate = { idx -> viewModel.setLightboxIndex(idx) },
                    onDownload = { imgUrl -> viewModel.showToast("Image saved") }
                )
            }

            // Feed Settings Sheet
            if (showSettingsSheet) {
                FeedSettingsSheet(
                    cardOrder = userPrefs.feedCardOrder,
                    hiddenCards = userPrefs.feedHiddenCards,
                    layoutMode = userPrefs.feedLayoutMode,
                    calculationMethod = userPrefs.prayerCalculationMethod,
                    asrMadhhab = userPrefs.prayerAsrMadhhab,
                    fajrReminder = prayerRemindersMap["Fajr"] ?: true,
                    dhuhrReminder = prayerRemindersMap["Dhuhr"] ?: true,
                    asrReminder = prayerRemindersMap["Asr"] ?: true,
                    maghribReminder = prayerRemindersMap["Maghrib"] ?: true,
                    ishaReminder = prayerRemindersMap["Isha"] ?: true,
                    onDismiss = { viewModel.closeSettingsSheet() },
                    onMoveCardUp = { viewModel.moveCardUp(it) },
                    onMoveCardDown = { viewModel.moveCardDown(it) },
                    onToggleCardHidden = { cardId, hidden -> viewModel.toggleCardHidden(cardId, hidden) },
                    onChangeLayoutMode = { viewModel.setLayoutMode(it) },
                    onChangeCalculationMethod = { viewModel.setPrayerCalculationMethod(it) },
                    onChangeAsrMadhhab = { viewModel.setPrayerAsrMadhhab(it) },
                    onTogglePrayerReminder = { pName, enabled -> viewModel.togglePrayerReminder(context, pName, enabled) }
                )
            }
        }
    }
}

@Composable
private fun RenderFeedCard(
    cardId: String,
    weather: tn.keeftalk.chat.domain.model.WeatherInfo?,
    isWeatherExpanded: Boolean,
    weatherDayIndex: Int,
    hijriDateText: String,
    prayerDayIndex: Int,
    prayerTimes: List<tn.keeftalk.chat.domain.prayer.PrayerTime>,
    verse: tn.keeftalk.chat.domain.model.VerseInfo,
    marketAssets: List<MarketAsset>,
    viewModel: FeedViewModel,
    onOpenWeatherLocationSettings: () -> Unit = {},
    onOpenPrayerLocationSettings: () -> Unit = {},
    onOpenVerseDetail: () -> Unit = {}
) {
    when (cardId) {
        "weather" -> {
            WeatherCardExact(
                weather = weather,
                isExpanded = isWeatherExpanded,
                selectedDayIndex = weatherDayIndex,
                onToggleExpand = { viewModel.toggleWeatherExpand() },
                onSelectDay = { viewModel.setWeatherDayIndex(it) },
                onOpenDetail = { viewModel.showToast("Opening Weather Details") },
                onOpenLocationSettings = { onOpenWeatherLocationSettings() }
            )
        }
        "verse" -> {
            VerseCardExact(
                verse = verse,
                onClick = { onOpenVerseDetail() }
            )
        }
        "prayer" -> {
            PrayerCardExact(
                hijriDate = hijriDateText,
                selectedDayIndex = prayerDayIndex,
                prayerTimes = prayerTimes,
                onSelectDay = { viewModel.setPrayerDayIndex(it) },
                onClick = { viewModel.showToast("Opening Prayer Schedule") },
                onOpenLocationSettings = { onOpenPrayerLocationSettings() }
            )
        }
        "markets" -> {
            MarketSectionExact(
                assets = marketAssets,
                isLive = true,
                onSelectAsset = { asset -> viewModel.selectMarketAsset(asset) }
            )
        }
    }
}
