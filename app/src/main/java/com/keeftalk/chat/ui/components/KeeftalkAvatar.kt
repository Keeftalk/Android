package com.keeftalk.chat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.keeftalk.chat.ui.theme.OnlineColor
import com.keeftalk.chat.util.AvatarUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface

import androidx.compose.ui.graphics.graphicsLayer
import coil.decode.DataSource
import com.keeftalk.chat.util.PersistentAvatarManager
import java.io.File

@Composable
fun KeeftalkAvatar(
    avatarUrl: String?,
    initials: String,
    modifier: Modifier = Modifier,
    seed: String? = null,
    size: Dp = 40.dp,
    fontSize: Float = if (size < 20.dp) (size.value * 0.6f) else (size.value * 0.4f),
    isOnline: Boolean = false,
    isKeeftalkUser: Boolean = false,
    isSkeleton: Boolean = false
) {
    val context = LocalContext.current
    val avatarColor = remember(seed, initials) { AvatarUtils.getAvatarColor(seed ?: initials) }
    val textColor = remember(avatarColor) { AvatarUtils.getTextColorForBackground(avatarColor) }
    
    // Persistent caching logic
    var localAvatarFile by remember(seed) { 
        mutableStateOf(seed?.let { PersistentAvatarManager.getLocalAvatarFile(context, it) }) 
    }

    val borderWidth = if (size < 20.dp) 1.dp else 2.dp

    Box(modifier = modifier.size(size)) {
        // Avatar circular container
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = avatarColor,
            border = BorderStroke(borderWidth, Color.White)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (!isSkeleton && (!avatarUrl.isNullOrBlank() || localAvatarFile != null)) {
                    val model = remember(avatarUrl, localAvatarFile) {
                        ImageRequest.Builder(context)
                            .data(localAvatarFile ?: avatarUrl)
                            .crossfade(true)
                            .size(width = 256, height = 256)
                            .transformations(CircleCropTransformation())
                            .listener(
                                onSuccess = { _, result ->
                                    // If we successfully loaded from network, save it locally for persistence
                                    if (result.dataSource == DataSource.NETWORK && seed != null) {
                                        val drawable = result.drawable
                                        if (drawable is android.graphics.drawable.BitmapDrawable) {
                                            val bitmap = drawable.bitmap
                                            val stream = java.io.ByteArrayOutputStream()
                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                                            PersistentAvatarManager.saveAvatar(context, seed, stream.toByteArray())
                                        }
                                    }
                                }
                            )
                            .build()
                    }
                    
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center
                    )
                } else {
                    InitialsAvatar(initials, fontSize, textColor)
                }
            }
        }
        
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .background(OnlineColor, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        } else if (isKeeftalkUser) {
            Box(
                modifier = Modifier
                    .size(size * 0.35f)
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(size * 0.05f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = com.keeftalk.chat.R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@Composable
private fun InitialsAvatar(initials: String, fontSize: Float, textColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = fontSize.sp,
                color = textColor
            )
        )
    }
}
