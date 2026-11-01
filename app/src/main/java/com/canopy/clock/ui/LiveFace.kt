package com.canopy.clock.ui

import android.media.AudioAttributes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.canopy.clock.audio.TonePlayer
import com.canopy.clock.audio.ToneSynth
import com.canopy.clock.faces.FaceOptions
import com.canopy.clock.faces.FaceRenderer
import com.canopy.clock.faces.FaceSpec
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/** Dibuja un reloj en marcha. A 60 fps si hace falta movimiento suave; si no, una vez por segundo. */
@Composable
fun LiveFace(spec: FaceSpec, opts: FaceOptions, modifier: Modifier = Modifier, frozen: LocalDateTime? = null, sound: Boolean = false) {
    var now by remember { mutableStateOf(frozen ?: LocalDateTime.now()) }
    val smooth = (opts.sweep && opts.showSeconds) || spec.id in setOf("cubo", "radio", "televisor")
    if (frozen == null) {
        LaunchedEffect(spec.id, opts) {
            var lastSec = -1
            while (true) {
                if (smooth) {
                    withFrameNanos { }
                    now = LocalDateTime.now()
                } else {
                    val n = LocalDateTime.now()
                    now = n
                    delay(1000L - n.nano / 1_000_000 + 5)
                }
                if (sound && opts.tick && now.second != lastSec) {
                    if (lastSec != -1) TonePlayer.once(ToneSynth.click(), 0.6f, AudioAttributes.USAGE_MEDIA)
                    lastSec = now.second
                }
            }
        }
    }
    Canvas(modifier) {
        drawIntoCanvas { FaceRenderer.draw(it.nativeCanvas, size.width, size.height, spec, opts, frozen ?: now) }
    }
}
