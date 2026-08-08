package com.keeftalk.chat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    latitude: Double,
    longitude: Double,
    title: String? = null,
    isLive: Boolean = false,
    onBack: () -> Unit
) {
    val location = LatLng(latitude, longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 15f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title ?: "Location")
                        if (isLive) {
                            Text(
                                "Live Location",
                                style = MaterialTheme.typography.labelSmall,
                                color = androidx.compose.ui.graphics.Color(0xFF25D366)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        GoogleMap(
            modifier = Modifier.padding(padding).fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = isLive),
            uiSettings = MapUiSettings(myLocationButtonEnabled = isLive)
        ) {
            Marker(
                state = rememberUpdatedMarkerState(position = location),
                title = title,
                icon = if (isLive) null else null // Could use custom icon for live
            )
        }
    }
}
