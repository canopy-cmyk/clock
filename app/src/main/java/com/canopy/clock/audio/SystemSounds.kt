package com.canopy.clock.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.canopy.clock.data.ToneSettings
import kotlinx.coroutines.flow.MutableStateFlow

/** Sonidos del sistema (los mismos que ofrece Android en sus ajustes). */
object Sounds {
    data class Sound(val title: String, val uri: String)

    enum class Kind(val label: String, val type: Int) {
        ALARM("Alarmas", RingtoneManager.TYPE_ALARM),
        NOTIFICATION("Notificaciones", RingtoneManager.TYPE_NOTIFICATION),
        RINGTONE("Tonos de llamada", RingtoneManager.TYPE_RINGTONE),
        FILES("Mis archivos", -1),
    }

    fun list(ctx: Context, kind: Kind): List<Sound> {
        if (kind == Kind.FILES) return com.canopy.clock.data.Store.customSounds().map { Sound(it.second, it.first) }
        val out = mutableListOf<Sound>()
        runCatching {
            val rm = RingtoneManager(ctx); rm.setType(kind.type)
            val c = rm.cursor
            while (c.moveToNext()) {
                val title = c.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
                out += Sound(title, rm.getRingtoneUri(c.position).toString())
            }
        }
        return out.distinctBy { it.uri }
    }

    fun defaultAlarm(): Uri = Settings.System.DEFAULT_ALARM_ALERT_URI
    fun defaultNotification(): Uri = Settings.System.DEFAULT_NOTIFICATION_URI

    fun resolve(uri: String, fallback: Uri = defaultAlarm()): Uri = if (uri.isBlank()) fallback else Uri.parse(uri)

    fun title(ctx: Context, t: ToneSettings): String = when {
        t.silent -> "Solo vibración"
        t.title.isNotBlank() -> t.title
        else -> "Alarma predeterminada"
    }

    /** Toque corto (cambio de fase en intervalos). */
    fun cue(ctx: Context, uri: String) {
        runCatching {
            val r = RingtoneManager.getRingtone(ctx, resolve(uri, defaultNotification())) ?: return
            r.audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            r.play()
            Handler(Looper.getMainLooper()).postDelayed({ runCatching { r.stop() } }, 4000)
        }
    }

    /** Pitido del sistema para la cuenta atrás 3-2-1. */
    private var tg: ToneGenerator? = null
    fun beep() {
        runCatching {
            val g = tg ?: ToneGenerator(AudioManager.STREAM_ALARM, 70).also { tg = it }
            g.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        }
    }
}

/** Reproduce un sonido del sistema en bucle, con subida progresiva de volumen. */
class AlarmPlayer(private val ctx: Context, private val s: ToneSettings) {
    private var mp: MediaPlayer? = null
    private var synth: SynthLoop? = null
    private val handler = Handler(Looper.getMainLooper())
    private var started = 0L

    private val ramp = object : Runnable {
        override fun run() {
            val p = mp ?: return
            val el = (System.currentTimeMillis() - started) / 1000f
            val g = if (s.crescendoSec <= 0) 1f else (0.06f + 0.94f * el / s.crescendoSec).coerceAtMost(1f)
            val v = g * s.volume
            runCatching { p.setVolume(v, v) }
            if (g < 1f) handler.postDelayed(this, 250)
        }
    }

    fun start() {
        if (s.silent) return
        started = System.currentTimeMillis()
        val candidates = listOf(Sounds.resolve(s.uri), Sounds.defaultAlarm(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
        for (u in candidates) {
            val p = runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    setDataSource(ctx, u)
                    isLooping = true
                    prepare()
                    val v0 = if (s.crescendoSec <= 0) s.volume else 0.06f * s.volume
                    setVolume(v0, v0)
                    start()
                }
            }.getOrNull()
            if (p != null) { mp = p; handler.post(ramp); return }
        }
        // sin ningún sonido disponible: pitido sintetizado para que la alarma nunca quede muda
        synth = SynthLoop(s.volume).also { it.start() }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        runCatching { mp?.stop() }; runCatching { mp?.release() }; mp = null
        synth?.stop(); synth = null
    }
}

/** Escucha previa al elegir un sonido. La pantalla muestra cuál está sonando. */
object Preview {
    val playing = MutableStateFlow<String?>(null)
    private var mp: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    fun toggle(ctx: Context, uri: String, fallback: Uri = Sounds.defaultAlarm(), volume: Float = 0.9f, seconds: Int = 8) {
        if (playing.value == uri) { stop(); return }
        stop()
        val p = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(ctx, Sounds.resolve(uri, fallback))
                isLooping = true
                prepare()
                setVolume(volume, volume)
                start()
            }
        }.getOrNull() ?: return
        mp = p
        playing.value = uri
        handler.postDelayed({ stop() }, seconds * 1000L)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        runCatching { mp?.stop() }; runCatching { mp?.release() }; mp = null
        playing.value = null
    }
}

/** Nombre legible de un archivo de audio elegido en el navegador de archivos. */
fun fileTitle(ctx: Context, uri: Uri): String {
    val name = runCatching {
        ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment ?: "Archivo"
    return name.substringBeforeLast('.')
}
