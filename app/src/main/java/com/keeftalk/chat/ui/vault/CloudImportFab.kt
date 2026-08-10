package com.keeftalk.chat.ui.vault

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CloudImportFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cloudLogos: List<Int> = listOf(
        R.drawable.dropbox_96,
        R.drawable.igoogle_drive_96,
        R.drawable.google_photos_96,
        R.drawable.icloud_96,
        R.drawable.microsoft_onedrive_96
    )

    var currentLogoIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3.seconds)
            currentLogoIndex = (currentLogoIndex + 1) % cloudLogos.size
        }
    }

    // Floating image button - purely the PNG with animation
    // Size reduced by 30% from 56dp -> 40dp
    Box(
        modifier = modifier
            .size(40.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Image-only look
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = cloudLogos[currentLogoIndex],
            transitionSpec = {
                // Enter: fade in and scale up after 1s delay
                // Exit: fade out and spin 360 after 1s delay
                (fadeIn(animationSpec = tween(1000, delayMillis = 1000)) + 
                 scaleIn(animationSpec = tween(1000, delayMillis = 1000), initialScale = 0.8f))
                    .togetherWith(
                        fadeOut(animationSpec = tween(1000, delayMillis = 1000)) + 
                        scaleOut(animationSpec = tween(1000, delayMillis = 1000), targetScale = 0.8f)
                    )
            },
            label = "CloudLogoAnimation"
        ) { logoRes ->
            // Animate rotation based on the internal EnterExitState of the transition
            val rotation by transition.animateFloat(
                transitionSpec = { tween(1000, delayMillis = 1000) },
                label = "Rotation"
            ) { enterExitState ->
                if (enterExitState == EnterExitState.PreEnter || enterExitState == EnterExitState.Visible) 0f else 360f
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(logoRes)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cloud Import",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = rotation
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}
