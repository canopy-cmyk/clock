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
    class Img(val resId: Int, val cx: Float, val cy: Float, val r: Float) {
        var bitmap: Bitmap? = null
    }

    val ALL: Map<String, Img> = mapOf(
        "pedestal" to Img(R.drawable.img_pedestal, 0.4960f, 0.3329f, 0.2177f),
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
