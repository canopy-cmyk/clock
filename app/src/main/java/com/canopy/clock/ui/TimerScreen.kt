package com.canopy.clock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canopy.clock.alarm.TimerControl
import com.canopy.clock.alarm.TimerNotifier
import com.canopy.clock.data.RunningTimer
import com.canopy.clock.data.Store
import com.canopy.clock.data.TimerPreset
import kotlinx.coroutines.delay

private fun fmtPreset(s: Int): String {
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return listOfNotNull(if (h > 0) "$h h" else null, if (m > 0) "$m min" else null, if (sec > 0) "$sec s" else null).joinToString(" ").ifBlank { "0 s" }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen() {
    storeVersion()
    val ctx = LocalContext.current
    var entry by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    var editPresets by remember { mutableStateOf(false) }
    var showTone by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(200); now = System.currentTimeMillis() } }

    val padded = entry.padStart(6, '0')
    val eh = padded.substring(0, 2).toInt(); val em = padded.substring(2, 4).toInt(); val es = padded.substring(4, 6).toInt()
    val entrySec = eh * 3600 + em * 60 + es

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Temporizador")

        for (t in Store.timers()) RunningCard(t, now)

        // ---------- entrada tipo teclado ----------
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            for ((v, u) in listOf(padded.substring(0, 2) to "h", padded.substring(2, 4) to "m", padded.substring(4, 6) to "s")) {
                Text(v, style = T.huge.copy(color = if (entry.isEmpty()) C.Muted else C.Ink))
                Text(u, style = T.head.copy(color = C.Muted), modifier = Modifier.padding(start = 2.dp, end = 10.dp, bottom = 10.dp))
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
            val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("00", "0", "⌫"))
            for (row in keys) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (k in row) Box(
                    Modifier.weight(1f).aspectRatio(1.8f).padding(4.dp).clip(RoundedCornerShape(50)).background(C.Card).clickable {
                        entry = when (k) {
                            "⌫" -> entry.dropLast(1)
                            else -> (entry + k).trimStart('0').take(6)
                        }
                    }, contentAlignment = Alignment.Center
                ) { Text(k, style = T.head.copy(fontSize = 22.sp)) }
            }
        }
        OutlinedTextField(label, { label = it }, singleLine = true, placeholder = { Text("Nombre (opcional)") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            RoundButton("Iniciar", if (entrySec > 0) C.Orange else C.Line, 84.dp) {
                if (entrySec > 0) { TimerControl.start(ctx, entrySec, label); entry = ""; label = "" }
            }
            if (entrySec > 0) {
                HGap(16.dp)
                Pill("Guardar preset") {
                    Store.savePresets(Store.presets() + TimerPreset(label.ifBlank { fmtPreset(entrySec) }, entrySec)); entry = ""; label = ""
                }
            }
        }

        // ---------- presets ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Presets", Modifier.weight(1f))
            Text(if (editPresets) "Listo" else "Editar", style = T.small.copy(color = C.Ink, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.clickable { editPresets = !editPresets }.padding(horizontal = 20.dp, vertical = 8.dp))
        }
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (p in Store.presets()) {
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(if (editPresets) C.Red.copy(alpha = 0.12f) else C.Card).clickable {
                        if (editPresets) Store.savePresets(Store.presets() - p) else TimerControl.start(ctx, p.seconds, p.name)
                    }.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(p.name, style = T.body.copy(fontWeight = FontWeight.SemiBold))
                    Text(if (editPresets) "Quitar" else fmtPreset(p.seconds), style = T.small)
                }
            }
        }

        SectionLabel("Sonido del temporizador")
        if (showTone) ToneEditor(Store.timerTone) { Store.timerTone = it }
        else Box(Modifier.padding(horizontal = 16.dp)) { Pill(com.canopy.clock.audio.Sounds.title(ctx, Store.timerTone) + " · cambiar") { showTone = true } }
        Gap(30.dp)
    }
}

@Composable
private fun RunningCard(t: RunningTimer, now: Long) {
    val ctx = LocalContext.current
    val left = t.left(now)
    val frac = if (t.totalMs > 0) left.toFloat() / t.totalMs else 0f
    CardBox {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(92.dp)) {
                    drawCircle(C.Paper)
                    // disco que se va consumiendo
                    drawArc(if (t.running) C.Orange else C.Muted, -90f, 360f * frac, true, Offset.Zero, size)
                    drawCircle(C.Ink, size.minDimension / 2, style = Stroke(2f))
                }
            }
            HGap(16.dp)
            Column(Modifier.weight(1f)) {
                Text(t.label.ifBlank { "Temporizador" }, style = T.small)
                Text(TimerNotifier.fmt(left), style = T.big)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Chip(if (t.running) "Pausar" else "Seguir", t.running) { TimerControl.toggle(ctx, t.id) }
                    Chip("+1 min", false) { TimerControl.addMinute(ctx, t.id) }
                    Chip("×", false) { TimerControl.remove(ctx, t.id) }
                }
                if (!t.running && left != t.totalMs) Text("Reiniciar", style = T.small.copy(color = C.Ink),
                    modifier = Modifier.clickable { TimerControl.reset(ctx, t.id) }.padding(top = 6.dp))
            }
        }
    }
}

