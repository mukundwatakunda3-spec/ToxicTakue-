package com.example.utils

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.sin

object SoundSynth {
    fun playEngine(baseFreq: Float, durationMs: Int = 120) {
        playSynth(durationMs) { sampleIndex, sampleRate ->
            // Sawtooth-like wave for engine growl
            val period = sampleRate / baseFreq
            val phase = (sampleIndex % period) / period
            val value = (phase * 2.0 - 1.0) * 0.12
            value.toFloat()
        }
    }

    fun playNitro(durationMs: Int = 350) {
        playSynth(durationMs) { sampleIndex, sampleRate ->
            // High frequency whistle sliding down dynamically
            val progress = sampleIndex.toFloat() / (sampleRate * (durationMs / 1000f))
            val freq = 750f + (1 - progress) * 1100f
            val rad = 2.0 * Math.PI * freq * sampleIndex / sampleRate
            val noise = (Math.random() * 2.0 - 1.0) * 0.02
            val value = (sin(rad) * 0.08 + noise) * (1.0 - progress)
            value.toFloat()
        }
    }

    fun playChomp() {
        playSynth(100) { sampleIndex, sampleRate ->
            // Sound of bite munching: white noise mixed with pulsed low rectangle wave
            val progress = sampleIndex.toFloat() / (sampleRate * 0.10f)
            val noise = (Math.random() * 2.0 - 1.0) * 0.15
            val square = if ((sampleIndex / 60) % 2 == 0) 0.06 else -0.06
            val value = (noise + square) * (1.0 - progress)
            value.toFloat()
        }
    }

    fun playSplash() {
        playSynth(220) { sampleIndex, sampleRate ->
            // Bubble splash: modulated noise
            val progress = sampleIndex.toFloat() / (sampleRate * 0.22f)
            val noise = (Math.random() * 2.0 - 1.0) * 0.18
            val modulation = sin(2.0 * Math.PI * 18.0 * sampleIndex / sampleRate) * 0.4 + 0.6
            val value = noise * modulation * (1.0 - progress)
            value.toFloat()
        }
    }

    fun playKiss() {
        playSynth(280) { sampleIndex, sampleRate ->
            // Pitch sliding upwards for kissing sound
            val progress = sampleIndex.toFloat() / (sampleRate * 0.28f)
            val freq = 420f + progress * 800f // slides from 420 to 1220 Hz
            val rad = 2.0 * Math.PI * freq * sampleIndex / sampleRate
            val envelope = sin(progress * Math.PI) // humped shape
            val value = sin(rad) * 0.15 * envelope
            value.toFloat()
        }
    }

    fun playCoin() {
        playSynth(130) { sampleIndex, sampleRate ->
            // Double-ping coin tone
            val progress = sampleIndex.toFloat() / (sampleRate * 0.13f)
            val freq = if (progress < 0.35f) 950f else 1350f
            val rad = 2.0 * Math.PI * freq * sampleIndex / sampleRate
            val value = sin(rad) * 0.15 * (1.0 - progress)
            value.toFloat()
        }
    }

    fun playCrash() {
        playSynth(400) { sampleIndex, sampleRate ->
            // Low-frequency explosion / crunch noise
            val progress = sampleIndex.toFloat() / (sampleRate * 0.40f)
            val noise = (Math.random() * 2.0 - 1.0) * 0.35
            val crunchFreq = 80f + (1 - progress) * 120f
            val rad = 2.0 * Math.PI * crunchFreq * sampleIndex / sampleRate
            val value = (noise + sin(rad) * 0.15) * (1.0 - progress)
            value.toFloat()
        }
    }

    fun playBeatTone(artist: String, baseFreq: Float) {
        when (artist) {
            "Kendrick Lamar" -> {
                playSynth(80) { sampleIndex, sampleRate ->
                    val progress = sampleIndex.toFloat() / (sampleRate * 0.08f)
                    val freq = baseFreq * 1.5f
                    val rad = 2.0 * Math.PI * freq * sampleIndex / sampleRate
                    val noise = (Math.random() * 2.0 - 1.0) * 0.05
                    val value = (sin(rad) * 0.12 + noise) * (1.0 - progress)
                    value.toFloat()
                }
            }
            "Travis Scott" -> {
                playSynth(130) { sampleIndex, sampleRate ->
                    val progress = sampleIndex.toFloat() / (sampleRate * 0.13f)
                    val slideFreq = baseFreq * (1.0f - progress * 0.5f)
                    val rad = 2.0 * Math.PI * slideFreq * sampleIndex / sampleRate
                    val wave = sin(rad) * 1.5
                    val clipped = wave.coerceIn(-0.15, 0.15)
                    val value = clipped * (1.0 - progress)
                    value.toFloat()
                }
            }
            "Lithe" -> {
                playSynth(150) { sampleIndex, sampleRate ->
                    val progress = sampleIndex.toFloat() / (sampleRate * 0.15f)
                    val rad1 = 2.0 * Math.PI * baseFreq * sampleIndex / sampleRate
                    val rad2 = 2.0 * Math.PI * (baseFreq * 1.25f) * sampleIndex / sampleRate
                    val envelope = sin(progress * Math.PI)
                    val value = (sin(rad1) + sin(rad2)) * 0.08 * envelope
                    value.toFloat()
                }
            }
            "Runna rulez" -> {
                playSynth(100) { sampleIndex, sampleRate ->
                    val progress = sampleIndex.toFloat() / (sampleRate * 0.10f)
                    val rad = 2.0 * Math.PI * 880f * sampleIndex / sampleRate
                    val square = if ((sampleIndex / 30) % 2 == 0) 0.08 else -0.08
                    val value = (sin(rad) * 0.08 + square * 0.04) * (1.0 - progress)
                    value.toFloat()
                }
            }
            else -> {
                playCoin()
            }
        }
    }

    private fun playSynth(durationMs: Int, generator: (Int, Float) -> Float) {
        val sampleRate = 22050f
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = FloatArray(numSamples)
        for (i in 0 until numSamples) {
            samples[i] = generator(i, sampleRate).coerceIn(-1.0f, 1.0f)
        }

        // Write to PCM bytes
        val buffer = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val sample = (samples[i] * 32767).toInt().coerceIn(-32768, 32767)
            buffer[i * 2] = (sample and 0xFF).toByte()
            buffer[i * 2 + 1] = ((sample ushr 8) and 0xFF).toByte()
        }

        Thread {
            try {
                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate.toInt(),
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size,
                    AudioTrack.MODE_STATIC
                )
                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 40)
                track.stop()
                track.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
