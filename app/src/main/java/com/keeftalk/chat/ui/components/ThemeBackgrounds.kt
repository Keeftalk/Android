package com.keeftalk.chat.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun RosaThemeBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "RosaBackground")
    
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "gradientShift"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // 1. Space Layer (HTML .space)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(Color(0xFFFCE4EC), Color(0xFFF8BBD0), Color(0xFFF48FB1), Color(0xFFF06292))
            
            // Simulating background-size: 400% 400% and background-position animation
            // 145deg is approximately from Top-Left to Bottom-Right
            val startOffset = Offset(widthPx * (gradientShift - 0.5f), heightPx * (gradientShift - 0.5f))
            val endOffset = Offset(widthPx * (gradientShift + 0.5f), heightPx * (gradientShift + 0.5f))

            drawRect(
                brush = Brush.linearGradient(
                    colors = colors,
                    start = startOffset,
                    end = endOffset
                )
            )
        }

        with(density) {
            // 2. Bokeh Container (HTML .bokeh-container)
            // Bokeh 1: top: -50px; left: -50px; width: 300px; duration: 18s
            BokehSphere(Color(0xFFFF9A9E), Offset(-50.dp.toPx(), -50.dp.toPx()), 300.dp.toPx(), 18000)
            
            // Bokeh 2: bottom: -100px; right: -100px; width: 400px; duration: 25s; delay: 2s
            BokehSphere(
                Color(0xFFFECFEF), 
                Offset(widthPx - 300.dp.toPx(), heightPx - 300.dp.toPx()), 
                400.dp.toPx(), 
                25000, 
                2000
            )
            
            // Bokeh 3: top: 50%; left: 30%; width: 200px; duration: 15s; delay: 4s
            BokehSphere(Color(0xFFA18CD1), Offset(widthPx * 0.3f, heightPx * 0.5f), 200.dp.toPx(), 15000, 4000)
            
            // Bokeh 4: bottom: 20%; right: 10%; width: 250px; duration: 22s; delay: 1s
            BokehSphere(
                Color(0xFFFBC2EB), 
                Offset(widthPx * 0.9f - 250.dp.toPx(), heightPx * 0.8f - 250.dp.toPx()), 
                250.dp.toPx(), 
                22000, 
                1000
            )
        }

        // 3. Floating Hearts Layer (Synced with HTML .hearts)
        repeat(12) { index ->
            FloatingHeart(index)
        }
    }
}

@Composable
fun AlphaThemeBackground() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle Radial Gradients
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFB47828).copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.3f),
                    radius = size.minDimension * 0.8f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFB47828).copy(alpha = 0.04f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.7f),
                    radius = size.minDimension * 0.8f
                )
            )
        }

        // Minimal Floating Particles
        repeat(8) { index ->
            FloatingParticle(index)
        }
    }
}

@Composable
fun BokehSphere(color: Color, baseOffset: Offset, sizePx: Float, duration: Int, delay: Int = 0) {
    val infiniteTransition = rememberInfiniteTransition(label = "BokehSphere")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, delayMillis = delay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "animValue"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val x = baseOffset.x + (animValue * 80.dp.toPx())
        val y = baseOffset.y + (animValue * 60.dp.toPx())
        val radius = (sizePx / 2) * (1f + animValue * 0.2f)
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(x, y),
                radius = radius
            ),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

@Composable
fun FloatingHeart(index: Int) {
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.toFloat()
    val screenHeight = config.screenHeightDp.toFloat()
    
    val emojis = listOf("💖", "🌸", "💕", "✨", "💗", "🌷", "💓", "🩷", "💞", "🌸", "💖", "✨")
    val emoji = emojis[index % emojis.size]
    
    val startX = remember { 
        when(index) {
            0 -> screenWidth * 0.05f; 1 -> screenWidth * 0.15f; 2 -> screenWidth * 0.25f; 3 -> screenWidth * 0.35f
            4 -> screenWidth * 0.45f; 5 -> screenWidth * 0.55f; 6 -> screenWidth * 0.65f; 7 -> screenWidth * 0.75f
            8 -> screenWidth * 0.85f; 9 -> screenWidth * 0.95f; 10 -> screenWidth * 0.10f; 11 -> screenWidth * 0.50f
            else -> Random.nextFloat() * screenWidth
        }
    }
    val duration = remember { 
        when(index) {
            0 -> 22000; 1 -> 28000; 2 -> 18000; 3 -> 32000; 4 -> 20000; 5 -> 26000
            6 -> 30000; 7 -> 24000; 8 -> 21000; 9 -> 27000; 10 -> 35000; 11 -> 19000
            else -> Random.nextInt(18000, 35000)
        }
    }
    val delay = remember {
        when(index) {
            1 -> 2000; 2 -> 5000; 3 -> 1000; 4 -> 4000; 5 -> 7000
            6 -> 3000; 7 -> 6000; 8 -> 8000; 9 -> 2000; 10 -> 10000; 11 -> 5000
            else -> 0
        }
    }
    val fontSize = remember {
        when(index) {
            0 -> 14; 1 -> 22; 2 -> 16; 3 -> 26; 4 -> 18; 5 -> 24
            6 -> 15; 7 -> 19; 8 -> 13; 9 -> 21; 10 -> 12; 11 -> 23
            else -> 12 + Random.nextInt(15)
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "HeartTransition")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = screenHeight + 50,
        targetValue = -100f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, delayMillis = delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "yOffset"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, delayMillis = delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, delayMillis = delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "alpha"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, delayMillis = delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = emoji,
            modifier = Modifier
                .offset(x = startX.dp, y = yOffset.dp)
                .graphicsLayer {
                    rotationZ = rotation
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                },
            fontSize = fontSize.sp
        )
    }
}

@Composable
fun FloatingParticle(index: Int) {
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.toFloat()
    val screenHeight = config.screenHeightDp.toFloat()
    
    val startX = remember { Random.nextFloat() * screenWidth }
    val startY = remember { Random.nextFloat() * screenHeight }
    val duration = remember { Random.nextInt(22000, 38000) }
    val delay = remember { Random.nextInt(0, 8000) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "ParticleTransition")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -screenHeight,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, delayMillis = delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "yOffset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val y = (startY.dp.toPx() + yOffset.dp.toPx()) % size.height
        drawCircle(
            color = Color(0xFFC9A84C).copy(alpha = 0.2f),
            radius = 1.dp.toPx(),
            center = Offset(startX.dp.toPx(), if (y < 0) y + size.height else y)
        )
    }
}
