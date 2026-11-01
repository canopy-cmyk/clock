package com.canopy.clock.faces

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.canopy.clock.R
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Paleta de colores planos de la app. */
object Pal {
    val INK = Color.parseColor("#151515")
    val PAPER = Color.parseColor("#ECEAE4")
    val WHITE = Color.parseColor("#FAFAF7")
    val ORANGE = Color.parseColor("#F08A1C")
    val YELLOW = Color.parseColor("#F4D40B")
    val GREEN = Color.parseColor("#3E9B5E")
    val RED = Color.parseColor("#E4472B")
    val BLUE = Color.parseColor("#2E5FA8")
    val GREY = Color.parseColor("#DAD7D0")
    val LCD = Color.parseColor("#B9BFA8")
    val TEAL = Color.parseColor("#5FE3C9")
    val LUME = Color.parseColor("#CFE8C9")
    val SLATE = Color.parseColor("#1E1F1C")

    /** Muestras que se ofrecen en el editor de cada esfera. */
    val SWATCHES = listOf(INK, SLATE, PAPER, WHITE, GREY, ORANGE, YELLOW, GREEN, RED, BLUE, LCD, TEAL, LUME,
        Color.parseColor("#0C1312"), Color.parseColor("#1A1714"), Color.parseColor("#8C8A84"))
}

/** Opciones propias de cada esfera, editables por el usuario. */
data class FaceOptions(
    val bg: Int,
    val ink: Int,
    val accent: Int,
    val sweep: Boolean = false,      // segundero continuo (true) o a saltos (false)
    val showSeconds: Boolean = true,
    val tick: Boolean = false,       // tic-tac audible en la app
    val h24: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject().put("bg", bg).put("ink", ink).put("accent", accent)
        .put("sweep", sweep).put("sec", showSeconds).put("tick", tick).put("h24", h24)

    companion object {
        fun fromJson(j: JSONObject, d: FaceOptions) = FaceOptions(
            j.optInt("bg", d.bg), j.optInt("ink", d.ink), j.optInt("accent", d.accent),
            j.optBoolean("sweep", d.sweep), j.optBoolean("sec", d.showSeconds),
            j.optBoolean("tick", d.tick), j.optBoolean("h24", d.h24)
        )
    }
}

enum class FaceKind { ANALOG, DIGITAL }

class FaceSpec(
    val id: String,
    val name: String,
    val kind: FaceKind,
    val blurb: String,
    val defaults: FaceOptions,
)

object Faces {
    val ALL = listOf(
        FaceSpec("funcional", "Funcional", FaceKind.ANALOG, "Esfera oscura, numeración clara, segundero de color.",
            FaceOptions(Pal.INK, Pal.WHITE, Pal.ORANGE)),
        FaceSpec("indices", "Índices", FaceKind.ANALOG, "Solo marcas, sin números. Máxima limpieza.",
            FaceOptions(Pal.WHITE, Pal.INK, Pal.RED)),
        FaceSpec("geometrico", "Geométrico", FaceKind.ANALOG, "Formas primarias como manecillas.",
            FaceOptions(Pal.PAPER, Pal.INK, Pal.BLUE)),
        FaceSpec("discos", "Discos", FaceKind.ANALOG, "La hora se lee por círculos que giran.",
            FaceOptions(Pal.GREY, Pal.INK, Pal.ORANGE)),
        FaceSpec("instrumento", "Instrumento", FaceKind.ANALOG, "Escalas técnicas, como un cuadro de mandos.",
            FaceOptions(Pal.SLATE, Pal.PAPER, Pal.YELLOW, sweep = true)),
        FaceSpec("herramienta", "Herramienta", FaceKind.ANALOG, "Bisel y marcas luminosas para la noche.",
            FaceOptions(Color.parseColor("#101211"), Pal.LUME, Pal.ORANGE)),
        FaceSpec("bloque", "Bloque", FaceKind.ANALOG, "Media pantalla de color sólido y hora en grande.",
            FaceOptions(Pal.WHITE, Pal.INK, Pal.ORANGE)),
        FaceSpec("lcd", "LCD", FaceKind.DIGITAL, "Siete segmentos con los apagados visibles.",
            FaceOptions(Pal.LCD, Color.parseColor("#262A22"), Pal.INK, showSeconds = false)),
        FaceSpec("persiana", "Persiana", FaceKind.DIGITAL, "Cartelas que caen al cambiar el minuto.",
            FaceOptions(Pal.PAPER, Pal.INK, Pal.WHITE, showSeconds = false)),
        FaceSpec("tubos", "Tubos", FaceKind.DIGITAL, "Dígitos de neón naranja en tubos de cristal.",
            FaceOptions(Color.parseColor("#1A1714"), Pal.ORANGE, Color.parseColor("#4A3D31"), showSeconds = false)),
        FaceSpec("vfd", "VFD", FaceKind.DIGITAL, "Verde azulado sobre negro, de radiodespertador.",
            FaceOptions(Color.parseColor("#0C1312"), Pal.TEAL, Pal.TEAL, showSeconds = false)),
        FaceSpec("matriz", "Matriz", FaceKind.DIGITAL, "Rejilla de puntos LED.",
            FaceOptions(Color.parseColor("#140F0E"), Pal.RED, Color.parseColor("#3A2A26"), showSeconds = false)),
        FaceSpec("tipografico", "Tipográfico", FaceKind.DIGITAL, "Solo números gigantes.",
            FaceOptions(Pal.YELLOW, Pal.INK, Pal.INK, showSeconds = false)),
        FaceSpec("palabras", "Palabras", FaceKind.DIGITAL, "La hora escrita en español.",
            FaceOptions(Pal.INK, Pal.WHITE, Pal.ORANGE, showSeconds = false)),
        FaceSpec("binario", "Binario", FaceKind.DIGITAL, "Columnas de bits para horas, minutos y segundos.",
            FaceOptions(Pal.PAPER, Pal.INK, Pal.ORANGE)),
    )

    fun byId(id: String?): FaceSpec = ALL.firstOrNull { it.id == id } ?: ALL[0]
}

/** Tipografías compartidas. */
object Fonts {
    var regular: Typeface = Typeface.DEFAULT
    var semi: Typeface = Typeface.DEFAULT_BOLD
    var heavy: Typeface = Typeface.DEFAULT_BOLD
    private var loaded = false
    fun init(ctx: Context) {
        if (loaded) return
        regular = ResourcesCompat.getFont(ctx, R.font.grotesk_400) ?: regular
        semi = ResourcesCompat.getFont(ctx, R.font.grotesk_600) ?: semi
        heavy = ResourcesCompat.getFont(ctx, R.font.grotesk_800) ?: heavy
        loaded = true
    }
}

/**
 * Dibuja cualquier esfera en un lienzo de Android. Lo usan la app, los widgets,
 * el modo reloj y el salvapantallas, así que todos se ven idénticos.
 * Todo se dibuja en un espacio de 200x200 escalado al tamaño disponible.
 */
object FaceRenderer {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    fun draw(c: Canvas, w: Float, h: Float, spec: FaceSpec, o: FaceOptions, t: LocalDateTime, fillBackground: Boolean = true) {
        val s = min(w, h) / 200f
        c.save()
        c.translate((w - 200f * s) / 2f, (h - 200f * s) / 2f)
        c.scale(s, s)
        p.reset(); p.isAntiAlias = true
        val nano = t.nano / 1_000_000_000f
        val sec = t.second + if (o.sweep) nano else 0f
        val min = t.minute + sec / 60f
        val hr = (t.hour % 12) + min / 60f
        val a = Angles(hr * 30f, min * 6f, sec * 6f)
        when (spec.id) {
            "funcional" -> funcional(c, o, a)
            "indices" -> indices(c, o, a)
            "geometrico" -> geometrico(c, o, a, fillBackground)
            "discos" -> discos(c, o, a, fillBackground)
            "instrumento" -> instrumento(c, o, a)
            "herramienta" -> herramienta(c, o, a)
            "bloque" -> bloque(c, o, a, t, fillBackground)
            "lcd" -> lcd(c, o, t)
            "persiana" -> persiana(c, o, t, fillBackground)
            "tubos" -> tubos(c, o, t)
            "vfd" -> vfd(c, o, t)
            "matriz" -> matriz(c, o, t)
            "tipografico" -> tipografico(c, o, t, fillBackground)
            "palabras" -> palabras(c, o, t, fillBackground)
            "binario" -> binario(c, o, t, fillBackground)
        }
        c.restore()
    }

    private class Angles(val h: Float, val m: Float, val s: Float)

    // ---------- utilidades ----------
    private fun fill(color: Int) { p.style = Paint.Style.FILL; p.color = color; p.pathEffect = null }
    private fun stroke(color: Int, wd: Float, cap: Paint.Cap = Paint.Cap.ROUND) {
        p.style = Paint.Style.STROKE; p.color = color; p.strokeWidth = wd; p.strokeCap = cap
    }

    private fun hand(c: Canvas, angle: Float, len: Float, wd: Float, color: Int, tail: Float = 0f, cap: Paint.Cap = Paint.Cap.ROUND) {
        stroke(color, wd, cap)
        c.save(); c.rotate(angle, 100f, 100f)
        c.drawLine(100f, 100f + tail, 100f, 100f - len, p)
        c.restore()
    }

    private fun ticks(c: Canvas, n: Int, r1: Float, r2: Float, color: Int, wd: Float, skipEvery: Int = 0) {
        stroke(color, wd, Paint.Cap.BUTT)
        for (i in 0 until n) {
            if (skipEvery > 0 && i % skipEvery == 0) continue
            c.save(); c.rotate(i * 360f / n, 100f, 100f)
            c.drawLine(100f, 100f - r1, 100f, 100f - r2, p)
            c.restore()
        }
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, tf: Typeface, align: Paint.Align = Paint.Align.CENTER, spacing: Float = 0f) {
        fill(color); p.typeface = tf; p.textSize = size; p.textAlign = align; p.letterSpacing = spacing
        c.drawText(s, x, y, p)
        p.letterSpacing = 0f
    }

    private fun centeredText(c: Canvas, s: String, x: Float, cy: Float, size: Float, color: Int, tf: Typeface) {
        fill(color); p.typeface = tf; p.textSize = size; p.textAlign = Paint.Align.CENTER
        val fm = p.fontMetrics
        c.drawText(s, x, cy - (fm.ascent + fm.descent) / 2f, p)
    }

    private fun numeral(c: Canvas, i: Int, radius: Float, size: Float, color: Int, tf: Typeface) {
        val ang = Math.toRadians(i * 30.0)
        centeredText(c, i.toString(), 100f + radius * sin(ang).toFloat(), 100f - radius * cos(ang).toFloat(), size, color, tf)
    }

    private fun blend(a: Int, b: Int, f: Float): Int {
        val r = (Color.red(a) * (1 - f) + Color.red(b) * f).toInt()
        val g = (Color.green(a) * (1 - f) + Color.green(b) * f).toInt()
        val bl = (Color.blue(a) * (1 - f) + Color.blue(b) * f).toInt()
        return Color.rgb(r, g, bl)
    }

    private fun hours(t: LocalDateTime, h24: Boolean): Int = if (h24) t.hour else ((t.hour + 11) % 12 + 1)

    private fun digits(t: LocalDateTime, o: FaceOptions): String =
        "%02d%02d".format(hours(t, o.h24), t.minute)

    // ---------- analógicos ----------
    private fun funcional(c: Canvas, o: FaceOptions, a: Angles) {
        fill(o.bg); c.drawCircle(100f, 100f, 96f, p)
        ticks(c, 60, 86f, 81f, blend(o.bg, o.ink, 0.45f), 1f)
        ticks(c, 12, 86f, 76f, o.ink, 2f)
        for (i in listOf(12, 3, 6, 9)) numeral(c, i, 62f, 18f, o.ink, Fonts.semi)
        hand(c, a.h, 44f, 7f, o.ink, 10f)
        hand(c, a.m, 70f, 5f, o.ink, 10f)
        if (o.showSeconds) {
            hand(c, a.s, 78f, 2f, o.accent, 22f)
            c.save(); c.rotate(a.s, 100f, 100f); fill(o.accent); c.drawCircle(100f, 120f, 5f, p); c.restore()
        }
        fill(o.accent); c.drawCircle(100f, 100f, 5f, p)
    }

    private fun indices(c: Canvas, o: FaceOptions, a: Angles) {
        fill(o.bg); c.drawCircle(100f, 100f, 96f, p)
        stroke(blend(o.bg, o.ink, 0.15f), 1f); c.drawCircle(100f, 100f, 96f, p)
        ticks(c, 12, 84f, 66f, o.ink, 5f)
        hand(c, a.h, 46f, 8f, o.ink, 0f, Paint.Cap.BUTT)
        hand(c, a.m, 74f, 6f, o.ink, 0f, Paint.Cap.BUTT)
        if (o.showSeconds) hand(c, a.s, 80f, 1.5f, o.accent, 20f)
        fill(o.accent); c.drawCircle(100f, 100f, 3.5f, p)
    }

    private fun geometrico(c: Canvas, o: FaceOptions, a: Angles, bg: Boolean) {
        if (bg) { fill(o.bg); c.drawRect(4f, 4f, 196f, 196f, p) }
        fill(o.ink)
        val tri = android.graphics.Path().apply { moveTo(100f, 14f); lineTo(109f, 30f); lineTo(91f, 30f); close() }
        c.drawPath(tri, p)
        c.drawRect(178f, 96f, 186f, 104f, p); c.drawRect(96f, 178f, 104f, 186f, p); c.drawCircle(18f, 100f, 4.5f, p)
        hand(c, a.h, 48f, 14f, Pal.RED, 0f, Paint.Cap.BUTT)
        hand(c, a.m, 74f, 8f, Pal.YELLOW, 0f, Paint.Cap.BUTT)
        if (o.showSeconds) { c.save(); c.rotate(a.s, 100f, 100f); fill(o.accent); c.drawCircle(100f, 26f, 9f, p); c.restore() }
        fill(o.ink); c.drawCircle(100f, 100f, 7f, p)
    }

    private fun discos(c: Canvas, o: FaceOptions, a: Angles, bg: Boolean) {
        if (bg) { fill(o.bg); r.set(4f, 4f, 196f, 196f); c.drawRoundRect(r, 14f, 14f, p) }
        c.save(); c.rotate(a.h, 100f, 100f); fill(o.accent); c.drawCircle(100f, 62f, 36f, p); c.restore()
        c.save(); c.rotate(a.m, 100f, 100f); fill(o.ink); c.drawCircle(100f, 36f, 20f, p); c.restore()
        if (o.showSeconds) { c.save(); c.rotate(a.s, 100f, 100f); fill(o.accent); c.drawCircle(100f, 16f, 7f, p); c.restore() }
        fill(o.ink); c.drawCircle(100f, 100f, 4f, p)
    }

    private fun instrumento(c: Canvas, o: FaceOptions, a: Angles) {
        fill(o.bg); c.drawCircle(100f, 100f, 96f, p)
        ticks(c, 60, 88f, 83f, blend(o.bg, o.ink, 0.55f), 1f)
        ticks(c, 12, 88f, 77f, o.accent, 2f)
        for (i in 0 until 12) {
            val ang = Math.toRadians(i * 30.0)
            centeredText(c, "%02d".format(i * 5), 100f + 66f * sin(ang).toFloat(), 100f - 66f * cos(ang).toFloat(), 11f, blend(o.bg, o.ink, 0.8f), Fonts.regular)
        }
        stroke(blend(o.bg, o.ink, 0.35f), 1f); c.drawCircle(100f, 136f, 16f, p)
        // subesfera: segundos en pequeño
        stroke(o.accent, 1.5f); c.save(); c.rotate(a.s, 100f, 136f); c.drawLine(100f, 136f, 100f, 123f, p); c.restore()
        hand(c, a.h, 42f, 4f, o.ink)
        hand(c, a.m, 64f, 3f, o.ink)
        if (o.showSeconds) hand(c, a.s, 82f, 1.2f, o.accent, 18f)
        fill(o.accent); c.drawCircle(100f, 100f, 3f, p)
    }

    private fun herramienta(c: Canvas, o: FaceOptions, a: Angles) {
        fill(blend(o.bg, Color.WHITE, 0.12f)); c.drawCircle(100f, 100f, 98f, p)
        fill(o.bg); c.drawCircle(100f, 100f, 78f, p)
        ticks(c, 60, 96f, 90f, blend(o.bg, Color.WHITE, 0.5f), 1f)
        ticks(c, 12, 96f, 84f, Color.WHITE, 3f)
        fill(o.accent)
        c.drawPath(android.graphics.Path().apply { moveTo(100f, 4f); lineTo(108f, 17f); lineTo(92f, 17f); close() }, p)
        fill(o.ink)
        for (i in 1 until 12) {
            if (i % 3 == 0) continue
            val ang = Math.toRadians(i * 30.0)
            c.drawCircle(100f + 66f * sin(ang).toFloat(), 100f - 66f * cos(ang).toFloat(), 5f, p)
        }
        c.drawRect(94f, 28f, 106f, 44f, p); c.drawRect(156f, 94f, 172f, 106f, p)
        c.drawRect(94f, 156f, 106f, 172f, p); c.drawRect(28f, 94f, 44f, 106f, p)
        hand(c, a.h, 40f, 10f, o.ink, 0f, Paint.Cap.BUTT)
        hand(c, a.m, 64f, 7f, o.ink, 0f, Paint.Cap.BUTT)
        if (o.showSeconds) hand(c, a.s, 68f, 2f, o.accent, 16f)
        fill(o.accent); c.drawCircle(100f, 100f, 5f, p)
    }

    private fun bloque(c: Canvas, o: FaceOptions, a: Angles, t: LocalDateTime, bg: Boolean) {
        if (bg) { fill(o.bg); c.drawRect(4f, 4f, 196f, 196f, p) }
        fill(o.accent); c.drawRect(4f, 4f, 196f, 104f, p)
        val hh = "%02d:%02d".format(hours(t, o.h24), t.minute)
        text(c, hh, 16f, 86f, 64f, o.ink, Fonts.heavy, Paint.Align.LEFT, -0.04f)
        val day = t.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES")).replaceFirstChar { it.uppercase() }
        val date = "${t.dayOfMonth} " + t.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        text(c, day, 16f, 142f, 14f, o.ink, Fonts.semi, Paint.Align.LEFT)
        text(c, date, 16f, 162f, 14f, blend(o.bg, o.ink, 0.55f), Fonts.regular, Paint.Align.LEFT)
        c.save(); c.translate(116f, 114f); c.scale(0.38f, 0.38f)
        stroke(o.ink, 5f); c.drawCircle(100f, 100f, 94f, p)
        hand(c, a.h, 46f, 10f, o.ink); hand(c, a.m, 72f, 7f, o.ink)
        if (o.showSeconds) hand(c, a.s, 80f, 3f, o.accent, 18f)
        c.restore()
    }

    // ---------- digitales ----------
    private val SEG = mapOf(
        '0' to "abcdef", '1' to "bc", '2' to "abged", '3' to "abgcd", '4' to "fgbc",
        '5' to "afgcd", '6' to "afgedc", '7' to "abc", '8' to "abcdefg", '9' to "abcfgd"
    )

    private fun segDigit(c: Canvas, d: Char, x: Float, y: Float, w: Float, h: Float, on: Int, off: Int, t: Float) {
        val lit = SEG[d] ?: ""
        val parts = mapOf(
            'a' to floatArrayOf(x + t, y, w - 2 * t, t),
            'b' to floatArrayOf(x + w - t, y + t, t, h / 2 - t),
            'c' to floatArrayOf(x + w - t, y + h / 2, t, h / 2 - t),
            'd' to floatArrayOf(x + t, y + h - t, w - 2 * t, t),
            'e' to floatArrayOf(x, y + h / 2, t, h / 2 - t),
            'f' to floatArrayOf(x, y + t, t, h / 2 - t),
            'g' to floatArrayOf(x + t, y + h / 2 - t / 2, w - 2 * t, t),
        )
        for ((k, v) in parts) {
            fill(if (k in lit) on else off)
            r.set(v[0], v[1], v[0] + v[2], v[1] + v[3])
            c.drawRoundRect(r, t / 3, t / 3, p)
        }
    }

    private fun segTime(c: Canvas, s: String, x: Float, y: Float, w: Float, h: Float, on: Int, off: Int, t: Float, gap: Float, colon: Boolean) {
        var cx = x
        for (i in s.indices) {
            segDigit(c, s[i], cx, y, w, h, on, off, t)
            cx += w + gap
            if (i == 1) {
                fill(if (colon) on else off)
                c.drawRect(cx, y + h * 0.28f, cx + t, y + h * 0.28f + t, p)
                c.drawRect(cx, y + h * 0.64f, cx + t, y + h * 0.64f + t, p)
                cx += t + gap
            }
        }
    }

    private fun lcd(c: Canvas, o: FaceOptions, t: LocalDateTime) {
        fill(Color.parseColor("#2B2B28")); r.set(4f, 36f, 196f, 164f); c.drawRoundRect(r, 10f, 10f, p)
        fill(o.bg); r.set(14f, 48f, 186f, 152f); c.drawRoundRect(r, 4f, 4f, p)
        val off = blend(o.bg, o.ink, 0.1f)
        segTime(c, digits(t, o), 26f, 66f, 30f, 64f, o.ink, off, 6f, 6f, !o.showSeconds || t.second % 2 == 0)
        if (!o.h24) text(c, if (t.hour < 12) "AM" else "PM", 176f, 146f, 11f, o.ink, Fonts.semi, Paint.Align.RIGHT)
        if (o.showSeconds) text(c, "%02d".format(t.second), 176f, 64f, 11f, o.ink, Fonts.semi, Paint.Align.RIGHT)
    }

    private fun persiana(c: Canvas, o: FaceOptions, t: LocalDateTime, bg: Boolean) {
        if (bg) { fill(o.bg); c.drawRect(4f, 4f, 196f, 196f, p) }
        val d = digits(t, o)
        for (i in 0..1) {
            val x = 10f + i * 92f
            fill(o.ink); r.set(x, 44f, x + 86f, 156f); c.drawRoundRect(r, 8f, 8f, p)
            centeredText(c, d.substring(i * 2, i * 2 + 2), x + 43f, 100f, 64f, o.accent, Fonts.heavy)
            fill(o.bg); c.drawRect(x, 99f, x + 86f, 101.5f, p)
        }
        if (o.showSeconds) text(c, "%02d".format(t.second), 192f, 176f, 13f, o.ink, Fonts.semi, Paint.Align.RIGHT)
    }

    private fun tubos(c: Canvas, o: FaceOptions, t: LocalDateTime) {
        fill(o.bg); r.set(4f, 36f, 196f, 164f); c.drawRoundRect(r, 10f, 10f, p)
        val d = digits(t, o)
        for (i in 0..3) {
            val x = 14f + i * 44f + if (i > 1) 6f else 0f
            fill(blend(o.bg, o.accent, 0.35f)); r.set(x, 50f, x + 38f, 150f); c.drawRoundRect(r, 18f, 18f, p)
            stroke(o.accent, 1f); c.drawRoundRect(r, 18f, 18f, p)
            // malla del tubo
            stroke(blend(o.bg, o.accent, 0.7f), 0.6f)
            var yy = 60f; while (yy < 142f) { c.drawLine(x + 6f, yy, x + 32f, yy, p); yy += 9f }
            centeredText(c, d[i].toString(), x + 19f, 100f, 54f, o.ink, Fonts.regular)
        }
        fill(o.ink); c.drawCircle(100f, 88f, 2.5f, p); c.drawCircle(100f, 112f, 2.5f, p)
    }

    private fun vfd(c: Canvas, o: FaceOptions, t: LocalDateTime) {
        fill(o.bg); r.set(4f, 46f, 196f, 154f); c.drawRoundRect(r, 8f, 8f, p)
        segTime(c, digits(t, o), 24f, 64f, 28f, 58f, o.ink, blend(o.bg, o.ink, 0.12f), 5f, 6f, t.second % 2 == 0)
        if (o.showSeconds) text(c, "%02d".format(t.second), 184f, 144f, 11f, o.accent, Fonts.semi, Paint.Align.RIGHT)
        if (!o.h24) text(c, if (t.hour < 12) "AM" else "PM", 20f, 144f, 11f, o.accent, Fonts.semi, Paint.Align.LEFT)
    }

    private val DOTS = mapOf(
        '0' to listOf("111", "101", "101", "101", "111"), '1' to listOf("010", "110", "010", "010", "111"),
        '2' to listOf("111", "001", "111", "100", "111"), '3' to listOf("111", "001", "111", "001", "111"),
        '4' to listOf("101", "101", "111", "001", "001"), '5' to listOf("111", "100", "111", "001", "111"),
        '6' to listOf("111", "100", "111", "101", "111"), '7' to listOf("111", "001", "010", "010", "010"),
        '8' to listOf("111", "101", "111", "101", "111"), '9' to listOf("111", "101", "111", "001", "111"),
    )

    private fun matriz(c: Canvas, o: FaceOptions, t: LocalDateTime) {
        fill(o.bg); r.set(4f, 40f, 196f, 160f); c.drawRoundRect(r, 10f, 10f, p)
        val d = digits(t, o)
        val step = 11f; var x0 = 22f
        val cols = listOf(d[0], d[1], ':', d[2], d[3])
        for (ch in cols) {
            val width = if (ch == ':') 1 else 3
            for (row in 0 until 7) for (col in 0 until width) {
                val on = if (ch == ':') (row == 2 || row == 4) && (!o.showSeconds || t.second % 2 == 0)
                else row in 1..5 && DOTS[ch]!![row - 1][col] == '1'
                fill(if (on) o.ink else o.accent)
                c.drawCircle(x0 + col * step, 67f + row * step, 4f, p)
            }
            x0 += if (ch == ':') 17f else 39f
        }
    }

    private fun tipografico(c: Canvas, o: FaceOptions, t: LocalDateTime, bg: Boolean) {
        if (bg) { fill(o.bg); c.drawRect(4f, 4f, 196f, 196f, p) }
        text(c, "%02d".format(hours(t, o.h24)), 12f, 94f, 98f, o.ink, Fonts.heavy, Paint.Align.LEFT, -0.06f)
        text(c, "%02d".format(t.minute), 12f, 186f, 98f, o.ink, Fonts.heavy, Paint.Align.LEFT, -0.06f)
        if (o.showSeconds) {
            fill(o.accent)
            val f = t.second / 60f
            r.set(158f, 20f, 186f, 48f); c.drawArc(r, -90f, f * 360f, true, p)
        } else { fill(o.accent); c.drawCircle(172f, 34f, 8f, p) }
    }

    /** La hora en palabras, redondeada a cinco minutos. */
    fun words(t: LocalDateTime): List<String> {
        val names = listOf("doce", "una", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "once")
        val m5 = ((t.minute + 2) / 5) * 5
        var h = t.hour % 12
        val (h2, tail) = when (m5) {
            0 -> h to "en punto"; 5 -> h to "y cinco"; 10 -> h to "y diez"; 15 -> h to "y cuarto"
            20 -> h to "y veinte"; 25 -> h to "y veinticinco"; 30 -> h to "y media"
            35 -> (h + 1) % 12 to "menos veinticinco"; 40 -> (h + 1) % 12 to "menos veinte"
            45 -> (h + 1) % 12 to "menos cuarto"; 50 -> (h + 1) % 12 to "menos diez"
            55 -> (h + 1) % 12 to "menos cinco"; else -> (h + 1) % 12 to "en punto"
        }
        h = h2
        val head = if (h == 1) "es la" else "son las"
        return listOf(head, names[h], tail)
    }

    private fun palabras(c: Canvas, o: FaceOptions, t: LocalDateTime, bg: Boolean) {
        if (bg) { fill(o.bg); c.drawRect(4f, 4f, 196f, 196f, p) }
        val w = words(t)
        text(c, w[0], 18f, 58f, 24f, blend(o.bg, o.ink, 0.55f), Fonts.semi, Paint.Align.LEFT)
        // ajusta el tamaño para que la palabra de la hora quepa entera
        p.typeface = Fonts.heavy; p.textSize = 52f
        val hs = min(52f, 52f * 164f / p.measureText(w[1]))
        text(c, w[1], 16f, 112f, hs, o.ink, Fonts.heavy, Paint.Align.LEFT, -0.02f)
        p.typeface = Fonts.semi; p.textSize = 24f
        val ts = min(24f, 24f * 164f / p.measureText(w[2]))
        text(c, w[2], 18f, 150f, ts, o.accent, Fonts.semi, Paint.Align.LEFT)
        if (o.showSeconds) { fill(o.accent); r.set(18f, 172f, 18f + 164f * t.second / 59f, 175f); c.drawRect(r, p) }
    }

    private fun binario(c: Canvas, o: FaceOptions, t: LocalDateTime, bg: Boolean) {
        if (bg) { fill(o.bg); c.drawRect(4f, 4f, 196f, 196f, p) }
        val hh = hours(t, o.h24)
        val vals = mutableListOf(hh / 10, hh % 10, t.minute / 10, t.minute % 10)
        val bits = mutableListOf(2, 4, 3, 4)
        if (o.showSeconds) { vals += listOf(t.second / 10, t.second % 10); bits += listOf(3, 4) }
        val n = vals.size
        val total = n * 24f + 12f * (n / 2 - 1)
        var x = 100f - total / 2f + 12f
        for (i in 0 until n) {
            for (b in 0 until bits[i]) {
                val on = (vals[i] shr b) and 1 == 1
                val cy = 160f - b * 30f
                if (on) { fill(o.accent); c.drawCircle(x, cy, 10f, p) }
                stroke(o.ink, 1.5f); c.drawCircle(x, cy, 10f, p)
            }
            x += 24f + if (i % 2 == 1) 12f else 0f
        }
    }
}
