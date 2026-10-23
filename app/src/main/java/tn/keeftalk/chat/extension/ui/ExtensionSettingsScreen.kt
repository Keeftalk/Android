package tn.keeftalk.chat.extension.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import tn.keeftalk.chat.extension.data.RemoteStoreExtension
import tn.keeftalk.chat.extension.model.InstalledExtension

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionSettingsScreen(
    onBack: () -> Unit,
    onOpenReview: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val viewModel: ExtensionViewModel = viewModel { ExtensionViewModel(context.applicationContext as android.app.Application) }

    val installedExtensions by viewModel.installedExtensions.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val selectedDetail by viewModel.selectedDetailExtension.collectAsState()
    val rankingMode by viewModel.rankingMode.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val reviewState by viewModel.reviewState.collectAsState()
    val downloadStatuses by viewModel.downloadStatuses.collectAsState()
    val selectedReviews by viewModel.selectedReviews.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Discover, 1: Search, 2: Installed, 3: Settings

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    if (selectedDetail != null) {
        val ext = selectedDetail!!
        val isInstalled = installedExtensions.any { it.id == ext.id }
        val status = downloadStatuses[ext.id] ?: DownloadStatus.Idle
        ExtensionDetailsScreen(
            extension = ext,
            isInstalled = isInstalled,
            downloadStatus = status,
            reviews = selectedReviews,
            onInstall = { viewModel.downloadAndInstallRemote(ext.id) },
            onUninstall = { viewModel.uninstallExtension(ext.id) },
            onSubmitReview = { rating, title, text -> viewModel.submitUserReview(ext.id, rating, title, text) },
            onBack = { viewModel.closeDetails() }
        )
        return
    }

    if (reviewState is ReviewState.Ready) {
        val readyState = reviewState as ReviewState.Ready
        ExtensionReviewScreen(
            manifest = readyState.manifest,
            report = readyState.report,
            onConfirmInstall = {
                viewModel.confirmInstallation {
                    activeTab = 2
                }
            },
            onCancel = { viewModel.cancelReview() }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            when (activeTab) {
                                0 -> "Discover"
                                1 -> "Search"
                                2 -> "Installed"
                                else -> "Settings"
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            when (activeTab) {
                                0 -> "Curated for you"
                                1 -> "Find extensions"
                                2 -> "${installedExtensions.size} extensions"
                                else -> "Manage preferences"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Discover") },
                    label = { Text("Discover") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.DownloadDone, contentDescription = "Installed") },
                    label = { Text("Installed (${installedExtensions.size})") }
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (activeTab) {
                0 -> DiscoverTabFull(
                    catalog = catalog,
                    rankingMode = rankingMode,
                    installedIds = installedExtensions.map { it.id }.toSet(),
                    onSelectRanking = { mode -> viewModel.setRankingMode(mode) },
                    onSelectExtension = { ext -> viewModel.openDetails(ext) },
                    onInstallRemote = { id -> viewModel.downloadAndInstallRemote(id) }
                )
                1 -> SearchTabFull(
                    catalog = catalog,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    installedIds = installedExtensions.map { it.id }.toSet(),
                    onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                    onSelectCategory = { cat -> viewModel.setSelectedCategory(cat) },
                    onSelectExtension = { ext -> viewModel.openDetails(ext) },
                    onInstallRemote = { id -> viewModel.downloadAndInstallRemote(id) }
                )
                2 -> InstalledTabFull(
                    installed = installedExtensions,
                    onToggleEnabled = { id, enabled -> viewModel.toggleExtensionEnabled(id, enabled) },
                    onUninstall = { id -> viewModel.uninstallExtension(id) }
                )
                3 -> SettingsTabFull()
            }
        }
    }
}

@Composable
private fun DiscoverTabFull(
    catalog: List<RemoteStoreExtension>,
    rankingMode: String,
    installedIds: Set<String>,
    onSelectRanking: (String) -> Unit,
    onSelectExtension: (RemoteStoreExtension) -> Unit,
    onInstallRemote: (String) -> Unit
) {
    val hero = catalog.firstOrNull { it.isFeatured } ?: catalog.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = rankingMode == "global",
                    onClick = { onSelectRanking("global") },
                    label = { Text("Worldwide") },
                    leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = rankingMode == "country",
                    onClick = { onSelectRanking("country") },
                    label = { Text("Popular in Tunisia") },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        if (hero != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectExtension(hero) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6), Color(0xFFAF52DE))
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("EXTENSION OF THE DAY", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.ExtraBold)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("★ ${"%.1f".format(hero.ratingAvg)}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!hero.coverUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = hero.coverUrl,
                                        contentDescription = hero.name,
                                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column {
                                    Text(hero.name, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                    Text(hero.shortDescription, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onSelectExtension(hero) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                ) {
                                    Text("Details", fontWeight = FontWeight.Bold)
                                }
                                if (!installedIds.contains(hero.id)) {
                                    OutlinedButton(
                                        onClick = { onInstallRemote(hero.id) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text("Get · ${(hero.packageSizeBytes / 1024 / 1024)} MB")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Week's Picks", subtitle = "Handpicked for your Keeftalk setup")
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(catalog) { ext ->
                    Card(
                        modifier = Modifier
                            .width(260.dp)
                            .clickable { onSelectExtension(ext) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!ext.coverUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ext.coverUrl,
                                        contentDescription = ext.name,
                                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column {
                                    Text(ext.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(ext.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                }
                            }
                            Text(ext.shortDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("★ ${"%.1f".format(ext.ratingAvg)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                if (installedIds.contains(ext.id)) {
                                    Text("Installed", style = MaterialTheme.typography.labelSmall, color = Color(0xFF30D158), fontWeight = FontWeight.Bold)
                                } else {
                                    Button(onClick = { onInstallRemote(ext.id) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                        Text("Get")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Top Charts", subtitle = "Most downloaded this week")
        }

        items(catalog.take(8)) { ext ->
            val idx = catalog.indexOf(ext) + 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectExtension(ext) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("$idx", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (!ext.coverUrl.isNullOrBlank()) {
                        AsyncImage(model = ext.coverUrl, contentDescription = ext.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = Color.White)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ext.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        if (ext.publisher?.isVerified == true) {
                            Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color(0xFF30D158), modifier = Modifier.size(14.dp))
                        }
                    }
                    Text("${ext.category.uppercase()} • ★ ${"%.1f".format(ext.ratingAvg)} • ${if (ext.downloadCount > 1000) "${ext.downloadCount / 1000}k" else "${ext.downloadCount}"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (installedIds.contains(ext.id)) {
                    Text("Installed", style = MaterialTheme.typography.labelSmall, color = Color(0xFF30D158), fontWeight = FontWeight.Bold)
                } else {
                    Button(onClick = { onInstallRemote(ext.id) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("Get")
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTabFull(
    catalog: List<RemoteStoreExtension>,
    searchQuery: String,
    selectedCategory: String,
    installedIds: Set<String>,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectExtension: (RemoteStoreExtension) -> Unit,
    onInstallRemote: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search extensions, dictionaries, themes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(14.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("all", "dictionaries", "themes", "icons", "code", "tools", "security", "productivity", "media", "study", "finance")) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onSelectCategory(cat) },
                    label = { Text(cat.uppercase()) }
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(catalog) { ext ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectExtension(ext) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!ext.coverUrl.isNullOrBlank()) {
                                AsyncImage(model = ext.coverUrl, contentDescription = ext.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.Extension, contentDescription = null, tint = Color.White)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ext.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(ext.shortDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        if (installedIds.contains(ext.id)) {
                            Text("Installed", style = MaterialTheme.typography.labelSmall, color = Color(0xFF30D158), fontWeight = FontWeight.Bold)
                        } else {
                            Button(onClick = { onInstallRemote(ext.id) }) { Text("Get") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstalledTabFull(
    installed: List<InstalledExtension>,
    onToggleEnabled: (String, Boolean) -> Unit,
    onUninstall: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Storage Footprint", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("${installed.sumOf { it.sizeBytes } / 1024 / 1024} MB Total Storage Used", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (installed.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No Extensions Installed", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(installed, key = { it.id }) { ext ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ext.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Switch(checked = ext.isEnabled, onCheckedChange = { enabled -> onToggleEnabled(ext.id, enabled) })
                            }
                            Text("${ext.type.toDisplayName()} • v${ext.version} • ${(ext.sizeBytes / 1024 / 1024)} MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ext.publisherName, style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = { onUninstall(ext.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTabFull() {
    var autoUpdateWifi by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Store Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto-update on Wi-Fi", fontWeight = FontWeight.SemiBold)
                        Text("Download extension updates automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = autoUpdateWifi, onCheckedChange = { autoUpdateWifi = it })
                }
            }
        }

        Text("Developer & Publishing Console", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Developer Portal (store.keeftalk.com)", fontWeight = FontWeight.Bold)
                Text("Publish official packages, review submission reports, and track regional installation analytics.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
