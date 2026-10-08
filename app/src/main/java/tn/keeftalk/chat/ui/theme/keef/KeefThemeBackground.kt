package tn.keeftalk.chat.ui.theme.keef

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import tn.keeftalk.chat.domain.model.EnvironmentalState
import tn.keeftalk.chat.domain.model.GroundType
import tn.keeftalk.chat.domain.model.PeriodOfDay
import tn.keeftalk.chat.domain.model.WeatherConditionType
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun KeefThemeBackground(
    modifier: Modifier = Modifier,
    environmentalState: EnvironmentalState = LocalEnvironmentalState.current,
    tokens: KeefThemeTokens = LocalKeefThemeTokens.current
) {
    val sky1Animated by animateColorAsState(tokens.sky1, tween(1200), label = "sky1")
    val sky2Animated by animateColorAsState(tokens.sky2, tween(1200), label = "sky2")

    val isNight = environmentalState.period == PeriodOfDay.NIGHT
    val starsAlpha by animateFloatAsState(if (isNight) 1.0f else 0.0f, tween(1400), label = "starsAlpha")

    // Continuous Frame Time Accumulator for smooth, glitch-free animations
    var timeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastNanos = System.nanoTime()
        while (isActive) {
            withFrameNanos { frameNanos ->
                val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                lastNanos = frameNanos
                timeSec += dt
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Sky Gradient & Veil Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(sky1Animated, sky2Animated)
                )
            )

            if (tokens.veilAlpha > 0f) {
                drawRect(color = tokens.veilColor.copy(alpha = tokens.veilAlpha))
            }
        }

        // 2. Stars Layer (Night atmosphere, affected by light pollution)
        if (starsAlpha > 0f) {
            KeefStarsLayer(
                starsAlpha = starsAlpha,
                starsDensityPercent = environmentalState.starsDensity,
                lightPollutionPercent = environmentalState.lightPollution
            )
        }

        // 3. Celestial Orbs (Sun & Moon realistically positioned & rendered)
        KeefSunAndMoonOrbs(
            environmentalState = environmentalState,
            period = environmentalState.period
        )

        // 4. Volumetric Sky Clouds
        KeefCloudsLayer(
            cloudColor = tokens.cloudColor,
            cloudShade = tokens.cloudShade,
            cloudCoverPercent = environmentalState.cloudCover,
            cloudSmoothness = environmentalState.cloudSmoothness,
            windSpeed = environmentalState.windSpeed,
            timeSec = timeSec
        )

        // 5. Realistic Weather Particle Effects & Thunder / Lightning
        if (environmentalState.weatherCondition != WeatherConditionType.CLEAR ||
            environmentalState.rainDensity > 0 ||
            environmentalState.thunderFrequency > 0
        ) {
            KeefWeatherEffectsLayer(
                condition = environmentalState.weatherCondition,
                windSpeed = environmentalState.windSpeed,
                rainDensityPercent = environmentalState.rainDensity,
                thunderFrequencyPercent = environmentalState.thunderFrequency,
                timeSec = timeSec
            )
        }

        // 6. Ground Layer (Forest, City, Sahara, Green landscapes lowered 50%)
        KeefGroundLayer(
            groundType = environmentalState.groundType,
            period = environmentalState.period,
            lightPollutionPercent = environmentalState.lightPollution,
            timeSec = timeSec
        )
    }
}

@Composable
private fun KeefStarsLayer(
    starsAlpha: Float,
    starsDensityPercent: Int = 60,
    lightPollutionPercent: Int = 30
) {
    val infiniteTransition = rememberInfiniteTransition(label = "starsTwinkle")
    val twinkleFactor by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkleFactor"
    )

    // Generate stable pseudo-random star positions across the upper sky
    val rawStarCoords = remember {
        val rand = Random(42L)
        List(140) {
            val rx = rand.nextFloat()
            val ry = rand.nextFloat() * 0.65f // Upper 65% of sky
            val sz = 1.2f + rand.nextFloat() * 2.4f // Star size
            val alphaMult = 0.35f + rand.nextFloat() * 0.65f
            Triple(rx, ry, Pair(sz, alphaMult))
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Stars density scales inversely with light pollution
        val pollutionRatio = (lightPollutionPercent.coerceIn(0, 100) / 100f)
        val densityRatio = (starsDensityPercent.coerceIn(0, 100) / 100f)
        val effectiveCount = ((densityRatio * (1f - pollutionRatio * 0.85f)) * 140).toInt().coerceAtLeast(4)

        // Urban Skyglow Horizon Light if light pollution is present
        if (pollutionRatio > 0.15f) {
            val skyglowAlpha = (pollutionRatio * starsAlpha * 0.35f).coerceIn(0f, 0.40f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFFB273).copy(alpha = skyglowAlpha * 0.4f),
                        Color(0xFFFF9E43).copy(alpha = skyglowAlpha)
                    ),
                    startY = h * 0.50f,
                    endY = h
                )
            )
        }

        // Faint Milky Way Dust Band when light pollution is very low (<25%)
        if (pollutionRatio < 0.25f && starsAlpha > 0.2f) {
            val mwAlpha = ((0.25f - pollutionRatio) / 0.25f * starsAlpha * 0.18f).coerceIn(0f, 0.20f)
            val mwPath = Path().apply {
                moveTo(w * 0.10f, 0f)
                cubicTo(w * 0.35f, h * 0.20f, w * 0.65f, h * 0.45f, w * 0.90f, h * 0.65f)
                lineTo(w * 1.05f, h * 0.65f)
                cubicTo(w * 0.75f, h * 0.40f, w * 0.45f, h * 0.15f, w * 0.25f, 0f)
                close()
            }
            drawPath(
                path = mwPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE2E8FF).copy(alpha = mwAlpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.3f),
                    radius = w * 0.6f
                )
            )
        }

        // Draw selected stars
        val selectedStars = rawStarCoords.take(effectiveCount)
        val baseAlpha = starsAlpha * twinkleFactor

        selectedStars.forEach { (rx, ry, prop) ->
            val (radius, alphaMult) = prop
            val pos = Offset(rx * w, ry * h)
            val starAlpha = (baseAlpha * alphaMult * (1f - (ry / 0.65f) * pollutionRatio * 0.6f)).coerceIn(0f, 1f)
            if (starAlpha > 0.05f) {
                drawCircle(
                    color = Color.White.copy(alpha = starAlpha),
                    radius = radius,
                    center = pos
                )
            }
        }
    }
}

@Composable
private fun KeefSunAndMoonOrbs(
    environmentalState: EnvironmentalState,
    period: PeriodOfDay
) {
    val isNight = period == PeriodOfDay.NIGHT
    val sunAlpha by animateFloatAsState(if (isNight) 0f else 1f, tween(1000), label = "sunAlpha")
    val moonAlpha by animateFloatAsState(if (isNight || period == PeriodOfDay.DUSK) 1f else 0f, tween(1000), label = "moonAlpha")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Sun Movement & Realistic Solar Orb
        if (sunAlpha > 0f) {
            val progress = environmentalState.sunProgress.coerceIn(0f, 1f)
            val sunX = (0.10f + progress * 0.80f) * w
            val arcY = sin(progress * PI.toFloat())
            val sunY = (0.80f - arcY * 0.60f) * h

            val sunCenter = Offset(sunX, sunY)
            val sunRadius = 40.dp.toPx()

            // Solar Outer Atmospheric Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD563).copy(alpha = 0.50f * sunAlpha),
                        Color(0xFFFFB03A).copy(alpha = 0.18f * sunAlpha),
                        Color.Transparent
                    ),
                    center = sunCenter,
                    radius = sunRadius * 3.0f
                ),
                radius = sunRadius * 3.0f,
                center = sunCenter
            )

            // Sun Core Disk
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFF1A8), Color(0xFFFFCC4D)),
                    center = sunCenter - Offset(sunRadius * 0.2f, sunRadius * 0.2f),
                    radius = sunRadius
                ),
                radius = sunRadius,
                center = sunCenter,
                alpha = sunAlpha
            )
        }

        // 2. Realistic Moon Movement & Photorealistic Lunar Orb
        if (moonAlpha > 0f) {
            val moonProg = environmentalState.moonProgress.coerceIn(0f, 1f)
            val moonX = (0.10f + moonProg * 0.80f) * w
            val arcY = sin(moonProg * PI.toFloat())
            val moonY = (0.78f - arcY * 0.58f) * h

            val moonCenter = Offset(moonX, moonY)
            val moonRadius = 34.dp.toPx()

            // Lunar Luminous Outer Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD6E2FF).copy(alpha = 0.38f * moonAlpha),
                        Color(0xFF9BB7FF).copy(alpha = 0.12f * moonAlpha),
                        Color.Transparent
                    ),
                    center = moonCenter,
                    radius = moonRadius * 2.8f
                ),
                radius = moonRadius * 2.8f,
                center = moonCenter
            )

            val moonPath = Path().apply {
                addOval(Rect(moonCenter, moonRadius))
            }

            clipPath(moonPath) {
                // Base Lunar Disk (Silvery white to slate gray gradient)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFE8EEFA),
                            Color(0xFFC2CDDF)
                        ),
                        center = moonCenter - Offset(moonRadius * 0.25f, moonRadius * 0.25f),
                        radius = moonRadius * 1.1f
                    ),
                    radius = moonRadius,
                    center = moonCenter,
                    alpha = moonAlpha
                )

                // Realistic Lunar Surface Features (Maria / Dark Basalt Plains)
                val mariaAlpha = 0.42f * moonAlpha
                val mariaColor = Color(0xFF6B7790).copy(alpha = mariaAlpha)

                // Oceanus Procellarum & Mare Imbrium
                drawCircle(
                    color = mariaColor,
                    radius = moonRadius * 0.42f,
                    center = moonCenter + Offset(-moonRadius * 0.22f, -moonRadius * 0.15f)
                )

                // Mare Tranquillitatis & Mare Serenitatis
                drawCircle(
                    color = mariaColor,
                    radius = moonRadius * 0.35f,
                    center = moonCenter + Offset(moonRadius * 0.25f, -moonRadius * 0.10f)
                )

                // Mare Crisium & Mare Fecunditatis
                drawCircle(
                    color = mariaColor,
                    radius = moonRadius * 0.28f,
                    center = moonCenter + Offset(moonRadius * 0.38f, moonRadius * 0.22f)
                )

                // Tycho Crater Ray System (Southern Pole)
                val tychoCenter = moonCenter + Offset(-moonRadius * 0.08f, moonRadius * 0.55f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f * moonAlpha),
                    radius = moonRadius * 0.09f,
                    center = tychoCenter
                )
                // Radiating Ray Lines
                for (angle in listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)) {
                    val rad = angle * PI.toFloat() / 180f
                    val rayEnd = tychoCenter + Offset(cos(rad) * moonRadius * 0.5f, sin(rad) * moonRadius * 0.5f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f * moonAlpha),
                        start = tychoCenter,
                        end = rayEnd,
                        strokeWidth = 1.2f
                    )
                }

                // 3D Lunar Terminator & Phase Lighting (NO overlapping "two row circles")
                val illumination = environmentalState.moonState.illumination.coerceIn(0f, 1f)
                if (illumination < 0.98f) {
                    val shadowAlpha = 0.88f * moonAlpha
                    val phaseProg = environmentalState.moonState.phaseProgress.coerceIn(0f, 1f)
                    val isWaxing = phaseProg < 0.5f

                    val unlitWidth = (1.0f - illumination) * (moonRadius * 2.0f)
                    val shadowX = if (isWaxing) {
                        moonCenter.x - moonRadius + unlitWidth * 0.5f
                    } else {
                        moonCenter.x + moonRadius - unlitWidth * 0.5f
                    }

                    // Soft Earthshine on Night Side of Moon
                    drawCircle(
                        color = Color(0xFF090E26).copy(alpha = shadowAlpha),
                        radius = moonRadius * 1.02f,
                        center = Offset(shadowX, moonCenter.y)
                    )

                    // Subtle Earthshine ambient highlight
                    drawCircle(
                        color = Color(0xFF1B2850).copy(alpha = 0.18f * moonAlpha),
                        radius = moonRadius,
                        center = moonCenter
                    )
                }
            }
        }
    }
}

@Composable
private fun KeefGroundLayer(
    groundType: GroundType,
    period: PeriodOfDay,
    lightPollutionPercent: Int,
    timeSec: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Ground base height moved down 50% (starts at bottom ~11% of screen)
        val groundTop = h * 0.89f

        // Period-specific ground color palettes
        val (baseColor, accentColor, highlightColor) = when (period) {
            PeriodOfDay.DAWN -> Triple(Color(0xFF2C1E30), Color(0xFF4A2E43), Color(0xFFFF9E7A))
            PeriodOfDay.DAY -> Triple(Color(0xFF1B4D2E), Color(0xFF2E6B43), Color(0xFF8FD89F))
            PeriodOfDay.DUSK -> Triple(Color(0xFF1F1A3A), Color(0xFF382D5E), Color(0xFFFF8A65))
            PeriodOfDay.NIGHT -> Triple(Color(0xFF090F24), Color(0xFF121B3B), Color(0xFF2D3C6B))
        }

        when (groundType) {
            GroundType.FOREST -> {
                // Background Mountain / Hill Silhouette
                val bgHillPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, groundTop - 12f)
                    cubicTo(w * 0.25f, groundTop - 30f, w * 0.50f, groundTop - 5f, w * 0.75f, groundTop - 22f)
                    cubicTo(w * 0.88f, groundTop - 32f, w * 0.95f, groundTop - 15f, w, groundTop - 10f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = bgHillPath, color = accentColor.copy(alpha = 0.70f))

                // Dense Pine Forest Layer
                val treeCount = 18
                val windSway = sin(timeSec * 1.5f) * 1.8f
                for (i in 0..treeCount) {
                    val tx = (i.toFloat() / treeCount) * w
                    val treeH = 22f + ((i * 17) % 18)
                    val treeBaseY = groundTop + 5f + ((i * 13) % 12)
                    val treeTopY = treeBaseY - treeH

                    // Pine Tree Triangle Tiers
                    val pinePath = Path().apply {
                        // Top tier
                        moveTo(tx + windSway, treeTopY)
                        lineTo(tx - 6f, treeTopY + treeH * 0.4f)
                        lineTo(tx + 6f, treeTopY + treeH * 0.4f)
                        close()

                        // Middle tier
                        moveTo(tx + windSway * 0.7f, treeTopY + treeH * 0.25f)
                        lineTo(tx - 9f, treeTopY + treeH * 0.7f)
                        lineTo(tx + 9f, treeTopY + treeH * 0.7f)
                        close()

                        // Bottom tier
                        moveTo(tx + windSway * 0.4f, treeTopY + treeH * 0.5f)
                        lineTo(tx - 12f, treeBaseY)
                        lineTo(tx + 12f, treeBaseY)
                        close()
                    }
                    drawPath(path = pinePath, color = baseColor)

                    // Tree highlight edge
                    drawLine(
                        color = highlightColor.copy(alpha = 0.25f),
                        start = Offset(tx + windSway, treeTopY),
                        end = Offset(tx - 12f, treeBaseY),
                        strokeWidth = 1.0f
                    )
                }

                // Foreground Forest Floor Base
                drawRect(
                    color = baseColor,
                    topLeft = Offset(0f, groundTop + 10f),
                    size = Size(w, h - groundTop)
                )
            }

            GroundType.CITY -> {
                // Background Far City Skyline
                val bgCityPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, groundTop - 8f)
                    var cx = 0f
                    val bCount = 12
                    val bWidth = w / bCount
                    for (i in 0 until bCount) {
                        val bh = 15f + ((i * 23) % 22)
                        lineTo(cx, groundTop - bh)
                        lineTo(cx + bWidth * 0.85f, groundTop - bh)
                        lineTo(cx + bWidth * 0.85f, groundTop - 8f)
                        cx += bWidth
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(path = bgCityPath, color = accentColor.copy(alpha = 0.65f))

                // Foreground Detailed Buildings Skyline
                val fgBuildings = listOf(
                    Triple(0f, 0.12f, 38f),
                    Triple(0.12f, 0.22f, 55f), // Tall Tower
                    Triple(0.22f, 0.35f, 30f),
                    Triple(0.35f, 0.48f, 48f),
                    Triple(0.48f, 0.58f, 65f), // Skyscraper with spire
                    Triple(0.58f, 0.70f, 40f),
                    Triple(0.70f, 0.82f, 52f),
                    Triple(0.82f, 1.0f, 32f)
                )

                fgBuildings.forEach { (startRatio, endRatio, heightPx) ->
                    val bx = startRatio * w
                    val bw = (endRatio - startRatio) * w
                    val by = groundTop - heightPx

                    // Building Body
                    drawRect(
                        color = baseColor,
                        topLeft = Offset(bx, by),
                        size = Size(bw, h - by)
                    )

                    // Spire antenna on high buildings
                    if (heightPx > 50f) {
                        drawLine(
                            color = highlightColor.copy(alpha = 0.6f),
                            start = Offset(bx + bw * 0.5f, by),
                            end = Offset(bx + bw * 0.5f, by - 11f),
                            strokeWidth = 1.5f
                        )
                    }

                    // Lit Windows at Night / Dusk
                    if (period == PeriodOfDay.NIGHT || period == PeriodOfDay.DUSK) {
                        val rows = (heightPx / 12f).toInt().coerceAtLeast(1)
                        val cols = (bw / 10f).toInt().coerceAtLeast(1)
                        val rand = Random((startRatio * 1000).toLong())

                        for (r in 0 until rows) {
                            for (c in 0 until cols) {
                                if (rand.nextFloat() > 0.35f) {
                                    val wx = bx + 2f + c * 8f
                                    val wy = by + 4f + r * 10f
                                    drawRect(
                                        color = Color(0xFFFFE082).copy(alpha = 0.85f),
                                        topLeft = Offset(wx, wy),
                                        size = Size(4f, 5f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Ground Street Level Glow (intensity scales with light pollution)
                val glowAlpha = (lightPollutionPercent / 100f * 0.35f).coerceIn(0.15f, 0.50f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFB74D).copy(alpha = glowAlpha),
                            baseColor
                        ),
                        startY = groundTop,
                        endY = groundTop + 18f
                    ),
                    topLeft = Offset(0f, groundTop),
                    size = Size(w, 18f)
                )
            }

            GroundType.SAHARA -> {
                val desertBase = when (period) {
                    PeriodOfDay.DAWN -> Color(0xFF8D4B32)
                    PeriodOfDay.DAY -> Color(0xFFD48B38)
                    PeriodOfDay.DUSK -> Color(0xFF9E3A2B)
                    PeriodOfDay.NIGHT -> Color(0xFF1B1B36)
                }
                val duneHighlight = when (period) {
                    PeriodOfDay.DAWN -> Color(0xFFFFB07C)
                    PeriodOfDay.DAY -> Color(0xFFFFD580)
                    PeriodOfDay.DUSK -> Color(0xFFFF8A65)
                    PeriodOfDay.NIGHT -> Color(0xFF3D3C66)
                }

                // Distant Sand Dune Ridge
                val bgDunePath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, groundTop - 18f)
                    cubicTo(w * 0.35f, groundTop - 35f, w * 0.65f, groundTop - 5f, w, groundTop - 22f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = bgDunePath, color = desertBase.copy(alpha = 0.75f))

                // Foreground Grand Dune Wave
                val fgDunePath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, groundTop - 5f)
                    cubicTo(w * 0.25f, groundTop + 8f, w * 0.55f, groundTop - 28f, w, groundTop - 8f)
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = fgDunePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(duneHighlight, desertBase)
                    )
                )

                // Dune Ridge Crest Line Highlight
                val crestPath = Path().apply {
                    moveTo(0f, groundTop - 5f)
                    cubicTo(w * 0.25f, groundTop + 8f, w * 0.55f, groundTop - 28f, w, groundTop - 8f)
                }
                drawPath(
                    path = crestPath,
                    color = duneHighlight.copy(alpha = 0.85f),
                    style = Stroke(width = 1.8f)
                )

                // Desert Palm Tree Silhouette
                val palmX = w * 0.82f
                val palmY = groundTop - 12f
                val trunkPath = Path().apply {
                    moveTo(palmX, palmY + 20f)
                    quadraticTo(palmX - 4f, palmY + 10f, palmX - 6f, palmY)
                }
                drawPath(path = trunkPath, color = desertBase, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

                // Fronds
                for (angle in listOf(-120f, -60f, 0f, 60f, 120f)) {
                    val rad = angle * PI.toFloat() / 180f
                    val fx = (palmX - 6f) + cos(rad) * 12f
                    val fy = palmY + sin(rad) * 8f
                    drawLine(
                        color = desertBase,
                        start = Offset(palmX - 6f, palmY),
                        end = Offset(fx, fy),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            GroundType.GREEN -> {
                // Background Rolling Hill
                val bgHillPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, groundTop - 15f)
                    cubicTo(w * 0.30f, groundTop - 32f, w * 0.70f, groundTop - 5f, w, groundTop - 20f)
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = bgHillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor, baseColor)
                    )
                )

                // Foreground Meadow Hill
                val fgHillPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, groundTop - 5f)
                    cubicTo(w * 0.35f, groundTop + 10f, w * 0.65f, groundTop - 22f, w, groundTop - 10f)
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = fgHillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(highlightColor.copy(alpha = 0.85f), baseColor)
                    )
                )

                // Meadow Grass Tufts along hill ridge
                val grassTufts = 20
                for (i in 0..grassTufts) {
                    val gx = (i.toFloat() / grassTufts) * w
                    val gy = groundTop - 5f + sin(i.toFloat()) * 8f
                    for (b in -1..1) {
                        drawLine(
                            color = highlightColor.copy(alpha = 0.60f),
                            start = Offset(gx, gy),
                            end = Offset(gx + b * 2f, gy - 4f - (i % 3) * 1f),
                            strokeWidth = 1.0f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeefCloudsLayer(
    cloudColor: Color,
    cloudShade: Color,
    cloudCoverPercent: Int,
    cloudSmoothness: Float = 0.8f,
    windSpeed: Float = 12f,
    timeSec: Float = 0f
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val densityRatio = (cloudCoverPercent.coerceIn(0, 400) / 100f)
        val clusterCount = (densityRatio * 6).toInt().coerceIn(2, 24)

        val softness = cloudSmoothness.coerceIn(0.2f, 1.0f)
        val windFactor = windSpeed.coerceIn(0f, 300f) / 40f

        for (i in 0 until clusterCount) {
            val baseSpeed = (0.05f + (i % 5) * 0.02f) * (0.8f + windFactor)
            val rawX = ((timeSec * baseSpeed + (i * 0.18f)) % 2.5f) - 0.50f
            val cx = rawX * w

            val verticalRatio = 0.05f + (i.toFloat() / (clusterCount.coerceAtLeast(1))) * 0.65f
            val cy = verticalRatio * h

            val randomScale = 0.9f + ((i * 37) % 100 / 100f) * 2.2f
            val r = 45.dp.toPx() * randomScale

            val puffs = listOf(
                Offset(cx, cy) to r,
                Offset(cx - r * 0.65f, cy + r * 0.12f) to (r * 0.82f),
                Offset(cx + r * 0.65f, cy + r * 0.12f) to (r * 0.85f),
                Offset(cx - r * 0.32f, cy - r * 0.38f) to (r * 0.72f),
                Offset(cx + r * 0.35f, cy - r * 0.35f) to (r * 0.70f)
            )

            puffs.forEach { (center, radius) ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cloudShade.copy(alpha = 0.32f * softness),
                            cloudShade.copy(alpha = 0.10f * softness),
                            Color.Transparent
                        ),
                        center = center + Offset(0f, radius * 0.3f),
                        radius = radius * 1.45f * softness
                    ),
                    radius = radius * 1.45f * softness,
                    center = center + Offset(0f, radius * 0.3f)
                )
            }

            puffs.forEach { (center, radius) ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cloudColor.copy(alpha = 0.88f),
                            cloudColor.copy(alpha = 0.72f * softness),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * (1.0f + 0.35f * softness)
                    ),
                    radius = radius * (1.0f + 0.35f * softness),
                    center = center
                )
            }
        }
    }
}

@Composable
private fun KeefWeatherEffectsLayer(
    condition: WeatherConditionType,
    windSpeed: Float = 12f,
    rainDensityPercent: Int = 25,
    thunderFrequencyPercent: Int = 0,
    timeSec: Float = 0f
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val windNorm = windSpeed.coerceIn(0f, 300f) / 300f
        val slantX = windNorm * 140f

        if (condition == WeatherConditionType.RAIN || condition == WeatherConditionType.STORM || rainDensityPercent > 0) {
            val dropCount = ((rainDensityPercent / 100f) * 90).toInt().coerceAtLeast(15)

            for (i in 0 until dropCount) {
                val fallSpeed = 1.0f + (i % 5) * 0.35f
                val animProgress = (timeSec * fallSpeed) % 1.0f

                val startX = (i * 37f + animProgress * 280f) % (w + 200f) - 100f
                val startY = ((i * 71f + animProgress * h * 1.9f) % (h + 120f)) - 60f

                val dropLength = 24f + (i % 4) * 14f
                val endX = startX - slantX * (dropLength / 30f)
                val endY = startY + dropLength

                val dropAlpha = 0.35f + (i % 3) * 0.20f
                drawCircle(
                    color = Color.White.copy(alpha = dropAlpha * 0.85f),
                    radius = 1.8f,
                    center = Offset(endX, endY)
                )

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = dropAlpha * 0.05f),
                            Color.White.copy(alpha = dropAlpha)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY)
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )

                if (endY >= h * 0.89f) {
                    val splashProgress = ((endY - h * 0.89f) / (h * 0.11f)).coerceIn(0f, 1f)
                    val splashRadius = 3f + splashProgress * 6f
                    drawCircle(
                        color = Color.White.copy(alpha = (0.6f - splashProgress * 0.5f).coerceAtLeast(0f)),
                        radius = splashRadius,
                        center = Offset(endX - splashProgress * (slantX * 0.2f), h - 4f)
                    )
                }
            }
        }

        val isThunderActive = thunderFrequencyPercent > 0 || condition == WeatherConditionType.STORM
        if (isThunderActive) {
            val freqThreshold = (thunderFrequencyPercent / 100f).coerceIn(0.1f, 1.0f)
            val interval = (8f - freqThreshold * 6f).coerceAtLeast(1.5f)
            val flashPhase = (timeSec % interval) / interval

            val isFlashing = flashPhase < (0.08f + freqThreshold * 0.05f)

            if (isFlashing) {
                val flashAlpha = (0.20f + 0.35f * freqThreshold).coerceIn(0.1f, 0.6f)
                drawRect(color = Color.White.copy(alpha = flashAlpha))

                val seed = (timeSec / interval).toInt()
                val boltRand = Random(seed.toLong() * 9999L)
                val boltStartX = w * (0.25f + boltRand.nextFloat() * 0.50f)

                val mainPath = listOf(
                    Offset(boltStartX, 0f),
                    Offset(boltStartX - 20f + boltRand.nextFloat() * 40f, h * 0.22f),
                    Offset(boltStartX - 30f + boltRand.nextFloat() * 60f, h * 0.45f),
                    Offset(boltStartX - 25f + boltRand.nextFloat() * 50f, h * 0.70f)
                )

                for (idx in 0 until mainPath.size - 1) {
                    drawLine(
                        color = Color(0xFFE2ECFF),
                        start = mainPath[idx],
                        end = mainPath[idx + 1],
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )

                    if (idx == 1) {
                        val branchEnd = Offset(mainPath[idx].x + 35f, mainPath[idx].y + h * 0.15f)
                        drawLine(
                            color = Color(0xFFC0D5FF).copy(alpha = 0.8f),
                            start = mainPath[idx],
                            end = branchEnd,
                            strokeWidth = 2.0f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        if (condition == WeatherConditionType.SNOW) {
            val flakeCount = ((rainDensityPercent / 100f) * 50).toInt().coerceAtLeast(15)
            for (i in 0 until flakeCount) {
                val fallProgress = (timeSec * (0.15f + (i % 3) * 0.05f)) % 1.0f
                val sway = sin((timeSec * 2f + i).toDouble()).toFloat() * (8f + windSpeed * 0.15f)
                val sx = (i * 43f + sway) % (w + 40f) - 20f
                val sy = (fallProgress * (h + 40f)) - 20f

                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = (2.5f + (i % 3)),
                    center = Offset(sx, sy)
                )
            }
        }
    }
}
