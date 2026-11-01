package com.canopy.clock.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.canopy.clock.data.ToneSettings
import com.canopy.clock.data.ToneType
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Sintetizador de tonos. Todos los sonidos se generan en el móvil a partir de
 * formas de onda; no hay grabaciones. Cada tipo imita una tecnología de despertador.
 */
object ToneSynth {
    const val RATE = 44100

    /** Genera un ciclo del patrón (se repite en bucle mientras suena). */
    fun pattern(type: ToneType, pitch: Float, tempo: Float): FloatArray {
        val t = 1f / tempo.coerceIn(0.3f, 3f)
        val pt = pitch.coerceIn(0.4f, 2.5f)
        return when (type) {
            ToneType.QUARTZ -> {
                // ráfaga de cuatro pitidos agudos y silencio, como un despertador de cuarzo
                val f = 2730f * pt
                seq(listOf(
                    beep(f, 0.065f * t, square = true), silence(0.06f * t),
                    beep(f, 0.065f * t, square = true), silence(0.06f * t),
                    beep(f, 0.065f * t, square = true), silence(0.06f * t),
                    beep(f, 0.065f * t, square = true), silence(0.55f * t),
                ))
            }
            ToneType.BELL -> seq(listOf(bell(880f * pt, 0.38f * t), bell(1175f * pt, 0.38f * t), silence(0.25f * t)))
            ToneType.CHIRP -> seq(listOf(
                beep(1600f * pt, 0.05f * t), beep(2000f * pt, 0.05f * t), beep(2400f * pt, 0.07f * t), silence(0.12f * t),
                beep(1600f * pt, 0.05f * t), beep(2000f * pt, 0.05f * t), beep(2400f * pt, 0.07f * t), silence(0.7f * t),
            ))
            ToneType.GONG -> seq(listOf(gong(196f * pt, 2.6f * t)))
            ToneType.PULSE -> silence(1f)
        }
    }

    /** Señal corta para cambios de intervalo o vista previa. */
    fun cue(type: ToneType, count: Int, pitch: Float = 1f): FloatArray {
        val parts = mutableListOf<FloatArray>()
        repeat(count) {
            parts += when (type) {
                ToneType.BELL -> bell(1046f * pitch, 0.35f)
                ToneType.GONG -> gong(262f * pitch, 0.9f)
                ToneType.CHIRP -> seq(listOf(beep(1800f * pitch, 0.05f), beep(2400f * pitch, 0.06f)))
                else -> beep(2730f * pitch, 0.09f, square = true)
            }
            parts += silence(0.12f)
        }
        return seq(parts)
    }

    fun click(): FloatArray {
        val n = (RATE * 0.006f).toInt()
        return FloatArray(n) { i -> val e = 1f - i.toFloat() / n; (if (i % 2 == 0) 1f else -1f) * e * e * 0.35f }
    }

    private fun seq(parts: List<FloatArray>): FloatArray {
        val out = FloatArray(parts.sumOf { it.size })
        var o = 0
        for (p in parts) { p.copyInto(out, o); o += p.size }
        return out
    }

    private fun silence(sec: Float) = FloatArray((RATE * sec).toInt())

    private fun beep(f: Float, sec: Float, square: Boolean = false): FloatArray {
        val n = (RATE * sec).toInt()
        val ramp = (RATE * 0.004f).toInt()
        return FloatArray(n) { i ->
            val ph = 2 * PI * f * i / RATE
            var v = sin(ph)
            if (square) v = v + sin(3 * ph) / 3 + sin(5 * ph) / 5   // cuadrada suavizada, timbre piezoeléctrico
            val env = min(1f, min(i.toFloat() / ramp, (n - i).toFloat() / ramp))
            (v * env * 0.55).toFloat()
        }
    }

    private fun bell(f: Float, sec: Float): FloatArray {
        val n = (RATE * sec).toInt()
        val partials = floatArrayOf(1f, 2.76f, 5.4f, 8.93f)
        val amps = floatArrayOf(1f, 0.5f, 0.25f, 0.12f)
        return FloatArray(n) { i ->
            val tt = i.toFloat() / RATE
            var v = 0.0
            for (k in partials.indices) v += amps[k] * sin(2 * PI * f * partials[k] * tt) * exp(-tt * (5.0 + k * 4))
            val attack = min(1f, i / (RATE * 0.002f))
            (v * 0.45 * attack).toFloat()
        }
    }

    private fun gong(f: Float, sec: Float): FloatArray {
        val n = (RATE * sec).toInt()
        val partials = floatArrayOf(1f, 1.48f, 2.03f, 2.69f, 3.6f)
        val amps = floatArrayOf(1f, 0.6f, 0.45f, 0.3f, 0.2f)
        return FloatArray(n) { i ->
            val tt = i.toFloat() / RATE
            var v = 0.0
            for (k in partials.indices) v += amps[k] * sin(2 * PI * f * partials[k] * tt + k) * exp(-tt * (1.1 + k * 0.6))
            val attack = min(1f, i / (RATE * 0.03f))
            (v * 0.32 * attack).toFloat()
        }
    }

    fun toPcm(src: FloatArray, gain: Float): ShortArray =
        ShortArray(src.size) { i -> (max(-1f, min(1f, src[i] * gain)) * 32767).toInt().toShort() }

    fun newTrack(usage: Int = AudioAttributes.USAGE_ALARM, bufferSamples: Int = RATE): AudioTrack =
        AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(max(AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT), bufferSamples * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
}

/** Reproduce un tono en bucle con subida progresiva de volumen. */
class TonePlayer(private val s: ToneSettings, private val usage: Int = AudioAttributes.USAGE_ALARM) {
    @Volatile private var running = false
    private var thread: Thread? = null

    fun start(maxSeconds: Int = 600) {
        if (running || s.type == ToneType.PULSE) return
        running = true
        thread = Thread {
            val track = ToneSynth.newTrack(usage)
            val loop = ToneSynth.pattern(s.type, s.pitch, s.tempo)
            track.play()
            val start = System.currentTimeMillis()
            try {
                while (running && System.currentTimeMillis() - start < maxSeconds * 1000L) {
                    val el = (System.currentTimeMillis() - start) / 1000f
                    val g = if (s.crescendoSec <= 0) 1f else (0.08f + 0.92f * (el / s.crescendoSec)).coerceAtMost(1f)
                    val pcm = ToneSynth.toPcm(loop, g * s.volume)
                    track.write(pcm, 0, pcm.size)
                }
            } finally {
                runCatching { track.pause(); track.flush(); track.stop() }
                track.release()
            }
        }.also { it.start() }
    }

    fun stop() { running = false; thread = null }

    companion object {
        /** Reproduce un fragmento corto una vez (vista previa, avisos de intervalo, tic-tac). */
        fun once(samples: FloatArray, gain: Float = 0.9f, usage: Int = AudioAttributes.USAGE_ALARM) {
            Thread {
                val track = ToneSynth.newTrack(usage, samples.size + ToneSynth.RATE / 10)
                val pcm = ToneSynth.toPcm(samples, gain)
                track.play()
                track.write(pcm, 0, pcm.size)
                // silencio final para que el búfer termine de sonar
                val tail = ShortArray(ToneSynth.RATE / 5)
                track.write(tail, 0, tail.size)
                runCatching { track.stop() }
                track.release()
            }.start()
        }

        fun preview(s: ToneSettings, seconds: Float = 3f) {
            if (s.type == ToneType.PULSE) return
            val loop = ToneSynth.pattern(s.type, s.pitch, s.tempo)
            val n = (ToneSynth.RATE * seconds).toInt()
            val out = FloatArray(n) { loop[it % loop.size] }
            once(out, s.volume)
        }
    }
}
