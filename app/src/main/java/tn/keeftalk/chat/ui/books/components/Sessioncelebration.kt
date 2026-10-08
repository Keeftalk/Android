package tn.keeftalk.chat.ui.books.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/* ==========================================================================
 * Port of the HTML session-summary celebration (palette, tiers, quiet rule,
 * confetti / wave engine, synthesized sounds). Pure Kotlin + Compose draw APIs,
 * no WebView.
 * ========================================================================== */

/* ---------- BRAND PALETTE (same hex values as the HTML) ---------- */
internal object SsColors {
    val Cyan = Color(0xFF00CCCC)
    val CyanLight = Color(0xFF4DF2F2)
    val CyanDark = Color(0xFF009999)
    val Purple = Color(0xFF7D5CFF)
    val PurpleLight = Color(0xFFA78BFA)
    val PurpleDark = Color(0xFF5B3FD9)
    val Sky = Color(0xFF4DD6E8)
    val Lavender = Color(0xFFC4B5FD)
    val Text = Color(0xFFF0EDFF)
    val TextDim = Color(0xFF9C92C9)
    val TextDimmer = Color(0xFF6E6595)
    val Muted = Color(0xFFB8B2D0)
}

internal val SsConfettiColors = listOf(
    SsColors.Cyan, SsColors.CyanLight, SsColors.CyanDark,
    SsColors.Purple, SsColors.PurpleLight, SsColors.PurpleDark,
    SsColors.Sky, Color.White, SsColors.Lavender
)
internal val SsBrandColors = listOf(
    SsColors.Cyan, SsColors.Purple, SsColors.CyanLight, SsColors.PurpleLight
)

/* ---------- TIERS + QUIET-LOG RULE (same cutoffs as the HTML) ---------- */

/** Sessions shorter than this are logged quietly: no XP number, no trophy, no confetti, no fanfare. */
internal const val SESSION_MIN_CELEBRATION_MINUTES = 3

internal data class SessionTier(val min: Int, val level: Int, val label: String)

internal val SESSION_TIER_ZERO = SessionTier(Int.MIN_VALUE, 0, "Session Logged")

internal val SESSION_TIERS = listOf(
    SessionTier(min = 0, level = 1, label = "Tier 1 Celebration"),
    SessionTier(min = 50, level = 2, label = "Tier 2 Celebration"),
    SessionTier(min = 120, level = 3, label = "Tier 3 Celebration"),
    SessionTier(min = 220, level = 4, label = "Tier 4 Celebration"),
    SessionTier(min = 350, level = 5, label = "MEGA Celebration")
)

internal fun sessionXpToTier(xp: Long): SessionTier {
    var tier = SESSION_TIERS.first()
    for (t in SESSION_TIERS) if (xp >= t.min) tier = t
    return tier
}

/** How big each celebration level is (same numbers as the HTML `scale` table). */
internal data class SessionTierScale(
    val cannonCount: Int,
    val burstCount: Int,
    val sideCannons: Int,
    val sparkles: Int,
    val fireworks: Int,
    val mega: Boolean
)

internal fun sessionTierScale(level: Int): SessionTierScale = when (level) {
    1 -> SessionTierScale(28, 40, 0, 0, 0, false)
    2 -> SessionTierScale(40, 55, 1, 0, 0, false)
    3 -> SessionTierScale(55, 70, 2, 50, 3, false)
    4 -> SessionTierScale(70, 90, 3, 80, 6, false)
    5 -> SessionTierScale(90, 120, 4, 130, 9, true)
    else -> SessionTierScale(28, 40, 0, 0, 0, false)
}

/* ==========================================================================
 * FX ENGINE (confetti, sparkles, shockwaves)
 * All coordinates / velocities are in dp so the numbers match the HTML's CSS px.
 * ========================================================================== */

internal enum class SsFxShape { Rect, Circle, Star, Ribbon }

private const val TWO_PI_F = (2.0 * PI).toFloat()
private const val MAX_PARTICLES = 1200

private fun fxRand(a: Float, b: Float): Float = a + Random.nextFloat() * (b - a)
private fun fxChance(p: Float): Boolean = Random.nextFloat() < p
private fun <T> fxPick(list: List<T>): T = list[Random.nextInt(list.size)]

/** Unit star (outer radius 1, inner 0.45), same geometry as the HTML drawStar(). */
private val unitStarPath: Path = Path().apply {
    for (i in 0 until 10) {
        val ang = (PI / 5.0) * i - PI / 2.0
        val rad = if (i % 2 == 0) 1f else 0.45f
        val x = (cos(ang) * rad).toFloat()
        val y = (sin(ang) * rad).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private class FxParticle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    val size: Float,
    val color: Color,
    val shape: SsFxShape,
    val gravity: Float,
    val drag: Float,
    val sway: Float,
    val decay: Float,
    val glow: Boolean
) {
    var swayPhase = Random.nextFloat() * TWO_PI_F
    val swaySpeed = fxRand(1.2f, 3.2f)
    var rotation = Random.nextFloat() * TWO_PI_F
    val rotSpeed = fxRand(-9f, 9f)
    var flip = Random.nextFloat() * TWO_PI_F
    val flipSpeed = fxRand(4f, 10f)
    var life = 1f
    var dead = false
}

private class FxWave(
    val x: Float, val y: Float,
    val color: Color,
    val maxR: Float,
    val lineWidth: Float
) {
    var radius = 0f
    var life = 1f
}

internal class SessionFxEngine {
    var width = 0f
        private set
    var height = 0f
        private set

    private val particles = ArrayList<FxParticle>()
    private val waves = ArrayList<FxWave>()

    val isActive: Boolean get() = particles.isNotEmpty() || waves.isNotEmpty()

    fun setSize(wDp: Float, hDp: Float) {
        width = wDp
        height = hDp
    }

    private fun add(p: FxParticle) {
        if (particles.size < MAX_PARTICLES) particles.add(p)
    }

    /** Confetti cannon: a directed spray. */
    fun cannon(x: Float, y: Float, angle: Float, count: Int, power: Float, palette: List<Color> = SsConfettiColors) {
        repeat(count) {
            val a = angle + fxRand(-0.34f, 0.34f)
            val sp = power * fxRand(0.5f, 1.15f)
            val shape = when {
                fxChance(0.22f) -> SsFxShape.Circle
                fxChance(0.3f) -> SsFxShape.Star
                fxChance(0.4f) -> SsFxShape.Ribbon
                else -> SsFxShape.Rect
            }
            add(
                FxParticle(
                    x = x, y = y, vx = cos(a) * sp, vy = sin(a) * sp,
                    size = fxRand(6f, 14f), color = fxPick(palette), shape = shape,
                    gravity = fxRand(900f, 1200f), drag = fxRand(0.976f, 0.988f),
                    sway = fxRand(20f, 70f), decay = fxRand(0.20f, 0.34f),
                    glow = fxChance(0.2f)
                )
            )
        }
    }

    /** Radial burst. */
    fun burst(
        x: Float, y: Float, count: Int, power: Float,
        palette: List<Color> = SsConfettiColors,
        glow: Float = 0.22f, decay: Float? = null, gravity: Float? = null
    ) {
        repeat(count) {
            val a = Random.nextFloat() * TWO_PI_F
            val sp = power * fxRand(0.35f, 1.25f)
            val shape = when {
                fxChance(0.3f) -> SsFxShape.Circle
                fxChance(0.5f) -> SsFxShape.Star
                else -> SsFxShape.Rect
            }
            add(
                FxParticle(
                    x = x, y = y, vx = cos(a) * sp, vy = sin(a) * sp - fxRand(0f, 120f),
                    size = fxRand(5f, 13f), color = fxPick(palette), shape = shape,
                    gravity = gravity ?: fxRand(700f, 1100f), drag = fxRand(0.972f, 0.986f),
                    sway = fxRand(10f, 50f), decay = decay ?: fxRand(0.22f, 0.40f),
                    glow = fxChance(glow)
                )
            )
        }
    }

    /** Slow confetti rain from the top edge. */
    fun rain(count: Int, palette: List<Color> = SsConfettiColors) {
        repeat(count) {
            add(
                FxParticle(
                    x = Random.nextFloat() * width, y = -30f,
                    vx = fxRand(-40f, 40f), vy = fxRand(80f, 200f),
                    size = fxRand(5f, 11f), color = fxPick(palette),
                    shape = if (fxChance(0.5f)) SsFxShape.Rect else SsFxShape.Ribbon,
                    gravity = fxRand(120f, 260f), drag = 0.998f,
                    sway = fxRand(30f, 90f), decay = fxRand(0.10f, 0.18f),
                    glow = false
                )
            )
        }
    }

    /** Glowing stars floating up across the whole screen. */
    fun sparkles(count: Int) {
        repeat(count) {
            add(
                FxParticle(
                    x = Random.nextFloat() * width, y = Random.nextFloat() * height,
                    vx = fxRand(-30f, 30f), vy = fxRand(-30f, 30f),
                    size = fxRand(4f, 9f), color = fxPick(SsBrandColors), shape = SsFxShape.Star,
                    gravity = -50f, drag = 0.99f,
                    sway = fxRand(15f, 40f), decay = fxRand(0.4f, 0.7f),
                    glow = true
                )
            )
        }
    }

    fun wave(x: Float, y: Float, color: Color, maxR: Float, width: Float = 3f) {
        waves.add(FxWave(x, y, color, maxR, width))
    }

    /** One firework: glowing radial burst + shockwave ring. */
    fun firework(x: Float, y: Float, color: Color) {
        burst(x, y, 40, 520f, listOf(color, Color.White, color), glow = 0.6f, decay = 0.4f, gravity = 380f)
        wave(x, y, color, 340f, 2f)
    }

    fun update(dt: Float) {
        var i = waves.size - 1
        while (i >= 0) {
            val w = waves[i]
            w.radius += 900f * dt
            w.life -= 1.9f * dt
            if (w.life <= 0f || w.radius > w.maxR) waves.removeAt(i)
            i--
        }
        i = particles.size - 1
        while (i >= 0) {
            val p = particles[i]
            step(p, dt)
            if (p.dead) particles.removeAt(i)
            i--
        }
    }

    private fun step(p: FxParticle, dt: Float) {
        val s = dt * 60f
        p.vy += p.gravity * dt
        val dragF = p.drag.pow(s)
        p.vx *= dragF
        p.vy *= dragF
        p.swayPhase += p.swaySpeed * dt
        p.x += p.vx * dt + sin(p.swayPhase) * p.sway * dt
        p.y += p.vy * dt
        p.rotation += p.rotSpeed * dt
        p.flip += p.flipSpeed * dt
        p.life -= p.decay * dt
        if (p.life <= 0f || p.y > height + 120f || p.x < -160f || p.x > width + 160f) p.dead = true
    }

    /** Draw in dp space (caller applies scale(density)). */
    fun draw(scope: DrawScope) {
        with(scope) {
            for (w in waves) {
                val a = (w.life.coerceAtLeast(0f)) * 0.55f
                val lw = w.lineWidth * w.life
                if (a <= 0f || lw <= 0f) continue
                val c = Offset(w.x, w.y)
                // soft glow underneath, crisp ring on top
                drawCircle(color = w.color, radius = w.radius, center = c, alpha = a * 0.28f, style = Stroke(width = lw * 4f))
                drawCircle(color = w.color, radius = w.radius, center = c, alpha = a, style = Stroke(width = lw))
            }
            for (p in particles) {
                val a = min(1f, p.life * 2.4f)
                if (a <= 0f) continue
                val sz = p.size
                val c = Offset(p.x, p.y)
                val deg = p.rotation * 180f / PI.toFloat()
                if (p.glow) {
                    drawCircle(color = p.color, radius = sz * 1.15f, center = c, alpha = a * 0.22f)
                }
                when (p.shape) {
                    SsFxShape.Circle -> drawCircle(color = p.color, radius = sz * 0.5f, center = c, alpha = a)
                    SsFxShape.Star -> withTransform({
                        translate(p.x, p.y)
                        rotate(deg, Offset.Zero)
                        scale(sz * 0.72f, sz * 0.72f, Offset.Zero)
                    }) {
                        drawPath(path = unitStarPath, color = p.color, alpha = a)
                    }
                    SsFxShape.Ribbon -> withTransform({
                        translate(p.x, p.y)
                        rotate(deg, Offset.Zero)
                    }) {
                        drawRect(
                            color = p.color, alpha = a,
                            topLeft = Offset(-sz * 0.13f, -sz * 1.3f),
                            size = Size(sz * 0.26f, sz * 2.6f)
                        )
                    }
                    SsFxShape.Rect -> {
                        val sy = abs(cos(p.flip)) * 0.88f + 0.12f
                        withTransform({
                            translate(p.x, p.y)
                            rotate(deg, Offset.Zero)
                            scale(1f, sy, Offset.Zero)
                        }) {
                            drawRect(
                                color = p.color, alpha = a,
                                topLeft = Offset(-sz * 0.5f, -sz * 0.34f),
                                size = Size(sz, sz * 0.68f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ==========================================================================
 * SOUNDS
 * The HTML synthesizes everything with WebAudio; here the same voices are
 * rendered once into a PCM buffer (whoosh, thump, fanfare, odometer ticks,
 * soft pop) and played with a single AudioTrack, so it stays in sync with
 * the visual timeline.
 * ========================================================================== */
internal object SessionSounds {
    private const val SR = 22050

    private enum class Wave { SINE, TRIANGLE, SQUARE, SAW }

    private class Mix(seconds: Double) {
        val buf = FloatArray((seconds * SR).toInt())

        /**
         * One synthesized voice.
         * Envelope = WebAudio style: attack to [peak] then exponential decay to 0.0001 at [endSec].
         * Frequency glides exponentially f0 -> f1 over [sweepSec] (0 = constant).
         * Optional one-pole low-pass sweeping lpFrom -> lpTo over the same glide.
         */
        fun voice(
            f0: Double, f1: Double = f0, sweepSec: Double = 0.0,
            offSec: Double, wave: Wave, peak: Double,
            attack: Double, endSec: Double,
            lpFrom: Double = 0.0, lpTo: Double = 0.0
        ) {
            val start = (offSec * SR).toInt()
            val n = (endSec * SR).toInt()
            var phase = 0.0
            var lp = 0.0
            for (i in 0 until n) {
                val idx = start + i
                if (idx < 0) continue
                if (idx >= buf.size) break
                val t = i.toDouble() / SR
                val f = if (sweepSec > 0.0) f0 * (f1 / f0).pow(min(1.0, t / sweepSec)) else f0
                phase += 2.0 * PI * f / SR
                var s = when (wave) {
                    Wave.SINE -> sin(phase)
                    Wave.TRIANGLE -> (2.0 / PI) * asin(sin(phase))
                    Wave.SQUARE -> if (sin(phase) >= 0.0) 1.0 else -1.0
                    Wave.SAW -> 2.0 * ((phase / (2.0 * PI)) % 1.0) - 1.0
                }
                if (lpFrom > 0.0) {
                    val fc = lpFrom * (lpTo / lpFrom).pow(min(1.0, t / sweepSec))
                    val a = 1.0 - exp(-2.0 * PI * fc / SR)
                    lp += a * (s - lp)
                    s = lp
                }
                val env = if (t < attack) peak * (t / attack)
                else peak * (0.0001 / peak).pow((t - attack) / (endSec - attack))
                buf[idx] += (s * env).toFloat()
            }
        }

        fun tone(freq: Double, off: Double, dur: Double, wave: Wave, vol: Double) =
            voice(f0 = freq, offSec = off, wave = wave, peak = vol, attack = 0.012, endSec = dur)

        fun toPcm(): ShortArray = ShortArray(buf.size) { i ->
            (buf[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        }
    }

    /** Quiet log: just the soft confirmation pop. */
    fun buildQuiet(): ShortArray {
        val m = Mix(0.5)
        m.voice(f0 = 420.0, f1 = 280.0, sweepSec = 0.18, offSec = 0.04, wave = Wave.SINE, peak = 0.08, attack = 0.02, endSec = 0.28)
        return m.toPcm()
    }

    /** Whoosh + thump at 60 ms, fanfare at 280 ms, odometer ticks from 420 ms. */
    fun buildCelebration(level: Int): ShortArray {
        val m = Mix(3.6)

        // whoosh (sawtooth 140 -> 900 Hz through a sweeping low-pass)
        m.voice(
            f0 = 140.0, f1 = 900.0, sweepSec = 0.32, offSec = 0.06, wave = Wave.SAW,
            peak = 0.055, attack = 0.05, endSec = 0.4, lpFrom = 350.0, lpTo = 3200.0
        )
        // thump
        m.voice(f0 = 160.0, f1 = 45.0, sweepSec = 0.28, offSec = 0.06, wave = Wave.SINE, peak = 0.22, attack = 0.015, endSec = 0.35)

        // fanfare
        val b = 0.28
        m.tone(523.25, b + 0.00, 0.45, Wave.TRIANGLE, 0.11)
        m.tone(659.25, b + 0.07, 0.45, Wave.TRIANGLE, 0.11)
        m.tone(783.99, b + 0.14, 0.55, Wave.TRIANGLE, 0.11)
        m.tone(1046.50, b + 0.21, 0.75, Wave.TRIANGLE, 0.13)
        m.tone(1318.51, b + 0.30, 0.95, Wave.SINE, 0.07)
        m.tone(1567.98, b + 0.38, 0.80, Wave.SINE, 0.05)
        if (level >= 3) {
            m.tone(2093.00, b + 0.52, 1.4, Wave.SINE, 0.05)
            m.tone(2637.02, b + 0.60, 1.4, Wave.SINE, 0.04)
            m.tone(3135.96, b + 0.70, 1.6, Wave.SINE, 0.03)
        }
        if (level >= 5) {
            // MEGA sub-bass drop
            m.voice(f0 = 90.0, f1 = 35.0, sweepSec = 0.7, offSec = b, wave = Wave.SINE, peak = 0.28, attack = 0.02, endSec = 0.9)
        }

        // odometer ticks while the score rolls (about every 67 ms for the first 85% of 1.5 s)
        var k = 0
        while (k * 0.067 <= 1.275) {
            val f = 1800.0 + Random.nextDouble() * 800.0
            m.voice(f0 = f, offSec = 0.42 + k * 0.067, wave = Wave.SQUARE, peak = 0.018, attack = 0.004, endSec = 0.06)
            k++
        }
        return m.toPcm()
    }

    /** Plays the buffer, skipping [startOffsetMs] so audio stays aligned with the visuals. */
    fun play(samples: ShortArray, startOffsetMs: Long = 0L): AudioTrack? = try {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SR)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        val skipFrames = ((startOffsetMs * SR) / 1000L).toInt().coerceIn(0, samples.size - 1)
        if (skipFrames > 0) track.playbackHeadPosition = skipFrames
        track.play()
        track
    } catch (_: Exception) {
        null
    }
}