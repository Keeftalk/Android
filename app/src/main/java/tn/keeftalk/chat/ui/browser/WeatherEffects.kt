package tn.keeftalk.chat.ui.browser

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.sin
import kotlin.random.Random

data class WeatherParticle(
    val layer: Int, // 0, 1, 2
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    val length: Float,
    val phase: Float,
    val sway: Float,
    val z: Float
)

data class CloudItem(
    val width: Float,
    val height: Float,
    var x: Float,
    var y: Float,
    val speed: Float,
    val opacity: Float,
    val puffOffsets: List<Triple<Float, Float, Float>> // relX, relY, relRadius
)

data class LightningBolt(
    val mainPath: List<Offset>,
    val branches: List<List<Offset>>
)

object WeatherEffectsHelper {

    fun createCloud(initX: Float = -200f, canvasWidth: Float, density: Int): CloudItem {
        val z = Random.nextFloat()
        val w = 150f + z * 230f
        val h = w * 0.42f
        val opacity = (0.4f + Random.nextFloat() * 0.4f + density / 250f).coerceAtMost(0.95f)
        val y = Random.nextFloat() * 390f + 40f + (z - 0.5f) * 40f
        val speed = 5f + z * 20f

        val puffs = mutableListOf<Triple<Float, Float, Float>>()
        for (k in 0 until 6) {
            val pw = w * (0.3f + Random.nextFloat() * 0.25f)
            val px = Random.nextFloat() * (w - pw)
            val py = Random.nextFloat() * (h * 0.5f)
            puffs.add(Triple(px, py, pw))
        }

        val startX = if (initX > -100f) initX else -w * 1.1f
        return CloudItem(w, h, startX, y, speed, opacity, puffs)
    }

    fun createParticle(condition: String, initY: Float = -20f, canvasWidth: Float, canvasHeight: Float): WeatherParticle {
        val z = Random.nextFloat()
        val layer = if (z < 0.4f) 0 else if (z < 0.8f) 1 else 2
        return if (condition == "snow") {
            WeatherParticle(
                layer = layer,
                x = Random.nextFloat() * (canvasWidth + 80f) - 40f,
                y = if (initY > -10f) initY else Random.nextFloat() * canvasHeight,
                speed = 25f + z * 55f,
                size = 1f + z * 2.6f,
                length = 0f,
                phase = Random.nextFloat() * 6.28f,
                sway = 8f + Random.nextFloat() * 18f,
                z = z
            )
        } else {
            WeatherParticle(
                layer = layer,
                x = Random.nextFloat() * (canvasWidth + 180f) - 120f,
                y = if (initY > -10f) initY else Random.nextFloat() * canvasHeight,
                speed = 520f + z * 900f,
                size = 1f + z * 1.5f,
                length = 10f + z * 26f,
                phase = 0f,
                sway = 0f,
                z = z
            )
        }
    }

    fun generateLightningBolt(canvasWidth: Float, canvasHeight: Float): LightningBolt {
        var x = (0.15f + Random.nextFloat() * 0.7f) * canvasWidth
        var y = 0f
        val mainPts = mutableListOf(Offset(x, y))
        val branches = mutableListOf<List<Offset>>()
        val targetY = (0.3f + Random.nextFloat() * 0.3f) * canvasHeight

        while (y < targetY) {
            y += 12f + Random.nextFloat() * 20f
            x += (Random.nextFloat() - 0.5f) * 52f
            mainPts.add(Offset(x, y))

            if (Random.nextFloat() < 0.13f) {
                var bx = x
                var by = y
                val bList = mutableListOf(Offset(bx, by))
                val dir = if (Random.nextBoolean()) -1f else 1f
                val count = (3..6).random()
                for (k in 0 until count) {
                    bx += dir * (4f + Random.nextFloat() * 22f)
                    by += 10f + Random.nextFloat() * 14f
                    bList.add(Offset(bx, by))
                }
                branches.add(bList)
            }
        }
        return LightningBolt(mainPts, branches)
    }

    fun playThunderRumble(context: Context, volume: Float = 0.8f) {
        try {
            val sampleRate = 22050
            val durationSec = 3
            val numSamples = sampleRate * durationSec
            val samples = ShortArray(numSamples)
            var b = 0.0

            for (i in 0 until numSamples) {
                b = b * 0.985 + (Random.nextDouble() * 2.0 - 1.0) * 0.1
                val env = 1.0 - (i.toDouble() / numSamples)
                val sampleVal = (b * (1.0 + 0.6 * sin(i.toDouble() / sampleRate * 6.0)) * env * 32767.0 * volume).toInt()
                    .coerceIn(-32768, 32767)
                samples[i] = sampleVal.toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
        } catch (_: Exception) {}
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun triggerThunderHaptics(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                val timings = longArrayOf(0, 25, 40, 50)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
        } catch (_: Exception) {}
    }
}
