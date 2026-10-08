package tn.keeftalk.chat.ui.market

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import tn.keeftalk.chat.domain.model.AppNotification
import tn.keeftalk.chat.ui.components.KeeftalkFeatureTopBar
import java.util.Locale

// Market Custom Theme Palette
object MarketColors {
    @Composable
    fun getBg(isDark: Boolean = false) = MaterialTheme.colorScheme.background

    @Composable
    fun getCardBg(isDark: Boolean = false) = MaterialTheme.colorScheme.surface

    @Composable
    fun getTextPrimary(isDark: Boolean = false) = MaterialTheme.colorScheme.onSurface

    @Composable
    fun getTextMuted(isDark: Boolean = false) = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun getLine(isDark: Boolean = false) = MaterialTheme.colorScheme.outlineVariant

    val Accent @Composable get() = MaterialTheme.colorScheme.primary
    val Gradient: Brush @Composable get() {
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.tertiary
        return Brush.linearGradient(listOf(primary, secondary))
    }
    val Ok = Color(0xFF16A36A)
    val Wa = Color(0xFFE69512)
    val Bad = Color(0xFFE5484D)
    val Star = Color(0xFFF5A524)
}

fun generateGradientBrush(seed: String): Brush {
    val h = kotlin.math.abs(seed.fold(7) { acc, c -> acc * 31 + c.code }) % 360f
    val c1 = Color.hsl(h, 0.70f, 0.62f)
    val c2 = Color.hsl((h + 50f) % 360f, 0.70f, 0.52f)
    return Brush.linearGradient(listOf(c1, c2))
}

@Composable
fun MarketScreen(
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    notifications: List<AppNotification> = emptyList(),
    onNotificationClick: (AppNotification) -> Unit = {},
    onMarkAllNotificationsAsRead: () -> Unit = {},
    searchQuery: String = "",
    viewModel: MarketViewModel = viewModel()
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val state by viewModel.state.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedCat by viewModel.selectedCat.collectAsState()
    val selectedSub by viewModel.selectedSub.collectAsState()
    val selectedCond by viewModel.selectedCond.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val selectedMinR by viewModel.selectedMinR.collectAsState()
    val selectedRad by viewModel.selectedRad.collectAsState()
    val currentSearchQuery by viewModel.searchQuery.collectAsState()
    val filterPanelOpen by viewModel.filterPanelOpen.collectAsState()
    val activeSheet by viewModel.activeSheet.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val filteredListings by viewModel.filteredListings.collectAsState()
    val gateInfo by viewModel.gateInfo.collectAsState()
    val wrapUpCount by viewModel.wrapUpCount.collectAsState()

    // Sync topbar search query
    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    // Handle Toast
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Geolocation Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setLocation(MarketConstants.TUNIS_LOCATION)
        } else {
            viewModel.showToast("Permission denied or unavailable — try the demo area")
        }
    }

    val bg = MarketColors.getBg(isDark)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main View Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    MarketTab.Browse -> BrowseView(
                        viewModel = viewModel,
                        state = state,
                        filteredListings = filteredListings,
                        selectedCat = selectedCat,
                        selectedSub = selectedSub,
                        selectedCond = selectedCond,
                        selectedSort = selectedSort,
                        selectedMinR = selectedMinR,
                        selectedRad = selectedRad,
                        filterPanelOpen = filterPanelOpen,
                        searchQuery = searchQuery,
                        wrapUpCount = wrapUpCount,
                        isDark = isDark,
                        onOpenLocationSheet = {
                            viewModel.openSheet(SheetContent.Location {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            })
                        }
                    )

                    MarketTab.Saved -> SavedView(
                        viewModel = viewModel,
                        state = state,
                        isDark = isDark
                    )

                    MarketTab.Chats -> ChatsView(
                        viewModel = viewModel,
                        state = state,
                        gateInfo = gateInfo,
                        wrapUpCount = wrapUpCount,
                        isDark = isDark
                    )

                    MarketTab.Me -> MeView(
                        viewModel = viewModel,
                        state = state,
                        isDark = isDark
                    )
                }
            }
        }

        // Active Bottom Sheet / Modal Overlay
        activeSheet?.let { sheet ->
            MarketBottomSheetModal(
                sheet = sheet,
                viewModel = viewModel,
                state = state,
                isDark = isDark,
                onDismiss = { viewModel.closeSheet() },
                onRequestLocation = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            )
        }
    }
}

// Bottom Navigation Icons Bar
@Composable
fun TopIconsBar(
    currentTab: MarketTab,
    unreadBadge: Boolean,
    onTabSelected: (MarketTab) -> Unit,
    onPostClicked: () -> Unit,
    userProfile: UserProfile,
    isDark: Boolean
) {
    val line = MarketColors.getLine(isDark)
    val cardBg = MarketColors.getCardBg(isDark)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(cardBg)
            .border(1.dp, line, RoundedCornerShape(99.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onTabSelected(MarketTab.Browse) },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (currentTab == MarketTab.Browse) MarketColors.Accent else Color.Transparent)
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = "Market",
                tint = if (currentTab == MarketTab.Browse) Color.White else MarketColors.getTextMuted(isDark),
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = { onTabSelected(MarketTab.Saved) },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (currentTab == MarketTab.Saved) MarketColors.Accent else Color.Transparent)
        ) {
            Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = "Saved",
                tint = if (currentTab == MarketTab.Saved) Color.White else MarketColors.getTextMuted(isDark),
                modifier = Modifier.size(20.dp)
            )
        }

        Box {
            IconButton(
                onClick = { onTabSelected(MarketTab.Chats) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (currentTab == MarketTab.Chats) MarketColors.Accent else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Chats",
                    tint = if (currentTab == MarketTab.Chats) Color.White else MarketColors.getTextMuted(isDark),
                    modifier = Modifier.size(20.dp)
                )
            }
            if (unreadBadge) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MarketColors.Bad)
                        .align(Alignment.TopEnd)
                )
            }
        }

        // User Avatar Button
        val myName = if (userProfile.mode == "real") userProfile.real else userProfile.alias
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(
                    2.dp,
                    if (currentTab == MarketTab.Me) MarketColors.Accent else line,
                    CircleShape
                )
                .background(generateGradientBrush(myName))
                .clickable { onTabSelected(MarketTab.Me) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (myName.firstOrNull() ?: '?').uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        // Post (+) Button
        IconButton(
            onClick = onPostClicked,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MarketColors.Gradient)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Post Item",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// BROWSE VIEW
@Composable
fun BrowseView(
    viewModel: MarketViewModel,
    state: MarketStateData,
    filteredListings: List<MarketListing>,
    selectedCat: String,
    selectedSub: String,
    selectedCond: String,
    selectedSort: String,
    selectedMinR: Double,
    selectedRad: Double,
    filterPanelOpen: Boolean,
    searchQuery: String,
    wrapUpCount: Int,
    isDark: Boolean,
    onOpenLocationSheet: () -> Unit
) {
    val cardBg = MarketColors.getCardBg(isDark)
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)
    val line = MarketColors.getLine(isDark)

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Market",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "Buy & sell near you",
                    fontSize = 12.sp,
                    color = textMuted
                )
            }

            TopIconsBar(
                currentTab = MarketTab.Browse,
                unreadBadge = wrapUpCount > 0,
                onTabSelected = { viewModel.setTab(it) },
                onPostClicked = { viewModel.openSheet(SheetContent.Post) },
                userProfile = state.me,
                isDark = isDark
            )
        }

        // Pending Deals Banner
        if (wrapUpCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3CD))
                    .clickable { viewModel.setTab(MarketTab.Chats) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFF856404)
                    )
                    Text(
                        text = "$wrapUpCount deal${if (wrapUpCount > 1) "s are" else " is"} waiting to be wrapped up — tap to review ›",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF856404)
                    )
                }
            }
        }

        // Category Chips Row + Filter Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filter Toggle Button
            val hasActiveFilter = selectedSort != "foryou" || selectedMinR > 0 || selectedRad > 0 || selectedCond != "all"
            IconButton(
                onClick = { viewModel.toggleFilterPanel() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (filterPanelOpen) MarketColors.Accent else cardBg)
                    .border(1.dp, line, CircleShape)
            ) {
                Box {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = if (filterPanelOpen) Color.White else textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    if (hasActiveFilter && !filterPanelOpen) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MarketColors.Bad)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Category Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedCat == "all",
                        onClick = { viewModel.setCategory("all") },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text("All")
                            }
                        }
                    )
                }
                items(MarketConstants.CATEGORIES.values.toList()) { cat ->
                    FilterChip(
                        selected = selectedCat == cat.id,
                        onClick = { viewModel.setCategory(cat.id) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(getCategoryIcon(cat.id), contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(cat.name)
                            }
                        }
                    )
                }
            }
        }

        // Subcategory Chips
        if (selectedCat != "all" && MarketConstants.CATEGORIES.containsKey(selectedCat)) {
            val subs = MarketConstants.CATEGORIES[selectedCat]!!.subcategories
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSub == "all",
                        onClick = { viewModel.setSubcategory("all") },
                        label = { Text("All ${MarketConstants.CATEGORIES[selectedCat]!!.name}", fontSize = 12.sp) }
                    )
                }
                items(subs) { sub ->
                    FilterChip(
                        selected = selectedSub == sub,
                        onClick = { viewModel.setSubcategory(sub) },
                        label = { Text(sub, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Expandable Filter Panel
        AnimatedVisibility(visible = filterPanelOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sort Selector
                    var sortExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { sortExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = when (selectedSort) {
                                    "foryou" -> "For you"
                                    "new" -> "Newest"
                                    "rev" -> "Best reviewed"
                                    "near" -> "Nearest"
                                    "low" -> "Price: low-high"
                                    "high" -> "Price: high-low"
                                    else -> "Sort"
                                },
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                            DropdownMenuItem(text = { Text("For you") }, onClick = { viewModel.setSort("foryou"); sortExpanded = false })
                            DropdownMenuItem(text = { Text("Newest") }, onClick = { viewModel.setSort("new"); sortExpanded = false })
                            DropdownMenuItem(text = { Text("Best reviewed") }, onClick = { viewModel.setSort("rev"); sortExpanded = false })
                            DropdownMenuItem(text = { Text("Nearest") }, onClick = { viewModel.setSort("near"); sortExpanded = false })
                            DropdownMenuItem(text = { Text("Price: low to high") }, onClick = { viewModel.setSort("low"); sortExpanded = false })
                            DropdownMenuItem(text = { Text("Price: high to low") }, onClick = { viewModel.setSort("high"); sortExpanded = false })
                        }
                    }

                    // Rating Selector
                    var ratingExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { ratingExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = when (selectedMinR) {
                                    4.0 -> "Sellers 4★ & up"
                                    4.5 -> "Sellers 4.5★ & up"
                                    else -> "Any rating"
                                },
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(expanded = ratingExpanded, onDismissRequest = { ratingExpanded = false }) {
                            DropdownMenuItem(text = { Text("Any seller rating") }, onClick = { viewModel.setMinRating(0.0); ratingExpanded = false })
                            DropdownMenuItem(text = { Text("Sellers 4★ & up") }, onClick = { viewModel.setMinRating(4.0); ratingExpanded = false })
                            DropdownMenuItem(text = { Text("Sellers 4.5★ & up") }, onClick = { viewModel.setMinRating(4.5); ratingExpanded = false })
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Radius Selector
                    var radiusExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { radiusExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (selectedRad > 0) "Within ${selectedRad.toInt()} km" else "Anywhere",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(expanded = radiusExpanded, onDismissRequest = { radiusExpanded = false }) {
                            DropdownMenuItem(text = { Text("Anywhere") }, onClick = { viewModel.setRadius(0.0); radiusExpanded = false })
                            DropdownMenuItem(text = { Text("Within 5 km") }, onClick = { viewModel.setRadius(5.0); radiusExpanded = false })
                            DropdownMenuItem(text = { Text("Within 10 km") }, onClick = { viewModel.setRadius(10.0); radiusExpanded = false })
                            DropdownMenuItem(text = { Text("Within 25 km") }, onClick = { viewModel.setRadius(25.0); radiusExpanded = false })
                            DropdownMenuItem(text = { Text("Within 50 km") }, onClick = { viewModel.setRadius(50.0); radiusExpanded = false })
                            DropdownMenuItem(text = { Text("Within 100 km") }, onClick = { viewModel.setRadius(100.0); radiusExpanded = false })
                        }
                    }

                    // Condition Selector
                    var condExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { condExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = when (selectedCond) {
                                    "n" -> "New"
                                    "u" -> "Used"
                                    else -> "Any condition"
                                },
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(expanded = condExpanded, onDismissRequest = { condExpanded = false }) {
                            DropdownMenuItem(text = { Text("Any condition") }, onClick = { viewModel.setCondition("all"); condExpanded = false })
                            DropdownMenuItem(text = { Text("New") }, onClick = { viewModel.setCondition("n"); condExpanded = false })
                            DropdownMenuItem(text = { Text("Used") }, onClick = { viewModel.setCondition("u"); condExpanded = false })
                        }
                    }
                }

                // Location info & Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = textMuted
                        )
                        Text(
                            text = if (state.loc != null) "Approximate area active (±1 km)" else "No area set",
                            fontSize = 11.5.sp,
                            color = textMuted
                        )
                    }

                    Row {
                        TextTextButton(text = if (state.loc != null) "Update" else "Set area") { onOpenLocationSheet() }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextTextButton(text = "Reset") { viewModel.resetFilters() }
                    }
                }
            }
        }

        // Hero Card
        if (state.uiHero == 1) {
            val myName = if (state.me.mode == "real") state.me.real else state.me.alias
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MarketColors.Accent)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "You're shopping as $myName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (state.me.mode == "alias") "Your real name stays hidden · Change from Me icon" else "Sellers see your real name · Change from Me icon",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .clickable { viewModel.hideHero() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hide",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }

        // Listings Grid
        if (filteredListings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = textMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No items match your filters.",
                        color = textMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredListings, key = { it.id }) { listing ->
                    val isHorizontal = listing.shape == "h"
                    MarketListingCard(
                        listing = listing,
                        state = state,
                        isDark = isDark,
                        isHorizontal = isHorizontal,
                        onCardClick = { viewModel.openSheet(SheetContent.Details(listing.id)) },
                        onLikeClick = { viewModel.toggleLike(listing.id) },
                        onSaveClick = { viewModel.toggleSave(listing.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MarketListingCard(
    listing: MarketListing,
    state: MarketStateData,
    isDark: Boolean,
    isHorizontal: Boolean,
    onCardClick: () -> Unit,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = MarketColors.getCardBg(isDark)
    val line = MarketColors.getLine(isDark)
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)

    val isLiked = state.likes.any { it.id == listing.id }
    val isSaved = state.saved.contains(listing.id)
    val condition = MarketConstants.getCondition(listing.cd)

    val sellerName = if (listing.seller == "me") {
        if (listing.asIdentity == "real") state.me.real else state.me.alias
    } else {
        val s = state.sellers[listing.seller]
        if (listing.asIdentity == "real") s?.real ?: "" else s?.alias ?: ""
    }

    val sellerRev = if (listing.seller == "me") state.me.rev else state.sellers[listing.seller]?.rev ?: emptyList()
    val sellerAvg = if (sellerRev.isNotEmpty()) sellerRev.sumOf { it.s.toDouble() } / sellerRev.size else 0.0
    val distKm = MarketConstants.calculateDistanceKm(state.loc, listing.off, listing.pos)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, line, RoundedCornerShape(16.dp))
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (!isHorizontal) {
            // Vertical Layout
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(generateGradientBrush(listing.cat + listing.id))
                ) {
                    val firstImage = listing.media.firstOrNull { it.t == "i" && !it.src.isNullOrEmpty() }
                    if (firstImage != null) {
                        AsyncImage(
                            model = firstImage.src,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = getCategoryIcon(listing.cat),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center),
                            tint = textMuted
                        )
                    }

                    // Like & Save Actions
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onLikeClick,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.92f))
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) MarketColors.Bad else Color(0xFF444444),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = onSaveClick,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.92f))
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (isSaved) MarketColors.Accent else Color(0xFF444444),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Media Count Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${listing.media.size}", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }

                // Card Body
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = MarketConstants.formatMoney(listing.price),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = textPrimary
                    )

                    Text(
                        text = listing.title,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BadgeChip(text = condition.shortLabel, isOk = condition.type == "n")
                        Text(
                            text = MarketConstants.formatPostedAgo(listing.ts).replace("Posted ", ""),
                            fontSize = 10.sp,
                            color = textMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sellerName,
                            fontSize = 11.sp,
                            color = textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text("★", color = MarketColors.Star, fontSize = 11.sp)
                        Text(
                            text = if (sellerRev.isNotEmpty()) String.format(Locale.US, "%.1f", sellerAvg) else "–",
                            fontSize = 11.sp,
                            color = textMuted
                        )
                        if (distKm != null) {
                            Text(" · ${maxOf(1, kotlin.math.round(distKm).toInt())}km", fontSize = 11.sp, color = textMuted)
                        }
                    }
                }
            }
        } else {
            // Horizontal Layout
            Row(modifier = Modifier.height(110.dp)) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .background(generateGradientBrush(listing.cat + listing.id))
                ) {
                    val firstImage = listing.media.firstOrNull { it.t == "i" && !it.src.isNullOrEmpty() }
                    if (firstImage != null) {
                        AsyncImage(
                            model = firstImage.src,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = getCategoryIcon(listing.cat),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Center),
                            tint = textMuted
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = MarketConstants.formatMoney(listing.price),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = textPrimary
                            )
                            Text(
                                text = listing.title,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = textPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(onClick = onLikeClick, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isLiked) MarketColors.Bad else textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(onClick = onSaveClick, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (isSaved) MarketColors.Accent else textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BadgeChip(text = condition.shortLabel, isOk = condition.type == "n")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("★", color = MarketColors.Star, fontSize = 11.sp)
                            Text(
                                text = if (sellerRev.isNotEmpty()) String.format(Locale.US, "%.1f", sellerAvg) else "–",
                                fontSize = 11.sp,
                                color = textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeChip(text: String, isOk: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (isOk) MarketColors.Ok.copy(alpha = 0.14f) else MarketColors.Accent.copy(alpha = 0.13f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isOk) MarketColors.Ok else MarketColors.Accent
        )
    }
}

// SAVED VIEW
@Composable
fun SavedView(
    viewModel: MarketViewModel,
    state: MarketStateData,
    isDark: Boolean
) {
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)
    val savedListings = state.listings.filter { state.saved.contains(it.id) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Saved",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            TopIconsBar(
                currentTab = MarketTab.Saved,
                unreadBadge = false,
                onTabSelected = { viewModel.setTab(it) },
                onPostClicked = { viewModel.openSheet(SheetContent.Post) },
                userProfile = state.me,
                isDark = isDark
            )
        }

        if (savedListings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = textMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Items you save stay here until the post is deleted.",
                        color = textMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(savedListings, key = { it.id }) { listing ->
                    MarketListingCard(
                        listing = listing,
                        state = state,
                        isDark = isDark,
                        isHorizontal = listing.shape == "h",
                        onCardClick = { viewModel.openSheet(SheetContent.Details(listing.id)) },
                        onLikeClick = { viewModel.toggleLike(listing.id) },
                        onSaveClick = { viewModel.toggleSave(listing.id) }
                    )
                }
            }
        }
    }
}

// CHATS VIEW
@Composable
fun ChatsView(
    viewModel: MarketViewModel,
    state: MarketStateData,
    gateInfo: GateInfo,
    wrapUpCount: Int,
    isDark: Boolean
) {
    val cardBg = MarketColors.getCardBg(isDark)
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)
    val line = MarketColors.getLine(isDark)

    val chatEntries = state.chats.entries
        .filter { state.listings.any { l -> l.id == it.key } }
        .sortedByDescending { it.value.start }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Chats",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "End-to-end encrypted · deleted with listing",
                    fontSize = 11.5.sp,
                    color = textMuted
                )
            }

            TopIconsBar(
                currentTab = MarketTab.Chats,
                unreadBadge = false,
                onTabSelected = { viewModel.setTab(it) },
                onPostClicked = { viewModel.openSheet(SheetContent.Post) },
                userProfile = state.me,
                isDark = isDark
            )
        }

        // Gate Lock Card if active
        if (gateInfo.need.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { viewModel.openSheet(SheetContent.Gate) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, line)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = textPrimary
                        )
                        Text(
                            text = "New chats are locked",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                    }
                    Text(
                        text = "Review your latest 60% of chats to unlock · ${gateInfo.done}/${gateInfo.k} done",
                        fontSize = 12.sp,
                        color = textMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (gateInfo.k > 0) gateInfo.done.toFloat() / gateInfo.k else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = MarketColors.Accent,
                        trackColor = line
                    )
                }
            }
        }

        // Wrap-up Hero
        if (wrapUpCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MarketColors.Accent)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "$wrapUpCount chat${if (wrapUpCount > 1) "s" else ""} waiting for your review",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Takes 10 seconds and helps others spot honest sellers · ${MarketConstants.getLevelTitle(state.me.given)}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        if (chatEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = textMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No conversations yet. Open an item and tap “Message seller”.",
                        color = textMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(chatEntries, key = { it.key }) { (chatId, chat) ->
                    val listing = state.listings.firstOrNull { it.id == chatId }
                    if (listing != null) {
                        val sellerName = if (listing.seller == "me") {
                            if (listing.asIdentity == "real") state.me.real else state.me.alias
                        } else {
                            val s = state.sellers[listing.seller]
                            if (listing.asIdentity == "real") s?.real ?: "" else s?.alias ?: ""
                        }
                        val lastMsg = chat.msgs.lastOrNull { it.f != "sys" }
                        val daysLeft = MarketConstants.daysLeft(listing.exp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openSheet(SheetContent.Chat(chatId)) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(generateGradientBrush(sellerName)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (sellerName.firstOrNull() ?: '?').uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sellerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = MarketConstants.formatTimeAgo(chat.start),
                                        fontSize = 11.sp,
                                        color = textMuted
                                    )
                                }

                                Text(
                                    text = listing.title,
                                    fontSize = 12.sp,
                                    color = textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val msgPreview = when {
                                    lastMsg?.meet != null -> "Meetup proposal"
                                    lastMsg?.m != null -> "Media"
                                    lastMsg != null -> lastMsg.t
                                    else -> ""
                                }
                                if (msgPreview.isNotEmpty()) {
                                    Text(
                                        text = msgPreview,
                                        fontSize = 12.sp,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (chat.status != null) {
                                        BadgeChip(text = if (chat.rv?.done == true) "Wrapped" else "Review sealed", isOk = true)
                                    } else if (viewModel.isChatPending(chat)) {
                                        BadgeChip(text = "Review pending", isOk = false)
                                    }
                                    if (daysLeft <= 10) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(99.dp))
                                                .background(MarketColors.Bad.copy(alpha = 0.14f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Deletes in ${daysLeft}d", fontSize = 10.sp, color = MarketColors.Bad, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = line, modifier = Modifier.padding(start = 76.dp))
                    }
                }
            }
        }
    }
}

// ME VIEW
@Composable
fun MeView(
    viewModel: MarketViewModel,
    state: MarketStateData,
    isDark: Boolean
) {
    val cardBg = MarketColors.getCardBg(isDark)
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)
    val line = MarketColors.getLine(isDark)

    val myName = if (state.me.mode == "real") state.me.real else state.me.alias
    val myListings = state.listings.filter { it.seller == "me" }
    val myReviews = state.me.rev
    val myAvg = if (myReviews.isNotEmpty()) myReviews.sumOf { it.s.toDouble() } / myReviews.size else 0.0
    val (trustLabel, trustType) = MarketConstants.calculateTrust(state.sellers["me"] ?: SellerProfile(state.me.alias, state.me.real, 2026, 0))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Me",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                TopIconsBar(
                    currentTab = MarketTab.Me,
                    unreadBadge = false,
                    onTabSelected = { viewModel.setTab(it) },
                    onPostClicked = { viewModel.openSheet(SheetContent.Post) },
                    userProfile = state.me,
                    isDark = isDark
                )
            }
        }

        // Identity Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, line)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(generateGradientBrush(myName)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (myName.firstOrNull() ?: '?').uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = myName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = textPrimary
                            )
                            Text(
                                text = if (state.me.mode == "alias") "Hidden identity" else "Real identity visible",
                                fontSize = 12.sp,
                                color = textMuted
                            )
                        }

                        Button(
                            onClick = { viewModel.openSheet(SheetContent.Identity) },
                            shape = RoundedCornerShape(99.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MarketColors.Accent)
                        ) {
                            Text("Edit", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (myReviews.isNotEmpty()) String.format(Locale.US, "%.1f", myAvg) else "–",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("★".repeat(myAvg.toInt().coerceIn(0, 5)), color = MarketColors.Star, fontSize = 14.sp)
                            Text("${myReviews.size} reviews", fontSize = 12.sp, color = textMuted)
                        }
                        BadgeChip(
                            text = trustLabel,
                            isOk = trustType == "ok"
                        )
                    }
                }
            }
        }

        // Reviewer Level Badge Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, line)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = MarketConstants.getLevelTitle(state.me.given),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textPrimary
                    )
                    Text(
                        text = "You've reviewed ${state.me.given} seller${if (state.me.given == 1) "" else "s"}. Close deals in chat and rate them to earn badges — it makes the market safer for everyone.",
                        fontSize = 12.sp,
                        color = textMuted
                    )
                }
            }
        }

        // Taste Box
        item {
            val catCounts = mutableMapOf<String, Int>()
            state.likes.forEach { catCounts[it.cat] = (catCounts[it.cat] ?: 0) + 1 }
            val sortedTaste = catCounts.entries.sortedByDescending { it.value }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, line)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MarketColors.Bad
                        )
                        Text(
                            text = "Your taste",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textPrimary
                        )
                    }
                    Text(
                        text = "Likes boost similar items in “For you”. Saves are separate.",
                        fontSize = 12.sp,
                        color = textMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (sortedTaste.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sortedTaste.forEach { (catId, count) ->
                                val catDef = MarketConstants.CATEGORIES[catId]
                                if (catDef != null) {
                                    BadgeChip(text = "${catDef.name} ×$count", isOk = true)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextTextButton(text = "Reset taste") { viewModel.resetTaste() }
                    } else {
                        Text(
                            text = "Tap heart on items to teach your feed.",
                            fontSize = 12.sp,
                            color = textMuted
                        )
                    }
                }
            }
        }

        // My Listings Section Header
        item {
            Text(
                text = "My listings (${myListings.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 8.dp)
            )
        }

        if (myListings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = textMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You have not posted anything yet.",
                            color = textMuted,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.openSheet(SheetContent.Post) },
                            colors = ButtonDefaults.buttonColors(containerColor = MarketColors.Accent)
                        ) {
                            Text("Post your first item", color = Color.White)
                        }
                    }
                }
            }
        } else {
            items(myListings, key = { it.id }) { listing ->
                val daysLeft = MarketConstants.daysLeft(listing.exp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { viewModel.openSheet(SheetContent.Details(listing.id)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, line)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(generateGradientBrush(listing.cat + listing.id)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(listing.cat),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = textMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = listing.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = MarketConstants.formatMoney(listing.price),
                                fontSize = 13.sp,
                                color = textPrimary
                            )
                            Text(
                                text = if (listing.sold) "Sold" else "${daysLeft}d left · ${if (listing.ext > 0) "60-day max used" else "can extend +30d"}",
                                fontSize = 11.sp,
                                color = textMuted
                            )
                        }
                    }
                }
            }
        }

        // Safety Advice Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, line)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MarketColors.Accent
                        )
                        Text(
                            text = "Stay safe",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Meet in public places, inspect items before paying, and never send money in advance. Keeftalk Market has no payments — deals happen between you and the seller. Listing media is not encrypted; chats are.",
                        fontSize = 12.sp,
                        color = textMuted
                    )
                }
            }
        }
    }
}

// BOTTOM SHEET MODAL CONTAINER
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketBottomSheetModal(
    sheet: SheetContent,
    viewModel: MarketViewModel,
    state: MarketStateData,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRequestLocation: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.92f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MarketColors.getBg(isDark),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
            when (sheet) {
                is SheetContent.Details -> ItemDetailsSheet(
                    listingId = sheet.listingId,
                    initialMediaIndex = sheet.mediaIndex,
                    viewModel = viewModel,
                    state = state,
                    isDark = isDark,
                    onDismiss = onDismiss
                )

                is SheetContent.Seller -> SellerProfileSheet(
                    listingId = sheet.listingId,
                    viewModel = viewModel,
                    state = state,
                    isDark = isDark,
                    onDismiss = onDismiss
                )

                is SheetContent.Chat -> ChatDetailSheet(
                    chatId = sheet.chatId,
                    focusInput = sheet.focusInput,
                    viewModel = viewModel,
                    state = state,
                    isDark = isDark,
                    onDismiss = onDismiss
                )

                is SheetContent.Deal -> DealSheet(
                    chatId = sheet.chatId,
                    viewModel = viewModel,
                    state = state,
                    isDark = isDark
                )

                is SheetContent.Review -> ReviewSheet(
                    chatId = sheet.chatId,
                    outcome = sheet.outcome,
                    viewModel = viewModel,
                    state = state,
                    isDark = isDark
                )

                is SheetContent.Report -> ReportSheet(
                    kind = sheet.kind,
                    id = sheet.id,
                    viewModel = viewModel,
                    isDark = isDark,
                    onDismiss = onDismiss
                )

                SheetContent.Identity -> IdentitySheet(
                    viewModel = viewModel,
                    state = state,
                    isDark = isDark,
                    onDismiss = onDismiss
                )

                SheetContent.Post -> PostItemSheet(
                    viewModel = viewModel,
                    state = state,
                    isDark = isDark,
                    onDismiss = onDismiss
                )

                is SheetContent.Location -> LocationSheet(
                    onRequestLocation = onRequestLocation,
                    viewModel = viewModel,
                    isDark = isDark,
                    onDismiss = onDismiss
                )

                SheetContent.Gate -> GateSheet(
                    viewModel = viewModel,
                    state = state,
                    isDark = isDark,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

// ITEM DETAILS SHEET
@Composable
fun ItemDetailsSheet(
    listingId: String,
    initialMediaIndex: Int,
    viewModel: MarketViewModel,
    state: MarketStateData,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val listing = state.listings.firstOrNull { it.id == listingId } ?: return
    var selectedMediaIdx by remember { mutableIntStateOf(initialMediaIndex) }

    val cardBg = MarketColors.getCardBg(isDark)
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)
    val line = MarketColors.getLine(isDark)

    val isMine = listing.seller == "me"
    val isLiked = state.likes.any { it.id == listingId }
    val isSaved = state.saved.contains(listingId)
    val condition = MarketConstants.getCondition(listing.cd)
    val daysLeft = MarketConstants.daysLeft(listing.exp)

    val sellerName = if (isMine) {
        if (listing.asIdentity == "real") state.me.real else state.me.alias
    } else {
        val s = state.sellers[listing.seller]
        if (listing.asIdentity == "real") s?.real ?: "" else s?.alias ?: ""
    }

    val seller = if (isMine) state.sellers["me"] ?: SellerProfile(state.me.alias, state.me.real, 2026, 0) else state.sellers[listing.seller] ?: SellerProfile(sellerName, sellerName, 2026, 0)
    val sellerAvg = if (seller.rev.isNotEmpty()) seller.rev.sumOf { it.s.toDouble() } / seller.rev.size else 0.0
    val (trustLabel, trustType) = MarketConstants.calculateTrust(seller)
    val distKm = MarketConstants.calculateDistanceKm(state.loc, listing.off, listing.pos)

    Column(modifier = Modifier.fillMaxSize()) {
        // Sheet Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = textPrimary)
            }
            Text("Item details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary, modifier = Modifier.weight(1f))

            IconButton(onClick = { viewModel.toggleLike(listingId) }) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) MarketColors.Bad else textMuted
                )
            }

            IconButton(onClick = { viewModel.toggleSave(listingId) }) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save",
                    tint = if (isSaved) MarketColors.Accent else textMuted
                )
            }

            if (!isMine) {
                IconButton(onClick = { viewModel.openSheet(SheetContent.Report("post", listingId)) }) {
                    Icon(Icons.Outlined.Flag, contentDescription = "Report", tint = textMuted)
                }
            }
        }

        // Body Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            // Media Preview
            item {
                val currentMedia = listing.media.getOrNull(selectedMediaIdx)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(generateGradientBrush(listing.cat + listing.id + selectedMediaIdx)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentMedia?.src != null) {
                        AsyncImage(
                            model = currentMedia.src,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = getCategoryIcon(listing.cat),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = textMuted
                        )
                    }
                }

                // Thumbnails
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listing.media.forEachIndexed { idx, item ->
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    2.dp,
                                    if (idx == selectedMediaIdx) MarketColors.Accent else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .background(generateGradientBrush(listing.cat + listing.id + idx))
                                .clickable { selectedMediaIdx = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.t == "v") {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }

                Text(
                    text = "Listing photos & videos are public. Chats are end-to-end encrypted.",
                    fontSize = 11.5.sp,
                    color = textMuted,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Price & Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = MarketConstants.formatMoney(listing.price),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = textPrimary
                    )
                    BadgeChip(text = condition.label, isOk = condition.type == "n")
                }

                Text(
                    text = listing.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                val catName = MarketConstants.CATEGORIES[listing.cat]?.name ?: listing.cat
                Text(
                    text = "$catName › ${listing.sub} · ${MarketConstants.formatPostedAgo(listing.ts)} · ${if (distKm != null) "~${maxOf(1, kotlin.math.round(distKm).toInt())} km away" else "Approximate area only"}",
                    fontSize = 12.sp,
                    color = textMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Text(
                    text = listing.desc,
                    fontSize = 14.sp,
                    color = textPrimary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            // Expiry Box if mine
            if (isMine) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, line)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Live for $daysLeft more day${if (daysLeft == 1) "" else "s"}. It is then deleted together with its chats.",
                                fontSize = 12.sp,
                                color = textPrimary
                            )
                        }
                    }
                }
            }

            // Seller Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable { viewModel.openSheet(SheetContent.Seller(listingId)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, line)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(generateGradientBrush(sellerName)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (sellerName.firstOrNull() ?: '?').uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(sellerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View items ›", fontSize = 12.sp, color = MarketColors.Accent)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    BadgeChip(
                                        text = if (listing.asIdentity == "real") "Real identity" else "Alias",
                                        isOk = listing.asIdentity == "real"
                                    )
                                    if (seller.ver == 1) {
                                        BadgeChip(text = "✓ Verified", isOk = true)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (seller.rev.isNotEmpty()) String.format(Locale.US, "%.1f", sellerAvg) else "–",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("★".repeat(sellerAvg.toInt().coerceIn(0, 5)), color = MarketColors.Star, fontSize = 14.sp)
                                Text("${seller.rev.size} reviews · Member since ${seller.since}", fontSize = 12.sp, color = textMuted)
                            }
                            BadgeChip(text = trustLabel, isOk = trustType == "ok")
                        }
                    }
                }
            }

            // Reviews List
            item {
                Text("Seller reviews", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                if (seller.rev.isEmpty()) {
                    Text("No reviews yet. Be careful with new sellers and meet in public places.", fontSize = 12.sp, color = textMuted)
                } else {
                    seller.rev.take(3).forEach { rev ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(rev.n, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                                Text(MarketConstants.formatTimeAgo(rev.ts), fontSize = 11.sp, color = textMuted)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("★".repeat(rev.s), color = MarketColors.Star, fontSize = 12.sp)
                                if (rev.deal.isNotEmpty()) {
                                    val (label, type) = MarketConstants.DEAL_MAP[rev.deal] ?: ("" to "")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BadgeChip(text = label, isOk = type == "ok")
                                }
                            }
                            if (rev.t.isNotEmpty()) {
                                Text(rev.t, fontSize = 13.sp, color = textPrimary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        HorizontalDivider(color = line)
                    }
                }
            }
        }

        // Action Buttons Bar
        Surface(
            color = MarketColors.getBg(isDark),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isMine) {
                    OutlinedButton(
                        onClick = { viewModel.extendListing(listingId) },
                        enabled = listing.ext == 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+30 days")
                    }

                    OutlinedButton(
                        onClick = { viewModel.markSold(listingId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (listing.sold) "Relist" else "Mark sold")
                    }

                    Button(
                        onClick = { viewModel.deleteListing(listingId) },
                        colors = ButtonDefaults.buttonColors(containerColor = MarketColors.Bad.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete", color = MarketColors.Bad)
                    }
                } else {
                    Button(
                        onClick = { viewModel.openChatForListing(listingId) },
                        colors = ButtonDefaults.buttonColors(containerColor = MarketColors.Accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Message seller", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// SELLER PROFILE SHEET
@Composable
fun SellerProfileSheet(
    listingId: String,
    viewModel: MarketViewModel,
    state: MarketStateData,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val listing = state.listings.firstOrNull { it.id == listingId } ?: return
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)

    val sellerName = if (listing.seller == "me") {
        if (listing.asIdentity == "real") state.me.real else state.me.alias
    } else {
        val s = state.sellers[listing.seller]
        if (listing.asIdentity == "real") s?.real ?: "" else s?.alias ?: ""
    }

    val seller = if (listing.seller == "me") state.sellers["me"] ?: SellerProfile(state.me.alias, state.me.real, 2026, 0) else state.sellers[listing.seller] ?: SellerProfile(sellerName, sellerName, 2026, 0)
    val sellerAvg = if (seller.rev.isNotEmpty()) seller.rev.sumOf { it.s.toDouble() } / seller.rev.size else 0.0
    val (trustLabel, trustType) = MarketConstants.calculateTrust(seller)

    val sellerItems = state.listings.filter {
        it.seller == listing.seller && it.asIdentity == listing.asIdentity && !it.sold && !state.hidden.contains(it.id)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.openSheet(SheetContent.Details(listingId)) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
            }
            Text("Seller", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary, modifier = Modifier.weight(1f))

            if (listing.seller != "me") {
                IconButton(onClick = { viewModel.openSheet(SheetContent.Report("user", listingId)) }) {
                    Icon(Icons.Outlined.Flag, contentDescription = "Report user", tint = textMuted)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(generateGradientBrush(sellerName)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (sellerName.firstOrNull() ?: '?').uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(sellerName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            BadgeChip(
                                text = if (listing.asIdentity == "real") "Real identity" else "Alias",
                                isOk = listing.asIdentity == "real"
                            )
                            if (seller.ver == 1) {
                                BadgeChip(text = "✓ Verified", isOk = true)
                            }
                            BadgeChip(text = trustLabel, isOk = trustType == "ok")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (seller.rev.isNotEmpty()) String.format(Locale.US, "%.1f", sellerAvg) else "–",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("★".repeat(sellerAvg.toInt().coerceIn(0, 5)), color = MarketColors.Star, fontSize = 14.sp)
                        Text("${seller.rev.size} reviews · Member since ${seller.since}", fontSize = 12.sp, color = textMuted)
                    }
                }

                Text(
                    text = "For sale (${sellerItems.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textPrimary,
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                )
            }

            // Seller items grid items
            items(sellerItems, key = { it.id }) { item ->
                MarketListingCard(
                    listing = item,
                    state = state,
                    isDark = isDark,
                    isHorizontal = true,
                    onCardClick = { viewModel.openSheet(SheetContent.Details(item.id)) },
                    onLikeClick = { viewModel.toggleLike(item.id) },
                    onSaveClick = { viewModel.toggleSave(item.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// CHAT DETAIL SHEET
@Composable
fun ChatDetailSheet(
    chatId: String,
    focusInput: Boolean,
    viewModel: MarketViewModel,
    state: MarketStateData,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val listing = state.listings.firstOrNull { it.id == chatId } ?: return
    val chat = state.chats[chatId] ?: return

    val cardBg = MarketColors.getCardBg(isDark)
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)
    val line = MarketColors.getLine(isDark)

    val sellerName = if (listing.seller == "me") {
        if (listing.asIdentity == "real") state.me.real else state.me.alias
    } else {
        val s = state.sellers[listing.seller]
        if (listing.asIdentity == "real") s?.real ?: "" else s?.alias ?: ""
    }

    var messageText by remember { mutableStateOf("") }
    var quickRepliesExpanded by remember { mutableStateOf(false) }

    val bothSent = chat.msgs.any { it.f == "me" } && chat.msgs.any { it.f == "them" }
    val isClosed = chat.status != null
    val daysLeft = MarketConstants.daysLeft(listing.exp)

    val keyboardController = LocalSoftwareKeyboardController.current

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendMessage(
                chatId = chatId,
                text = "",
                media = MarketMedia("i", uri.toString())
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.openSheet(SheetContent.Details(chatId)) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.openSheet(SheetContent.Seller(chatId)) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(generateGradientBrush(sellerName)),
                    contentAlignment = Alignment.Center
                ) {
                    Text((sellerName.firstOrNull() ?: '?').uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(sellerName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                    Text("Started ${MarketConstants.formatTimeAgo(chat.start)}", fontSize = 11.sp, color = textMuted)
                }
            }

            IconButton(onClick = { viewModel.openSheet(SheetContent.Report("user", chatId)) }) {
                Icon(Icons.Outlined.Flag, contentDescription = "Report", tint = textMuted)
            }

            if (!isClosed) {
                Button(
                    onClick = { viewModel.openSheet(SheetContent.Deal(chatId)) },
                    enabled = bothSent,
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MarketColors.Accent)
                ) {
                    Text("Deal", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // Listing Summary Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg)
                .clickable { viewModel.openSheet(SheetContent.Details(chatId)) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(generateGradientBrush(listing.cat + listing.id)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(listing.cat),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = textMuted
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("About this item", fontSize = 10.sp, color = textMuted)
                Text(listing.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(MarketConstants.formatMoney(listing.price), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
        }

        // Warning bar
        if (!isClosed && !bothSent) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(line)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("“Deal” unlocks once you've both sent at least one message", fontSize = 11.sp, color = textMuted)
            }
        }

        if (daysLeft <= 10) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MarketColors.Bad.copy(alpha = 0.15f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("This chat and the listing are deleted in $daysLeft day${if (daysLeft == 1) "" else "s"}", fontSize = 11.sp, color = MarketColors.Bad)
            }
        }

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(chat.msgs) { msg ->
                when (msg.f) {
                    "sys" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = msg.t,
                                fontSize = 11.sp,
                                color = textMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(line)
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    "me" -> {
                        Column(modifier = Modifier.align(Alignment.End)) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .widthIn(max = 280.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MarketColors.Gradient)
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Column {
                                    if (msg.meet != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                            Text("Meetup proposal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                        }
                                        Text(msg.meet.whenTime, fontSize = 12.sp, color = Color.White)
                                        Text(msg.meet.wherePlace, fontSize = 12.sp, color = Color.White)
                                    } else if (msg.m?.src != null) {
                                        AsyncImage(
                                            model = msg.m.src,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Text(msg.t, fontSize = 13.sp, color = Color.White)
                                    }
                                }
                            }
                            Text(
                                text = MarketConstants.formatTimeAgo(msg.ts),
                                fontSize = 10.sp,
                                color = textMuted,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(end = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    "them" -> {
                        Column(modifier = Modifier.align(Alignment.Start)) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .widthIn(max = 280.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(cardBg)
                                    .border(1.dp, line, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Column {
                                    if (msg.meet != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = textPrimary)
                                            Text("Meetup proposal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                                        }
                                        Text(msg.meet.whenTime, fontSize = 12.sp, color = textPrimary)
                                        Text(msg.meet.wherePlace, fontSize = 12.sp, color = textPrimary)
                                    } else if (msg.m?.src != null) {
                                        AsyncImage(
                                            model = msg.m.src,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Text(msg.t, fontSize = 13.sp, color = textPrimary)
                                    }
                                }
                            }
                            Text(
                                text = MarketConstants.formatTimeAgo(msg.ts),
                                fontSize = 10.sp,
                                color = textMuted,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }

            if (chat.typing) {
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("…", fontSize = 16.sp, color = textMuted)
                    }
                }
            }
        }

        // Bottom Input Area
        if (isClosed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✅ Wrapped up — read-only",
                    fontSize = 12.sp,
                    color = textMuted
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MarketColors.getBg(isDark))
            ) {
                // Quick Replies
                LazyRow(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val defaultReplies = listOf("Is it still available?", "Is price negotiable?", "Where can we meet?", "I'm interested")
                    items(defaultReplies) { reply ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.sendMessage(chatId, reply) },
                            label = { Text(reply, fontSize = 12.sp) }
                        )
                    }
                }

                // Text Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { mediaPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(1.dp, line, CircleShape)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = textMuted)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Write a message…", fontSize = 13.sp, color = textMuted) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            focusedBorderColor = MarketColors.Accent,
                            unfocusedBorderColor = line,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (messageText.trim().isNotEmpty()) {
                                viewModel.sendMessage(chatId, messageText.trim())
                                messageText = ""
                                keyboardController?.hide()
                            }
                        })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.trim().isNotEmpty()) {
                                viewModel.sendMessage(chatId, messageText.trim())
                                messageText = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MarketColors.Gradient)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

// DEAL WRAP UP SHEET
@Composable
fun DealSheet(
    chatId: String,
    viewModel: MarketViewModel,
    state: MarketStateData,
    isDark: Boolean
) {
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)
    val cardBg = MarketColors.getCardBg(isDark)
    val line = MarketColors.getLine(isDark)

    val listing = state.listings.firstOrNull { it.id == chatId } ?: return
    val sellerName = if (listing.seller == "me") {
        if (listing.asIdentity == "real") state.me.real else state.me.alias
    } else {
        val s = state.sellers[listing.seller]
        if (listing.asIdentity == "real") s?.real ?: "" else s?.alias ?: ""
    }

    var meetupLocation by remember { mutableStateOf("") }
    var meetupTime by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("Wrap up this deal", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
            Text("${listing.title} · $sellerName", fontSize = 12.sp, color = textMuted)

            Spacer(modifier = Modifier.height(16.dp))

            Text("1 · Meetup (optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)

            // Meetup Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val placePresets = listOf("Café", "Mall entrance", "Metro station", "Main square", "Public car park")
                placePresets.forEach { place ->
                    FilterChip(
                        selected = false,
                        onClick = { meetupLocation = place },
                        label = { Text(place, fontSize = 12.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = meetupLocation,
                onValueChange = { meetupLocation = it },
                placeholder = { Text("Where? e.g. Café near Lac 1, main entrance", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = meetupTime,
                onValueChange = { meetupTime = it },
                placeholder = { Text("When? e.g. Tomorrow at 18:00", fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    if (meetupLocation.length >= 3 && meetupTime.isNotEmpty()) {
                        viewModel.sendMessage(
                            chatId = chatId,
                            text = "",
                            meetup = MarketMeetup(meetupTime, meetupLocation)
                        )
                        viewModel.openSheet(SheetContent.Chat(chatId))
                    } else {
                        viewModel.showToast("Enter meetup details")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MarketColors.Accent)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Text("Send meetup to chat", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("2 · What happened?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
            Text("Pick one, then rate $sellerName. Reviews stay sealed until both are in.", fontSize = 12.sp, color = textMuted)

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Outcome Options List
        val outcomes = listOf(
            Triple("bought", "Met and bought", "Deal done — verified purchase"),
            Triple("notbought", "Met, but didn't buy", "Item or price didn't fit"),
            Triple("nomeet", "Cancelled by both of us", "No one at fault"),
            Triple("gone", "Item no longer available", "Sold elsewhere or withdrawn"),
            Triple("noshow", "They didn't show up", "Bad review · max 2★"),
            Triple("ghost", "They stopped replying", "Review · max 3★"),
            Triple("dishonest", "Not honest / not as described", "Bad review · max 2★")
        )

        items(outcomes) { (key, title, subtitle) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { viewModel.openSheet(SheetContent.Review(chatId, key)) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, line)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                        Text(subtitle, fontSize = 11.5.sp, color = textMuted)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textMuted)
                }
            }
        }
    }
}

// REVIEW SHEET
@Composable
fun ReviewSheet(
    chatId: String,
    outcome: String,
    viewModel: MarketViewModel,
    state: MarketStateData,
    isDark: Boolean
) {
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)
    val cardBg = MarketColors.getCardBg(isDark)
    val line = MarketColors.getLine(isDark)

    val listing = state.listings.firstOrNull { it.id == chatId } ?: return
    val sellerName = if (listing.seller == "me") {
        if (listing.asIdentity == "real") state.me.real else state.me.alias
    } else {
        val s = state.sellers[listing.seller]
        if (listing.asIdentity == "real") s?.real ?: "" else s?.alias ?: ""
    }

    val cap = MarketConstants.RATING_CAPS[outcome] ?: 5
    var starRating by remember { mutableIntStateOf(if (cap < 5) cap else 5) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var commentText by remember { mutableStateOf("") }

    val tagsList = if (cap < 5) MarketConstants.NEGATIVE_TAGS else MarketConstants.POSITIVE_TAGS

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Rate $sellerName", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
        Text("Star rating is required", fontSize = 12.sp, color = textMuted)

        // Stars Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            (1..5).forEach { star ->
                IconButton(onClick = {
                    val finalStar = if (star > cap) {
                        viewModel.showToast("Reviews for this outcome are capped at $cap★")
                        cap
                    } else star
                    starRating = finalStar
                }) {
                    Text(
                        text = "★",
                        fontSize = 36.sp,
                        color = if (star <= starRating) MarketColors.Star else line
                    )
                }
            }
        }

        Text("What stood out? (tap all that apply)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tagsList.forEach { tag ->
                val selected = selectedTags.contains(tag)
                FilterChip(
                    selected = selected,
                    onClick = {
                        selectedTags = if (selected) selectedTags - tag else selectedTags + tag
                    },
                    label = { Text(tag, fontSize = 12.sp) }
                )
            }
        }

        Text("Comment (optional)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary, modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            placeholder = { Text("Was the item as described? Was the seller on time and honest?", fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, line)
        ) {
            Text(
                text = "🔐 Your review stays sealed until $sellerName reviews you too. Honest reviews protect the community.",
                fontSize = 11.5.sp,
                color = textMuted,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.submitReview(
                    chatId = chatId,
                    outcome = outcome,
                    stars = starRating,
                    tags = selectedTags.toList(),
                    comment = commentText.trim()
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MarketColors.Accent)
        ) {
            Text("Submit review & wrap up", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// REPORT SHEET
@Composable
fun ReportSheet(
    kind: String,
    id: String,
    viewModel: MarketViewModel,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)

    val reasons = if (kind == "post") MarketConstants.REPORT_REASONS_POST else MarketConstants.REPORT_REASONS_USER
    var selectedReasonIdx by remember { mutableIntStateOf(-1) }
    var detailsText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (kind == "post") "Report this post" else "Report this user",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        reasons.forEachIndexed { idx, reason ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedReasonIdx = idx }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedReasonIdx == idx,
                    onClick = { selectedReasonIdx = idx }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(reason, fontSize = 14.sp, color = textPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Details (optional)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textMuted)
        OutlinedTextField(
            value = detailsText,
            onValueChange = { detailsText = it },
            placeholder = { Text("Tell us what happened", fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Text(
            text = "Reports are reviewed by our safety team. The other person is not told who reported them.",
            fontSize = 11.5.sp,
            color = textMuted,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (selectedReasonIdx >= 0) {
                    viewModel.sendReport(
                        kind = kind,
                        id = id,
                        reason = reasons[selectedReasonIdx],
                        text = detailsText.trim()
                    )
                } else {
                    viewModel.showToast("Choose a reason")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MarketColors.Accent)
        ) {
            Text("Send report", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// IDENTITY SHEET
@Composable
fun IdentitySheet(
    viewModel: MarketViewModel,
    state: MarketStateData,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)

    var realName by remember { mutableStateOf(state.me.real) }
    var selectedMode by remember { mutableStateOf(state.me.mode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Your market identity", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
        Text("Everyone gets a random alias by default and it can't be changed. Others only see your real name if you choose to show it.", fontSize = 12.sp, color = textMuted)

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MarketColors.getCardBg(isDark))
                .padding(3.dp)
        ) {
            Button(
                onClick = { selectedMode = "alias" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedMode == "alias") MarketColors.Accent else Color.Transparent
                ),
                shape = RoundedCornerShape(9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.TheaterComedy, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (selectedMode == "alias") Color.White else textMuted)
                    Text("Alias", color = if (selectedMode == "alias") Color.White else textMuted)
                }
            }

            Button(
                onClick = { selectedMode = "real" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedMode == "real") MarketColors.Accent else Color.Transparent
                ),
                shape = RoundedCornerShape(9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (selectedMode == "real") Color.White else textMuted)
                    Text("Real name", color = if (selectedMode == "real") Color.White else textMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Alias (assigned by Keeftalk)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textMuted)
        OutlinedTextField(
            value = state.me.alias,
            onValueChange = {},
            enabled = false,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Real name", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textMuted)
        OutlinedTextField(
            value = realName,
            onValueChange = { realName = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.updateIdentity(realName, selectedMode) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MarketColors.Accent)
        ) {
            Text("Save identity", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// POST ITEM SHEET
@Composable
fun PostItemSheet(
    viewModel: MarketViewModel,
    state: MarketStateData,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)

    var mediaList by remember { mutableStateOf(listOf<MarketMedia>()) }
    var shape by remember { mutableStateOf("v") }
    var title by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("") }
    var subcategory by remember { mutableStateOf("") }
    var conditionType by remember { mutableStateOf("n") }
    var exactCondition by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var postAs by remember { mutableStateOf(state.me.mode) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            if (mediaList.size < 10) {
                val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
                val (width, height) = getMediaDimensions(context, uri, isVideo)
                if (maxOf(width, height) >= 1920 && minOf(width, height) >= 1080) {
                    mediaList = mediaList + MarketMedia(if (isVideo) "v" else "i", uri.toString())
                } else {
                    viewModel.showToast("Rejected: Media is below Full HD (1920×1080)")
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Post an item", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
            Text("Required: at least 3 photos and 1 video (Full HD 1920×1080 or higher).", fontSize = 12.sp, color = textMuted)
        }

        // Media Pickers Grid
        item {
            val photosCount = mediaList.count { it.t == "i" }
            val videosCount = mediaList.count { it.t == "v" }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mediaList.forEachIndexed { idx, item ->
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MarketColors.getCardBg(isDark))
                    ) {
                        if (item.src != null) {
                            AsyncImage(
                                model = item.src,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        IconButton(
                            onClick = { mediaList = mediaList.filterIndexed { i, _ -> i != idx } },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (mediaList.size < 10) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, textMuted, RoundedCornerShape(12.dp))
                            .clickable { photoPickerLauncher.launch("*/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Add", tint = textMuted)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = if (photosCount >= 3) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = if (photosCount >= 3) MarketColors.Ok else textMuted
                )
                Text("Photos $photosCount/3+", fontSize = 11.5.sp, color = textMuted)
                Text("·", fontSize = 11.5.sp, color = textMuted)
                Icon(
                    imageVector = if (videosCount >= 1) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = if (videosCount >= 1) MarketColors.Ok else textMuted
                )
                Text("Video $videosCount/1+", fontSize = 11.5.sp, color = textMuted)
                Text("· ${mediaList.size}/10 files", fontSize = 11.5.sp, color = textMuted)
            }
        }

        item {
            Text("Card layout in the market", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = shape == "v", onClick = { shape = "v" }, label = { Text("▯ Vertical") })
                FilterChip(selected = shape == "h", onClick = { shape = "h" }, label = { Text("▭ Horizontal") })
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                placeholder = { Text("e.g. iPhone 13 128GB") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Price (TND) *") },
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Category & Subcategory Selectors
        item {
            var catExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { catExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val catDef = MarketConstants.CATEGORIES[categoryId]
                    if (catDef != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(getCategoryIcon(catDef.id), contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(catDef.name)
                        }
                    } else {
                        Text("Choose category *")
                    }
                }
                DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    MarketConstants.CATEGORIES.values.forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(getCategoryIcon(cat.id), contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(cat.name)
                                }
                            },
                            onClick = {
                                categoryId = cat.id
                                subcategory = ""
                                catExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (categoryId.isNotEmpty() && MarketConstants.CATEGORIES.containsKey(categoryId)) {
            item {
                var subExpanded by remember { mutableStateOf(false) }
                val subs = MarketConstants.CATEGORIES[categoryId]!!.subcategories
                Box {
                    OutlinedButton(
                        onClick = { subExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (subcategory.isNotEmpty()) subcategory else "Choose subcategory *")
                    }
                    DropdownMenu(expanded = subExpanded, onDismissRequest = { subExpanded = false }) {
                        subs.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub) },
                                onClick = {
                                    subcategory = sub
                                    subExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Condition
        item {
            Text("Condition *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = conditionType == "n", onClick = { conditionType = "n"; exactCondition = "" }, label = { Text("New") })
                FilterChip(selected = conditionType == "u", onClick = { conditionType = "u"; exactCondition = "" }, label = { Text("Used") })
            }

            var condExpanded by remember { mutableStateOf(false) }
            val allowedConditions = MarketConstants.CONDITIONS.filter { it.type == conditionType }
            Box(modifier = Modifier.padding(top = 6.dp)) {
                OutlinedButton(
                    onClick = { condExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val cond = MarketConstants.CONDITIONS.firstOrNull { it.key == exactCondition }
                    Text(cond?.label ?: "Select exact condition *")
                }
                DropdownMenu(expanded = condExpanded, onDismissRequest = { condExpanded = false }) {
                    allowedConditions.forEach { cond ->
                        DropdownMenuItem(
                            text = { Text(cond.label) },
                            onClick = {
                                exactCondition = cond.key
                                condExpanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *") },
                placeholder = { Text("Details, accessories, defects, where to meet…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Text("Post as", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = postAs == "alias",
                    onClick = { postAs = "alias" },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.TheaterComedy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Text(state.me.alias)
                        }
                    }
                )
                FilterChip(
                    selected = postAs == "real",
                    onClick = { postAs = "real" },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(15.dp))
                            Text(state.me.real)
                        }
                    }
                )
            }
        }

        item {
            Button(
                onClick = {
                    val priceVal = priceText.toDoubleOrNull() ?: 0.0
                    viewModel.publishListing(
                        title = title,
                        price = priceVal,
                        cat = categoryId,
                        sub = subcategory,
                        cd = exactCondition,
                        desc = description,
                        asIdentity = postAs,
                        shape = shape,
                        media = mediaList
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MarketColors.Accent)
            ) {
                Text("Publish listing", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// LOCATION SHEET
@Composable
fun LocationSheet(
    onRequestLocation: () -> Unit,
    viewModel: MarketViewModel,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Search near you", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MarketColors.Accent
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Keeftalk rounds your position to about 1 km and never stores or shows your exact location. It is only used to sort by distance and filter by radius.",
            fontSize = 13.sp,
            color = textMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                onRequestLocation()
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MarketColors.Accent)
        ) {
            Text("Allow approximate location", color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                viewModel.setLocation(MarketConstants.TUNIS_LOCATION)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use demo area (Tunis)")
        }

        TextButton(onClick = onDismiss) {
            Text("Not now")
        }
    }
}

// GATE SHEET
@Composable
fun GateSheet(
    viewModel: MarketViewModel,
    state: MarketStateData,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val textPrimary = MarketColors.getTextPrimary(isDark)
    val textMuted = MarketColors.getTextMuted(isDark)
    val cardBg = MarketColors.getCardBg(isDark)
    val line = MarketColors.getLine(isDark)

    val gateInfo by viewModel.gateInfo.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("New chats are locked", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, line)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Review your latest 60% of chats before starting new ones. ${gateInfo.done}/${gateInfo.k} done.",
                    fontSize = 13.sp,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (gateInfo.k > 0) gateInfo.done.toFloat() / gateInfo.k else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = MarketColors.Accent,
                    trackColor = line
                )
            }
        }

        Text("Still to review", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(gateInfo.need) { chatId ->
                val listing = state.listings.firstOrNull { it.id == chatId }
                if (listing != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.openSheet(SheetContent.Chat(chatId))
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, line)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(generateGradientBrush(listing.cat + listing.id)),
                                contentAlignment = Alignment.Center
                            ) {
                                val catEmoji = MarketConstants.CATEGORIES[listing.cat]?.emoji ?: "📦"
                                Text(catEmoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(listing.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                                Text("Review required", fontSize = 11.sp, color = MarketColors.Wa)
                            }
                            BadgeChip(text = "Review ›", isOk = false)
                        }
                    }
                }
            }
        }
    }
}

// Helper Components
@Composable
fun TextTextButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MarketColors.Accent,
        modifier = Modifier.clickable { onClick() }
    )
}

fun getMediaDimensions(context: Context, uri: Uri, isVideo: Boolean): Pair<Int, Int> {
    return try {
        if (isVideo) {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
            retriever.release()
            Pair(w, h)
        } else {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            Pair(options.outWidth, options.outHeight)
        }
    } catch (_: Exception) {
        Pair(1920, 1080)
    }
}
