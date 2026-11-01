package com.canopy.clock.faces

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.canopy.clock.R

/**
 * Fotografías de los aparatos (sin hora). La hora se dibuja encima con el código,
 * en la posición medida de la esfera o la ventana de cada imagen.
 * Coordenadas normalizadas 0–1 respecto a la imagen cuadrada.
 */
object DeviceImages {
    /** v: medidas propias de cada aparato (centro y radio de la esfera, o rectángulos de las cartelas). */
    class Img(val resId: Int, vararg val v: Float) {
        var bitmap: Bitmap? = null
    }

    val ALL: Map<String, Img> = mapOf(
        "pedestal" to Img(R.drawable.img_pedestal, 0.4960f, 0.3329f, 0.2177f),
        "despertador" to Img(R.drawable.img_despertador, 0.4976f, 0.5343f, 0.2871f),
        "mesilla" to Img(R.drawable.img_mesilla, 627f / 1254, 575f / 1254, 280f / 1254),
        "estrella" to Img(R.drawable.img_estrella, 628f / 1254, 613f / 1254, 330f / 1254, 33f / 1254),
        // cartela izquierda (l, t, r, b), borde izq. y der. de la derecha, radio de esquina
        "cubo" to Img(R.drawable.img_cubo, 0.1754f, 0.3708f, 0.4825f, 0.6730f, 0.5175f, 0.8230f, 0.0191f),
        "radio" to Img(R.drawable.img_radio, 0.4099f, 0.4665f, 0.6180f, 0.6332f, 0.6587f, 0.8668f, 0.0112f),
        "televisor" to Img(R.drawable.img_televisor, 0.1898f, 0.3581f, 0.4769f, 0.6467f, 0.5207f, 0.8070f, 0.0175f),
        // pantalla LCD: l, t, r, b
        "lcd" to Img(R.drawable.img_lcd, 0.0973f, 0.4035f, 0.9019f, 0.6268f),
        // rejilla LED 19x5: x0, paso x, y0, paso y, radio
        "matriz" to Img(R.drawable.img_matriz, 0.1252f, 0.0419f, 0.4569f, 0.0363f, 0.0072f),
        // ventanas LCD: izquierda, derecha, y arriba/abajo de cada una
        "torre" to Img(R.drawable.img_torre, 0.2951f, 0.6986f, 0.2711f, 0.4617f, 0.5335f, 0.7265f),
    )

    /** Contorno real del aparato dentro de la foto (píxeles sobre 1254), para que llene el widget. */
    private val BBOX = mapOf(
        "pedestal" to intArrayOf(227, 22, 1053, 1236), "despertador" to intArrayOf(97, 64, 1150, 1190),
        "cubo" to intArrayOf(58, 75, 1193, 1200), "radio" to intArrayOf(15, 381, 1238, 934),
        "televisor" to intArrayOf(37, 118, 1211, 1180), "torre" to intArrayOf(266, 24, 987, 1231),
        "lcd" to intArrayOf(8, 335, 1245, 935), "matriz" to intArrayOf(9, 412, 1247, 894),
        "mesilla" to intArrayOf(50, 223, 1254, 1039), "estrella" to intArrayOf(21, 181, 1230, 1072),
    )

    /** Contorno normalizado 0–1 (l, t, r, b) o null si el reloj no usa foto. */
    fun bounds(id: String): FloatArray? {
        if (get(id) == null) return null
        val b = BBOX[id] ?: return null
        return floatArrayOf(b[0] / 1254f, b[1] / 1254f, b[2] / 1254f, b[3] / 1254f)
    }

    fun init(ctx: Context) {
        for (img in ALL.values) if (img.bitmap == null) {
            img.bitmap = runCatching {
                BitmapFactory.decodeResource(ctx.resources, img.resId, BitmapFactory.Options().apply { inScaled = false })
            }.getOrNull()
        }
    }

    fun get(id: String): Img? = ALL[id]?.takeIf { it.bitmap != null }
}
