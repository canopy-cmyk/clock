package com.canopy.clock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.canopy.clock.data.Store

fun fmtSw(ms: Long): String {
    val cs = (ms / 10) % 100; val s = (ms / 1000) % 60; val m = (ms / 60_000) % 60; val h = ms / 3_600_000
    return if (h > 0) "%d:%02d:%02d,%02d".format(h, m, s, cs) else "%02d:%02d,%02d".format(m, s, cs)
}

@Composable
fun StopwatchScreen() {
    storeVersion()
    val running = Store.swRunning
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running) { while (running) { withFrameMillis { }; now = System.currentTimeMillis() } }
    val elapsed = Store.swAccum + if (running) now - Store.swStart else 0
    val laps = Store.laps()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Header("Cronómetro")
        Box(Modifier.fillMaxWidth().padding(horizontal = 48.dp).aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                drawCircle(C.Card, r)
                for (i in 0 until 60) rotate(i * 6f) {
                    val long = i % 5 == 0
                    drawLine(if (long) C.Ink else C.Line, Offset(center.x, center.y - r + 8f), Offset(center.x, center.y - r + if (long) 30f else 18f), if (long) 4f else 2f)
                }
                // vuelta en curso: arco gris
                val lapStart = laps.lastOrNull() ?: 0L
                val lapSec = ((elapsed - lapStart) % 60_000) / 60_000f
                drawArc(C.Line, -90f, lapSec * 360f, false, Offset(center.x - r + 46f, center.y - r + 46f), androidx.compose.ui.geometry.Size(2 * r - 92f, 2 * r - 92f), style = Stroke(6f))
                val a = (elapsed % 60_000) / 60_000f * 360f
                rotate(a) {
                    drawLine(C.Orange, Offset(center.x, center.y + 24f), Offset(center.x, center.y - r + 14f), 5f, StrokeCap.Round)
                }
                drawCircle(C.Orange, 9f)
                drawCircle(C.Ink, 3f)
            }
        }
        Text(fmtSw(elapsed), style = T.huge, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            RoundButton(if (running) "Pausa" else if (elapsed > 0) "Seguir" else "Iniciar", C.Orange, 84.dp) {
                val t = System.currentTimeMillis()
                if (running) { Store.swAccum = Store.swAccum + (t - Store.swStart); Store.swRunning = false }
                else { Store.swStart = t; Store.swRunning = true }
            }
            RoundButton(if (running) "Vuelta" else "Reiniciar", C.Card, 84.dp) {
                if (running) Store.saveLaps(laps + elapsed)
                else { Store.swAccum = 0; Store.saveLaps(emptyList()) }
            }
        }
        if (laps.isNotEmpty()) {
            val splits = laps.mapIndexed { i, v -> v - (if (i == 0) 0 else laps[i - 1]) }
            val best = if (splits.size > 1) splits.min() else -1L
            val worst = if (splits.size > 1) splits.max() else -1L
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text("VUELTA", style = T.label, modifier = Modifier.weight(1f))
                    Text("PARCIAL", style = T.label, modifier = Modifier.weight(1f))
                    Text("TOTAL", style = T.label)
                }
                for (i in laps.indices.reversed()) {
                    val sp = splits[i]
                    val col = when (sp) { best -> C.Green; worst -> C.Red; else -> C.Ink }
                    HorizontalDivider(color = C.Line)
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text("${i + 1}", style = T.body.copy(color = col), modifier = Modifier.weight(1f))
                        Text(fmtSw(sp), style = T.body.copy(color = col), modifier = Modifier.weight(1f))
                        Text(fmtSw(laps[i]), style = T.body)
                    }
                }
            }
        }
        Gap(30.dp)
    }
}
