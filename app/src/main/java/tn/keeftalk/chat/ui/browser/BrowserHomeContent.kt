package tn.keeftalk.chat.ui.browser

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
    val lastTabIndex by viewModel.lastTabIndex.collectAsState()
    val simulatedDate by viewModel.simulatedDate.collectAsState()

    var searchInput by remember { mutableStateOf("") }
    val isNight = period == "night"
    val inkColor = if (isNight) Color(0xFFeef2ff) else Color(0xFF0e2a3f)
    val ink2Color = if (isNight) Color(0xFFa5b1d8) else Color(0xFF47657b)
    val cardBg = if (isNight) Color(0xFF12183c).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.62f)
    val cardBorder = if (isNight) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.7f)
    val barBg = if (isNight) Color(0xFF0e1330).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.8f)

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

            // 2. Search / Address Field
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
                                viewModel.navigateToUrl(searchInput)
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
                                        viewModel.navigateToUrl(searchInput)
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

            // 3. Weather Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                backgroundColor = cardBg,
                borderColor = cardBorder,
                cornerRadius = 22.dp,
                onClick = { viewModel.navigateToUrl("weather.keeftalk.app/forecast") }
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            val isF = browserSettings.unit == "F"
                            val baseTempC = when (weatherCondition) {
                                "cloudy" -> 21
                                "rain" -> 17
                                "storm" -> 16
                                "snow" -> -1
                                else -> 26
                            } + (if (period == "night") -4 else if (period == "dawn") -3 else if (period == "dusk") -1 else 0)

                            val displayTemp = if (isF) "${(baseTempC * 9 / 5 + 32)}°" else "${baseTempC}°"
                            val hiTemp = if (isF) "${(27 * 9 / 5 + 32)}°" else "27°"
                            val loTemp = if (isF) "${(18 * 9 / 5 + 32)}°" else "18°"

                            Text(
                                text = displayTemp,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkColor,
                                lineHeight = 56.sp,
                                letterSpacing = (-2).sp
                            )
                            Text(
                                text = when (weatherCondition) {
                                    "cloudy" -> "Mostly cloudy"
                                    "rain" -> "Light rain"
                                    "storm" -> "Thunderstorms"
                                    "snow" -> "Snowfall"
                                    else -> "Clear sky"
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = inkColor,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "H $hiTemp · L $loTemp",
                                fontSize = 13.sp,
                                color = ink2Color
                            )
                        }

                        Icon(
                            imageVector = when (weatherCondition) {
                                "cloudy" -> Icons.Default.Cloud
                                "rain" -> Icons.Default.WaterDrop
                                "storm" -> Icons.Default.FlashOn
                                "snow" -> Icons.Default.AcUnit
                                else -> Icons.Default.WbSunny
                            },
                            contentDescription = null,
                            tint = Color(0xFF0a6fe0),
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = cardBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Hourly row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val isF = browserSettings.unit == "F"
                        val base = when (weatherCondition) {
                            "cloudy" -> 21
                            "rain" -> 17
                            "storm" -> 16
                            "snow" -> -1
                            else -> 26
                        }
                        val offsets = listOf(0, 1, 1, 0, -1)
                        listOf("Now", "+1h", "+2h", "+3h", "+4h").forEachIndexed { i, timeLabel ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(timeLabel, fontSize = 12.sp, color = ink2Color)
                                val valC = base + offsets[i]
                                val tempStr = if (isF) "${(valC * 9 / 5 + 32)}°" else "${valC}°"
                                Text(tempStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = inkColor)
                            }
                        }
                    }

                    // Astronomy row
                    val sunTimes = remember(displayDate) {
                        AstronomyEngine.sunTimes(displayDate)
                    }
                    val elong = remember(displayDate) {
                        AstronomyEngine.elong(displayDate)
                    }
                    val phaseStr = remember(elong) {
                        AstronomyEngine.phaseName(elong)
                    }
                    val phasePct = remember(elong) {
                        ((1 - Math.cos(elong * Math.PI / 180.0)) / 2 * 100).toInt()
                    }
                    val timeFormat = SimpleDateFormat(if (browserSettings.h24) "HH:mm" else "h:mm a", Locale.getDefault())

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = cardBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sunrise ${sunTimes.rise?.let { timeFormat.format(it) } ?: "6:14 AM"}",
                            fontSize = 12.sp,
                            color = ink2Color
                        )
                        Text(
                            text = "Sunset ${sunTimes.set?.let { timeFormat.format(it) } ?: "6:42 PM"}",
                            fontSize = 12.sp,
                            color = ink2Color
                        )
                        Text(
                            text = "$phaseStr, $phasePct%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = inkColor
                        )
                    }
                }
            }

            // 4. Context Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                backgroundColor = cardBg,
                borderColor = cardBorder,
                cornerRadius = 22.dp,
                onClick = { viewModel.navigateToUrl("discover.keeftalk.app/$weatherCondition") }
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
                                                viewModel.navigateToUrl(link.url)
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
                                                Icon(
                                                    imageVector = when (link.iconName) {
                                                        "wallet" -> Icons.Default.AccountBalanceWallet
                                                        "news" -> Icons.AutoMirrored.Filled.Article
                                                        "map" -> Icons.Default.Map
                                                        "music" -> Icons.Default.MusicNote
                                                        "play" -> Icons.Default.PlayArrow
                                                        "bag" -> Icons.Default.ShoppingBag
                                                        "game" -> Icons.Default.SportsEsports
                                                        else -> Icons.Default.Add
                                                    },
                                                    contentDescription = link.title,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
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
                                onClick = { viewModel.selectTab(idx) }
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
                                    .clickable { viewModel.navigateToUrl(readItem.url) }
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

        // Home Bottom Bar
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .height(62.dp),
            backgroundColor = barBg,
            borderColor = cardBorder,
            cornerRadius = 24.dp,
            enableTouchScale = false
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBackInTab() },
                    enabled = false
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = inkColor.copy(alpha = 0.35f))
                }
                IconButton(
                    onClick = { viewModel.goForwardInTab() },
                    enabled = lastTabIndex >= 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = if (lastTabIndex >= 0) inkColor else inkColor.copy(alpha = 0.35f))
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0a6fe0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                }
                IconButton(
                    onClick = { viewModel.isTabsOverlayOpen.value = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tab, contentDescription = "Tabs", tint = inkColor)
                        Text(
                            text = "${tabs.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = inkColor
                        )
                    }
                }
                IconButton(
                    onClick = { viewModel.isMenuOpen.value = !viewModel.isMenuOpen.value }
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = inkColor)
                }
            }
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
