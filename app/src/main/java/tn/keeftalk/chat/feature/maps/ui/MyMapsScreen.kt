package tn.keeftalk.chat.feature.maps.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.maps.MapView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import tn.keeftalk.chat.domain.model.CanonicalLocation
import tn.keeftalk.chat.feature.maps.data.MapStyle
import tn.keeftalk.chat.ui.theme.LucideIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMapsScreen(
    viewModel: MyMapsViewModel,
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    initialTitle: String? = null,
    isPickerMode: Boolean = false,
    chatId: String? = null,
    onBack: () -> Unit,
    onLocationPicked: ((CanonicalLocation) -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    var hasLocationPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var maplibreMapRef by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var isDockOpen by remember { mutableStateOf(true) }
    var isAddMode by remember { mutableStateOf(false) }
    var isMeasureMode by remember { mutableStateOf(false) }
    var measurePoints by remember { mutableStateOf(listOf<LatLng>()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            viewModel.fetchCurrentLocation()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initPickerMode(isPickerMode, chatId)
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (initialLatitude != null && initialLongitude != null) {
            val loc = CanonicalLocation(
                latitude = initialLatitude,
                longitude = initialLongitude,
                name = initialTitle ?: "Shared Location",
                address = "Lat: $initialLatitude, Lon: $initialLongitude"
            )
            viewModel.selectLocation(loc)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            LucideIcons.MapPinned,
                            contentDescription = null,
                            tint = Color(0xFF00CCCC),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                if (isPickerMode) "Select Location" else "My Maps",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE9EEF8)
                            )
                            uiState.selectedLocation?.name?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8A94A8)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFE9EEF8))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleDrawer(!uiState.isDrawerOpen) }) {
                        Icon(Icons.Default.Menu, contentDescription = "Saved Places", tint = Color(0xFFE9EEF8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xEE0C111B)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF070A10))
        ) {
            // 1. Native MapLibre Map View
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        onCreate(null)
                        getMapAsync { mapboxMap ->
                            maplibreMapRef = mapboxMap
                            mapboxMap.setStyle(uiState.mapStyle.styleUrl) { _ ->
                                uiState.selectedLocation?.let { sel ->
                                    mapboxMap.cameraPosition = CameraPosition.Builder()
                                        .target(LatLng(sel.latitude, sel.longitude))
                                        .zoom(14.0)
                                        .build()
                                } ?: run {
                                    uiState.currentLocation?.let { cur ->
                                        mapboxMap.cameraPosition = CameraPosition.Builder()
                                            .target(LatLng(cur.latitude, cur.longitude))
                                            .zoom(14.0)
                                            .build()
                                    }
                                }
                            }
                            mapboxMap.addOnMapClickListener { latLng ->
                                coroutineScope.launch {
                                    if (isAddMode) {
                                        isAddMode = false
                                        val rev = viewModel.reverseGeocode(latLng.latitude, latLng.longitude)
                                        val newLoc = rev ?: CanonicalLocation(
                                            latitude = latLng.latitude,
                                            longitude = latLng.longitude,
                                            name = "Dropped Pin",
                                            address = "Lat: ${latLng.latitude}, Lon: ${latLng.longitude}"
                                        )
                                        viewModel.selectLocation(newLoc)
                                    } else if (isMeasureMode) {
                                        measurePoints = measurePoints + latLng
                                    } else {
                                        viewModel.reverseGeocode(latLng.latitude, latLng.longitude)
                                    }
                                }
                                true
                            }
                        }
                        mapViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Mode Banner when adding or measuring
            if (isAddMode || isMeasureMode) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = Color(0xEE0D131E),
                    border = BorderStroke(1.dp, Color(0x5500CCCC)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF00CCCC), CircleShape)
                        ) {
                            // Pulsing dot
                        }
                        Text(
                            text = if (isAddMode) "Tap anywhere on the map to drop a pin" else "Click along a path to measure distance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE9EEF8),
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = {
                            isAddMode = false
                            isMeasureMode = false
                            measurePoints = emptyList()
                        }) {
                            Text("Esc", color = Color(0xFF00CCCC), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Collapsible Tool Dock (Top-Right)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .width(280.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Dock Toggle Button
                IconButton(
                    onClick = { isDockOpen = !isDockOpen },
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(12.dp, RoundedCornerShape(15.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF))),
                            RoundedCornerShape(15.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Tools",
                        tint = Color(0xFF04161C),
                        modifier = Modifier.size(24.dp)
                    )
                }

                AnimatedVisibility(visible = isDockOpen) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        // Search bar
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search places or addresses…", color = Color(0xFF5C6679)) },
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF8A94A8)) },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Close, null, tint = Color(0xFF8A94A8))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xE60D131E), RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0x8800CCCC),
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedTextColor = Color(0xFFE9EEF8),
                                    unfocusedTextColor = Color(0xFFE9EEF8),
                                    focusedContainerColor = Color(0xE6141D2C),
                                    unfocusedContainerColor = Color(0xE60D131E)
                                )
                            )

                            if (uiState.searchResults.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 54.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xF2101725)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                                ) {
                                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                                        items(uiState.searchResults) { place ->
                                            ListItem(
                                                headlineContent = { Text(place.name ?: "Place", color = Color(0xFFE9EEF8), fontWeight = FontWeight.Bold) },
                                                supportingContent = { Text(place.address ?: "", color = Color(0xFF8A94A8), maxLines = 1) },
                                                modifier = Modifier.clickable {
                                                    viewModel.selectLocation(place)
                                                    maplibreMapRef?.cameraPosition = CameraPosition.Builder()
                                                        .target(LatLng(place.latitude, place.longitude))
                                                        .zoom(15.0)
                                                        .build()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Basemap Selector Pill
                        Row(
                            modifier = Modifier
                                .background(Color(0xE60D131E), RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MapStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = uiState.mapStyle == style,
                                    onClick = {
                                        viewModel.setMapStyle(style)
                                        maplibreMapRef?.setStyle(style.styleUrl)
                                    },
                                    label = { Text(style.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00CCCC),
                                        selectedLabelColor = Color(0xFF04161C),
                                        containerColor = Color.Transparent,
                                        labelColor = Color(0xFF8A94A8)
                                    )
                                )
                            }
                        }

                        // Tool buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.weight(1f))

                            ControlBtn(
                                icon = Icons.Default.MyLocation,
                                title = "My Location",
                                onClick = {
                                    if (hasLocationPermission) {
                                        viewModel.fetchCurrentLocation()
                                        uiState.currentLocation?.let { cur ->
                                            maplibreMapRef?.cameraPosition = CameraPosition.Builder()
                                                .target(LatLng(cur.latitude, cur.longitude))
                                                .zoom(15.0)
                                                .build()
                                        }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                }
                            )
                            ControlBtn(
                                icon = Icons.Default.Add,
                                title = "Add Place",
                                active = isAddMode,
                                onClick = { isAddMode = !isAddMode; isMeasureMode = false }
                            )
                            ControlBtn(
                                icon = Icons.Default.Straighten,
                                title = "Measure",
                                active = isMeasureMode,
                                onClick = { isMeasureMode = !isMeasureMode; isAddMode = false; if (!isMeasureMode) measurePoints = emptyList() }
                            )
                            ControlBtn(
                                icon = Icons.Default.ZoomOutMap,
                                title = "Fit All",
                                onClick = {
                                    viewModel.fetchCurrentLocation()
                                }
                            )
                        }
                    }
                }
            }

            // 4. Bottom Selected Place Card
            uiState.selectedLocation?.let { sel ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF20B101A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sel.name ?: "Selected Location",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE9EEF8)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sel.address ?: "Lat: ${sel.latitude}, Lon: ${sel.longitude}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF8A94A8)
                                )
                            }
                            IconButton(onClick = { viewModel.selectLocation(uiState.currentLocation ?: sel) }) {
                                Icon(Icons.Default.Close, "Dismiss", tint = Color(0xFF8A94A8))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isPickerMode) {
                                Button(
                                    onClick = { onLocationPicked?.invoke(sel) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00CCCC),
                                        contentColor = Color(0xFF04161C)
                                    )
                                ) {
                                    Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Send Location to Chat", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.saveCurrentSelectedPlace() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00CCCC),
                                        contentColor = Color(0xFF04161C)
                                    )
                                ) {
                                    Icon(Icons.Default.BookmarkAdd, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Place", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Saved Places Drawer (Left)
            if (uiState.isDrawerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .background(Color(0xF90C111B))
                        .align(Alignment.CenterStart)
                        .shadow(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Saved Places", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFE9EEF8))
                            IconButton(onClick = { viewModel.toggleDrawer(false) }) {
                                Icon(Icons.Default.Close, "Close", tint = Color(0xFF8A94A8))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.savedPlaces.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No saved places yet", color = Color(0xFF5C6679))
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(uiState.savedPlaces) { place ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectLocation(place)
                                                viewModel.toggleDrawer(false)
                                                maplibreMapRef?.cameraPosition = CameraPosition.Builder()
                                                    .target(LatLng(place.latitude, place.longitude))
                                                    .zoom(15.0)
                                                    .build()
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121A28))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(place.name ?: "Place", fontWeight = FontWeight.Bold, color = Color(0xFFE9EEF8))
                                                Text(place.address ?: "", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A94A8), maxLines = 1)
                                            }
                                            IconButton(onClick = { viewModel.deleteSavedPlace(place.placeId ?: place.latitude.toString()) }) {
                                                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF5C7A))
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
}

@Composable
fun ControlBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .shadow(8.dp, RoundedCornerShape(13.dp))
            .background(
                if (active) Brush.linearGradient(listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF)))
                else Brush.linearGradient(listOf(Color(0xDD0D131E), Color(0xDD0D131E))),
                RoundedCornerShape(13.dp)
            )
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = if (active) Color(0xFF04161C) else Color(0xFF8A94A8),
            modifier = Modifier.size(20.dp)
        )
    }
}
