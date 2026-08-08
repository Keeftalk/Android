package com.keeftalk.chat.ui.vault

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.keeftalk.chat.domain.model.VaultItem
import com.keeftalk.chat.domain.model.VaultStorageInfo
import com.keeftalk.chat.domain.model.FileType

@Composable
fun VaultCategoryChip(
    title: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (isSelected) color.copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VaultStorageCard(
    info: VaultStorageInfo?,
    onAnalyzeClick: () -> Unit
) {
    val limit = 5L * 1024 * 1024 * 1024 // 5GB
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Cloud Storage", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = formatVaultSize(info?.totalUsed ?: 0),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }
                
                IconButton(
                    onClick = onAnalyzeClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val progress = (info?.totalUsed?.toFloat() ?: 0f) / limit.toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${formatVaultSize(info?.totalUsed ?: 0)} of ${formatVaultSize(limit)} used",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text("Upgrade Plan", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { })
            }
        }
    }
}

@Composable
fun VaultBottomNavigation(
    selectedTab: VaultTab,
    onTabSelected: (VaultTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(VaultTab.HOME, VaultTab.FOLDERS, VaultTab.FAVORITES, VaultTab.SHARED, VaultTab.TRASH)
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                    label = "tab_scale"
                )

                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.5f,
                    label = "tab_alpha"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTabSelected(tab)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = getIconForTab(tab),
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (tab == VaultTab.FOLDERS) "Folders" else tab.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.graphicsLayer { this.alpha = alpha }
                    )
                    
                    // Custom Animated Indicator
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(width = 12.dp, height = 3.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaultPieChart(
    info: VaultStorageInfo?,
    limit: Long,
    modifier: Modifier = Modifier
) {
    val categories = info?.categories ?: emptyMap()
    val totalUsed = info?.totalUsed ?: 0L
    val usedPercentage = (totalUsed.toFloat() / limit.toFloat() * 100).coerceIn(0f, 100f)

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(info) {
        animatedProgress.animateTo(1f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(240.dp)) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 38f
            val innerStrokeWidth = 30f
            
            // Draw Background Track (Full Circle)
            drawArc(
                color = Color.Gray.copy(alpha = 0.08f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )

            var startAngle = -90f
            
            // Sort categories to have a consistent visual order
            val sortedCategories = categories.toList().sortedByDescending { it.second }

            sortedCategories.forEach { (type, size) ->
                val sweepAngle = (size.toFloat() / limit.toFloat() * 360f) * animatedProgress.value
                if (sweepAngle > 0.1f) {
                    val color = getVaultIconColorForType(getFileTypeFromVaultType(type))
                    
                    // Glow effect for the slice
                    drawArc(
                        color = color.copy(alpha = 0.15f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth + 6f, cap = StrokeCap.Round)
                    )
                    
                    // Main slice
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = innerStrokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.1f%%".format(usedPercentage),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "OF 5 GB USED",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultStorageAnalyzer(
    info: VaultStorageInfo?,
    onDismiss: () -> Unit
) {
    val limit = 5L * 1024 * 1024 * 1024 // 5GB
    val totalUsed = info?.totalUsed ?: 0L
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Storage Analysis",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = "Detailed breakdown of your secured data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp).align(Alignment.Start)
            )

            VaultPieChart(info = info, limit = limit, modifier = Modifier.padding(vertical = 16.dp))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Legend / Detailed List
            Column(modifier = Modifier.fillMaxWidth()) {
                info?.categories?.forEach { (type, size) ->
                    val fileType = getFileTypeFromVaultType(type)
                    StorageBreakdownRow(
                        label = type.name.lowercase().replaceFirstChar { it.uppercase() },
                        size = size,
                        limit = limit,
                        color = getVaultIconColorForType(fileType),
                        icon = getVaultIconForType(fileType)
                    )
                }
                
                StorageBreakdownRow(
                    label = "Free Space",
                    size = (limit - totalUsed).coerceAtLeast(0),
                    limit = limit,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    icon = Icons.Default.CloudQueue
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Smart Recommendation
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Storage Tip", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Move large videos to the cloud to free up 1.2 GB", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { }) {
                        Text("Review")
                    }
                }
            }
        }
    }
}

@Composable
fun StorageBreakdownRow(
    label: String,
    size: Long,
    limit: Long,
    color: Color,
    icon: ImageVector
) {
    val percentage = (size.toFloat() / limit.toFloat() * 100).coerceAtMost(100f)
    Row(
        modifier = Modifier
            .padding(vertical = 14.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "%.1f%% of total".format(percentage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Text(
            text = formatVaultSize(size),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getFileTypeFromVaultType(type: com.keeftalk.chat.domain.model.VaultItemType): FileType {
    return when(type) {
        com.keeftalk.chat.domain.model.VaultItemType.PHOTO -> FileType.IMAGE
        com.keeftalk.chat.domain.model.VaultItemType.VIDEO -> FileType.VIDEO
        com.keeftalk.chat.domain.model.VaultItemType.AUDIO -> FileType.AUDIO
        com.keeftalk.chat.domain.model.VaultItemType.DOCUMENT -> FileType.DOCUMENT
        else -> FileType.OTHER
    }
}

@Composable
fun VaultItemGrid(
    item: VaultItem,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.file?.thumbnailPath != null) {
                    AsyncImage(
                        model = item.file.thumbnailPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = getVaultIconForType(item.file?.fileType), 
                        contentDescription = null, 
                        modifier = Modifier.size(44.dp),
                        tint = getVaultIconColorForType(item.file?.fileType)
                    )
                }
                
                if (item.locked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(12.dp))
                    }
                }
            }
            Column(modifier = Modifier.padding(start = 16.dp, end = 12.dp, bottom = 16.dp, top = 4.dp)) {
                Text(
                    text = item.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatVaultSize(item.file?.fileSize ?: 0),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun VaultItemList(
    item: VaultItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getVaultIconForType(item.file?.fileType),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = getVaultIconColorForType(item.file?.fileType)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${item.file?.fileType?.name ?: "UNKNOWN"} • ${formatVaultSize(item.file?.fileSize ?: 0)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun getVaultIconForType(type: FileType?): ImageVector {
    return when (type) {
        FileType.IMAGE -> Icons.Outlined.Image
        FileType.VIDEO -> Icons.Outlined.VideoLibrary
        FileType.AUDIO -> Icons.Outlined.MusicNote
        FileType.DOCUMENT -> Icons.Outlined.Description
        else -> Icons.Outlined.Description
    }
}

fun getVaultIconColorForType(type: FileType?): Color {
    return when (type) {
        FileType.IMAGE -> Color(0xFF3B82F6)
        FileType.VIDEO -> Color(0xFFEF4444)
        FileType.AUDIO -> Color(0xFF10B981)
        FileType.DOCUMENT -> Color(0xFFF59E0B)
        else -> Color(0xFF94A3B8)
    }
}

fun formatVaultSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}

@Composable
fun PremiumSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        "Search vault...", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ) 
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultItemActionsBottomSheet(
    item: VaultItem,
    onDismiss: () -> Unit,
    onAction: (VaultAction) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getVaultIconForType(item.file?.fileType), null, tint = getVaultIconColorForType(item.file?.fileType))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(formatVaultSize(item.file?.fileSize ?: 0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (item.isDeleted) {
                VaultActionItem("Restore File", Icons.Default.Restore, Color(0xFF10B981)) { onAction(VaultAction.RESTORE) }
                VaultActionItem("Delete Permanently", Icons.Default.DeleteForever, MaterialTheme.colorScheme.error) { onAction(VaultAction.DELETE_PERMANENT) }
            } else {
                VaultActionItem("Open / View", Icons.Default.Visibility, MaterialTheme.colorScheme.onSurface) { onAction(VaultAction.OPEN) }
                VaultActionItem("Send Internally", Icons.AutoMirrored.Filled.Send, MaterialTheme.colorScheme.primary) { onAction(VaultAction.SEND_INTERNAL) }
                VaultActionItem("Copy File", Icons.Default.ContentCopy, MaterialTheme.colorScheme.onSurface) { onAction(VaultAction.COPY) }
                VaultActionItem("Cut File", Icons.Default.ContentCut, MaterialTheme.colorScheme.onSurface) { onAction(VaultAction.CUT) }
                VaultActionItem("Download to Device", Icons.Default.Download, MaterialTheme.colorScheme.onSurface) { onAction(VaultAction.DOWNLOAD) }
                VaultActionItem(if (item.favorite) "Remove from Starred" else "Add to Starred", if (item.favorite) Icons.Default.Star else Icons.Default.StarOutline, Color(0xFFF59E0B)) { onAction(VaultAction.TOGGLE_FAVORITE) }
                VaultActionItem("Share Externally", Icons.Default.Share, MaterialTheme.colorScheme.onSurface) { onAction(VaultAction.SHARE) }
                VaultActionItem("Move to Trash", Icons.Default.Delete, MaterialTheme.colorScheme.error) { onAction(VaultAction.MOVE_TO_TRASH) }
                VaultActionItem("Rename", Icons.Default.Edit, MaterialTheme.colorScheme.onSurface) { onAction(VaultAction.RENAME) }
                VaultActionItem("File Info", Icons.Default.Info, MaterialTheme.colorScheme.onSurface) { onAction(VaultAction.INFO) }
            }
        }
    }
}

@Composable
fun VaultActionItem(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

enum class VaultAction {
    OPEN, SEND_INTERNAL, COPY, CUT, DOWNLOAD, TOGGLE_FAVORITE, SHARE, MOVE_TO_TRASH, RENAME, INFO, RESTORE, DELETE_PERMANENT
}

@Composable
fun VaultFileInfoDialog(
    item: VaultItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("File Information") },
        text = {
            Column {
                InfoRow("Name", item.title)
                InfoRow("Type", item.file?.fileType?.name ?: "Unknown")
                InfoRow("Size", formatVaultSize(item.file?.fileSize ?: 0))
                InfoRow("Created", java.text.DateFormat.getDateTimeInstance().format(java.util.Date(item.createdAt)))
                InfoRow("Encryption", "AES-256 (Cloud-E2EE)")
                InfoRow("Owner ID", item.userId.take(8) + "...")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CloudImportBanner(
    onImportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bannerGlow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    ) {
        Box {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)), 
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudQueue, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bring your souvenirs to life",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Securely import from Google Photos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    onClick = onImportClick,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(44.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text("Import", fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPickerDialog(
    chats: List<com.keeftalk.chat.domain.model.Chat>,
    contacts: List<com.keeftalk.chat.domain.model.User>,
    onDismiss: () -> Unit,
    onChatsSelected: (List<String>) -> Unit
) {
    val selectedChats = remember { mutableStateListOf<String>() }
    var searchQuery by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onChatsSelected(selectedChats.toList()) },
                enabled = selectedChats.isNotEmpty()
            ) {
                Text("Send (${selectedChats.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Select Chats") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    placeholder = { Text("Search contacts...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    val filteredChats = chats.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
                    val filteredContacts = contacts.filter { (it.name ?: it.username).contains(searchQuery, ignoreCase = true) }

                    if (filteredChats.isNotEmpty()) {
                        item {
                            Text(
                                "RECENT CHATS", 
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), 
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )
                        }
                        filteredChats.forEach { chat ->
                            item(key = "chat_${chat.id}") {
                                ChatPickerItem(
                                    title = chat.displayName,
                                    isSelected = selectedChats.contains(chat.id),
                                    onClick = {
                                        if (selectedChats.contains(chat.id)) selectedChats.remove(chat.id)
                                        else selectedChats.add(chat.id)
                                    }
                                )
                            }
                        }
                    }

                    if (filteredContacts.isNotEmpty()) {
                        item {
                            Text(
                                "CONTACTS", 
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), 
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        filteredContacts.forEach { user ->
                            item(key = "user_${user.id}") {
                                ChatPickerItem(
                                    title = user.name ?: user.username,
                                    isSelected = selectedChats.contains(user.id), // In this app, chat id for DMs often equals user id or maps to it
                                    onClick = {
                                        if (selectedChats.contains(user.id)) selectedChats.remove(user.id)
                                        else selectedChats.add(user.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun ChatPickerItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isSelected, onCheckedChange = { onClick() })
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun VaultInputDialog(
    title: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSortBottomSheet(
    currentOrder: VaultSortOrder,
    onDismiss: () -> Unit,
    onSortSelected: (VaultSortOrder) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            VaultSortItem("Name (A-Z)", VaultSortOrder.NAME_ASC, currentOrder) { onSortSelected(it) }
            VaultSortItem("Name (Z-A)", VaultSortOrder.NAME_DESC, currentOrder) { onSortSelected(it) }
            VaultSortItem("Newest First", VaultSortOrder.DATE_DESC, currentOrder) { onSortSelected(it) }
            VaultSortItem("Oldest First", VaultSortOrder.DATE_ASC, currentOrder) { onSortSelected(it) }
            VaultSortItem("Size (Large First)", VaultSortOrder.SIZE_DESC, currentOrder) { onSortSelected(it) }
            VaultSortItem("Size (Small First)", VaultSortOrder.SIZE_ASC, currentOrder) { onSortSelected(it) }
        }
    }
}

@Composable
fun VaultSortItem(
    title: String,
    order: VaultSortOrder,
    currentOrder: VaultSortOrder,
    onClick: (VaultSortOrder) -> Unit
) {
    val isSelected = order == currentOrder
    Surface(
        onClick = { onClick(order) },
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title, 
                style = MaterialTheme.typography.bodyLarge, 
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
