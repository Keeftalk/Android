package com.keeftalk.chat.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import coil.compose.AsyncImage
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.util.AvatarUtils
import com.keeftalk.chat.ui.screens.formatJoinDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onChatStarted: (String) -> Unit = {},
    onConnectionPathClick: () -> Unit = {},
    isCurrentUser: Boolean = true
) {
    val profile by viewModel.profile.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val icons = LocalAppIcons.current

    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            val bytes = context.contentResolver.openInputStream(selectedUri)?.readBytes()
            bytes?.let { viewModel.uploadAvatar(it) }
        }
    }

    val coverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            bytes?.let { viewModel.uploadCover(it) }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            profile?.let { p ->
                Crossfade(targetState = isEditing, label = "ProfileMode") { editing ->
                    if (editing) {
                        EditProfileContent(
                            profile = p,
                            onProfileUpdate = { viewModel.updateProfileLocal(it) },
                            onAvatarClick = { avatarLauncher.launch("image/*") },
                            onCoverClick = { coverLauncher.launch("image/*") },
                            viewModel = viewModel,
                            onCancel = { viewModel.cancelEditing() },
                            onSave = { viewModel.saveProfile() },
                            modifier = Modifier.padding(padding)
                        )
                    } else {
                        ViewProfileContent(
                            profile = p,
                            viewModel = viewModel,
                            isCurrentUser = isCurrentUser,
                            onBack = onBack,
                            onEditClick = { viewModel.setEditing(true) },
                            onMessageClick = { viewModel.startChat(onChatStarted) },
                            onConnectionPathClick = onConnectionPathClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!isLoading) Text("Profile not found")
            }
            
            if (isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ViewProfileContent(
    profile: Profile,
    viewModel: ProfileViewModel,
    isCurrentUser: Boolean,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onMessageClick: () -> Unit,
    modifier: Modifier = Modifier,
    onConnectionPathClick: () -> Unit = {}
) {
    val icons = LocalAppIcons.current
    val scrollState = rememberScrollState()
    
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Cover Image with Parallax
        AsyncImage(
            model = profile.coverUrl ?: "https://images.unsplash.com/photo-1506744038136-46273834b3fb",
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .graphicsLayer {
                    alpha = 1f - (scrollState.value.toFloat() / 1000f).coerceIn(0f, 0.7f)
                    translationY = -scrollState.value.toFloat() * 0.5f
                },
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(240.dp))
            
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = Color.Black,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 80.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.fullName ?: profile.username,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp,
                                    color = Color.White
                                )
                            )
                            if (profile.isVerified) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            text = "@${profile.username}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (!profile.bio.isNullOrBlank()) {
                            Surface(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = profile.bio,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        if (!isCurrentUser) {
                            RelationshipCard(viewModel, onConnectionPathClick)
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        ProfileInfoSection(profile, viewModel)

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        CommunicationHistorySection(viewModel)

                        if (!isCurrentUser) {
                            Spacer(modifier = Modifier.height(32.dp))
                            val rel by viewModel.relationship.collectAsState()
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (rel?.distance == 1) {
                                    Button(
                                        onClick = onMessageClick,
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Message", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.onNudgeClick() },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Nudge", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Surface(
                                    modifier = Modifier.size(56.dp).clickable { /* Call */ },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(icons.call, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }

                // Avatar - Layered on top of the Surface
                KeeftalkAvatar(
                    avatarUrl = profile.avatarUrl,
                    initials = AvatarUtils.getInitials(profile.fullName ?: profile.username),
                    seed = profile.id,
                    size = 110.dp,
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .offset(y = (-55).dp)
                        .shadow(16.dp, CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .zIndex(5f) // High zIndex to ensure it's on top of everything in this Box
                )

                if (isCurrentUser) {
                    Button(
                        onClick = onEditClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 24.dp)
                            .zIndex(4f), // Above surface but below avatar maybe (avatar is left aligned anyway)
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(icons.edit, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit")
                    }
                }
            }
        }

        // Top Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(44.dp).clickable { onBack() },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icons.back, null, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun RelationshipCard(viewModel: ProfileViewModel, onConnectionPathClick: () -> Unit) {
    val relationship by viewModel.relationship.collectAsState()
    
    AnimatedContent(
        targetState = relationship,
        transitionSpec = {
            fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(400))
        },
        label = "RelationshipContent"
    ) { rel ->
        if (rel == null) {
            RelationshipCardPlaceholder()
        } else {
            val primaryColor = MaterialTheme.colorScheme.primary
            
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = 0.15f),
                    0.5f to Color.White.copy(alpha = 0.05f),
                    1.0f to primaryColor.copy(alpha = 0.2f)
                )),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = primaryColor.copy(alpha = 0.2f)
                    )
                    .clickable { onConnectionPathClick() }
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E1E1E),
                                Color(0xFF121212)
                            )
                        ))
                        .padding(24.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Degree Indicator Badge - High Premium
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(primaryColor.copy(alpha = 0.1f), CircleShape)
                                    .border(1.dp, primaryColor.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (rel.distance) {
                                        1 -> "1st"
                                        2 -> "2nd"
                                        3 -> "3rd"
                                        else -> rel.distance?.toString() ?: "∞"
                                    },
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = primaryColor
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (rel.distance) {
                                        1 -> "Direct Connection"
                                        2 -> "Mutual Connection"
                                        3 -> "3rd Degree Connection"
                                        else -> "Network Member"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = when (rel.distance) {
                                        1 -> "In your contact list"
                                        2 -> "Connected through a contact"
                                        3 -> "Part of your extended network"
                                        else -> "Outside your immediate circle"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            
                            Surface(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        null,
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        if (rel.mutualConnectionCount > 0) {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Surface(
                                color = Color.White.copy(alpha = 0.03f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (rel.mutualConnectionPreview.isNotEmpty()) {
                                        val visibleAvatars = rel.mutualConnectionPreview.take(3)
                                        val remainingCount = rel.mutualConnectionCount - visibleAvatars.size
                                        
                                        Box(modifier = Modifier.height(36.dp)) {
                                            visibleAvatars.forEachIndexed { index, profile ->
                                                KeeftalkAvatar(
                                                    avatarUrl = profile.avatarUrl,
                                                    initials = AvatarUtils.getInitials(profile.fullName ?: profile.username),
                                                    seed = profile.id,
                                                    size = 36.dp,
                                                    modifier = Modifier
                                                        .padding(start = (index * 24).dp)
                                                        .border(2.dp, Color(0xFF1E1E1E), CircleShape)
                                                )
                                            }
                                            
                                            if (remainingCount > 0) {
                                                Surface(
                                                    color = primaryColor,
                                                    shape = CircleShape,
                                                    border = BorderStroke(2.dp, Color(0xFF1E1E1E)),
                                                    modifier = Modifier
                                                        .padding(start = (visibleAvatars.size * 24).dp)
                                                        .size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "+$remainingCount",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 11.sp,
                                                                color = Color.Black
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        val stackWidth = (visibleAvatars.size + (if (remainingCount > 0) 1 else 0)) * 24 + 12
                                        Spacer(modifier = Modifier.width(stackWidth.dp))
                                    }
                                    
                                    val names = rel.mutualConnectionPreview.take(1).firstOrNull()?.let { it.fullName ?: it.username }
                                    Text(
                                        text = if (names != null) {
                                            if (rel.mutualConnectionCount > 1) "Connected through $names and ${rel.mutualConnectionCount - 1} others"
                                            else "Connected through $names"
                                        } else "${rel.mutualConnectionCount} mutual contacts",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
fun RelationshipCardPlaceholder() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        color = Color.White.copy(alpha = alpha),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth().height(110.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(14.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                )
            }
        }
    }
}

@Composable
fun ProfileInfoSection(profile: Profile, viewModel: ProfileViewModel) {
    val icons = LocalAppIcons.current
    val context = LocalContext.current
    val relationship by viewModel.relationship.collectAsState()
    val phoneNumberService = remember { com.keeftalk.chat.di.AppModule.providePhoneNumberService(context) }
    
    val formattedPhone = remember(profile.phone, profile.countryCode) {
        if (profile.phone != null && profile.countryCode != null) {
            phoneNumberService.formatForDisplay(profile.phone, profile.countryCode)
        } else {
            profile.phone ?: "Not set"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoRow(icons.email, "Email", profile.email ?: "Not set")
        InfoRow(icons.phone, "Phone", formattedPhone)
        InfoRow(icons.place, "Location", profile.country ?: "Not set")
        InfoRow(icons.calendar, "Joined", "Member since ${formatJoinDate(profile.joinDate)}")
        
        InfoRow(Icons.Default.RemoveRedEye, "Profile Views", "${profile.viewsCount} views")

        relationship?.let {
            InfoRow(Icons.Default.NotificationsActive, "Nudges", "${it.nudgesReceived} times nudged")
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileContent(
    profile: Profile,
    onProfileUpdate: (Profile) -> Unit,
    onAvatarClick: () -> Unit,
    onCoverClick: () -> Unit,
    viewModel: ProfileViewModel,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icons = LocalAppIcons.current
    var fullName by remember { mutableStateOf(profile.fullName ?: "") }
    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio ?: "") }
    var showCountryPicker by remember { mutableStateOf(false) }
    val currentCountry by viewModel.currentCountry.collectAsState()
    val usernameAvailable by viewModel.usernameAvailable.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(Color.Black)) {
        TopAppBar(
            title = { Text("Edit Profile", fontWeight = FontWeight.Black, color = Color.White) },
            navigationIcon = {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            actions = {
                TextButton(onClick = onSave) {
                    Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Media Change Section
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(100.dp)) {
                    KeeftalkAvatar(
                        avatarUrl = profile.avatarUrl,
                        initials = AvatarUtils.getInitials(fullName),
                        seed = profile.id,
                        size = 100.dp,
                        modifier = Modifier.clip(CircleShape).clickable { onAvatarClick() }
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).size(32.dp).clickable { onAvatarClick() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Icon(icons.camera, null, modifier = Modifier.padding(8.dp).size(16.dp))
                    }
                }
                
                Surface(
                    modifier = Modifier.weight(1f).height(100.dp).clickable { onCoverClick() },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (profile.coverUrl != null) {
                            AsyncImage(model = profile.coverUrl, contentDescription = null, contentScale = ContentScale.Crop)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icons.image, null, tint = Color.White.copy(alpha = 0.5f))
                            Text("Change Cover", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; onProfileUpdate(profile.copy(fullName = it)) },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Person, null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                )
            )
            
            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it
                    viewModel.checkUsername(it)
                    onProfileUpdate(profile.copy(username = it)) 
                },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("@") },
                shape = RoundedCornerShape(16.dp),
                isError = usernameAvailable == false,
                supportingText = {
                    if (usernameAvailable == false) {
                        Text("Username already taken", color = MaterialTheme.colorScheme.error)
                    } else if (usernameAvailable == true) {
                        Text("Username available", color = Color(0xFF4CAF50))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                )
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { 
                    if (it.length <= 150) {
                        bio = it
                        onProfileUpdate(profile.copy(bio = it))
                    }
                },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                supportingText = {
                    Text("${bio.length}/150", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End, color = Color.White.copy(alpha = 0.5f))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                )
            )

            // Read-only Info for Consistency
            OutlinedTextField(
                value = profile.email ?: "Not set",
                onValueChange = {},
                readOnly = true,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(icons.email, null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White.copy(alpha = 0.5f),
                    unfocusedTextColor = Color.White.copy(alpha = 0.5f),
                    focusedBorderColor = Color.White.copy(alpha = 0.1f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                )
            )

            OutlinedTextField(
                value = profile.country ?: "Not set",
                onValueChange = {},
                readOnly = true,
                label = { Text("Location") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCountryPicker = true },
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(icons.place, null) },
                enabled = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    disabledTextColor = Color.White,
                    disabledBorderColor = Color.White.copy(alpha = 0.1f),
                    disabledLabelColor = Color.White.copy(alpha = 0.5f),
                    disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                )
            )
            
            val selectedCountry = remember(profile.countryCode) {
                com.keeftalk.chat.util.CountryData.getByIso(profile.countryCode) ?: currentCountry
            }

            com.keeftalk.chat.ui.components.KeeftalkPhoneField(
                value = profile.phone ?: "",
                onValueChange = { viewModel.onPhoneChange(it) },
                selectedCountry = selectedCountry,
                onCountryClick = { showCountryPicker = true },
                label = "Phone"
            )
            
            if (showCountryPicker) {
                com.keeftalk.chat.ui.components.CountryPickerDialog(
                    onCountrySelected = { viewModel.onCountryChange(it) },
                    onDismissRequest = { showCountryPicker = false }
                )
            }
        }
    }
}

@Composable
fun CommunicationHistorySection(viewModel: ProfileViewModel) {
    val callHistory by viewModel.callHistory.collectAsState(initial = emptyList())
    val smsHistory by viewModel.smsHistory.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }

    Column {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = Color.White.copy(alpha = 0.5f)
            ) {
                Text("Calls", modifier = Modifier.padding(12.dp), fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
            }
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = Color.White.copy(alpha = 0.5f)
            ) {
                Text("SMS", modifier = Modifier.padding(12.dp), fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (selectedTab) {
            0 -> {
                if (callHistory.isEmpty()) {
                    Text("No call history", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.4f))
                } else {
                    callHistory.take(5).forEach { entry ->
                        CallHistoryItem(entry)
                    }
                }
            }
            1 -> {
                if (smsHistory.isEmpty()) {
                    Text("No SMS history", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.4f))
                } else {
                    smsHistory.take(5).forEach { message ->
                        SmsHistoryItem(message)
                    }
                }
            }
        }
    }
}

@Composable
fun CallHistoryItem(entry: com.keeftalk.chat.data.local.CallLogEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (entry.type) {
                com.keeftalk.chat.data.local.CallLogType.MISSED -> Icons.AutoMirrored.Filled.CallMissed
                com.keeftalk.chat.data.local.CallLogType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
                else -> Icons.AutoMirrored.Filled.CallReceived
            },
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (entry.type == com.keeftalk.chat.data.local.CallLogType.MISSED) Color.Red else Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (entry.isKeeftalk) "Keeftalk Call" else "GSM Call",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = com.keeftalk.chat.ui.screens.formatRelativeTime(entry.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SmsHistoryItem(message: com.keeftalk.chat.domain.repository.SmsMessage) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (message.type == 2) Icons.Default.ArrowOutward else Icons.Default.ArrowDownward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            Text(
                text = com.keeftalk.chat.ui.screens.formatRelativeTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
