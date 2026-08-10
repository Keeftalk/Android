package com.keeftalk.chat.ui.settings.previews

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ProfileConceptEditorial() {
    val bgColor = Color(0xFFF8F5F2)
    val textColor = Color(0xFF1A1A1A)
    val accentColor = Color(0xFFD4AF37) // Muted Gold
    
    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Asymmetric Header
            Box(modifier = Modifier.fillMaxWidth().height(450.dp)) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .fillMaxHeight()
                        .align(Alignment.TopEnd),
                    contentScale = ContentScale.Crop
                )
                
                // Overlaying Text
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 40.dp)
                ) {
                    Text(
                        text = "Elena",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Light,
                            color = textColor,
                            fontSize = 80.sp
                        )
                    )
                    Text(
                        text = "Vaughan",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Thin,
                            fontStyle = FontStyle.Italic,
                            color = textColor,
                            fontSize = 80.sp
                        ),
                        modifier = Modifier.offset(x = 20.dp, y = (-20).dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Curator & Creative Director",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Light,
                            letterSpacing = 2.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = accentColor, thickness = 1.dp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Exploring the intersection of technology and human emotion through digital curation and editorial design.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Light,
                        lineHeight = 32.sp,
                        color = textColor
                    )
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Minimal Info Cards
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    EditorialInfo("LOCATION", "PARIS, FR")
                    EditorialInfo("MEMBER", "SINCE 2022")
                }

                Spacer(modifier = Modifier.height(64.dp))

                // Elegant Action
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { },
                    color = textColor,
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "CONTACT ELENA",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                letterSpacing = 4.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Secondary Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MailOutline, null, tint = textColor)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Public, null, tint = textColor)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun EditorialInfo(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color.Gray
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
