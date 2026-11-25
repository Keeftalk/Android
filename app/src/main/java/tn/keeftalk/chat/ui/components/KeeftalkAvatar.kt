package tn.keeftalk.chat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import tn.keeftalk.chat.ui.theme.OnlineColor
import tn.keeftalk.chat.util.AvatarUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface

import androidx.compose.ui.graphics.graphicsLayer
import coil.decode.DataSource
import tn.keeftalk.chat.util.PersistentAvatarManager
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
    isSkeleton: Boolean = false,
    useOriginal: Boolean = false
) {
    val context = LocalContext.current
    val avatarColor = remember(seed, initials) { AvatarUtils.getAvatarColor(seed ?: initials) }
    val textColor = remember(avatarColor) { AvatarUtils.getTextColorForBackground(avatarColor) }
    
    val resolvedUrl = remember(avatarUrl, useOriginal) {
        if (useOriginal) AvatarUtils.toOriginalUrl(avatarUrl) else AvatarUtils.toCompressedUrl(avatarUrl)
    }

    // Persistent caching logic checking URL match
    var localAvatarFile by remember(seed, resolvedUrl) { 
        mutableStateOf(seed?.let { PersistentAvatarManager.getLocalAvatarFile(context, it, resolvedUrl) }) 
    }

    var isLoadFailed by remember(resolvedUrl) { mutableStateOf(false) }

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
                if (!isSkeleton && !isLoadFailed && (!resolvedUrl.isNullOrBlank() || localAvatarFile != null)) {
                    val targetPx = remember(size) {
                        val density = context.resources.displayMetrics.density
                        (size.value * density).toInt().coerceAtLeast(128)
                    }
                    val model = remember(resolvedUrl, localAvatarFile) {
                        ImageRequest.Builder(context)
                            .data(localAvatarFile ?: resolvedUrl)
                            .crossfade(true)
                            .size(width = targetPx, height = targetPx)
                            .transformations(CircleCropTransformation())
                            .listener(
                                onSuccess = { _, result ->
                                    if (result.dataSource == DataSource.NETWORK && seed != null && resolvedUrl != null) {
                                        val drawable = result.drawable
                                        if (drawable is android.graphics.drawable.BitmapDrawable) {
                                            val bitmap = drawable.bitmap
                                            val stream = java.io.ByteArrayOutputStream()
                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                                            PersistentAvatarManager.saveAvatar(context, seed, stream.toByteArray(), resolvedUrl)
                                        }
                                    }
                                },
                                onError = { _, _ ->
                                    isLoadFailed = true
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
                    if (initials == "?") {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(size * 0.5f),
                            tint = textColor
                        )
                    } else {
                        InitialsAvatar(initials, fontSize, textColor)
                    }
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
                    painter = painterResource(id = tn.keeftalk.chat.R.drawable.ic_app_logo),
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
