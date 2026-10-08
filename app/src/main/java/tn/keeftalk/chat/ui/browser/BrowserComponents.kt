package tn.keeftalk.chat.ui.browser

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.62f),
    borderColor: Color = Color.White.copy(alpha = 0.7f),
    cornerRadius: Dp = 22.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .blur(0.dp) // Ensures rendering surface
    ) {
        content()
    }
}

@Composable
fun SkyBackground(
    period: String,
    condition: String,
    cloudDensity: Int,
    windSpeed: Int
) {
    val sky1 = when (period) {
        "dawn" -> Color(0xFFff9d7a)
        "dusk" -> Color(0xFF4d3f95)
        "night" -> Color(0xFF070c24)
        else -> Color(0xFF7cc4f2)
    }

    val sky2 = when (period) {
        "dawn" -> Color(0xFFffe6c4)
        "dusk" -> Color(0xFFf58a6a)
        "night" -> Color(0xFF22316a)
        else -> Color(0xFFe3f4ff)
    }

    val veilColor = when (condition) {
        "rain" -> Color(0xFF37465c).copy(alpha = 0.5f)
        "storm" -> Color(0xFF191e30).copy(alpha = 0.65f)
        "snow" -> Color(0xFFcddceb).copy(alpha = 0.35f)
        "cloudy" -> Color(0xFF6e7887).copy(alpha = 0.35f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(sky1, sky2))
            )
    ) {
        // Veil overlay for weather
        if (veilColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(veilColor)
            )
        }

        // Celestial body (Sun / Moon) and stars
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (period == "night") {
                // Draw stars
                val starPoints = listOf(
                    Offset(width * 0.2f, height * 0.3f),
                    Offset(width * 0.6f, height * 0.18f),
                    Offset(width * 0.8f, height * 0.45f),
                    Offset(width * 0.35f, height * 0.62f),
                    Offset(width * 0.12f, height * 0.7f),
                    Offset(width * 0.92f, height * 0.12f)
                )
                for (pt in starPoints) {
                    drawCircle(Color.White, radius = 1.5.dp.toPx(), center = pt)
                }

                // Draw Moon
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFFcfd8f5)),
                        center = Offset(width - 70f, 90f),
                        radius = 32f
                    ),
                    radius = 30f,
                    center = Offset(width - 70f, 90f)
                )
            } else {
                // Draw Sun
                val sunCenter = when (period) {
                    "dawn" -> Offset(width - 120f, 180f)
                    "dusk" -> Offset(width - 60f, 190f)
                    else -> Offset(width - 50f, 75f)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFfff7c2), Color(0xFFffd45e)),
                        center = sunCenter,
                        radius = 42f
                    ),
                    radius = 42f,
                    center = sunCenter
                )
            }
        }

        // Weather FX (Rain, Snow)
        if (condition == "rain" || condition == "storm" || condition == "snow") {
            WeatherParticleCanvas(condition = condition, density = cloudDensity, windSpeed = windSpeed)
        }
    }
}

@Composable
fun WeatherParticleCanvas(condition: String, density: Int, windSpeed: Int) {
    var tick by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            tick += 0.05f
        }
    }

    val particleCount = when (condition) {
        "rain" -> 50 + (density * 1.5).toInt()
        "storm" -> 80 + (density * 2).toInt()
        "snow" -> 40 + density
        else -> 0
    }

    val particles = remember(condition, density) {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 400f + 200f,
                size = if (condition == "snow") Random.nextFloat() * 3f + 2f else Random.nextFloat() * 1.5f + 1f,
                length = if (condition == "snow") 0f else Random.nextFloat() * 20f + 10f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val isSnow = condition == "snow"

        particles.forEach { p ->
            val currentY = (p.y * height + (tick * p.speed)) % (height + 50f) - 25f
            val windOffset = sin(tick + p.x * 10f) * (windSpeed / 100f) * 30f
            val currentX = (p.x * width + windOffset + width) % width

            if (isSnow) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = p.size,
                    center = Offset(currentX, currentY)
                )
            } else {
                drawLine(
                    color = Color(0xFFd0e6ff).copy(alpha = 0.6f),
                    start = Offset(currentX, currentY),
                    end = Offset(currentX - 5f, currentY + p.length),
                    strokeWidth = p.size
                )
            }
        }
    }
}

data class Particle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float,
    val length: Float
)
