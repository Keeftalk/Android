package tn.keeftalk.chat.ui.browser

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SkyBackground(
    modifier: Modifier = Modifier,
    period: String,
    condition: String,
    cloudDensity: Int,
    windSpeed: Int,
    effectsEnabled: Boolean = true,
    soundEnabled: Boolean = false,
    hapticsEnabled: Boolean = true,
    simulatedDate: Date? = null
) {
    val context = LocalContext.current
    val currentDate = simulatedDate ?: remember { Date() }

    val sunPos = remember(currentDate, period) {
        AstronomyEngine.sunPos(currentDate)
    }
    val moonPos = remember(currentDate, period) {
        AstronomyEngine.moonPos(currentDate)
    }
    val elong = remember(currentDate) {
        AstronomyEngine.elong(currentDate)
    }

    // Sky colors
    val skyColors = remember(sunPos, period) {
        val (topColor, botColor) = AstronomyEngine.skyColors(sunPos.alt, sunPos.az < 180.0)
        when (period) {
            "dawn" -> Pair(Color(0xFFff9d7a), Color(0xFFffe6c4))
            "dusk" -> Pair(Color(0xFF4d3f95), Color(0xFFf58a6a))
            "night" -> Pair(Color(0xFF070c24), Color(0xFF22316a))
            else -> Pair(topColor, botColor)
        }
    }

    // Veil color calculation
    val veilColor = remember(condition, cloudDensity) {
        val dd = cloudDensity / 100f
        val alpha = (0.35f * Math.pow(dd.toDouble(), 0.8)).toFloat().coerceIn(0f, 0.8f)
        when (condition) {
            "rain" -> Color(0xFF37465c).copy(alpha = (0.5f * alpha / 0.35f).coerceIn(0f, 0.75f))
            "storm" -> Color(0xFF191e30).copy(alpha = (0.65f * alpha / 0.35f).coerceIn(0f, 0.85f))
            "snow" -> Color(0xFFcddceb).copy(alpha = (0.35f * alpha / 0.35f).coerceIn(0f, 0.6f))
            "cloudy" -> Color(0xFF6e7887).copy(alpha = alpha)
            else -> Color.Transparent
        }
    }

    // Animation tick
    var tick by remember { mutableFloatStateOf(0f) }
    var lightningFlash by remember { mutableFloatStateOf(0f) }
    var currentLightningBolt by remember { mutableStateOf<LightningBolt?>(null) }

    LaunchedEffect(effectsEnabled, condition) {
        if (!effectsEnabled) return@LaunchedEffect
        var lastTime = System.currentTimeMillis()
        while (true) {
            delay(16)
            val now = System.currentTimeMillis()
            val dt = (now - lastTime) / 1000f
            lastTime = now
            tick += dt

            if (condition == "storm") {
                if (lightningFlash > 0.01f) {
                    lightningFlash *= 0.9f
                } else {
                    lightningFlash = 0f
                    currentLightningBolt = null

                    if (Math.random() < 0.008) {
                        lightningFlash = 0.8f
                        currentLightningBolt = WeatherEffectsHelper.generateLightningBolt(1080f, 2200f)
                        if (soundEnabled) {
                            WeatherEffectsHelper.playThunderRumble(context)
                        }
                        if (hapticsEnabled) {
                            WeatherEffectsHelper.triggerThunderHaptics(context)
                        }
                    }
                }
            } else {
                lightningFlash = 0f
                currentLightningBolt = null
            }
        }
    }

    val clouds = remember(condition, cloudDensity) {
        val count = (cloudDensity / 100f * 18).toInt().coerceAtLeast(if (cloudDensity > 0) 2 else 0)
        List(count) {
            WeatherEffectsHelper.createCloud(
                initX = (Math.random() * 1000).toFloat(),
                canvasWidth = 1080f,
                density = cloudDensity
            )
        }
    }

    val particles = remember(condition, cloudDensity) {
        val count = when (condition) {
            "rain" -> (80 + cloudDensity * 2.6f).toInt()
            "storm" -> (140 + cloudDensity * 3.4f).toInt()
            "snow" -> (60 + cloudDensity * 1.8f).toInt()
            else -> 0
        }
        List(count) {
            WeatherEffectsHelper.createParticle(
                condition = condition,
                initY = -1f,
                canvasWidth = 1080f,
                canvasHeight = 2200f
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(skyColors.first, skyColors.second)
                )
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

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Stars at Night
            if (period == "night") {
                val starOpacity = if (condition == "clear") 1f else 0.4f
                val starPoints = listOf(
                    Offset(width * 0.2f, height * 0.15f),
                    Offset(width * 0.6f, height * 0.09f),
                    Offset(width * 0.8f, height * 0.22f),
                    Offset(width * 0.35f, height * 0.31f),
                    Offset(width * 0.12f, height * 0.35f),
                    Offset(width * 0.92f, height * 0.06f)
                )
                for (pt in starPoints) {
                    drawCircle(
                        color = Color.White.copy(alpha = starOpacity),
                        radius = 1.5.dp.toPx(),
                        center = pt
                    )
                }
            }

            // 2. Celestial Orb (Sun or Moon) with Glowing Halo Aura
            val orbSize = 84f
            val isNight = period == "night"
            val targetPos = if (isNight) {
                AstronomyEngine.celestialPosition(width, height, moonPos.alt, moonPos.az, moonPos.maxAlt, orbSize)
            } else {
                AstronomyEngine.celestialPosition(width, height, sunPos.alt, sunPos.az, sunPos.maxAlt, orbSize)
            }

            if (isNight) {
                // Moon Aura Halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFa0b9ff).copy(alpha = 0.35f), Color.Transparent),
                        center = targetPos,
                        radius = orbSize * 2.0f
                    ),
                    radius = orbSize * 2.0f,
                    center = targetPos
                )
                // Moon Disc
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFFcfd8f5)),
                        center = targetPos,
                        radius = orbSize
                    ),
                    radius = orbSize / 2f,
                    center = targetPos
                )
                drawMoonPhaseShadow(targetPos, orbSize / 2f, elong)
            } else {
                // Sun Aura Halo
                val auraColor = when (period) {
                    "dawn" -> Color(255, 150, 90, 120)
                    "dusk" -> Color(255, 110, 90, 110)
                    else -> Color(255, 214, 90, 140)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(auraColor, Color.Transparent),
                        center = targetPos,
                        radius = orbSize * 2.5f
                    ),
                    radius = orbSize * 2.5f,
                    center = targetPos
                )
                // Sun Core
                val sunColorCore = if (period == "dawn") Color(0xFFfff0c9) else Color(0xFFfff7c2)
                val sunColorEdge = if (period == "dawn") Color(0xFFffb05e) else Color(0xFFffd45e)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(sunColorCore, sunColorEdge),
                        center = targetPos,
                        radius = orbSize
                    ),
                    radius = orbSize / 2f,
                    center = targetPos
                )
            }

            // 3. Volumetric Drifting Clouds
            if (effectsEnabled) {
                val windFactor = (windSpeed / 100f) * (if (condition == "storm") 1.5f else 1.0f)
                clouds.forEach { cloud ->
                    cloud.x += cloud.speed * windFactor * 0.016f * 60f
                    if (cloud.x > width + cloud.width) {
                        cloud.x = -cloud.width * 1.1f
                    }

                    val baseOpacity = cloud.opacity.coerceIn(0.1f, 0.95f)

                    val puffColorCore = when (period) {
                        "dusk" -> Color(255, 210, 190, (baseOpacity * 240).toInt())
                        "night" -> Color(160, 180, 230, (baseOpacity * 200).toInt())
                        else -> if (condition == "storm") Color(50, 55, 75, (baseOpacity * 240).toInt())
                        else Color(255, 255, 255, (baseOpacity * 240).toInt())
                    }
                    val puffColorMid = puffColorCore.copy(alpha = puffColorCore.alpha * 0.5f)
                    val puffColorEdge = puffColorCore.copy(alpha = 0f)

                    val shadowColor = when (period) {
                        "dusk" -> Color(180, 110, 100, (baseOpacity * 90).toInt())
                        "night" -> Color(30, 40, 80, (baseOpacity * 90).toInt())
                        else -> Color(80, 95, 120, (baseOpacity * 100).toInt())
                    }

                    cloud.puffOffsets.forEach { (px, py, pr) ->
                        // Underside shadow puff for 3D depth
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(shadowColor, shadowColor.copy(alpha = 0f)),
                                center = Offset(cloud.x + px, cloud.y + py + pr * 0.18f),
                                radius = pr * 0.65f
                            ),
                            radius = pr * 0.65f,
                            center = Offset(cloud.x + px, cloud.y + py + pr * 0.18f)
                        )

                        // Volumetric soft cloud puff
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to puffColorCore,
                                    0.55f to puffColorMid,
                                    1.0f to puffColorEdge
                                ),
                                center = Offset(cloud.x + px, cloud.y + py),
                                radius = pr * 0.65f
                            ),
                            radius = pr * 0.65f,
                            center = Offset(cloud.x + px, cloud.y + py)
                        )
                    }
                }
            }

            // 4. Weather Particles (Rain / Snow)
            if (effectsEnabled && (condition == "rain" || condition == "storm" || condition == "snow")) {
                val gust = 0.6f + 0.7f * (0.5f + 0.5f * sin(tick * 0.21f) * sin(tick * 0.07f + 1f))
                val windVx = (40f + (windSpeed / 100f) * 110f) * gust

                particles.forEach { p ->
                    if (condition == "snow") {
                        p.y += p.speed * 0.016f * 20f
                        p.x += (sin(tick * (0.6f + p.z) + p.phase) * p.sway + (windSpeed / 100f) * 28f) * 0.016f * 10f
                        if (p.y > height + 20f || p.x > width + 40f) {
                            p.y = -20f
                            p.x = (Math.random() * (width + 60) - 30).toFloat()
                        }
                        val snowAlpha = listOf(0.45f, 0.7f, 0.95f)[p.layer]
                        drawCircle(
                            color = Color.White.copy(alpha = snowAlpha),
                            radius = p.size,
                            center = Offset(p.x, p.y)
                        )
                    } else { // rain / storm
                        val vx = windVx * (0.5f + p.z * 0.6f)
                        p.y += p.speed * 0.016f * 25f
                        p.x += vx * 0.016f * 10f
                        if (p.y > height + 40f || p.x > width + 80f) {
                            p.y = -30f
                            p.x = (Math.random() * (width + 180) - 120).toFloat()
                        }
                        val rainAlpha = listOf(0.18f, 0.3f, 0.5f)[p.layer]
                        val strokeW = listOf(0.8f, 1.1f, 1.5f)[p.layer]
                        drawLine(
                            color = Color(0xFFd2e6ff).copy(alpha = rainAlpha),
                            start = Offset(p.x, p.y),
                            end = Offset(p.x - vx * p.length / p.speed, p.y - p.length),
                            strokeWidth = strokeW
                        )
                    }
                }
            }

            // 5. Lightning Bolt
            currentLightningBolt?.let { bolt ->
                if (lightningFlash > 0.02f) {
                    val mainPath = Path().apply {
                        bolt.mainPath.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = mainPath,
                        color = Color.White.copy(alpha = lightningFlash.coerceIn(0f, 1f)),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    bolt.branches.forEach { branch ->
                        val branchPath = Path().apply {
                            branch.forEachIndexed { idx, pt ->
                                if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                        }
                        drawPath(
                            path = branchPath,
                            color = Color.White.copy(alpha = (lightningFlash * 0.7f).coerceIn(0f, 1f)),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }
        }

        // Screen Flash for Lightning
        if (lightningFlash > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = (lightningFlash * 0.45f).coerceIn(0f, 0.6f)))
            )
        }
    }
}

private fun DrawScope.drawMoonPhaseShadow(center: Offset, radius: Float, elong: Double) {
    val f = (1.0 - cos(elong * PI / 180.0)) / 2.0
    if (f < 0.98) {
        val shadowAlpha = (1.0 - f).toFloat().coerceIn(0f, 0.75f)
        drawCircle(
            color = Color(0xFF0b1020).copy(alpha = shadowAlpha),
            radius = radius * 0.95f,
            center = Offset(center.x + (if (elong < 180) 4f else -4f), center.y)
        )
    }
}
