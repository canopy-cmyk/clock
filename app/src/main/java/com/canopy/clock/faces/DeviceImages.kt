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
        // cartela izquierda (l, t, r, b), borde izq. y der. de la derecha, radio de esquina
        "cubo" to Img(R.drawable.img_cubo, 0.1754f, 0.3708f, 0.4825f, 0.6730f, 0.5175f, 0.8230f, 0.0191f),
    )

    fun init(ctx: Context) {
        for (img in ALL.values) if (img.bitmap == null) {
            img.bitmap = runCatching {
                BitmapFactory.decodeResource(ctx.resources, img.resId, BitmapFactory.Options().apply { inScaled = false })
            }.getOrNull()
        }
    }

    fun get(id: String): Img? = ALL[id]?.takeIf { it.bitmap != null }
}
