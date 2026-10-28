package com.hematcount.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Haptic and audio feedback.
 * - Tap: crisp short tick (vibration + soft click tone).
 * - Correction: double light tick + lower tone.
 * - Target reached: three-note ascending chime + heavy vibration pattern.
 */
class Feedback(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private val sampleRate = 44100
    private val tickPcm: ShortArray by lazy { tone(listOf(1800.0), 0.028, 0.35) }
    private val lowTickPcm: ShortArray by lazy { tone(listOf(700.0), 0.06, 0.4) }
    private val chimePcm: ShortArray by lazy { chime() }

    fun tap(haptic: Boolean, sound: Boolean) {
        if (haptic) vibrateTick()
        if (sound) play(tickPcm)
    }

    fun correction(haptic: Boolean, sound: Boolean) {
        if (haptic) vibrate(longArrayOf(0, 18, 60, 18), intArrayOf(0, 160, 0, 160))
        if (sound) play(lowTickPcm)
    }

    fun denied(haptic: Boolean) {
        if (haptic) vibrate(longArrayOf(0, 40), intArrayOf(0, 120))
    }

    fun targetReached(haptic: Boolean, sound: Boolean) {
        if (haptic) vibrate(
            longArrayOf(0, 300, 120, 300, 120, 500),
            intArrayOf(0, 255, 0, 255, 0, 255),
        )
        if (sound) play(chimePcm)
    }

    private fun vibrateTick() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            v.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun vibrate(timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = if (v.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
        v.vibrate(effect)
    }

    // ------------------------------------------------------------------ audio
    private fun play(pcm: ShortArray) {
        thread(isDaemon = true) {
            runCatching {
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
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.size * 2)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep((pcm.size * 1000L / sampleRate) + 80)
                track.stop()
                track.release()
            }
        }
    }

    private fun tone(freqs: List<Double>, seconds: Double, volume: Double): ShortArray {
        val n = (sampleRate * seconds).toInt()
        return ShortArray(n) { i ->
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 60.0) * minOf(1.0, i / 40.0)
            val s = freqs.sumOf { sin(2 * PI * it * t) } / freqs.size
            (s * env * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** C6 – E6 – G6 – C7 bell chime with harmonics. */
    private fun chime(): ShortArray {
        val notes = listOf(1046.5, 1318.5, 1568.0, 2093.0)
        val step = 0.16
        val total = (sampleRate * (step * notes.size + 0.9)).toInt()
        val out = DoubleArray(total)
        notes.forEachIndexed { k, f ->
            val start = (sampleRate * step * k).toInt()
            val len = (sampleRate * 0.9).toInt()
            for (i in 0 until len) {
                val idx = start + i; if (idx >= total) break
                val t = i.toDouble() / sampleRate
                val env = exp(-t * 4.5) * minOf(1.0, i / 120.0)
                val s = sin(2 * PI * f * t) + 0.35 * sin(2 * PI * f * 2 * t) + 0.12 * sin(2 * PI * f * 3 * t)
                out[idx] += s * env
            }
        }
        val peak = out.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-6)
        return ShortArray(total) { (out[it] / peak * 0.85 * Short.MAX_VALUE).toInt().toShort() }
    }
}
