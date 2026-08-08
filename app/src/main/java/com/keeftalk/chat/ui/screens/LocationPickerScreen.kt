package com.keeftalk.chat.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    onBack: () -> Unit,
    onLocationShared: (Double, Double, Boolean, String) -> Unit
) {
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    
    var showLiveLocationOptions by remember { mutableStateOf(false) }
    var liveDuration by remember { mutableFloatStateOf(60f) } // Default 60 mins

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Location") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Refresh/Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Box(modifier = Modifier.weight(0.4f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = locationPermissionState.status.isGranted),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = true)
                ) {
                    Marker(
                        state = rememberUpdatedMarkerState(position = cameraPositionState.position.target)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 8.dp)
            ) {
                if (!showLiveLocationOptions) {
                    ListItem(
                        headlineContent = { Text("Share Live Location", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Updates your location in real-time") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF25D366), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MyLocation, null, tint = Color.White)
                            }
                        },
                        modifier = Modifier.clickable { showLiveLocationOptions = true }
                    )

                    ListItem(
                        headlineContent = { Text("Send your current location") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF34B7F1), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = Color.White)
                            }
                        },
                        modifier = Modifier.clickable {
                            val loc = cameraPositionState.position.target
                            onLocationShared(loc.latitude, loc.longitude, false, "")
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        "NEARBY PLACES",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Mock nearby places
                    listOf("Central Park", "Metropolitan Museum", "Times Square", "Empire State Building").forEach { place ->
                        ListItem(
                            headlineContent = { Text(place) },
                            leadingContent = { Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.clickable {
                                // For mock, just use current center
                                val loc = cameraPositionState.position.target
                                onLocationShared(loc.latitude, loc.longitude, false, place)
                            }
                        )
                    }
                } else {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Share live location for...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val durationText = when {
                            liveDuration < 60 -> "${liveDuration.toInt()} minutes"
                            liveDuration == 60f -> "1 hour"
                            liveDuration % 60 == 0f -> "${(liveDuration / 60).toInt()} hours"
                            else -> {
                                val hours = (liveDuration / 60).toInt()
                                val mins = (liveDuration % 60).toInt()
                                if (hours == 1) "1 hour $mins mins" else "$hours hours $mins mins"
                            }
                        }

                        Text(
                            text = durationText,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Slider(
                            value = liveDuration,
                            onValueChange = { liveDuration = it },
                            valueRange = 10f..480f, // 10 mins to 8 hours
                            steps = 47 // 10 min increments (roughly)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { showLiveLocationOptions = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val loc = cameraPositionState.position.target
                                    onLocationShared(loc.latitude, loc.longitude, true, durationText)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Share")
                            }
                        }
                    }
                }
            }
        }
    }
}
