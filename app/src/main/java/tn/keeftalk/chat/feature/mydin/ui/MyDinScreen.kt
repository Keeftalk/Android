package tn.keeftalk.chat.feature.mydin.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import tn.keeftalk.chat.domain.prayer.PrayerTime
import tn.keeftalk.chat.feature.mydin.model.*
import tn.keeftalk.chat.feature.mydin.viewmodel.MyDinViewModel
import java.util.Locale

val FabCyan = Color(0xFF00CCCC)
val FabPurple = Color(0xFF7D5CFF)
val FabGradient = Brush.linearGradient(listOf(FabCyan, FabPurple))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MyDinScreen(
    onBack: () -> Unit,
    viewModel: MyDinViewModel = viewModel()
) {
    val context = LocalContext.current
    val isReaderOpen by viewModel.isReaderOpen.collectAsState()
    val toastMessage by viewModel.toastEvent.collectAsState()
    val downloadDialogInfo by viewModel.downloadDialogInfo.collectAsState()
    val showCdnPrompt by viewModel.showCdnPrompt.collectAsState()

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val locationMode by viewModel.locationMode.collectAsState()

    BackHandler(enabled = isReaderOpen) {
        viewModel.closeReader(context)
    }

    LaunchedEffect(Unit) {
        viewModel.checkCdnPrompt()
    }

    LaunchedEffect(locationPermissionState.status.isGranted, locationMode) {
        if (locationPermissionState.status.isGranted && locationMode == "AUTO") {
            viewModel.updateGpsLocation(context)
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isReaderOpen) {
                SurahReaderView(
                    viewModel = viewModel,
                    onClose = { viewModel.closeReader(context) }
                )
            } else {
                MainMyDinView(
                    onBack = onBack,
                    viewModel = viewModel,
                    onRequestLocationPermission = {
                        locationPermissionState.launchPermissionRequest()
                    }
                )
            }

            // Data Usage Download Confirmation Dialog
            downloadDialogInfo?.let { info ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissDownloadDialog() },
                    title = { Text("تأكيد تحميل التلاوة", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "هل تريد تحميل تلاوة سورة ${info.surah.arName} بصوت ${info.reciter.arName}؟\n\nقد يتسبب ذلك في استهلاك البيانات (~${String.format(Locale.US, "%.1f", info.estimatedMb)} ميجابايت).",
                            lineHeight = 22.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmAudioDownload(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = FabCyan)
                        ) {
                            Text("تحميل", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { viewModel.dismissDownloadDialog() }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // Keeftalk CDN Package Download Prompt Dialog
            if (showCdnPrompt) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissCdnPrompt() },
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = FabCyan) },
                    title = { Text("خادم Keeftalk CDN", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "هل ترغب في تحميل الحزمة الكاملة لجميع سور القرآن الكريم والتفاسير والأحاديث الشريفة من خادم Keeftalk CDN (https://public.cdn.keeftalk.com) لتسريع الأداء والعمل بدون إنترنت بالكامل؟",
                            lineHeight = 22.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.downloadCustomCdnPack(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = FabCyan)
                        ) {
                            Text("تحميل الحزمة الآن (~٥ ميجابايت)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { viewModel.dismissCdnPrompt() }) {
                            Text("لاحقًا")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainMyDinView(
    onBack: () -> Unit,
    viewModel: MyDinViewModel,
    onRequestLocationPermission: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(3) } // Default to Quran Tab (3)
    var showCitySheet by remember { mutableStateOf(false) }

    val selectedCity by viewModel.selectedCity.collectAsState()
    val locationMode by viewModel.locationMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "السلام عليكم",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "ديني",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { showCitySheet = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (locationMode == "AUTO") Icons.Default.MyLocation else Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = FabCyan
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (locationMode == "AUTO") "الموقع التلقائي (${selectedCity.arName})" else "${selectedCity.arName}، ${selectedCity.countryArName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Mosque, contentDescription = "الصلاة") },
                    label = { Text("الصلاة", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FabCyan,
                        indicatorColor = FabCyan.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "الأدعية") },
                    label = { Text("الأدعية", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FabCyan,
                        indicatorColor = FabCyan.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.FormatQuote, contentDescription = "الأحاديث") },
                    label = { Text("الأحاديث", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FabCyan,
                        indicatorColor = FabCyan.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.AutoStories, contentDescription = "القرآن") },
                    label = { Text("القرآن", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FabCyan,
                        indicatorColor = FabCyan.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> SalatTabContent(viewModel = viewModel)
                1 -> DouaaTabContent(viewModel = viewModel)
                2 -> HadithTabContent(viewModel = viewModel)
                3 -> QuranTabContent(viewModel = viewModel)
            }
        }

        if (showCitySheet) {
            CitySelectionSheet(
                currentCity = selectedCity,
                locationMode = locationMode,
                onEnableGpsMode = {
                    onRequestLocationPermission()
                    viewModel.setLocationMode("AUTO")
                    showCitySheet = false
                },
                onSelectCity = {
                    viewModel.selectCity(it)
                    showCitySheet = false
                },
                onDismiss = { showCitySheet = false }
            )
        }
    }
}

// --- Tab 1: Salat Content ---
@Composable
private fun SalatTabContent(viewModel: MyDinViewModel) {
    val context = LocalContext.current
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val nextPrayer by viewModel.nextPrayer.collectAsState()
    val prevPrayer by viewModel.prevPrayer.collectAsState()
    val countdownText by viewModel.countdownText.collectAsState()
    val progressFraction by viewModel.progressFraction.collectAsState()
    val is24h by viewModel.is24HourFormat.collectAsState()
    val hijriDate by viewModel.hijriDateText.collectAsState()

    val prayerReminders by viewModel.prayerReminders.collectAsState()
    val prayerAdhans by viewModel.prayerAdhans.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = FabCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = hijriDate,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FabGradient)
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 0.8f,
                                targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(pulseScale)
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الصلاة القادمة",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = nextPrayer?.name ?: "—",
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = nextPrayer?.timeFormatted ?: "—",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = countdownText,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "السابقة: ${prevPrayer?.name ?: "—"} (${prevPrayer?.timeFormatted ?: "—"})",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "القادمة: ${nextPrayer?.name ?: "—"} (${nextPrayer?.timeFormatted ?: "—"})",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "صلوات اليوم",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = { viewModel.toggle24HourFormat() },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (is24h) "24 س" else "12 س",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        items(prayerTimes) { prayer ->
            val isNext = prayer.isNext
            val isReminderOn = prayerReminders[prayer.name] ?: false
            val isAdhanOn = prayerAdhans[prayer.name] ?: false

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNext) FabCyan.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isNext) FabCyan else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isNext) FabCyan else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (prayer.name) {
                                        "الفجر" -> Icons.Default.WbTwilight
                                        "الشروق" -> Icons.Default.WbSunny
                                        "الظهر" -> Icons.Default.WbSunny
                                        "العصر" -> Icons.Default.WbSunny
                                        "المغرب" -> Icons.Default.WbTwilight
                                        else -> Icons.Default.NightsStay
                                    },
                                    contentDescription = null,
                                    tint = if (isNext) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = prayer.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNext) FabCyan else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = prayer.timeFormatted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isNext) FabCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Prayer Reminder (🔔) & Adhan (📢) Controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.togglePrayerReminder(context, prayer.name) }) {
                            Icon(
                                imageVector = if (isReminderOn) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = "تفعيل التذكير",
                                tint = if (isReminderOn) FabCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.togglePrayerAdhan(context, prayer.name) }) {
                            Icon(
                                imageVector = if (isAdhanOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "تفعيل الأذان",
                                tint = if (isAdhanOn) FabPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَّوْقُوتًا",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "سورة النساء · ١٠٣",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FabCyan,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

// --- Tab 2: Douaa Content ---
@Composable
private fun DouaaTabContent(viewModel: MyDinViewModel) {
    val category by viewModel.douaaCategory.collectAsState()
    val searchQuery by viewModel.douaaSearchQuery.collectAsState()
    val dhikrCounters by viewModel.dhikrCounters.collectAsState()
    val favorites by viewModel.favoriteDouaaIds.collectAsState()

    val filteredDouaas = remember(category, searchQuery, favorites) {
        DOUAA_ITEMS.filter { item ->
            val catOk = category == "all" || item.cat == category
            val queryOk = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, true) ||
                    item.ar.contains(searchQuery, true) ||
                    item.meaning.contains(searchQuery, true)
            catOk && queryOk
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setDouaaSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("ابحث في الأدعية…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DOUAA_CATEGORIES) { cat ->
                val isSelected = category == cat.id
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setDouaaCategory(cat.id) },
                    label = { Text(cat.label, fontWeight = FontWeight.Bold) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FabCyan,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredDouaas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد أدعية مطابقة.\nجرّب تصنيفًا آخر أو كلمة بحث مختلفة.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDouaas, key = { it.id }) { item ->
                    val isFav = favorites.contains(item.id)
                    val count = dhikrCounters[item.id] ?: 0

                    DouaaCard(
                        item = item,
                        currentCount = count,
                        isFavorite = isFav,
                        onIncrement = { viewModel.incrementDhikr(item.id, item.repeat) },
                        onReset = { viewModel.resetDhikr(item.id) },
                        onToggleFavorite = { viewModel.toggleFavoriteDouaa(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DouaaCard(
    item: DouaaItem,
    currentCount: Int,
    isFavorite: Boolean,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FabCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${item.repeat}×",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FabCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.ar,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = item.meaning,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "— ${item.ref}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (item.repeat > 1) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onIncrement() }
                                ) {
                                    Surface(
                                        modifier = Modifier.size(54.dp),
                                        shape = CircleShape,
                                        color = if (currentCount >= item.repeat) FabPurple else FabCyan
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$currentCount / ${item.repeat}",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "عداد الذكر",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (currentCount >= item.repeat) "تم ✓ — اضغط للإعادة" else "اضغط الدائرة للعدّ",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(onClick = onReset) {
                                    Icon(Icons.Default.Refresh, contentDescription = "إعادة تعيين")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onToggleFavorite,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isFavorite) FabPurple else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFavorite) "محفوظ" else "حفظ")
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Douaa", "${item.title}\n\n${item.ar}\n\n${item.meaning}\n\n— ${item.ref}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ الدعاء", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ")
                        }

                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${item.title}\n\n${item.ar}\n\n${item.meaning}\n\n— ${item.ref}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة الدعاء"))
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة")
                        }
                    }
                }
            }
        }
    }
}

// --- Tab 3: Hadith Content (NEW TAB) ---
@Composable
private fun HadithTabContent(viewModel: MyDinViewModel) {
    val context = LocalContext.current
    val category by viewModel.hadithCategory.collectAsState()
    val searchQuery by viewModel.hadithSearchQuery.collectAsState()
    val selectedHadithBook by viewModel.selectedHadithBook.collectAsState()

    var showCreditsSheet by remember { mutableStateOf(false) }

    val filteredHadiths = remember(category, searchQuery) {
        HADITH_ITEMS.filter { item ->
            val catOk = category == "all" || item.category == category
            val queryOk = searchQuery.isBlank() ||
                    item.arabicText.contains(searchQuery, true) ||
                    item.narrator.contains(searchQuery, true) ||
                    item.book.contains(searchQuery, true)
            catOk && queryOk
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setHadithSearchQuery(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("ابحث في الأحاديث الشريفة…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showCreditsSheet = true },
                modifier = Modifier.background(FabCyan.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "حقوق ومصادر الأحاديث",
                    tint = FabCyan
                )
            }
        }

        // Category Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(HADITH_CATEGORIES) { cat ->
                val isSelected = category == cat.id
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setHadithCategory(cat.id) },
                    label = { Text(cat.label, fontWeight = FontWeight.Bold) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FabCyan,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Card: Hadith of the Day
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FabCyan)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FabCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "حديث اليوم الشريف",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FabCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = HADITH_DAILY_FEATURED.grade,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = HADITH_DAILY_FEATURED.narrator,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = HADITH_DAILY_FEATURED.arabicText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = HADITH_DAILY_FEATURED.sourceRef,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FabCyan,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            // Interactive Books Horizontal Row
            item {
                Column {
                    Text(
                        text = "الكتب الستة والمراجع المعتمدة (اضغط لاستعراض أحاديث الكتاب)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(HADITH_BOOKS) { book ->
                            Surface(
                                modifier = Modifier.clickable { viewModel.openHadithBook(context, book) },
                                shape = RoundedCornerShape(14.dp),
                                color = FabCyan.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FabCyan)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(book.nameAr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FabCyan)
                                    Text(book.countText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            items(filteredHadiths, key = { it.id }) { hadith ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = hadith.book,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Text(
                                text = hadith.grade,
                                fontSize = 11.sp,
                                color = FabCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = hadith.narrator,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = hadith.arabicText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 30.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = hadith.sourceRef,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Hadith", "${hadith.narrator}\n\n${hadith.arabicText}\n\n— ${hadith.sourceRef}")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ الحديث الشريف", Toast.LENGTH_SHORT).show()
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp))
                                }

                                IconButton(onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "${hadith.narrator}\n\n${hadith.arabicText}\n\n— ${hadith.sourceRef}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة الحديث الشريف"))
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedHadithBook != null) {
        HadithBookModalSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.closeHadithBook() }
        )
    }

    if (showCreditsSheet) {
        CreditsInfoModalSheet(onDismiss = { showCreditsSheet = false })
    }
}

// --- Interactive Hadith Book Viewer Modal Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HadithBookModalSheet(
    viewModel: MyDinViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val book by viewModel.selectedHadithBook.collectAsState()
    val hadiths by viewModel.bookHadithsList.collectAsState()
    val isLoading by viewModel.isBookHadithsLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val currentBook = book ?: return

    val filteredHadiths = remember(hadiths, searchQuery) {
        hadiths.filter {
            searchQuery.isBlank() ||
                    it.arabicText.contains(searchQuery, true) ||
                    it.narrator.contains(searchQuery, true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currentBook.nameAr,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentBook.authorAr} · ${currentBook.countText}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FabCyan.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "كتاب معتمد",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FabCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث في أحاديث ${currentBook.nameAr}…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FabCyan)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("جارٍ تحميل أحاديث ${currentBook.nameAr}…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredHadiths, key = { it.id }) { hadith ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = hadith.narrator,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = hadith.arabicText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 28.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = hadith.sourceRef,
                                        fontSize = 11.sp,
                                        color = FabCyan,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row {
                                        IconButton(onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Hadith", "${hadith.narrator}\n\n${hadith.arabicText}\n\n— ${hadith.sourceRef}")
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "تم نسخ الحديث الشريف", Toast.LENGTH_SHORT).show()
                                        }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(15.dp))
                                        }

                                        IconButton(onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "${hadith.narrator}\n\n${hadith.arabicText}\n\n— ${hadith.sourceRef}")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة الحديث الشريف"))
                                        }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Tab 4: Quran Content ---
@Composable
private fun QuranTabContent(viewModel: MyDinViewModel) {
    val context = LocalContext.current
    val searchQuery by viewModel.quranSearchQuery.collectAsState()
    val lastRead by viewModel.lastReadSurah.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    val batchProgress by viewModel.batchDownloadProgress.collectAsState()

    var selectedInfoSurah by remember { mutableStateOf<SurahMeta?>(null) }
    var showDownloadAllSheet by remember { mutableStateOf(false) }
    var showCreditsSheet by remember { mutableStateOf(false) }

    val filteredSurahs = remember(searchQuery) {
        SURAH_LIST.filter { surah ->
            searchQuery.isBlank() ||
                    surah.arName.contains(searchQuery, true) ||
                    surah.enName.contains(searchQuery, true) ||
                    surah.number.toString() == searchQuery.trim()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setQuranSearchQuery(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("ابحث عن سورة…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showDownloadAllSheet = true },
                modifier = Modifier.background(FabCyan.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "تحميل كامل القرآن",
                    tint = FabCyan
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = { showCreditsSheet = true },
                modifier = Modifier.background(FabCyan.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "معلومات وحقوق المصادر",
                    tint = FabCyan
                )
            }
        }

        // Active Batch Download Banner
        batchProgress?.let { bp ->
            if (bp.isDownloading) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = FabCyan.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FabCyan)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تحميل القرآن: سورة ${bp.surahName} (${bp.currentSurahNum}/${bp.totalSurahs})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FabCyan
                            )
                            Text(
                                text = "${bp.overallPercent.toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { bp.overallPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = FabCyan,
                            trackColor = FabCyan.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { viewModel.cancelBatchDownload() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("إلغاء التحميل الجماعي", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openSurah(context, lastRead.number) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FabGradient)
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "متابعة القراءة",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "سورة ${lastRead.arName}",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "سورة ${lastRead.number} · ${lastRead.ayahsCount} آيات",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "جميع السور (${filteredSurahs.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(filteredSurahs, key = { it.number }) { surah ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openSurah(context, surah.number) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = FabCyan.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${surah.number}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FabCyan
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "سورة ${surah.arName}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${surah.ayahsCount} آية · ${surah.revelationType}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { selectedInfoSurah = surah }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "معلومات السورة",
                                tint = FabCyan
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedInfoSurah != null) {
        SurahInfoModalSheet(
            surah = selectedInfoSurah!!,
            viewModel = viewModel,
            onDismiss = { selectedInfoSurah = null }
        )
    }

    if (showDownloadAllSheet) {
        DownloadAllQuranModalSheet(
            viewModel = viewModel,
            reciter = selectedReciter,
            onDismiss = { showDownloadAllSheet = false }
        )
    }

    if (showCreditsSheet) {
        CreditsInfoModalSheet(onDismiss = { showCreditsSheet = false })
    }
}

// --- Surah Reader View (Optimized for Long Surahs) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahReaderView(
    viewModel: MyDinViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val surah by viewModel.selectedSurah.collectAsState()
    val ayahs by viewModel.ayahsList.collectAsState()
    val isLoading by viewModel.isSurahLoading.collectAsState()
    val isTajweedOn by viewModel.isTajweedEnabled.collectAsState()
    val isTajweedAvailable by viewModel.isTajweedAvailable.collectAsState()
    val tafsirAyahNum by viewModel.tafsirAyahNumber.collectAsState()

    val selectedReciter by viewModel.selectedReciter.collectAsState()
    val playbackMode by viewModel.playbackMode.collectAsState()
    val playingAyah by viewModel.audioPlayingAyah.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()

    var showReciterSheet by remember { mutableStateOf(false) }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(playingAyah) {
        val activeAyah = playingAyah
        if (activeAyah != null && activeAyah in 1..ayahs.size) {
            val idx = ayahs.indexOfFirst { it.numberInSurah == activeAyah }
            if (idx >= 0) {
                listState.animateScrollToItem((idx + 1).coerceAtMost(ayahs.size))
            }
        }
    }

    val currentSurah = surah ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سورة ${currentSurah.arName}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentSurah.revelationType} · ${currentSurah.ayahsCount} آيات",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showToast("تم حفظ موضع القراءة") }) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "حفظ القراءة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            // Sticky Bottom Player with Tajweed Color Guide Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Tajweed Color Guide Bar inside Player Bar
                    if (isTajweedOn && isTajweedAvailable) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔵 مدّ طبيعي", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A6FDB))
                                Text("🟣 إخفاء/غنة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E24AA))
                                Text("🟢 إدغام", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Text("🔴 قلقلة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الآية ${playingAyah ?: 1} من ${currentSurah.ayahsCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.togglePlaybackMode() }) {
                                Icon(
                                    imageVector = playbackMode.icon,
                                    contentDescription = playbackMode.title,
                                    tint = FabCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            TextButton(onClick = { showReciterSheet = true }) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(selectedReciter.shortName, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.prevAyahAudio(context) },
                            modifier = Modifier.background(FabCyan.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "الآية السابقة", tint = FabCyan)
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { viewModel.togglePlayPauseAudio(context) },
                            shape = CircleShape,
                            color = FabCyan
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(FabGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "تشغيل / إيقاف",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        IconButton(
                            onClick = { viewModel.nextAyahAudio(context) },
                            modifier = Modifier.background(FabCyan.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "الآية التالية", tint = FabCyan)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FabCyan)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = isTajweedOn && isTajweedAvailable,
                                onClick = { viewModel.toggleTajweed() },
                                label = { Text("ألوان التجويد") },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )

                            Text(
                                text = "اضغط أي آية للتلاوة · «i» للتفسير",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (currentSurah.number != 1 && currentSurah.number != 9) {
                        item {
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = FabPurple,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }

                    // High-Performance Lazy Item list per Ayah (Zero Lag + No Overlap Guarantee!)
                    items(ayahs, key = { it.numberInSurah }) { ayah ->
                        val isCurrentPlaying = playingAyah == ayah.numberInSurah
                        val annotatedText = remember(ayah.numberInSurah, isTajweedOn, isTajweedAvailable) {
                            if (isTajweedOn && isTajweedAvailable && ayah.tajweedHtml.isNotBlank()) {
                                parseTajweedToAnnotatedString(ayah.tajweedHtml)
                            } else {
                                AnnotatedString(ayah.plainText)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playAyah(context, ayah.numberInSurah) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentPlaying) FabPurple.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrentPlaying) FabPurple else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Top Header Row: Verse Badge on Start + Tafsir "i" Icon Button on End (Zero Overlap!)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.clickable { viewModel.playAyah(context, ayah.numberInSurah) },
                                        shape = CircleShape,
                                        color = if (isCurrentPlaying) FabPurple else FabCyan.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrentPlaying) FabPurple else FabCyan)
                                    ) {
                                        Text(
                                            text = "آية ${ayah.numberInSurah}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isCurrentPlaying) Color.White else FabCyan,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.openTafsir(ayah.numberInSurah) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "التفسير الميسر",
                                            tint = FabCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Full-Width Ayah Text Below Header (Zero Overlap Guarantee)
                                Text(
                                    text = annotatedText,
                                    fontSize = 20.sp,
                                    lineHeight = 40.sp,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        if (tafsirAyahNum != null) {
            val ayah = ayahs.find { it.numberInSurah == tafsirAyahNum }
            if (ayah != null) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.closeTafsir() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "تفسير سورة ${currentSurah.arName} · الآية ${ayah.numberInSurah}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "التفسير الميسر",
                            fontSize = 12.sp,
                            color = FabCyan,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = ayah.plainText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (ayah.tafsirText.isNotBlank()) ayah.tafsirText else "لا يتوفر تفسير لهذه الآية حاليًا.",
                            fontSize = 14.sp,
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(onClick = { viewModel.navTafsir(-1) }) {
                                Text("السابقة")
                            }

                            Text(
                                text = "${ayah.numberInSurah} من ${ayahs.size}",
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )

                            OutlinedButton(onClick = { viewModel.navTafsir(1) }) {
                                Text("التالية")
                            }
                        }
                    }
                }
            }
        }

        if (showReciterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showReciterSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "اختر المقرئ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    RECITERS.forEach { reciter ->
                        val isSelected = selectedReciter.id == reciter.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.changeReciter(context, reciter)
                                    showReciterSheet = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = reciter.arName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) FabCyan else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = reciter.shortName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = FabCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Download All Quran Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadAllQuranModalSheet(
    viewModel: MyDinViewModel,
    reciter: Reciter,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var downloadTexts by remember { mutableStateOf(true) }
    var downloadAudio by remember { mutableStateOf(true) }
    var selectedBatchReciter by remember { mutableStateOf(reciter) }
    var showReciterPicker by remember { mutableStateOf(false) }

    val batchProgress by viewModel.batchDownloadProgress.collectAsState()

    val downloadedTextCount = remember(batchProgress) { viewModel.getDownloadedSurahsTextCount(context) }
    val downloadedAudioInfo = remember(batchProgress, selectedBatchReciter) { viewModel.getDownloadedAyahsCount(context, 1, selectedBatchReciter.id) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "تحميل كامل القرآن الكريم (أوفلاين)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Current Download Status Overview
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("حالة المحتوى المحمّل حاليًا على الجهاز:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• النصوص والتفاسير: $downloadedTextCount من ١١٤ سورة محمّلة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• التلاوات الصوتية للمقرئ ${selectedBatchReciter.shortName}: ${downloadedAudioInfo.first} من ${downloadedAudioInfo.second} آية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("مصدر البيانات: خادم Keeftalk CDN (https://public.cdn.keeftalk.com)", fontSize = 10.sp, color = FabCyan, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { downloadTexts = !downloadTexts }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = downloadTexts, onCheckedChange = { downloadTexts = it })
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("تحميل النصوص والتفاسير (جميع ١١٤ سورة)", fontWeight = FontWeight.Bold)
                    Text("حفظ نص الآيات والتفسير الميسر كاملاً لقراءتها بدون إنترنت", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { downloadAudio = !downloadAudio }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = downloadAudio, onCheckedChange = { downloadAudio = it })
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("تحميل التلاوات الصوتية (جميع ١١٤ سورة)", fontWeight = FontWeight.Bold)
                    Text("حفظ التلاوة الكاملة للمقرئ المحدد أوفلاين", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (downloadAudio) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReciterPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = FabCyan.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FabCyan)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = FabCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("المقرئ المحدد للتحميل الجماعي:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedBatchReciter.arName, fontWeight = FontWeight.Bold, color = FabCyan)
                            }
                        }

                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FabCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val bp = batchProgress
            if (bp != null && bp.isDownloading) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "جارٍ التحميل الجماعي: سورة ${bp.surahName} (${bp.currentSurahNum}/${bp.totalSurahs})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FabCyan
                        )
                        Text(
                            text = "${bp.overallPercent.toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { bp.overallPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = FabCyan,
                        trackColor = FabCyan.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.cancelBatchDownload() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إلغاء التحميل الجماعي")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.deleteAllDownloadedData(context) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حذف البيانات أوفلاين", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = {
                            viewModel.downloadAllQuran(
                                context = context,
                                downloadTexts = downloadTexts,
                                downloadAudio = downloadAudio,
                                reciter = selectedBatchReciter,
                                selectedSurahs = SURAH_LIST
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = FabCyan)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بدء التحميل", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showReciterPicker) {
        ModalBottomSheet(onDismissRequest = { showReciterPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text("اختر مقرئ التلاوة للتحميل الجماعي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                RECITERS.forEach { r ->
                    val isSelected = r.id == selectedBatchReciter.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedBatchReciter = r
                                showReciterPicker = false
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(r.arName, fontWeight = FontWeight.Bold, color = if (isSelected) FabCyan else MaterialTheme.colorScheme.onSurface)
                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = FabCyan)
                    }
                }
            }
        }
    }
}

// --- Credits & Source Info Modal Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditsInfoModalSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = FabCyan.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = FabCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "حقوق المصادر وتوثيق البيانات",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تطبيق ديني · Keeftalk",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚡ التنزيل الموصى به من خادم Keeftalk CDN", fontWeight = FontWeight.Bold, color = FabCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "💡 يُوصى بتحميل حزمة القرآن الكريم والتفاسير والأحاديث الشريفة مرّة واحدة من خادم Keeftalk CDN (https://public.cdn.keeftalk.com) لتصفّح وقراءة جميع السور والتفاسير والمراجع أوفلاين بدون إنترنت وبأسرع أداء.",
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("📖 القرآن الكريم والتفسير الميسر", fontWeight = FontWeight.Bold, color = FabCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• المصحف الشريف والنص القرآني: مجمع الملك فهد لطباعة المصحف الشريف بالمدينة المنورة.\n• التفسير: التفسير الميسر (إعداد نخبة من العلماء - مجمع الملك فهد).\n• خادم البيانات: Keeftalk CDN و Al Quran Cloud.",
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("🎙 التلاوات الصوتية والقراء", fontWeight = FontWeight.Bold, color = FabCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• التسجيلات الصوتية: مشروع EveryAyah المفتوح للمجتمع الإسلامي.\n• القراء المعتمدون: الحصري، المنشاوي، عبد الباسط، الغامدي، العفاسي، السديس.",
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("📜 الأحاديث الشريفة والسنة النبوية", fontWeight = FontWeight.Bold, color = FabCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• الكتب المعتمدة: الكتب الستة (صحيح البخاري، صحيح مسلم، سنن أبي داود، جامع الترمذي، سنن النسائي، سنن ابن ماجه) والأربعون النووية.\n• توثيق وتخريج الأحاديث: موسوعة السنة النبوية المعتمدة و Hadith API.",
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("🔒 الخصوصية والاستخدام المجاني", fontWeight = FontWeight.Bold, color = FabCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "جميع الخدمات والمحتويات الدينية في Keeftalk تُقدم مجانًا بالكامل بدون أي إعلانات أو تتبع أو جمع للبيانات الشخصية. الحقوق محفوظة لأصحابها ومصادرها الأصلية.",
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- City Selection Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CitySelectionSheet(
    currentCity: MyDinCity,
    locationMode: String,
    onEnableGpsMode: () -> Unit,
    onSelectCity: (MyDinCity) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredCities = remember(query) {
        CITIES.filter {
            query.isBlank() ||
                    it.arName.contains(query, true) ||
                    it.countryArName.contains(query, true) ||
                    it.name.contains(query, true)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "اختر مدينتك",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEnableGpsMode() },
                shape = RoundedCornerShape(14.dp),
                color = if (locationMode == "AUTO") FabCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (locationMode == "AUTO") FabCyan else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = FabCyan
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "الموقع التلقائي (GPS)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (locationMode == "AUTO") FabCyan else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "حساب مواقيت الصلاة تلقائيًا بناءً على موقعك الحالي",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (locationMode == "AUTO") {
                        Icon(Icons.Default.Check, contentDescription = null, tint = FabCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث عن مدينة من القائمة…") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                items(filteredCities) { city ->
                    val isSelected = locationMode == "MANUAL" && city.name == currentCity.name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCity(city) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = city.arName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) FabCyan else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = city.countryArName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = FabCyan)
                        }
                    }
                }
            }
        }
    }
}

// --- Surah Info Sheet & Download Advancement ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahInfoModalSheet(
    surah: SurahMeta,
    viewModel: MyDinViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    val downloadProgressInfo by viewModel.downloadProgressState.collectAsState()

    val downloadedInfo = remember(surah, selectedReciter, downloadProgressInfo) {
        viewModel.getDownloadedAyahsCount(context, surah.number, selectedReciter.id)
    }
    val isFullyDownloaded = downloadedInfo.first > 0 && downloadedInfo.first == downloadedInfo.second
    val isPartiallyDownloaded = downloadedInfo.first > 0 && downloadedInfo.first < downloadedInfo.second

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = FabCyan.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${surah.number}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FabCyan
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "سورة ${surah.arName}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${surah.revelationType} · ${surah.ayahsCount} آية",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val desc = getSurahDescription(surah.number)
            Text(
                text = desc,
                fontSize = 14.sp,
                lineHeight = 24.sp
            )

            val virtue = getSurahVirtue(surah.number)
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = FabPurple.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "فضل السورة: $virtue",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FabPurple,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Download Section with Advancement Progress
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "القراءة والاستماع بدون إنترنت",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "المقرئ الحالي: ${selectedReciter.arName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val activeProgress = downloadProgressInfo
                    if (activeProgress != null && activeProgress.surahNumber == surah.number && activeProgress.isDownloading) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "جارٍ التحميل... ${activeProgress.percent.toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FabCyan
                                )
                                Text(
                                    text = "${activeProgress.completedAyahs} / ${activeProgress.totalAyahs} آية",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { activeProgress.percent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = FabCyan,
                                trackColor = FabCyan.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { viewModel.cancelAudioDownload() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إلغاء التحميل")
                            }
                        }
                    } else if (isFullyDownloaded || isPartiallyDownloaded) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isFullyDownloaded) Icons.Default.CheckCircle else Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = FabCyan
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFullyDownloaded) "محمّلة بالكامل أوفلاين" else "محمّلة جزئيًا (${downloadedInfo.first} / ${downloadedInfo.second} آية)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FabCyan
                                    )
                                }

                                TextButton(onClick = { viewModel.deleteDownloadedAudio(context, surah.number, selectedReciter.id) }) {
                                    Text("حذف", color = MaterialTheme.colorScheme.error)
                                }
                            }

                            if (isPartiallyDownloaded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.requestAudioDownload(surah, selectedReciter) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("استكمال تحميل باقي الآيات")
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.requestAudioDownload(surah, selectedReciter) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FabCyan)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تحميل السورة والتلاوة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
