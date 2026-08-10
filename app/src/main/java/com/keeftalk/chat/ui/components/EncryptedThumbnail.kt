package com.keeftalk.chat.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.domain.model.File
import com.keeftalk.chat.domain.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EncryptedThumbnail(
    file: File?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    chatId: String? = null
) {
    val context = LocalContext.current
    val vaultRepo = remember { AppModule.provideVaultRepository(context) }
    
    val thumbPathResult = remember(file?.id) { mutableStateOf<String?>(file?.thumbnailLocalPath) }
    var thumbPath by thumbPathResult
    var isLoading by remember(file?.id) { mutableStateOf(false) }

    LaunchedEffect(file?.id) {
        if (file == null) return@LaunchedEffect
        
        val currentPath = thumbPath
        if ((currentPath == null) || !java.io.File(currentPath).exists()) {
            isLoading = true
            Log.d("VAULT_THUMB", "[VaultThumbnail] fileId=${file.id} | createdAt=${file.createdAt} | action=UI_REQUEST_START")
            val result = withContext(Dispatchers.IO) {
                val vaultItem = com.keeftalk.chat.domain.model.VaultItem(
                    id = "", userId = "", file = file, title = ""
                )
                vaultRepo.ensureThumbnail(vaultItem, chatId)
            }
            thumbPath = result.getOrNull()
            isLoading = false
            Log.d("VAULT_THUMB", "[VaultThumbnail] fileId=${file.id} | action=UI_REQUEST_COMPLETE | success=${thumbPath != null}")
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        when {
            thumbPath != null && java.io.File(thumbPath!!).exists() -> {
                AsyncImage(
                    model = thumbPath,
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
                
                if (file?.fileType == FileType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
            else -> {
                // Fallback to Icon
                val (icon, color) = getIconForFile(file)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(if (modifier == Modifier.fillMaxSize()) 48.dp else 24.dp)
                    )
                }
            }
        }
    }
}

private fun getIconForFile(file: File?): Pair<ImageVector, Color> {
    val type = file?.fileType ?: FileType.OTHER
    return when (type) {
        FileType.IMAGE -> Icons.Outlined.Image to Color(0xFF3B82F6)
        FileType.VIDEO -> Icons.Outlined.VideoLibrary to Color(0xFFEF4444)
        FileType.AUDIO -> Icons.Outlined.MusicNote to Color(0xFF10B981)
        FileType.DOCUMENT -> Icons.Outlined.Description to Color(0xFFF59E0B)
        FileType.OTHER -> {
            val mime = file?.mimeType?.lowercase() ?: ""
            when {
                mime.contains("zip") || mime.contains("archive") -> Icons.Outlined.FolderZip to Color(0xFF8B5CF6)
                else -> Icons.AutoMirrored.Outlined.InsertDriveFile to Color(0xFF94A3B8)
            }
        }
    }
}
