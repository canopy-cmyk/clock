package com.canopy.clock.faces

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.canopy.clock.R
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
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
    val LCD = Color.parseColor("#B9BFA8")
    val SLATE = Color.parseColor("#1E1F1C")
    val OLIVE = Color.parseColor("#5E7A48")
    val TEAL = Color.parseColor("#2F7F86")
    val CREAM = Color.parseColor("#ECEAE3")
    val WALNUT = Color.parseColor("#5A3B26")
    val GRAPHITE = Color.parseColor("#2C2C2E")
    val TOMATO = Color.parseColor("#C9372C")
    val AMBER = Color.parseColor("#F29A3A")
    val MUSTARD = Color.parseColor("#E2A92B")

    /** Muestras que se ofrecen en el editor de cada reloj. */
    val SWATCHES = listOf(
        INK, SLATE, GRAPHITE, PAPER, CREAM, WHITE, ORANGE, AMBER, YELLOW, MUSTARD, RED, TOMATO,
        GREEN, OLIVE, TEAL, BLUE, LCD, WALNUT, Color.parseColor("#8C8A84"), Color.parseColor("#1A1714"),
    )
}

/** Opciones propias de cada reloj, editables por el usuario. */
data class FaceOptions(
    val bg: Int,                     // fondo o carcasa
    val ink: Int,                    // manecillas o dígitos
    val accent: Int,                 // segundero, cartelas o retroiluminación
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

/** Nombres de los tres colores según el reloj (fondo/carcasa, manecillas/dígitos, acento). */
class FaceSpec(
    val id: String,
    val name: String,
    val kind: FaceKind,
    val blurb: String,
    val defaults: FaceOptions,
    val colorLabels: List<String> = listOf("Fondo", "Manecillas", "Segundero"),
    val hasSeconds: Boolean = true,
    val isDevice: Boolean = false,
    val wide: Boolean = false,
)

object Faces {
    private val DEV = listOf("Carcasa", "Dígitos", "Cartelas")

    val ALL = listOf(
        FaceSpec("funcional", "Funcional", FaceKind.ANALOG, "Esfera oscura, numeración clara y segundero de color.",
            FaceOptions(Pal.INK, Pal.WHITE, Pal.ORANGE)),
        FaceSpec("instrumento", "Instrumento", FaceKind.ANALOG, "Escalas técnicas, como un cuadro de mandos.",
            FaceOptions(Pal.SLATE, Pal.PAPER, Pal.YELLOW, sweep = true)),
        FaceSpec("bloque", "Bloque", FaceKind.ANALOG, "Media pantalla de color sólido con la hora en grande.",
            FaceOptions(Pal.WHITE, Pal.INK, Pal.ORANGE), listOf("Fondo", "Texto", "Bloque")),
        FaceSpec("pedestal", "Pedestal", FaceKind.ANALOG, "Ojo de buey sobre una columna cromada.",
            FaceOptions(Pal.CREAM, Color.parseColor("#F4F2EC"), Pal.ORANGE), listOf("", "Manecillas", "Segundero"), isDevice = true),
        FaceSpec("madera", "Madera", FaceKind.ANALOG, "Reloj de sobremesa con marco de nogal.",
            FaceOptions(Pal.WALNUT, Color.parseColor("#2B2B2B"), Color.parseColor("#D9412A")),
            listOf("Madera", "Manecillas", "Segundero"), isDevice = true, wide = true),
        FaceSpec("despertador", "Despertador", FaceKind.ANALOG, "Despertador de viaje con números grandes luminosos.",
            FaceOptions(Pal.INK, Pal.INK, Color.parseColor("#1FB33A")), listOf("", "Manecillas", "Segundero"), isDevice = true),
        FaceSpec("mesilla", "Mesilla", FaceKind.ANALOG, "Caja naranja partida con esfera negra redonda.",
            FaceOptions(Pal.ORANGE, Color.parseColor("#F4F2EC"), Pal.ORANGE), listOf("", "Manecillas", "Segundero"), isDevice = true, wide = true),
        FaceSpec("estrella", "Estrella", FaceKind.ANALOG, "Esfera crema con estrella de rayos y marco verde.",
            FaceOptions(Pal.GREEN, Pal.INK, Color.parseColor("#D9412A")), listOf("", "Manecillas", "Segundero"), isDevice = true, wide = true),
        FaceSpec("lcd", "LCD", FaceKind.DIGITAL, "Siete segmentos con los apagados visibles.",
            FaceOptions(Pal.LCD, Color.parseColor("#262A22"), Color.parseColor("#2B2B28"), showSeconds = false),
            listOf("", "Segmentos", ""), isDevice = true, wide = true),
        FaceSpec("tubos", "Tubos", FaceKind.DIGITAL, "Dígitos de neón en tubos de cristal.",
            FaceOptions(Color.parseColor("#1A1714"), Pal.ORANGE, Pal.ORANGE, showSeconds = false),
            listOf("Fondo", "Neón", "Tubos"), hasSeconds = false, wide = true),
        FaceSpec("matriz", "Matriz", FaceKind.DIGITAL, "Rejilla de puntos LED.",
            FaceOptions(Color.parseColor("#140F0E"), Pal.RED, Color.parseColor("#3A2A26"), showSeconds = false),
            listOf("", "LED encendido", ""), isDevice = true, wide = true),
        FaceSpec("cubo", "Cubo", FaceKind.DIGITAL, "Cartelas que caen dentro de un cubo de color.",
            FaceOptions(Pal.OLIVE, Color.parseColor("#F4F2EC"), Color.parseColor("#272727"), showSeconds = false), listOf("", "Dígitos", "Cartelas"),
            hasSeconds = false, isDevice = true),
        FaceSpec("radio", "Radio", FaceKind.DIGITAL, "Radiodespertador con rejilla de altavoz.",
            FaceOptions(Pal.TEAL, Color.parseColor("#F4F2EC"), Color.parseColor("#2A2B29"), showSeconds = false), listOf("", "Dígitos", "Cartelas"),
            hasSeconds = false, isDevice = true, wide = true),
        FaceSpec("torre", "Torre", FaceKind.DIGITAL, "Cartelas LCD retroiluminadas, una encima de otra.",
            FaceOptions(Pal.GRAPHITE, Color.parseColor("#3A2410"), Pal.AMBER, showSeconds = false),
            listOf("", "Dígitos", ""), hasSeconds = false, isDevice = true),
        FaceSpec("televisor", "Televisor", FaceKind.DIGITAL, "Pantalla redondeada sobre patas, con cartelas.",
            FaceOptions(Pal.TOMATO, Color.parseColor("#F4F2EC"), Color.parseColor("#2A2A29"), showSeconds = false), listOf("", "Dígitos", "Cartelas"),
            hasSeconds = false, isDevice = true),
    )

    fun byId(id: String?): FaceSpec = ALL.firstOrNull { it.id == id } ?: ALL[0]
}

/** Tipografías compartidas. */
object Fonts {
    var regular: Typeface = Typeface.DEFAULT
    var semi: Typeface = Typeface.DEFAULT_BOLD
    var heavy: Typeface = Typeface.DEFAULT_BOLD
    var seg: Typeface = Typeface.MONOSPACE
    var dots: Typeface = Typeface.MONOSPACE
    var tube: Typeface = Typeface.DEFAULT
    private var loaded = false
    fun init(ctx: Context) {
        DeviceImages.init(ctx)
        if (loaded) return
        regular = ResourcesCompat.getFont(ctx, R.font.grotesk_400) ?: regular
        semi = ResourcesCompat.getFont(ctx, R.font.grotesk_600) ?: semi
        heavy = ResourcesCompat.getFont(ctx, R.font.grotesk_800) ?: heavy
        seg = ResourcesCompat.getFont(ctx, R.font.seg7) ?: seg
        dots = ResourcesCompat.getFont(ctx, R.font.dots) ?: dots
        tube = ResourcesCompat.getFont(ctx, R.font.tube) ?: tube
        loaded = true
    }
}

/**
 * Dibuja cualquier reloj en un lienzo de Android. Lo usan la app, los widgets,
 * el modo reloj y el salvapantallas, así que todos se ven idénticos.
 * Todo se dibuja en un espacio de 200x200 escalado al tamaño disponible.
 * Los aparatos usan colores planos: el volumen sale de dos tonos y una sombra, sin degradados.
 */
object FaceRenderer {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()
    private const val TNUM = "tnum"
    private val path = Path()

    // ---------- capa en vivo para widgets ----------
    /** Manecilla que moverá el sistema: tipo 0 hora, 1 minuto, 2 segundo. Todo en píxeles del bitmap. */
    class HandRec(val type: Int, val len: Float, val wd: Float, val color: Int, val tail: Float, val cx: Float, val cy: Float, val cap: Paint.Cap)

    /** Texto que pondrá el sistema (reloj de texto nativo). x/w en píxeles, baseline en píxeles. */
    class TextRec(val font: String, val pattern24: String, val pattern12: String, val sample: String, val left: Float, val width: Float,
                  val baseline: Float, val size: Float, val color: Int, val spacing: Float, val centered: Boolean)

    class Live { val hands = mutableListOf<HandRec>(); val texts = mutableListOf<TextRec>(); var usesOverlay = false }

    private var live: Live? = null
    private var ov: Canvas? = null
    private val mtx = android.graphics.Matrix()
    private val pts = FloatArray(2)

    private fun map(c: Canvas, x: Float, y: Float): FloatArray {
        @Suppress("DEPRECATION") c.getMatrix(mtx)
        pts[0] = x; pts[1] = y; mtx.mapPoints(pts); return floatArrayOf(pts[0], pts[1])
    }
    private fun mapLen(c: Canvas, v: Float): Float { @Suppress("DEPRECATION") c.getMatrix(mtx); return mtx.mapRadius(v) }

    private fun recText(c: Canvas, font: String, p24: String, p12: String, sample: String, x: Float, w: Float, baseline: Float,
                        size: Float, color: Int, spacing: Float, centered: Boolean): Boolean {
        val l = live ?: return false
        val a = map(c, x, baseline)
        l.texts += TextRec(font, p24, p12, sample, a[0], mapLen(c, w), a[1], mapLen(c, size), color, spacing, centered)
        return true
    }

    /**
     * Dibuja solo la parte fija de un reloj (carcasa, esfera, cartelas vacías) y devuelve
     * qué manecillas y qué textos debe animar el sistema encima. La capa [overlay] recibe
     * lo que va por encima de los números (sombra y eje de las cartelas).
     */
    fun drawStatic(c: Canvas, overlay: Canvas, w: Float, h: Float, spec: FaceSpec, o: FaceOptions): Live {
        val l = Live(); live = l; ov = overlay; cardIndex = 0
        try { draw(c, w, h, spec, o, LocalDateTime.now()) } finally { live = null; ov = null }
        return l
    }

    fun draw(c: Canvas, w: Float, h: Float, spec: FaceSpec, o: FaceOptions, t: LocalDateTime, fillBackground: Boolean = true) {
        // Zona útil del reloj en el espacio 200x200: los aparatos con foto se recortan a su contorno
        // (más un margen para la sombra) y así llenan el espacio disponible sin deformarse.
        val bb = DeviceImages.bounds(spec.id)
        val cl: Float; val ct: Float; val cw: Float; val ch: Float
        if (bb != null) {
            cl = PL + bb[0] * PB - 2f; ct = PT + bb[1] * PB - 2f
            cw = (bb[2] - bb[0]) * PB + 4f; ch = (bb[3] - bb[1]) * PB + 9f
        } else { cl = 0f; ct = 0f; cw = 200f; ch = 200f }
        val s = min(w / cw, h / ch)
        c.save()
        c.translate((w - cw * s) / 2f - cl * s, (h - ch * s) / 2f - ct * s)
        c.scale(s, s)
        ov?.let { it.save(); it.translate((w - cw * s) / 2f - cl * s, (h - ch * s) / 2f - ct * s); it.scale(s, s) }
        p.reset(); p.isAntiAlias = true; p.fontFeatureSettings = TNUM
        val nano = t.nano / 1_000_000_000f
        val sec = t.second + if (o.sweep) nano else 0f
        val min = t.minute + sec / 60f
        val hr = (t.hour % 12) + min / 60f
        // en modo estático los ángulos son marcas que [hand] reconoce para anotar la manecilla
        val a = if (live != null) Angles(-1000f, -2000f, -3000f) else Angles(hr * 30f, min * 6f, sec * 6f)
        // las cartelas caen durante el primer medio segundo de cada minuto
        val flip = if (live != null) 1f else if (t.second == 0) (nano / 0.5f).coerceIn(0f, 1f) else 1f
        when (spec.id) {
            "funcional" -> funcional(c, o, a)
            "instrumento" -> instrumento(c, o, a)
            "bloque" -> bloque(c, o, a, t, fillBackground)
            "pedestal" -> pedestal(c, o, a)
            "madera" -> madera(c, o, a)
            "despertador" -> despertador(c, o, a)
            "mesilla" -> photoAnalog(c, o, a, "mesilla", floatArrayOf(0.55f, 0.075f, 0.85f, 0.055f, 0.95f, 0.02f))
            "estrella" -> photoAnalog(c, o, a, "estrella", floatArrayOf(0.5f, 0.05f, 0.82f, 0.035f, 0.92f, 0.014f))
            "lcd" -> lcd(c, o, t)
            "tubos" -> tubos(c, o, t)
            "matriz" -> matriz(c, o, t)
            "cubo" -> cubo(c, o, t, flip)
            "radio" -> radio(c, o, t, flip)
            "torre" -> torre(c, o, t)
            "televisor" -> televisor(c, o, t, flip)
        }
        c.restore()
        ov?.restore()
    }

    private class Angles(val h: Float, val m: Float, val s: Float)

    // ---------- utilidades ----------
    private fun fill(color: Int) { p.style = Paint.Style.FILL; p.color = color; p.alpha = Color.alpha(color) }
    private fun fillA(color: Int, alpha: Float) { fill(color); p.alpha = (alpha * 255).toInt() }
    private fun stroke(color: Int, wd: Float, cap: Paint.Cap = Paint.Cap.ROUND) {
        p.style = Paint.Style.STROKE; p.color = color; p.strokeWidth = wd; p.strokeCap = cap
    }

    private fun rr(c: Canvas, l: Float, t: Float, rt: Float, b: Float, rad: Float) { r.set(l, t, rt, b); c.drawRoundRect(r, rad, rad, p) }

    private fun hand(c: Canvas, angle: Float, len: Float, wd: Float, color: Int, tail: Float = 0f, cx: Float = 100f, cy: Float = 100f, cap: Paint.Cap = Paint.Cap.ROUND) {
        val l = live
        if (l != null && angle <= -999f) {
            val type = when (angle) { -1000f -> 0; -2000f -> 1; else -> 2 }
            val pc = map(c, cx, cy)
            l.hands += HandRec(type, mapLen(c, len), mapLen(c, wd), color, mapLen(c, tail), pc[0], pc[1], cap)
            return
        }
        stroke(color, wd, cap)
        c.save(); c.rotate(angle, cx, cy)
        c.drawLine(cx, cy + tail, cx, cy - len, p)
        c.restore()
    }

    private fun ticks(c: Canvas, n: Int, r1: Float, r2: Float, color: Int, wd: Float, cx: Float = 100f, cy: Float = 100f) {
        stroke(color, wd, Paint.Cap.BUTT)
        for (i in 0 until n) {
            c.save(); c.rotate(i * 360f / n, cx, cy)
            c.drawLine(cx, cy - r1, cx, cy - r2, p)
            c.restore()
        }
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, tf: Typeface, align: Paint.Align = Paint.Align.CENTER, spacing: Float = 0f) {
        fill(color); p.typeface = tf; p.textSize = size; p.textAlign = align; p.letterSpacing = spacing
        c.drawText(s, x, y, p)
        p.letterSpacing = 0f
    }

    private fun centeredText(c: Canvas, s: String, x: Float, cy: Float, size: Float, color: Int, tf: Typeface, spacing: Float = 0f) {
        fill(color); p.typeface = tf; p.textSize = size; p.textAlign = Paint.Align.CENTER; p.letterSpacing = spacing
        val fm = p.fontMetrics
        c.drawText(s, x, cy - (fm.ascent + fm.descent) / 2f, p)
        p.letterSpacing = 0f
    }

    fun blend(a: Int, b: Int, f: Float): Int {
        val rr = (Color.red(a) * (1 - f) + Color.red(b) * f).toInt()
        val g = (Color.green(a) * (1 - f) + Color.green(b) * f).toInt()
        val bl = (Color.blue(a) * (1 - f) + Color.blue(b) * f).toInt()
        return Color.rgb(rr, g, bl)
    }

    private fun darker(c: Int, f: Float = 0.18f) = blend(c, Color.BLACK, f)
    private fun lighter(c: Int, f: Float = 0.14f) = blend(c, Color.WHITE, f)
    private fun isDark(c: Int) = (Color.red(c) * 0.299 + Color.green(c) * 0.587 + Color.blue(c) * 0.114) < 110

    private fun shadow(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float = 7f) {
        fillA(Color.BLACK, 0.13f); r.set(cx - rx, cy - ry, cx + rx, cy + ry); c.drawOval(r, p)
    }

    private fun hours(t: LocalDateTime, h24: Boolean): Int = if (h24) t.hour else ((t.hour + 11) % 12 + 1)
    private fun hh(t: LocalDateTime, o: FaceOptions) = if (o.h24) "%02d".format(t.hour) else hours(t, false).toString()
    private fun digits(t: LocalDateTime, o: FaceOptions): String = "%02d%02d".format(hours(t, o.h24), t.minute)

    // ---------- cartelas que caen ----------
    private fun cardHalf(c: Canvas, l: Float, tp: Float, w: Float, h: Float, txt: String, top: Boolean, card: Int, ink: Int, size: Float, rad: Float = 3.5f) {
        c.save()
        val mid = tp + h / 2
        if (top) c.clipRect(l, tp, l + w, mid) else c.clipRect(l, mid, l + w, tp + h)
        fill(card); rr(c, l, tp, l + w, tp + h, rad)
        if (live == null) {
            centeredText(c, txt, l + w / 2, mid + size * 0.02f, size, ink, Fonts.heavy, -0.03f)
            if (!top) { fillA(Color.BLACK, 0.16f); rr(c, l, tp, l + w, tp + h, rad) }
        } else if (!top) {
            ov?.let { o2 -> o2.save(); o2.clipRect(l, mid, l + w, tp + h); fillA(Color.BLACK, 0.16f); rr(o2, l, tp, l + w, tp + h, rad); o2.restore() }
        }
        c.restore()
    }

    private var cardIndex = 0

    private fun flipCard(c: Canvas, l: Float, tp: Float, w: Float, h: Float, now: String, old: String, prog: Float, card: Int, ink: Int, size: Float, rad: Float = 3.5f) {
        val mid = tp + h / 2
        val changing = prog < 1f && now != old
        cardHalf(c, l, tp, w, h, now, true, card, ink, size, rad)
        cardHalf(c, l, tp, w, h, if (changing) old else now, false, card, ink, size, rad)
        if (changing) {
            val k = cos(prog * PI).toFloat()
            c.save()
            if (k > 0) { c.scale(1f, k, 0f, mid); cardHalf(c, l, tp, w, h, old, true, card, ink, size, rad) }
            else { c.scale(1f, -k, 0f, mid); cardHalf(c, l, tp, w, h, now, false, card, ink, size, rad) }
            c.restore()
        }
        // eje de la cartela (en estático va en la capa superior, por encima de los números del sistema)
        val hc = if (live != null) ov ?: c else c
        if (live != null) live!!.usesOverlay = true
        fillA(Color.BLACK, 0.65f); hc.drawRect(l, mid - 0.8f, l + w, mid + 0.8f, p)
        fill(darker(card, 0.4f)); hc.drawRect(l - 1.5f, mid - 2.5f, l, mid + 2.5f, p); hc.drawRect(l + w, mid - 2.5f, l + w + 1.5f, mid + 2.5f, p)
        if (live != null) {
            p.typeface = Fonts.heavy; p.textSize = size; p.letterSpacing = -0.03f
            val fm = p.fontMetrics
            val base = mid + size * 0.02f - (fm.ascent + fm.descent) / 2f
            p.letterSpacing = 0f
            val p24 = if (cardIndex % 2 == 0) "HH" else "mm"
            val p12 = if (cardIndex % 2 == 0) "h" else "mm"
            recText(c, "heavy", p24, p12, "00", l, w, base, size, ink, -0.03f, true)
            cardIndex++
        }
    }

    private fun flipPair(c: Canvas, o: FaceOptions, t: LocalDateTime, prog: Float, x: Float, y: Float, w: Float, h: Float, gap: Float, size: Float) {
        val prev = t.minusMinutes(1)
        flipCard(c, x, y, w, h, hh(t, o), hh(prev, o), prog, o.accent, o.ink, size)
        flipCard(c, x + w + gap, y, w, h, "%02d".format(t.minute), "%02d".format(prev.minute), prog, o.accent, o.ink, size)
        if (!o.h24) text(c, if (t.hour < 12) "AM" else "PM", x + 3f, y + h - 4f, 6f, blend(o.accent, o.ink, 0.6f), Fonts.semi, Paint.Align.LEFT)
    }

    // ---------- analógicos ----------
    private fun funcional(c: Canvas, o: FaceOptions, a: Angles) {
        fill(o.bg); c.drawCircle(100f, 100f, 96f, p)
        ticks(c, 60, 86f, 81f, blend(o.bg, o.ink, 0.45f), 1f)
        ticks(c, 12, 86f, 76f, o.ink, 2f)
        for (i in listOf(12, 3, 6, 9)) {
            val ang = Math.toRadians(i * 30.0)
            centeredText(c, i.toString(), 100f + 62f * sin(ang).toFloat(), 100f - 62f * cos(ang).toFloat(), 18f, o.ink, Fonts.semi)
        }
        hand(c, a.h, 44f, 7f, o.ink, 10f)
        hand(c, a.m, 70f, 5f, o.ink, 10f)
        if (o.showSeconds) {
            hand(c, a.s, 78f, 2f, o.accent, 22f)
            if (live == null) { c.save(); c.rotate(a.s, 100f, 100f); fill(o.accent); c.drawCircle(100f, 120f, 5f, p); c.restore() }
        }
        fill(o.accent); c.drawCircle(100f, 100f, 5f, p)
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
        ticks(c, 12, 16f, 13f, blend(o.bg, o.ink, 0.35f), 0.8f, 100f, 136f)
        hand(c, a.s, 13f, 1.5f, o.accent, 0f, 100f, 136f)
        hand(c, a.h, 42f, 4f, o.ink)
        hand(c, a.m, 64f, 3f, o.ink)
        if (o.showSeconds) hand(c, a.s, 82f, 1.2f, o.accent, 18f)
        fill(o.accent); c.drawCircle(100f, 100f, 3f, p)
    }

    private fun bloque(c: Canvas, o: FaceOptions, a: Angles, t: LocalDateTime, bg: Boolean) {
        if (bg) { fill(o.bg); c.drawRect(4f, 4f, 196f, 196f, p) }
        fill(o.accent); c.drawRect(4f, 4f, 196f, 104f, p)
        if (live != null) {
            recText(c, "heavy", "HH:mm", "hh:mm", "00:00", 16f, 0f, 86f, 64f, o.ink, -0.04f, false)
            recText(c, "semi", "EEEE", "EEEE", "", 16f, 0f, 142f, 14f, o.ink, 0f, false)
            recText(c, "regular", "d MMMM", "d MMMM", "", 16f, 0f, 162f, 14f, blend(o.bg, o.ink, 0.55f), 0f, false)
        } else {
            text(c, "%02d:%02d".format(hours(t, o.h24), t.minute), 16f, 86f, 64f, o.ink, Fonts.heavy, Paint.Align.LEFT, -0.04f)
            val es = Locale("es", "ES")
            val day = t.dayOfWeek.getDisplayName(TextStyle.FULL, es).replaceFirstChar { it.uppercase() }
            val date = "${t.dayOfMonth} " + t.month.getDisplayName(TextStyle.FULL, es)
            text(c, day, 16f, 142f, 14f, o.ink, Fonts.semi, Paint.Align.LEFT)
            text(c, date, 16f, 162f, 14f, blend(o.bg, o.ink, 0.55f), Fonts.regular, Paint.Align.LEFT)
        }
        c.save(); c.translate(116f, 114f); c.scale(0.38f, 0.38f)
        stroke(o.ink, 5f); c.drawCircle(100f, 100f, 94f, p)
        hand(c, a.h, 46f, 10f, o.ink); hand(c, a.m, 72f, 7f, o.ink)
        if (o.showSeconds) hand(c, a.s, 80f, 3f, o.accent, 18f)
        c.restore()
    }

    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dstRect = RectF()

    /** Dibuja la fotografía del aparato en el recuadro de 184x184 y devuelve el centro y radio de la esfera. */
    private const val PB = 184f; private const val PL = 8f; private const val PT = 6f
    private fun px(v: Float) = PL + v * PB
    private fun py(v: Float) = PT + v * PB

    private fun photo(c: Canvas, img: DeviceImages.Img) {
        dstRect.set(PL, PT, PL + PB, PT + PB)
        c.drawBitmap(img.bitmap!!, null, dstRect, bmpPaint)
    }

    /** Reloj analógico con foto: manecillas proporcionales al radio medido de la esfera. */
    private fun photoAnalog(c: Canvas, o: FaceOptions, a: Angles, id: String, hw: FloatArray) {
        val img = DeviceImages.get(id) ?: run { funcional(c, o, a); return }
        val bb = DeviceImages.bounds(id)!!
        shadow(c, px((bb[0] + bb[2]) / 2), py(bb[3]) + 1f, (bb[2] - bb[0]) * PB * 0.42f, 4f)
        photo(c, img)
        val cx = px(img.v[0]); val cy = py(img.v[1]); val rr = img.v[2] * PB
        hand(c, a.h, rr * hw[0], rr * hw[1], o.ink, 0f, cx, cy)
        hand(c, a.m, rr * hw[2], rr * hw[3], o.ink, 0f, cx, cy)
        if (o.showSeconds) hand(c, a.s, rr * hw[4], rr * hw[5], o.accent, rr * 0.18f, cx, cy)
        if (img.v.size > 3) { fill(o.ink); c.drawCircle(cx, cy, img.v[3] * PB, p) }
        else { fill(o.accent); c.drawCircle(cx, cy, rr * 0.05f, p) }
    }

    private fun despertador(c: Canvas, o: FaceOptions, a: Angles) {
        val img = DeviceImages.get("despertador")
        if (img == null) { funcional(c, o, a); return }
        shadow(c, 100f, 182f, 72f, 4f)
        photo(c, img)
        val cx = px(img.v[0]); val cy = py(img.v[1]); val rr = img.v[2] * PB
        hand(c, a.h, rr * 0.5f, rr * 0.075f, o.ink, 0f, cx, cy)
        hand(c, a.m, rr * 0.8f, rr * 0.055f, o.ink, 0f, cx, cy)
        if (o.showSeconds) hand(c, a.s, rr * 0.9f, rr * 0.022f, o.accent, rr * 0.18f, cx, cy)
        fill(o.accent); c.drawCircle(cx, cy, rr * 0.05f, p)
    }

    private fun pedestal(c: Canvas, o: FaceOptions, a: Angles) {
        DeviceImages.get("pedestal")?.let { img ->
            shadow(c, 100f, 189f, 56f, 4f)
            photo(c, img)
            val cx = px(img.v[0]); val cy = py(img.v[1]); val rr = img.v[2] * PB
            hand(c, a.h, rr * 0.55f, rr * 0.085f, o.ink, 0f, cx, cy)
            hand(c, a.m, rr * 0.85f, rr * 0.06f, o.ink, 0f, cx, cy)
            if (o.showSeconds) hand(c, a.s, rr * 0.95f, rr * 0.025f, o.accent, rr * 0.2f, cx, cy)
            fill(o.accent); c.drawCircle(cx, cy, rr * 0.06f, p)
            return
        }
        val chrome = Color.parseColor("#C9C9C4"); val chromeHi = Color.parseColor("#EDEDE9"); val chromeLo = Color.parseColor("#9D9D98")
        shadow(c, 100f, 184f, 46f, 6f)
        // base y columna cromadas: tres tonos planos para dar reflejo
        fill(chromeLo); r.set(58f, 168f, 142f, 186f); c.drawOval(r, p)
        fill(chrome); r.set(60f, 166f, 140f, 182f); c.drawOval(r, p)
        fill(chromeHi); r.set(72f, 168f, 104f, 174f); c.drawOval(r, p)
        fill(chrome); c.drawRect(94f, 130f, 106f, 174f, p)
        fill(chromeHi); c.drawRect(96f, 130f, 99.5f, 174f, p)
        fill(chromeLo); c.drawRect(103.5f, 130f, 106f, 174f, p)
        // carcasa
        fill(darker(o.bg, 0.12f)); c.drawCircle(101.5f, 83.5f, 64f, p)
        fill(o.bg); c.drawCircle(100f, 82f, 63f, p)
        fill(lighter(o.bg, 0.35f)); r.set(52f, 30f, 148f, 134f); p.style = Paint.Style.STROKE; p.strokeWidth = 3f
        c.drawArc(r, 200f, 70f, false, p)
        // bisel cromado y esfera
        fill(chrome); c.drawCircle(100f, 82f, 50f, p)
        fill(chromeHi); r.set(50f, 32f, 150f, 132f); c.drawArc(r, 190f, 90f, true, p)
        val dial = if (isDark(o.ink)) Pal.CREAM else Pal.INK
        fill(dial); c.drawCircle(100f, 82f, 46f, p)
        ticks(c, 60, 43f, 40.5f, blend(dial, o.ink, 0.45f), 0.8f, 100f, 82f)
        ticks(c, 12, 43f, 35f, o.ink, 2f, 100f, 82f)
        hand(c, a.h, 22f, 4f, o.ink, 0f, 100f, 82f)
        hand(c, a.m, 34f, 3f, o.ink, 0f, 100f, 82f)
        if (o.showSeconds) hand(c, a.s, 39f, 1.3f, o.accent, 10f, 100f, 82f)
        fill(o.accent); c.drawCircle(100f, 82f, 3f, p)
        // reflejo del cristal
        fillA(Color.WHITE, 0.10f); r.set(58f, 40f, 132f, 100f); c.drawArc(r, 200f, 80f, true, p)
    }

    private fun madera(c: Canvas, o: FaceOptions, a: Angles) {
        shadow(c, 100f, 162f, 92f, 6f)
        val wood = o.bg
        fill(darker(wood, 0.3f)); c.drawRect(30f, 156f, 46f, 163f, p); c.drawRect(154f, 156f, 170f, 163f, p)
        fill(wood); rr(c, 12f, 50f, 188f, 158f, 4f)
        fill(lighter(wood, 0.12f)); c.drawRect(12f, 50f, 188f, 56f, p)
        fill(darker(wood, 0.15f)); c.drawRect(12f, 152f, 188f, 158f, p)
        // veta de la madera: líneas finas planas
        stroke(darker(wood, 0.14f), 0.7f)
        val ys = floatArrayOf(58f, 61f, 66f, 72f, 79f, 88f, 96f, 103f, 112f, 121f, 129f, 137f, 144f, 150f)
        for ((i, y) in ys.withIndex()) {
            path.reset(); path.moveTo(12f, y)
            path.cubicTo(60f, y + (i % 3 - 1) * 2.5f, 120f, y - (i % 2) * 2f, 188f, y + 1f)
            c.drawPath(path, p)
        }
        // esfera
        val cream = Color.parseColor("#E9DFC6")
        fill(darker(wood, 0.35f)); c.drawRect(23f, 61f, 177f, 147f, p)
        fill(cream); c.drawRect(25f, 63f, 175f, 145f, p)
        val cx = 100f; val cy = 104f
        stroke(Color.parseColor("#B9AC8A"), 0.6f)
        for (i in 0 until 12) {
            val ang = i * PI / 6
            c.drawLine(cx, cy, cx + 74f * sin(ang).toFloat(), cy - 40f * cos(ang).toFloat(), p)
        }
        fill(cream); r.set(cx - 22f, cy - 14f, cx + 22f, cy + 14f); c.drawOval(r, p)
        for (i in 0 until 12) {
            val ang = i * PI / 6
            val x = cx + 64f * sin(ang).toFloat(); val y = cy - 33f * cos(ang).toFloat()
            fill(Color.parseColor("#9B8357")); c.save(); c.rotate(i * 30f, x, y); c.drawRect(x - 3.5f, y - 1.6f, x + 3.5f, y + 1.6f, p); c.restore()
        }
        for (i in listOf(12, 3, 6, 9)) {
            val ang = Math.toRadians(i * 30.0)
            centeredText(c, i.toString(), cx + 50f * sin(ang).toFloat(), cy - 25f * cos(ang).toFloat(), 11f, o.ink, Fonts.heavy)
        }
        hand(c, a.h, 18f, 3.5f, o.ink, 0f, cx, cy)
        hand(c, a.m, 30f, 2.5f, o.ink, 0f, cx, cy)
        if (o.showSeconds) hand(c, a.s, 34f, 1.3f, o.accent, 8f, cx, cy)
        fill(o.accent); c.drawCircle(cx, cy, 2.5f, p)
        fillA(Color.WHITE, 0.12f)
        path.reset(); path.moveTo(25f, 63f); path.lineTo(70f, 63f); path.lineTo(40f, 145f); path.lineTo(25f, 145f); path.close()
        c.drawPath(path, p)
    }

    // ---------- digitales ----------
    private val SEG = mapOf(
        '0' to "abcdef", '1' to "bc", '2' to "abged", '3' to "abgcd", '4' to "fgbc",
        '5' to "afgcd", '6' to "afgedc", '7' to "abc", '8' to "abcdefg", '9' to "abcfgd", ' ' to ""
    )

    private fun segDigit(c: Canvas, d: Char, x: Float, y: Float, w: Float, h: Float, on: Int, off: Int, t: Float) {
        val lit = SEG[d] ?: ""
        val parts = arrayOf(
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
            rr(c, v[0], v[1], v[0] + v[2], v[1] + v[3], t / 3)
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
        DeviceImages.get("lcd")?.let { img ->
            shadow(c, 100f, 150f, 92f, 4f)
            photo(c, img)
            val v = img.v
            val l = px(v[0]); val tp = py(v[1]); val r = px(v[2]); val b = py(v[3])
            val h = (b - tp) * 0.68f; val w = h * 0.5f; val th = h * 0.1f; val gap = w * 0.28f; val cg = th * 3
            var x = (l + r) / 2 - (4 * w + 3 * gap + cg) / 2; val y = (tp + b) / 2 - h / 2
            val off = Color.argb(22, 0, 0, 0)
            val stat = live != null
            if (stat) recText(c, "seg", "HH:mm", "hh:mm", "00:00", x, 0f, y + h, h, o.ink, 0f, false)
            val d = if (stat) "8888" else digits(t, o)
            val colonOn = !stat && (!o.showSeconds || t.second % 2 == 0)
            for (i in 0..3) {
                segDigit(c, d[i], x, y, w, h, if (stat) off else o.ink, off, th)
                x += w + gap
                if (i == 1) {
                    fill(if (colonOn) o.ink else off)
                    val cx = x + cg / 2 - gap / 2 - th / 2
                    c.drawRect(cx, y + h * 0.28f, cx + th, y + h * 0.28f + th, p)
                    c.drawRect(cx, y + h * 0.64f, cx + th, y + h * 0.64f + th, p)
                    x += cg
                }
            }
            if (!stat && o.showSeconds) text(c, "%02d".format(t.second), r - 3f, tp + 7f, 5.5f, o.ink, Fonts.semi, Paint.Align.RIGHT)
            if (!stat && !o.h24) text(c, if (t.hour < 12) "AM" else "PM", r - 3f, b - 3f, 5f, o.ink, Fonts.semi, Paint.Align.RIGHT)
            return
        }
        shadow(c, 100f, 166f, 92f, 5f)
        fill(o.accent); rr(c, 4f, 36f, 196f, 164f, 10f)
        fill(lighter(o.accent, 0.12f)); rr(c, 4f, 36f, 196f, 42f, 3f)
        fill(darker(o.bg, 0.25f)); rr(c, 13f, 47f, 187f, 153f, 5f)
        fill(o.bg); rr(c, 14f, 48f, 186f, 152f, 4f)
        val off = blend(o.bg, o.ink, 0.1f)
        segTime(c, digits(t, o), 26f, 66f, 30f, 64f, o.ink, off, 6f, 6f, !o.showSeconds || t.second % 2 == 0)
        if (!o.h24) text(c, if (t.hour < 12) "AM" else "PM", 176f, 146f, 11f, o.ink, Fonts.semi, Paint.Align.RIGHT)
        if (o.showSeconds) text(c, "%02d".format(t.second), 176f, 62f, 11f, o.ink, Fonts.semi, Paint.Align.RIGHT)
    }

    private fun tubos(c: Canvas, o: FaceOptions, t: LocalDateTime) {
        shadow(c, 100f, 166f, 92f, 5f)
        fill(o.bg); rr(c, 4f, 36f, 196f, 164f, 10f)
        fill(lighter(o.bg, 0.08f)); rr(c, 4f, 36f, 196f, 42f, 3f)
        val d = digits(t, o)
        for (i in 0..3) {
            val x = 14f + i * 44f + if (i > 1) 6f else 0f
            fill(blend(o.bg, o.accent, 0.22f)); rr(c, x, 50f, x + 38f, 150f, 18f)
            stroke(blend(o.bg, o.accent, 0.6f), 1f); r.set(x, 50f, x + 38f, 150f); c.drawRoundRect(r, 18f, 18f, p)
            stroke(blend(o.bg, o.accent, 0.4f), 0.6f)
            var yy = 60f; while (yy < 142f) { c.drawLine(x + 6f, yy, x + 32f, yy, p); yy += 9f }
            // dígitos apagados detrás, como en un tubo real
            centeredText(c, "8", x + 19f, 100f, 54f, blend(o.bg, o.accent, 0.3f), Fonts.regular)
            if (live == null) centeredText(c, d[i].toString(), x + 19f, 100f, 54f, o.ink, Fonts.regular)
            fillA(Color.WHITE, 0.08f); rr(c, x + 5f, 54f, x + 11f, 140f, 3f)
        }
        fill(o.ink); c.drawCircle(100f, 88f, 2.5f, p); c.drawCircle(100f, 112f, 2.5f, p)
        if (live != null) {
            p.typeface = Fonts.tube; p.textSize = 54f
            val fm = p.fontMetrics
            val base = 100f - (fm.ascent + fm.descent) / 2f
            recText(c, "tube", "HH", "hh", "00", 11f, 88f, base, 54f, o.ink, 0f, true)
            recText(c, "tube", "mm", "mm", "00", 105f, 88f, base, 54f, o.ink, 0f, true)
        }
    }

    private val DOTS = mapOf(
        '0' to listOf("111", "101", "101", "101", "111"), '1' to listOf("010", "110", "010", "010", "111"),
        '2' to listOf("111", "001", "111", "100", "111"), '3' to listOf("111", "001", "111", "001", "111"),
        '4' to listOf("101", "101", "111", "001", "001"), '5' to listOf("111", "100", "111", "001", "111"),
        '6' to listOf("111", "100", "111", "101", "111"), '7' to listOf("111", "001", "010", "010", "010"),
        '8' to listOf("111", "101", "111", "101", "111"), '9' to listOf("111", "101", "111", "001", "111"),
    )

    private fun matriz(c: Canvas, o: FaceOptions, t: LocalDateTime) {
        DeviceImages.get("matriz")?.let { img ->
            shadow(c, 100f, 143f, 92f, 4f)
            photo(c, img)
            val v = img.v
            if (live != null) {
                val dy = v[3] * PB
                val size = 5 * dy
                recText(c, "dots", "HH:mm", "hh:mm", "00:00", px(v[0] + v[1]) - 0.231f * size, 0f, py(v[2]) + 4.5f * dy, size, o.ink, 0f, false)
                return
            }
            val d = digits(t, o)
            val lit = HashSet<Int>()
            var col = 1
            for (ch in listOf(d[0], d[1], ':', d[2], d[3])) {
                if (ch == ':') {
                    if (!o.showSeconds || t.second % 2 == 0) { lit += col * 10 + 1; lit += col * 10 + 3 }
                    col += 2; continue
                }
                val rows = DOTS[ch]!!
                for (row in 0 until 5) for (k in 0 until 3) if (rows[row][k] == '1') lit += (col + k) * 10 + row
                col += 4
            }
            val r = v[4] * PB
            for (key in lit) {
                val x = px(v[0] + (key / 10) * v[1]); val y = py(v[2] + (key % 10) * v[3])
                fillA(o.ink, 0.24f); c.drawCircle(x, y, r * 2f, p)
                fill(o.ink); c.drawCircle(x, y, r, p)
                fill(lighter(o.ink, 0.55f)); c.drawCircle(x, y, r * 0.4f, p)
            }
            return
        }
        shadow(c, 100f, 162f, 92f, 5f)
        fill(o.bg); rr(c, 4f, 40f, 196f, 160f, 10f)
        val d = digits(t, o)
        val step = 11f; var x0 = 22f
        for (ch in listOf(d[0], d[1], ':', d[2], d[3])) {
            val width = if (ch == ':') 1 else 3
            for (row in 0 until 7) for (col in 0 until width) {
                val on = if (ch == ':') (row == 2 || row == 4) && (!o.showSeconds || t.second % 2 == 0)
                else row in 1..5 && DOTS[ch]!![row - 1][col] == '1'
                fill(if (on) o.ink else o.accent)
                c.drawCircle(x0 + col * step, 67f + row * step, 4f, p)
            }
            x0 += if (ch == ':') 17f else 39f
        }
        if (o.showSeconds) {
            for (i in 0 until 12) { fill(if (i <= t.second / 5) o.ink else o.accent); c.drawCircle(37f + i * 11.5f, 150f, 1.8f, p) }
        }
    }

    /** Aparato fotográfico con dos cartelas: la foto hace de carcasa y las cartelas se dibujan encima. */
    private fun photoFlip(c: Canvas, o: FaceOptions, t: LocalDateTime, flip: Float, id: String, shadowY: Float, shadowRx: Float): Boolean {
        val img = DeviceImages.get(id) ?: return false
        shadow(c, 100f, shadowY, shadowRx, 4f)
        photo(c, img)
        val v = img.v
        val top = py(v[1]); val h = (v[3] - v[1]) * PB
        val rad = v[6] * PB
        val prev = t.minusMinutes(1)
        val size = h * 0.66f
        flipCard(c, px(v[0]), top, (v[2] - v[0]) * PB, h, hh(t, o), hh(prev, o), flip, o.accent, o.ink, size, rad)
        flipCard(c, px(v[4]), top, (v[5] - v[4]) * PB, h, "%02d".format(t.minute), "%02d".format(prev.minute), flip, o.accent, o.ink, size, rad)
        if (live == null && !o.h24) text(c, if (t.hour < 12) "AM" else "PM", px(v[0]) + 2f, top + h - 2f, 4.5f, blend(o.accent, o.ink, 0.6f), Fonts.semi, Paint.Align.LEFT)
        return true
    }

    private fun cubo(c: Canvas, o: FaceOptions, t: LocalDateTime, flip: Float) {
        if (photoFlip(c, o, t, flip, "cubo", 183f, 74f)) return
        shadow(c, 100f, 170f, 72f)
        fill(darker(o.bg, 0.25f)); rr(c, 64f, 42f, 90f, 54f, 3f)
        fill(o.bg); rr(c, 38f, 50f, 162f, 168f, 18f)
        fill(darker(o.bg, 0.16f)); rr(c, 146f, 58f, 162f, 162f, 8f)
        fill(lighter(o.bg, 0.16f)); rr(c, 46f, 50f, 154f, 64f, 7f)
        // marco cromado de la ventana
        fill(Color.parseColor("#C9C9C4")); rr(c, 49f, 77f, 143f, 147f, 10f)
        fill(Color.parseColor("#222320")); rr(c, 52f, 80f, 140f, 144f, 8f)
        flipPair(c, o, t, flip, 58f, 86f, 36f, 52f, 4f, 30f)
        fillA(Color.WHITE, 0.07f); path.reset(); path.moveTo(52f, 80f); path.lineTo(90f, 80f); path.lineTo(70f, 144f); path.lineTo(52f, 144f); path.close(); c.drawPath(path, p)
    }

    private fun radio(c: Canvas, o: FaceOptions, t: LocalDateTime, flip: Float) {
        if (photoFlip(c, o, t, flip, "radio", 143f, 88f)) return
        shadow(c, 100f, 162f, 90f)
        val metal = Color.parseColor("#D8D6CF")
        fill(darker(metal, 0.2f)); c.drawCircle(58f, 76f, 8f, p)
        fill(metal); c.drawCircle(58f, 75f, 7f, p)
        fill(darker(metal, 0.2f)); rr(c, 128f, 70f, 158f, 80f, 3f)
        fill(metal); rr(c, 129f, 69f, 157f, 78f, 3f)
        fill(o.bg); rr(c, 14f, 78f, 186f, 160f, 10f)
        fill(lighter(o.bg, 0.14f)); rr(c, 14f, 78f, 186f, 89f, 5f)
        fill(darker(o.bg, 0.14f)); rr(c, 14f, 152f, 186f, 160f, 5f)
        // rejilla del altavoz
        for (i in 0 until 7) { fill(darker(o.bg, 0.28f)); rr(c, 26f + i * 7f, 98f, 29.5f + i * 7f, 146f, 1.7f) }
        fill(Color.parseColor("#C9C9C4")); rr(c, 80f, 94f, 178f, 150f, 7f)
        fill(Color.parseColor("#1E2021")); rr(c, 82f, 96f, 176f, 148f, 6f)
        flipPair(c, o, t, flip, 88f, 101f, 40f, 42f, 4f, 26f)
        fillA(Color.WHITE, 0.06f); path.reset(); path.moveTo(82f, 96f); path.lineTo(116f, 96f); path.lineTo(100f, 148f); path.lineTo(82f, 148f); path.close(); c.drawPath(path, p)
    }

    private fun torre(c: Canvas, o: FaceOptions, t: LocalDateTime) {
        DeviceImages.get("torre")?.let { img ->
            shadow(c, 100f, 186f, 58f, 4f)
            photo(c, img)
            val v = img.v
            val l = px(v[0]); val r = px(v[1])
            val stat = live != null
            for ((i, txt) in listOf(hh(t, o).padStart(2, ' '), "%02d".format(t.minute)).withIndex()) {
                val top = py(v[2 + i * 2]); val bot = py(v[3 + i * 2])
                val h = (bot - top) * 0.66f; val w = h * 0.5f; val th = h * 0.1f; val gap = h * 0.35f
                val x = (l + r) / 2 - (2 * w + gap) / 2; val y = (top + bot) / 2 - h / 2
                val off = Color.argb(18, 0, 0, 0)
                if (stat) {
                    recText(c, "segw", if (i == 0) "HH" else "mm", if (i == 0) "hh" else "mm", "00", x, 0f, y + h, h, o.ink, 0.21f, false)
                    segDigit(c, '8', x, y, w, h, off, off, th); segDigit(c, '8', x + w + gap, y, w, h, off, off, th)
                } else {
                    segDigit(c, txt[0], x, y, w, h, o.ink, off, th)
                    segDigit(c, txt[1], x + w + gap, y, w, h, o.ink, off, th)
                }
            }
            if (!stat && !o.h24) text(c, if (t.hour < 12) "am" else "pm", r - 2f, py(v[3]) - 2f, 5f, o.ink, Fonts.semi, Paint.Align.RIGHT)
            return
        }
        shadow(c, 100f, 188f, 48f, 5f)
        fill(darker(o.bg, 0.3f)); rr(c, 88f, 14f, 112f, 24f, 3f)
        fill(o.bg); rr(c, 56f, 20f, 144f, 186f, 15f)
        fill(lighter(o.bg, 0.1f)); rr(c, 62f, 20f, 138f, 30f, 5f)
        fill(darker(o.bg, 0.15f)); rr(c, 134f, 30f, 144f, 176f, 5f)
        val panel = Color.parseColor("#EDE9DF")
        fill(panel); rr(c, 66f, 34f, 134f, 174f, 8f)
        val off = blend(o.accent, Color.BLACK, 0.12f)
        for ((i, txt) in listOf(hh(t, o).padStart(2, ' '), "%02d".format(t.minute)).withIndex()) {
            val y = 44f + i * 64f
            fill(o.accent); rr(c, 74f, y, 126f, y + 58f, 4f)
            segDigit(c, txt[0], 80f, y + 10f, 18f, 38f, o.ink, off, 3.6f)
            segDigit(c, txt[1], 102f, y + 10f, 18f, 38f, o.ink, off, 3.6f)
            fillA(Color.BLACK, 0.35f); c.drawRect(74f, y + 28.6f, 126f, y + 29.6f, p)
        }
        if (!o.h24) text(c, if (t.hour < 12) "am" else "pm", 70f, 171f, 7f, Color.parseColor("#77736B"), Fonts.semi, Paint.Align.LEFT)
    }

    private fun televisor(c: Canvas, o: FaceOptions, t: LocalDateTime, flip: Float) {
        if (photoFlip(c, o, t, flip, "televisor", 179f, 78f)) return
        shadow(c, 100f, 176f, 72f)
        fill(Color.parseColor("#2A2A2A"))
        c.save(); c.rotate(10f, 47f, 150f); c.drawRect(44f, 150f, 50f, 174f, p); c.restore()
        c.save(); c.rotate(-10f, 153f, 150f); c.drawRect(150f, 150f, 156f, 174f, p); c.restore()
        fill(darker(o.bg, 0.25f)); c.drawRect(96f, 34f, 104f, 46f, p)
        fill(darker(o.bg, 0.15f)); rr(c, 28f, 46f, 174f, 158f, 36f)
        fill(o.bg); rr(c, 26f, 42f, 172f, 154f, 36f)
        fill(lighter(o.bg, 0.14f)); rr(c, 44f, 42f, 154f, 54f, 6f)
        fill(darker(o.bg, 0.28f)); rr(c, 42f, 58f, 158f, 140f, 24f)
        fill(Color.parseColor("#1C1D1E")); rr(c, 44f, 60f, 156f, 138f, 22f)
        flipPair(c, o, t, flip, 56f, 73f, 42f, 52f, 4f, 32f)
        fillA(Color.WHITE, 0.07f); path.reset(); path.moveTo(50f, 64f); path.lineTo(96f, 64f); path.lineTo(72f, 134f); path.lineTo(50f, 134f); path.close(); c.drawPath(path, p)
    }
}
