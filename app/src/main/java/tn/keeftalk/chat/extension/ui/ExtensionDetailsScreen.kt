package tn.keeftalk.chat.extension.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import tn.keeftalk.chat.extension.data.RemoteReview
import tn.keeftalk.chat.extension.data.RemoteStoreExtension

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionDetailsScreen(
    extension: RemoteStoreExtension,
    isInstalled: Boolean,
    downloadStatus: DownloadStatus = DownloadStatus.Idle,
    reviews: List<RemoteReview> = emptyList(),
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onSubmitReview: ((Int, String, String) -> Unit)? = null,
    onOpenPublisher: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    var isExpandedDescription by remember { mutableStateOf(false) }
    var showUninstallConfirmSheet by remember { mutableStateOf(false) }
    var showWriteReviewSheet by remember { mutableStateOf(false) }
    var showThanksDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(extension.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isInstalled) {
                        OutlinedButton(
                            onClick = { showUninstallConfirmSheet = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Uninstall")
                        }
                        Button(
                            onClick = { /* Open Extension */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open")
                        }
                    } else {
                        Button(
                            onClick = onInstall,
                            enabled = downloadStatus is DownloadStatus.Idle,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (downloadStatus) {
                                is DownloadStatus.Downloading -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                        Text("Downloading ${downloadStatus.progressPct}%")
                                    }
                                }
                                is DownloadStatus.Verifying -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                        Text("Verifying Signature…")
                                    }
                                }
                                is DownloadStatus.Installing -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                        Text("Installing…")
                                    }
                                }
                                else -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Text("Install Extension")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Hero Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!extension.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = extension.coverUrl,
                                contentDescription = extension.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(extension.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { onOpenPublisher?.invoke() }
                        ) {
                            if (extension.publisher?.isVerified == true) {
                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color(0xFF30D158), modifier = Modifier.size(16.dp))
                            }
                            Text(
                                extension.publisher?.displayName ?: "Keeftalk Official",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(extension.shortDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "RATING", value = "★ ${"%.1f".format(extension.ratingAvg)}")
                StatItem(label = "REVIEWS", value = "${extension.ratingCount}")
                StatItem(label = "DOWNLOADS", value = if (extension.downloadCount > 1000) "${extension.downloadCount / 1000}k" else "${extension.downloadCount}")
                StatItem(label = "AGE", value = extension.ageRating)
            }

            // Screenshots Gallery (Only if available, no mockup placeholders)
            val shots = extension.screenshots
            if (shots.isNotEmpty()) {
                HorizontalDivider()
                Text("Screenshots & Previews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(shots) { url ->
                        Card(
                            modifier = Modifier.size(180.dp, 340.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Screenshot",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // What's New
            Text("What's New", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Version ${extension.currentVersion} • Performance improvements, offline asset updates, and bug fixes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            HorizontalDivider()

            // About
            Text("About This Extension", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = if (isExpandedDescription) extension.longDescription.ifBlank { extension.shortDescription }
                else (extension.longDescription.ifBlank { extension.shortDescription }).take(180) + "...",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = { isExpandedDescription = !isExpandedDescription }) {
                Text(if (isExpandedDescription) "Show Less" else "Read More")
            }

            HorizontalDivider()

            // Ratings & Reviews Header with "Write a Review" button in yellow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ratings & Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isInstalled) {
                    TextButton(onClick = { showWriteReviewSheet = true }) {
                        Text(
                            "Write a Review",
                            color = Color(0xFFFF9F0A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "%.1f".format(extension.ratingAvg),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 38.sp
                        )
                        Row {
                            (1..5).forEach { star ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (star <= extension.ratingAvg.toInt()) Color(0xFFFF9F0A) else Color.Gray.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${extension.ratingCount} Ratings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Rating Distribution Histogram
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(5 to 0.75f, 4 to 0.18f, 3 to 0.04f, 2 to 0.02f, 1 to 0.01f).forEach { (star, pct) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("$star", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = Color(0xFFFF9F0A),
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            }

            // Individual Reviews List
            reviews.forEach { rv ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(rv.authorName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text(rv.authorName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(rv.createdAt.take(10).ifBlank { "Recently" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            (1..5).forEach { star ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (star <= rv.rating) Color(0xFFFF9F0A) else Color.Gray.copy(alpha = 0.3f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (rv.title.isNotBlank()) {
                            Text(rv.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (rv.reviewText.isNotBlank()) {
                            Text(rv.reviewText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }

                        if (!rv.developerReply.isNullOrBlank()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Developer Response", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(rv.developerReply, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            if (!isInstalled) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Install to Review", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("You must install this extension before leaving a rating or review.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider()

            // Works With Modules
            Text("Works With", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                extension.compatibleModules.ifEmpty { listOf("MY_FEED") }.forEach { mod ->
                    SuggestionChip(onClick = {}, label = { Text(mod) })
                }
            }

            HorizontalDivider()

            // Information Grid (including Size down)
            Text("Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            InfoRow("Provider", extension.publisher?.displayName ?: "Keeftalk")
            InfoRow("Category", extension.category.uppercase())
            InfoRow("Size", if (extension.packageSizeBytes > 0) "${extension.packageSizeBytes / 1024 / 1024} MB" else "4.9 MB")
            InfoRow("Age Rating", extension.ageRating)
            InfoRow("Languages", extension.languages.joinToString(", ").ifBlank { "English, Arabic, French" })
            InfoRow("Compatibility", "Requires Keeftalk v${extension.minAppVersion} or higher")
        }
    }

    if (showUninstallConfirmSheet) {
        ModalBottomSheet(
            onDismissRequest = { showUninstallConfirmSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Text("Uninstall '${extension.name}'?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "This will remove all package assets, offline cached definitions, and isolated extension preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showUninstallConfirmSheet = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            showUninstallConfirmSheet = false
                            onUninstall()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Uninstall")
                    }
                }
            }
        }
    }

    // Write a Review Modal Bottom Sheet
    if (showWriteReviewSheet) {
        var userRating by remember { mutableIntStateOf(5) }
        var userReviewTitle by remember { mutableStateOf("") }
        var userReviewText by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { showWriteReviewSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Write a Review", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { userRating = star },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= userRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star Stars",
                                tint = if (star <= userRating) Color(0xFFFF9F0A) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = userReviewTitle,
                    onValueChange = { userReviewTitle = it },
                    placeholder = { Text("Title (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = userReviewText,
                    onValueChange = { userReviewText = it },
                    placeholder = { Text("Write your review for ${extension.name}...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        showWriteReviewSheet = false
                        onSubmitReview?.invoke(userRating, userReviewTitle.trim(), userReviewText.trim())
                        showThanksDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Review")
                }
            }
        }
    }

    // Thanks Dialog with required review publication message
    if (showThanksDialog) {
        AlertDialog(
            onDismissRequest = { showThanksDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(48.dp)) },
            title = { Text("Thank you!", fontWeight = FontWeight.Bold) },
            text = { Text("Thank you! That helps us, and we will review your review and then publish it.") },
            confirmButton = {
                Button(onClick = { showThanksDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
