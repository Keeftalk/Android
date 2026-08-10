package com.keeftalk.chat.ui.settings.previews

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.keeftalk.chat.ui.theme.FabGradient
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.theme.Primary

// ─── CONCEPT 1: NEO-GLOW ───
@Composable
fun ProfileNeoGlow() {
    val auraColor = Color(0xFF6C63FF)
    val secondaryAura = Color(0xFF00CCCC)
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        // Dynamic Auras
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.2f,
            animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse)
        )
        
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-50).dp, y = (-50).dp)
                .scale(scale)
                .blur(100.dp)
                .background(auraColor.copy(alpha = 0.3f), CircleShape)
        )
        
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Glowing Avatar
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(160.dp).blur(30.dp).background(FabGradient, CircleShape))
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400",
                    contentDescription = null,
                    modifier = Modifier.size(130.dp).clip(CircleShape).border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("ALEX RIVERS", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
            Text("NEURAL ARCHITECT", style = MaterialTheme.typography.labelLarge, color = secondaryAura, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Ultra-Glass Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Designing the synapses between human intent and machine execution.",
                        style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("ESTABLISH LINK", color = Color.Black, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }
}

// ─── CONCEPT 2: SWISS LUXE ───
@Composable
fun ProfileSwissLuxe() {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp)
        ) {
            Text("01", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.Black, thickness = 4.dp, modifier = Modifier.width(40.dp))
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Text(
                "MARCUS\nTHORNE",
                style = MaterialTheme.typography.displayLarge.copy(
                    lineHeight = 64.sp, fontWeight = FontWeight.Black, letterSpacing = (-4).sp
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(80.dp).background(Color.Black)) {
                     AsyncImage(
                        model = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    Text("SYSTEMS ENGINEER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("LONDON, UK", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Text(
                "Minimalism is not the absence of something. It is the perfect amount of something.",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light, lineHeight = 32.sp
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth().height(1.dp),
                color = Color.Black.copy(alpha = 0.1f)
            ) {}
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LuxeStat("JOINED", "2024")
                LuxeStat("STATUS", "ACTIVE")
                LuxeStat("RANK", "GOLD")
            }
        }
    }
}

@Composable
private fun LuxeStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black)
    }
}

// ─── CONCEPT 3: INFINITE BENTO ───
@Composable
fun ProfileInfiniteBento() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F2F5))) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    modifier = Modifier.weight(1.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White
                ) {
                    Box(modifier = Modifier.padding(20.dp)) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400",
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.align(Alignment.BottomStart)) {
                            Text("Elena V.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text("Creative", style = MaterialTheme.typography.bodySmall, color = Primary)
                        }
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = Primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Verified, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BentoStat(Modifier.weight(1f), "12k", "Followers", Color(0xFFE8E6FF))
                BentoStat(Modifier.weight(1f), "850", "Posts", Color(0xFFD6F5F5))
            }
            
            Surface(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("CURATING EMOTIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Digital designer exploring the boundaries of minimalist aesthetics and high-performance interfaces.", style = MaterialTheme.typography.bodyLarge)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), color = Color.Black) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("CONNECT", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
                Surface(modifier = Modifier.size(80.dp), shape = RoundedCornerShape(24.dp), color = Color.White) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Share, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoStat(modifier: Modifier, value: String, label: String, color: Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(28.dp), color = color) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── CONCEPT 4: THE HERO ───
@Composable
fun ProfileHero() {
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800",
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(500.dp).graphicsLayer {
                alpha = 1f - (scrollState.value.toFloat() / 1500f).coerceIn(0f, 0.8f)
                scaleX = 1f + (scrollState.value.toFloat() / 2000f)
                scaleY = 1f + (scrollState.value.toFloat() / 2000f)
            },
            contentScale = ContentScale.Crop
        )
        
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f), Color.Black))))
        
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Spacer(modifier = Modifier.height(420.dp))
            Column(modifier = Modifier.padding(32.dp)) {
                Text("SOPHIA", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraLight, color = Color.White, letterSpacing = 8.sp)
                Text("WILDER", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-2).sp)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                HorizontalDivider(color = Primary, thickness = 2.dp, modifier = Modifier.width(60.dp))
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    "ADVENTURER • PHOTOGRAPHER • STORYTELLER",
                    style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.6f), letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    "Capturing the raw essence of the world, one frame at a time. Currently lost in the Icelandic highlands.",
                    style = MaterialTheme.typography.bodyLarge, color = Color.White, lineHeight = 28.sp
                )
                
                Spacer(modifier = Modifier.height(64.dp))
                
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().height(70.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("FOLLOW JOURNEY", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// ─── CONCEPT 5: TERMINAL PRO ───
@Composable
fun ProfileTerminalPro() {
    val bgColor = Color(0xFF0D0D0D)
    val termGreen = Color(0xFF00FF41)
    
    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("> keeftalk --profile user_id:8822", color = termGreen, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth().border(1.dp, termGreen.copy(alpha = 0.3f)),
                color = Color.Transparent
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(60.dp).border(1.dp, termGreen)) {
                             AsyncImage(
                                model = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400",
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("NAME: JAXON V.", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("ROLE: FULL_STACK_DEV", color = termGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TerminalSection("BIO") {
                Text("Building the decentralized web. Coffee driven. VIM user.", color = Color.White.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TerminalSection("STATS") {
                Column {
                    Text("COMMITS: 4,203", color = Color.White, fontFamily = FontFamily.Monospace)
                    Text("UPTIME: 99.99%", color = termGreen, fontFamily = FontFamily.Monospace)
                    Text("CLEARANCE: LEVEL_4", color = Color.White, fontFamily = FontFamily.Monospace)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth().height(50.dp).clickable { },
                color = termGreen,
                contentColor = Color.Black
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("INIT_HANDSHAKE", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun TerminalSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text("[$title]", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

// ─── CONCEPT 6: LIQUID GRADIENT ───
@Composable
fun ProfileLiquidGradient() {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing))
    )
    
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = phase }
                .scale(1.5f)
                .background(Brush.radialGradient(listOf(Color(0xFF6C63FF).copy(alpha = 0.1f), Color.Transparent)))
        )
        
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(80.dp))
            
            // Floating Liquid Avatar
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(160.dp).rotate(phase),
                    shape = RoundedCornerShape(60.dp),
                    color = Color(0xFF6C63FF).copy(alpha = 0.1f)
                ) {}
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text("LUNA BLAIR", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            Text("FLUID ARTIST", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(40.dp),
                color = Color.Black,
                contentColor = Color.White
            ) {
                Column(modifier = Modifier.padding(32.dp)) {
                    Text("THE PHILOSOPHY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Everything is in flux. My work attempts to capture the movement of light through digital space.", style = MaterialTheme.typography.bodyLarge)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(modifier = Modifier.weight(1f).height(100.dp), shape = RoundedCornerShape(32.dp), color = Color(0xFFF2F4F8)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("PORTFOLIO", fontWeight = FontWeight.Bold)
                    }
                }
                Surface(modifier = Modifier.weight(1f).height(100.dp), shape = RoundedCornerShape(32.dp), color = Color(0xFFE8E6FF)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Primary)
                    }
                }
            }
        }
    }
}

// ─── CONCEPT 7: DARK KNIGHT ───
@Composable
fun ProfileDarkKnight() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
                
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                    Text("DOMINIC", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White)
                    Text("CYBER SECURITY OPS", style = MaterialTheme.typography.labelLarge, color = Color(0xFF00CCCC), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
            
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Surface(color = Color(0xFF121212), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = Color(0xFF00CCCC))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("TRUST SCORE: 99.8", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                Text(
                    "Silence is the ultimate protection. Operating in the shadows to keep the light secure.",
                    style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ENCRYPTED MSG", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── CONCEPT 8: MAGAZINE EDITORIAL ───
@Composable
fun ProfileMagazine() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAF9F6))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth().height(500.dp)) {
                Column(modifier = Modifier.weight(1f).padding(24.dp)) {
                    Spacer(modifier = Modifier.height(40.dp))
                    Text("VOL 24", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(100.dp))
                    Text("THE\nART\nOF\nDESIGN", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Thin, fontFamily = FontFamily.Serif)
                }
                Box(modifier = Modifier.weight(1.2f)) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=800",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Column(modifier = Modifier.padding(24.dp)) {
                Text("ISABELLA ROSSI", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif)
                Text("MILAN, ITALY", style = MaterialTheme.typography.titleMedium, color = Color.Gray, fontStyle = FontStyle.Italic)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    "Designing spaces that breathe. A collection of works exploring the intersection of light, shadow, and human comfort.",
                    style = MaterialTheme.typography.bodyLarge, lineHeight = 28.sp
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth().height(60.dp).clickable { },
                    color = Color.Black,
                    contentColor = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("VIEW COLLECTION", letterSpacing = 4.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
    }
}

// ─── CONCEPT 9: SPLIT VIEW ───
@Composable
fun ProfileSplitView() {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left: The Persona
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Black)) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=800",
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.6f },
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text("LUCAS", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text("NIGHT", color = Color(0xFF6C63FF), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
        
        // Right: The Details
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Spacer(modifier = Modifier.height(60.dp))
                Text("DETAILS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(24.dp))
                
                SplitInfo("ROLE", "CEO @ KEEF")
                SplitInfo("BASE", "NEW YORK")
                SplitInfo("STATUS", "BUSY")
                
                Spacer(modifier = Modifier.weight(1f))
                
                Surface(
                    modifier = Modifier.fillMaxWidth().height(60.dp).clickable { },
                    color = Color.Black
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitInfo(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

// ─── CONCEPT 10: FLOATING HUD ───
@Composable
fun ProfileFloatingHUD() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF000814))) {
        // Grid background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 40.dp.toPx()
            for (x in 0..(size.width / step).toInt()) {
                drawLine(Color.White.copy(alpha = 0.05f), start = androidx.compose.ui.geometry.Offset(x * step, 0f), end = androidx.compose.ui.geometry.Offset(x * step, size.height))
            }
            for (y in 0..(size.height / step).toInt()) {
                drawLine(Color.White.copy(alpha = 0.05f), start = androidx.compose.ui.geometry.Offset(0f, y * step), end = androidx.compose.ui.geometry.Offset(size.width, y * step))
            }
        }
        
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Circular Progress HUD
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = 0.7f,
                    modifier = Modifier.size(200.dp),
                    color = Color(0xFF00CCCC),
                    strokeWidth = 2.dp
                )
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400",
                    contentDescription = null,
                    modifier = Modifier.size(160.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color(0xFF00CCCC).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("SYSTEM_USER: ZION_7", color = Color(0xFF00CCCC), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Digital entity optimizing network throughput and cryptographic integrity across all Keeftalk nodes.", color = Color.White.copy(alpha = 0.8f))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HUDBox(Modifier.weight(1f), "SYNC", "98%")
                HUDBox(Modifier.weight(1f), "LATENCY", "12ms")
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Surface(
                modifier = Modifier.fillMaxWidth().height(60.dp).clickable { },
                color = Color(0xFF00CCCC),
                contentColor = Color.Black
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("EXECUTE_SYNC", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
private fun HUDBox(modifier: Modifier, label: String, value: String) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = Color.Gray, fontSize = 10.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
