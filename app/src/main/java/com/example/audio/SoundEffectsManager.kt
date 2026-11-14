package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundEffectsManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050

    @Volatile
    var isEnabled: Boolean = true

    private val swooshBuffer: ShortArray by lazy { generateSwoosh() }
    private val errorBuffer: ShortArray by lazy { generateErrorThud() }
    private val hintBuffer: ShortArray by lazy { generateHintChime() }
    private val victoryBuffer: ShortArray by lazy { generateVictoryFanfare() }
    private val tapBuffer: ShortArray by lazy { generateSoftTap() }

    fun prewarm() {
        scope.launch {
            swooshBuffer.size
            errorBuffer.size
            hintBuffer.size
            victoryBuffer.size
            tapBuffer.size
        }
    }

    fun playSwoosh() {
        if (!isEnabled) return
        playBuffer(swooshBuffer)
    }

    fun playError() {
        if (!isEnabled) return
        playBuffer(errorBuffer)
    }

    fun playHint() {
        if (!isEnabled) return
        playBuffer(hintBuffer)
    }

    fun playVictory() {
        if (!isEnabled) return
        playBuffer(victoryBuffer)
    }

    fun playTap() {
        if (!isEnabled) return
        playBuffer(tapBuffer)
    }

    private fun playBuffer(samples: ShortArray) {
        scope.launch(Dispatchers.IO) {
            try {
                val track = AudioTrack.Builder()
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

                track.write(samples, 0, samples.size)
                track.setNotificationMarkerPosition(samples.size)
                track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(t: AudioTrack?) {
                        try {
                            t?.stop()
                            t?.release()
                        } catch (_: Throwable) {
                        }
                    }

                    override fun onPeriodicNotification(t: AudioTrack?) = Unit
                })
                track.play()
            } catch (_: Throwable) {
                // Gracefully ignore in environments without audio hardware (e.g., unit tests)
            }
        }
    }

    private fun generateSwoosh(): ShortArray {
        val durationSec = 0.22
        val count = (sampleRate * durationSec).toInt()
        val rng = Random(42)
        val out = ShortArray(count)
        var phase = 0.0
        var lp = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val env = sin(PI * t) * (1.0 - t * 0.35)
            // Rising airy pitch + filtered white noise whoosh
            val freq = 320.0 + 980.0 * t * t
            phase += 2.0 * PI * freq / sampleRate
            val tone = sin(phase) * 0.45
            val noise = rng.nextDouble(-1.0, 1.0)
            lp = lp * 0.82 + noise * 0.18
            val sample = (tone + lp * 0.85) * env * 0.48
            out[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun generateErrorThud(): ShortArray {
        val durationSec = 0.19
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val env = exp(-t * 10.0)
            val freq = 155.0 - 65.0 * t
            phase += 2.0 * PI * freq / sampleRate
            val buzz = sin(phase) * 0.7 + sin(phase * 2.0) * 0.25
            val sample = buzz * env * 0.55
            out[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun generateHintChime(): ShortArray {
        val durationSec = 0.32
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        val notes = doubleArrayOf(587.33, 880.0) // D5 -> A5
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = if (t < 0.35) notes[0] else notes[1]
            val localT = if (t < 0.35) t / 0.35 else (t - 0.35) / 0.65
            val env = exp(-localT * 4.5) * sin(PI * localT.coerceAtMost(0.5))
            phase += 2.0 * PI * freq / sampleRate
            val sample = (sin(phase) + 0.25 * sin(phase * 2.0)) * env * 0.42
            out[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun generateVictoryFanfare(): ShortArray {
        val durationSec = 0.68
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5 E5 G5 C6
        val stepSize = count / freqs.size
        val phases = DoubleArray(freqs.size)
        for (i in 0 until count) {
            val activeNotes = (i / stepSize + 1).coerceAtMost(freqs.size)
            var mix = 0.0
            for (n in 0 until activeNotes) {
                val noteStart = n * stepSize
                val localT = (i - noteStart).toDouble() / (count - noteStart).coerceAtLeast(1)
                val env = exp(-localT * 3.2) * (1.0 - exp(-localT * 35.0))
                phases[n] += 2.0 * PI * freqs[n] / sampleRate
                mix += (sin(phases[n]) + 0.2 * sin(phases[n] * 2.0)) * env
            }
            val sample = (mix / freqs.size) * 0.65
            out[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun generateSoftTap(): ShortArray {
        val durationSec = 0.05
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val env = exp(-t * 18.0)
            phase += 2.0 * PI * 640.0 / sampleRate
            val sample = sin(phase) * env * 0.25
            out[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    fun release() {
        scope.cancel()
    }
}
