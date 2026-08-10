package com.keeftalk.chat.ui.settings.previews

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ProfileConceptBrutalist() {
    val accentColor = Color(0xFFADFF2F) // Neon Green
    
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Image with thick border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .border(4.dp, Color.Black)
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1552058544-f2b08422138a?w=800",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Floating Tag
                Surface(
                    color = accentColor,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .border(2.dp, Color.Black),
                    shape = RectangleShape
                ) {
                    Text(
                        "AVAILABLE FOR HIRE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MARCUS\nTHORNE",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 36.sp,
                                    letterSpacing = (-2).sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Verified, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                        }
                        Text(
                            text = "/SENIOR DEVELOPER",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        )
                    }
                    
                    // Boxy Avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(4.dp, Color.Black)
                            .background(accentColor)
                    ) {
                         AsyncImage(
                            model = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bio with heavy background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Building decentralized systems and high-performance communication protocols. Minimalism is the only way forward.",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Grid Stats
                Row(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    BrutalistStat(Modifier.weight(1f), "NETWORK", "500+")
                    BrutalistStat(Modifier.weight(1f), "RANK", "#12")
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Huge Button
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .border(4.dp, Color.Black),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.Black
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("INITIATE CONTACT", fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.ArrowOutward, null)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun BrutalistStat(modifier: Modifier, label: String, value: String) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(2.dp, Color.Black)
            .padding(16.dp)
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
    }
}
