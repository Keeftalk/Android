package tn.keeftalk.chat.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tn.keeftalk.chat.domain.model.UserBrowserSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BrowserHomeContent(
    viewModel: BrowserViewModel,
    browserSettings: UserBrowserSettings,
    onClose: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val period by viewModel.period.collectAsState()
    val weatherCondition by viewModel.weatherCondition.collectAsState()
    val quickLinks by viewModel.quickLinks.collectAsState()
    val isQuickLinksEdit by viewModel.isQuickLinksEdit.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val readingList by viewModel.readingList.collectAsState()
    val isReadingListExpanded by viewModel.isReadingListExpanded.collectAsState()
    val simulatedDate by viewModel.simulatedDate.collectAsState()

    var searchInput by remember { mutableStateOf("") }
    var isLeaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val navigateAway: (String) -> Unit = { url ->
        if (url.isNotBlank()) {
            isLeaving = true
            scope.launch {
                delay(150L)
                viewModel.navigateToUrl(url)
            }
        }
    }

    val isNight = period == "night"
    val inkColor = if (isNight) Color(0xFFeef2ff) else Color(0xFF0e2a3f)
    val ink2Color = if (isNight) Color(0xFFa5b1d8) else Color(0xFF47657b)
    val cardBg = if (isNight) Color(0xFF12183c).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.62f)
    val cardBorder = if (isNight) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.7f)

    val displayDate = simulatedDate ?: remember { Date() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Scrollable Home Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 4.dp)
        ) {
            // 1. Header Greeting & Actions
            AnimatedVisibility(
                visible = !isLeaving,
                exit = fadeOut(animationSpec = tween(120))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val greetText = when (period) {
                            "dawn" -> "Good morning"
                            "dusk" -> "Good evening"
                            "night" -> "Good night"
                            else -> "Good day"
                        }
                        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                        Text(
                            text = greetText,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = inkColor,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = dateFormat.format(displayDate),
                            fontSize = 13.sp,
                            color = ink2Color
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassCard(
                            modifier = Modifier.size(42.dp),
                            backgroundColor = cardBg,
                            borderColor = cardBorder,
                            cornerRadius = 14.dp,
                            onClick = onSettingsClick
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = inkColor, modifier = Modifier.size(20.dp))
                            }
                        }
                        GlassCard(
                            modifier = Modifier.size(42.dp),
                            backgroundColor = cardBg,
                            borderColor = cardBorder,
                            cornerRadius = 14.dp,
                            onClick = onClose
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, contentDescription = "Close My Browser", tint = inkColor, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // 2. Search / Address Field (Transforms smoothly to top bar / URL field)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 14.dp),
                backgroundColor = cardBg,
                borderColor = cardBorder,
                cornerRadius = 25.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (searchInput.isNotBlank()) {
                                navigateAway(searchInput)
                                searchInput = ""
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = ink2Color)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchInput.isEmpty()) {
                            Text(
                                "Search with ${browserSettings.searchEngine} or enter address",
                                color = ink2Color,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = inkColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            singleLine = true,
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(inkColor),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    if (searchInput.isNotBlank()) {
                                        navigateAway(searchInput)
                                        searchInput = ""
                                    }
                                }
                            )
                        )
                    }
                    IconButton(
                        onClick = { viewModel.showToast("Voice search listening...") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice search", tint = ink2Color)
                    }
                }
            }

            AnimatedVisibility(
                visible = !isLeaving,
                exit = fadeOut(animationSpec = tween(120))
            ) {
                Column {
                    // 3. Weather Card (Reusing WeatherCardExact from feed)
                    val weatherInfo by viewModel.weatherInfo.collectAsState()
                    val isWeatherExpanded by viewModel.isWeatherExpanded.collectAsState()
                    val weatherDayIndex by viewModel.weatherDayIndex.collectAsState()

                    tn.keeftalk.chat.ui.feed.components.WeatherCardExact(
                        weather = weatherInfo,
                        isExpanded = isWeatherExpanded,
                        selectedDayIndex = weatherDayIndex,
                        onToggleExpand = { viewModel.toggleWeatherExpand() },
                        onSelectDay = { viewModel.setWeatherDayIndex(it) },
                        onOpenDetail = { navigateAway("weather.keeftalk.app/forecast") },
                        onOpenLocationSettings = { viewModel.showToast("Opening weather location settings") },
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // 4. Context Card
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        backgroundColor = cardBg,
                        borderColor = cardBorder,
                        cornerRadius = 22.dp,
                        onClick = { navigateAway("discover.keeftalk.app/$weatherCondition") }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0a6fe0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Explore, contentDescription = null, tint = Color.White)
                            }
                            Column {
                                val (title, sub) = when {
                                    period == "night" -> Pair("Wind down", "Calm sounds and late reads")
                                    weatherCondition == "rain" -> Pair("Rainy-day reads", "Long reads picked for the couch")
                                    weatherCondition == "storm" -> Pair("Stay in, stay safe", "Weather alerts for your area")
                                    weatherCondition == "snow" -> Pair("Cozy up with a playlist", "Warm drinks and winter mixes")
                                    else -> Pair("Great day to be outside", "Nearby trails and parks")
                                }
                                Text(title, fontWeight = FontWeight.Bold, color = inkColor, fontSize = 15.sp)
                                Text(sub, fontSize = 13.sp, color = ink2Color)
                            }
                        }
                    }

                    // 5. Quick Links Section
                    if (browserSettings.ql) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quick links", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = inkColor)
                            TextButton(onClick = { viewModel.isQuickLinksEdit.value = !isQuickLinksEdit }) {
                                Text(if (isQuickLinksEdit) "Done" else "Edit", fontWeight = FontWeight.Bold, color = Color(0xFF0a6fe0))
                            }
                        }

                        val infiniteTransition = rememberInfiniteTransition(label = "wiggle")
                        val wiggleRotation by infiniteTransition.animateFloat(
                            initialValue = -1.6f,
                            targetValue = 1.6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(180, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "wiggleRotation"
                        )

                        val chunks = quickLinks.chunked(4)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            chunks.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { link ->
                                        val index = quickLinks.indexOf(link)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .rotate(if (isQuickLinksEdit && !link.isAddButton) wiggleRotation else 0f)
                                        ) {
                                            GlassCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                backgroundColor = cardBg,
                                                borderColor = cardBorder,
                                                cornerRadius = 18.dp,
                                                onClick = {
                                                    if (link.isAddButton) {
                                                        viewModel.showToast("Open a page, then tap the star to save it")
                                                    } else {
                                                        navigateAway(link.url)
                                                    }
                                                }
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 12.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(parseHexColor(link.color)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (link.isAddButton) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = link.title,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        } else {
                                                            AsyncImage(
                                                                model = "https://www.google.com/s2/favicons?domain=${link.url}&sz=128",
                                                                contentDescription = link.title,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        link.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = inkColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            if (isQuickLinksEdit && !link.isAddButton) {
                                                IconButton(
                                                    onClick = { viewModel.removeQuickLink(index) },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(2.dp)
                                                        .size(20.dp)
                                                        .background(Color(0xFFe5484d), CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Delete link", tint = Color.White, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }
                                    repeat(4 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // 6. Pick Up Where You Left Off Rail
                    if (browserSettings.tabs) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pick up where you left off", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = inkColor)
                            TextButton(onClick = { viewModel.isTabsOverlayOpen.value = true }) {
                                Text("All tabs", fontWeight = FontWeight.Bold, color = Color(0xFF0a6fe0))
                            }
                        }

                        if (tabs.isEmpty()) {
                            Text("No open tabs. Search above to start one.", fontSize = 13.sp, color = ink2Color, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(tabs) { idx, tab ->
                                    val url = tab.currentUrl
                                    GlassCard(
                                        modifier = Modifier.width(150.dp),
                                        backgroundColor = cardBg,
                                        borderColor = cardBorder,
                                        cornerRadius = 18.dp,
                                        onClick = {
                                            isLeaving = true
                                            scope.launch {
                                                delay(150L)
                                                viewModel.selectTab(idx)
                                            }
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(72.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(
                                                                parseHexColor(listOf("#ff9a3c", "#2d9bf0", "#7a5cff", "#18a999")[idx % 4]),
                                                                parseHexColor(listOf("#ff4f7b", "#18a999", "#e0447a", "#ff9a3c")[idx % 4])
                                                            )
                                                        )
                                                    )
                                            )
                                            Text(
                                                viewModel.titleOf(url),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = inkColor
                                            )
                                            Text(
                                                viewModel.hostOf(url),
                                                fontSize = 11.sp,
                                                color = ink2Color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7. Reading List Section
                    if (browserSettings.reads) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reading list", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = inkColor)
                            TextButton(onClick = { viewModel.isReadingListExpanded.value = !isReadingListExpanded }) {
                                Text(if (isReadingListExpanded) "Show less" else "See all", fontWeight = FontWeight.Bold, color = Color(0xFF0a6fe0))
                            }
                        }

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            backgroundColor = cardBg,
                            borderColor = cardBorder,
                            cornerRadius = 22.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (readingList.isEmpty()) {
                                    Text(
                                        "No saved articles. Save pages from the menu while browsing.",
                                        fontSize = 13.sp,
                                        color = ink2Color,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                } else {
                                    val displayedReads = if (isReadingListExpanded) readingList else readingList.take(3)
                                    displayedReads.forEachIndexed { idx, readItem ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { navigateAway(readItem.url) }
                                                .padding(vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(parseHexColor(readItem.color))
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(readItem.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = inkColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(readItem.subtitle, fontSize = 12.sp, color = ink2Color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        if (idx < displayedReads.size - 1) {
                                            HorizontalDivider(color = cardBorder, thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        Color(0xFF0a7cff)
    }
}
