package com.keeftalk.chat.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.domain.model.WeatherInfo
import com.keeftalk.chat.ui.theme.FabGradient
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeatherCard(weather: WeatherInfo?, onLocationClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FabGradient)
                .padding(24.dp)
        ) {
            if (weather != null) {
                // Header: City and Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                weather.locationName,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = onLocationClick, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.EditLocation,
                                    contentDescription = "Change Location",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            "Updated ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(weather.timestamp))}",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    // Main Temp
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${weather.temperature.toInt()}°",
                            color = Color.White,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Text(
                                weather.condition,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Primary Details Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WeatherDetailItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Thermostat,
                        label = "Feels Like",
                        value = "${weather.apparentTemperature.toInt()}°"
                    )
                    WeatherDetailItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WaterDrop,
                        label = "Humidity",
                        value = "${weather.humidity}%"
                    )
                    WeatherDetailItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Air,
                        label = "Wind",
                        value = "${weather.windSpeed.toInt()} km/h"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Secondary Details Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WeatherDetailItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WbSunny,
                        label = "UV Index",
                        value = String.format(Locale.getDefault(), "%.1f", weather.uvIndex)
                    )
                    WeatherDetailItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Visibility,
                        label = "Visibility",
                        value = "${weather.visibility.toInt()} km"
                    )
                    WeatherDetailItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Cloud,
                        label = "Cloudiness",
                        value = "${weather.cloudCover}%"
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 20.dp),
                    color = Color.White.copy(alpha = 0.2f)
                )

                // Sunrise / Sunset with icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    SunItem(icon = Icons.Default.LightMode, label = "Sunrise", time = weather.sunrise)
                    SunItem(icon = Icons.Default.Nightlight, label = "Sunset", time = weather.sunset)
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Personalizing your weather...", color = Color.White.copy(alpha = 0.8f))
                        TextButton(onClick = onLocationClick) {
                            Text("Set Location", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherDetailItem(modifier: Modifier, icon: ImageVector, label: String, value: String) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SunItem(icon: ImageVector, label: String, time: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
            Text(time, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WeatherLocationDialog(
    currentMode: String,
    currentManualLocation: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var mode by remember { mutableStateOf(currentMode) }
    var location by remember { mutableStateOf(currentManualLocation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weather Location") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == "AUTO", onClick = { mode = "AUTO" })
                    Text("Current Location (GPS)", modifier = Modifier.clickable { mode = "AUTO" })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == "MANUAL", onClick = { mode = "MANUAL" })
                    Text("Manual City", modifier = Modifier.clickable { mode = "MANUAL" })
                }
                if (mode == "MANUAL") {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("City Name") },
                        placeholder = { Text("e.g. Paris, Tokyo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(mode, location) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
