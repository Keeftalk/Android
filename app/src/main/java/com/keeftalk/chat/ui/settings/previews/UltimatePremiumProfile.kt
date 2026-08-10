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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.keeftalk.chat.ui.theme.FabGradient
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.theme.Primary

@Composable
fun UltimatePremiumProfile() {
    val icons = LocalAppIcons.current
    val scrollState = rememberScrollState()
    
    // --- SIMULATION STATES ---
    var isEditing by remember { mutableStateOf(false) }
    var isPublicPreview by remember { mutableStateOf(false) }
    
    // Mock Data
    var fullName by remember { mutableStateOf("Alex Rivers") }
    var bio by remember { mutableStateOf("Senior Product Designer & Systems Architect. Crafting the future of secure communication.") }
    val username = "alex_rivers"
    val email = "alex.rivers@keeftalk.com"
    val phone = "+44 20 7946 0958"
    val location = "London, United Kingdom"
    
    val headerAlpha by remember {
        derivedStateOf { (scrollState.value.toFloat() / 500f).coerceIn(0f, 1f) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        
        // --- PARALLAX COVER ---
        AsyncImage(
            model = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1000",
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .graphicsLayer {
                    translationY = -scrollState.value.toFloat() * 0.4f
                    alpha = 1f - (scrollState.value.toFloat() / 1200f).coerceIn(0f, 0.7f)
                },
            contentScale = ContentScale.Crop
        )

        // --- SCROLLABLE CONTENT ---
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(280.dp))
            
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                    color = Color.Black,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 90.dp)
                    ) {
                        if (isEditing) {
                            PremiumEditContent(
                                fullName = fullName,
                                onNameChange = { fullName = it },
                                bio = bio,
                                onBioChange = { bio = it },
                                onSave = { isEditing = false },
                                onCancel = { isEditing = false }
                            )
                        } else {
                            PremiumViewContent(
                                fullName = fullName,
                                username = username,
                                bio = bio,
                                email = email,
                                phone = phone,
                                location = location,
                                isPublicPreview = isPublicPreview,
                                onTogglePublic = { isPublicPreview = !isPublicPreview },
                                onEditClick = { isEditing = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }

                // --- AVATAR ---
                Box(
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .offset(y = (-64).dp)
                        .zIndex(5f)
                ) {
                    Box(
                        modifier = Modifier.size(128.dp).offset(y = 4.dp).clip(CircleShape).background(brush = FabGradient, alpha = 0.3f)
                    )
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400",
                        contentDescription = null,
                        modifier = Modifier.size(120.dp).clip(CircleShape).border(4.dp, Color.Black, CircleShape).shadow(16.dp, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // --- STICKY HEADER ---
        StickyHeader(
            alpha = headerAlpha,
            name = fullName,
            onBack = {}
        )
    }
}

@Composable
private fun PremiumViewContent(
    fullName: String,
    username: String,
    bio: String,
    email: String,
    phone: String,
    location: String,
    isPublicPreview: Boolean,
    onTogglePublic: () -> Unit,
    onEditClick: () -> Unit
) {
    val icons = LocalAppIcons.current
    
    // Header Info
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(fullName, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Verified, null, tint = Primary, modifier = Modifier.size(24.dp))
        }
        Text("@$username", style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Action Row (Context Sensitive)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isPublicPreview) {
            Button(
                onClick = {},
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Message", fontWeight = FontWeight.Bold)
            }
            Surface(
                modifier = Modifier.size(56.dp).clickable {},
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(icons.call, null, tint = Primary) }
            }
        } else {
            Button(
                onClick = onEditClick,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(icons.edit, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile", fontWeight = FontWeight.Bold)
            }
        }
        
        // Toggle Preview Button
        Surface(
            modifier = Modifier.height(56.dp).clickable(onClick = onTogglePublic),
            shape = RoundedCornerShape(16.dp),
            color = if (isPublicPreview) Primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, if (isPublicPreview) Primary else Color.White.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isPublicPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = if (isPublicPreview) Primary else Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isPublicPreview) "Exit Preview" else "View as Public", style = MaterialTheme.typography.labelLarge, color = if (isPublicPreview) Primary else Color.White)
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Bio
    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Text(bio, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(20.dp), lineHeight = 26.sp)
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Bento Info Cards
    Text("ACCOUNT DETAILS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.3f), letterSpacing = 2.sp)
    Spacer(modifier = Modifier.height(16.dp))
    
    PremiumBentoCard(icons.email, "Email", email)
    PremiumBentoCard(icons.phone, "Phone", phone)
    PremiumBentoCard(icons.place, "Location", location)
}

@Composable
private fun PremiumEditContent(
    fullName: String,
    onNameChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Text("EDITING PROFILE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Primary, letterSpacing = 2.sp)
    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = fullName,
        onValueChange = onNameChange,
        label = { Text("Display Name") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedBorderColor = Primary, unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = bio,
        onValueChange = onBioChange,
        label = { Text("Bio") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedBorderColor = Primary, unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
        )
    )

    Spacer(modifier = Modifier.height(32.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onCancel, modifier = Modifier.weight(1f).height(56.dp)) {
            Text("Discard", color = Color.White.copy(alpha = 0.6f))
        }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Save Changes", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PremiumBentoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.02f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun StickyHeader(alpha: Float, name: String, onBack: () -> Unit) {
    val icons = LocalAppIcons.current
    Surface(
        modifier = Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha },
        color = Color.Black.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.statusBarsPadding().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(icons.back, null, tint = Color.White) }
            Spacer(modifier = Modifier.width(8.dp))
            AsyncImage(
                model = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100",
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
