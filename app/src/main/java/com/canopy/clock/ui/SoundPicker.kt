package com.canopy.clock.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.canopy.clock.audio.Preview
import com.canopy.clock.audio.Sounds
import com.canopy.clock.data.ToneSettings

/** Barras que se mueven mientras suena la escucha previa. */
@Composable
fun SoundBars(active: Boolean) {
    val tr = rememberInfiniteTransition(label = "bars")
    val phases = (0 until 4).map { i ->
        tr.animateFloat(0.25f, 1f, infiniteRepeatable(tween(380 + i * 90, easing = LinearEasing), RepeatMode.Reverse), label = "b$i")
    }
    Canvas(Modifier.size(22.dp)) {
        val bw = size.width / 7
        for (i in 0 until 4) {
            val h = if (active) phases[i].value * size.height else size.height * 0.2f
            drawRoundRect(if (active) C.Orange else C.Line, Offset(i * bw * 2, size.height - h), Size(bw, h), CornerRadius(bw / 2))
        }
    }
}

/**
 * Lista de sonidos del sistema. Tocar uno lo elige y lo reproduce unos segundos;
 * tocarlo otra vez lo para.
 */
@Composable
fun SoundPicker(
    selectedUri: String,
    kinds: List<Sounds.Kind>,
    defaultLabel: String,
    fallbackDefault: android.net.Uri,
    volume: Float = 0.9f,
    onPick: (uri: String, title: String) -> Unit,
) {
    val ctx = LocalContext.current
    val playing by Preview.playing.collectAsState()
    var kind by remember { mutableStateOf(kinds.first()) }
    storeVersion()
    val sounds = Sounds.list(ctx, kind)
    DisposableEffect(Unit) { onDispose { Preview.stop() } }
    // navegador de archivos del sistema: cualquier audio del móvil o de una carpeta compartida
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val title = com.canopy.clock.audio.fileTitle(ctx, uri)
            com.canopy.clock.data.Store.addCustomSound(uri.toString(), title)
            kind = Sounds.Kind.FILES
            onPick(uri.toString(), title)
            Preview.toggle(ctx, uri.toString(), fallbackDefault, volume)
        }
    }

    Column {
        val allKinds = kinds + Sounds.Kind.FILES
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (k in allKinds) Chip(k.label, k == kind) { kind = k }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Pill("Elegir archivo…") { pickFile.launch(arrayOf("audio/*", "application/ogg")) }
        }
        val rows = if (kind == Sounds.Kind.FILES) sounds else listOf(Sounds.Sound(defaultLabel, "")) + sounds
        for ((i, snd) in rows.withIndex()) {
            if (i > 0) HorizontalDivider(color = C.Line, modifier = Modifier.padding(horizontal = 18.dp))
            val sel = snd.uri == selectedUri
            Row(
                Modifier.fillMaxWidth().clickable {
                    onPick(snd.uri, if (snd.uri.isBlank()) "" else snd.title)
                    Preview.toggle(ctx, snd.uri, fallbackDefault, volume)
                }.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { if (sel) Dot(C.Orange, 10.dp) }
                Text(snd.title, style = T.body.copy(fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal), modifier = Modifier.weight(1f).padding(start = 8.dp))
                SoundBars(playing == snd.uri)
                if (kind == Sounds.Kind.FILES) Text("Quitar", style = T.small.copy(color = C.Red, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable { com.canopy.clock.data.Store.removeCustomSound(snd.uri) }.padding(start = 12.dp))
            }
        }
        if (sounds.isEmpty()) Text(
            if (kind == Sounds.Kind.FILES) "Aún no has añadido archivos. Pulsa «Elegir archivo…» para usar cualquier audio del móvil."
            else "Este móvil no ofrece sonidos de este tipo.",
            style = T.small, modifier = Modifier.padding(18.dp)
        )
    }
}

/** Editor de sonido para alarmas y temporizadores. */
@Composable
fun ToneEditor(t: ToneSettings, onChange: (ToneSettings) -> Unit) {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val playing by Preview.playing.collectAsState()
    CardBox {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sonido", style = T.small)
                    Text(Sounds.title(ctx, t), style = T.head)
                }
                Text(if (open) "Cerrar" else "Cambiar", style = T.body.copy(fontWeight = FontWeight.SemiBold))
            }
            if (open) {
                HorizontalDivider(color = C.Line)
                SoundPicker(t.uri, listOf(Sounds.Kind.ALARM, Sounds.Kind.RINGTONE, Sounds.Kind.NOTIFICATION), "Alarma predeterminada", Sounds.defaultAlarm(), t.volume) { uri, title ->
                    onChange(t.copy(uri = uri, title = title, silent = false))
                }
                HorizontalDivider(color = C.Line)
            }
            SwitchRow("Solo vibración", "Sin sonido", t.silent) { onChange(t.copy(silent = it, vibrate = it || t.vibrate)) }
            if (!t.silent) {
                SliderRow("Subida de volumen", t.crescendoSec.toFloat(), 0f..120f, if (t.crescendoSec == 0) "directa" else "${t.crescendoSec} s") { onChange(t.copy(crescendoSec = it.toInt())) }
                SliderRow("Volumen", t.volume, 0.1f..1f, "${(t.volume * 100).toInt()} %") { onChange(t.copy(volume = it)) }
            }
            SwitchRow("Vibrar", null, t.vibrate) { onChange(t.copy(vibrate = it)) }
            if (!t.silent) Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(if (playing == t.uri) "Parar" else "Escuchar", filled = true, color = C.Orange) {
                    Preview.toggle(ctx, t.uri, Sounds.defaultAlarm(), t.volume)
                }
                HGap(12.dp)
                SoundBars(playing == t.uri)
            }
        }
    }
}
