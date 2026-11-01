package com.canopy.clock.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canopy.clock.alarm.IntervalService
import com.canopy.clock.alarm.TimerNotifier
import com.canopy.clock.data.IntervalProgram
import com.canopy.clock.data.IntervalStep
import com.canopy.clock.data.Store
import kotlinx.coroutines.delay

private val STEP_COLORS = listOf(0xFFF08A1C, 0xFF3E9B5E, 0xFFE4472B, 0xFF2E5FA8, 0xFFF4D40B, 0xFF8C8A84, 0xFF151515).map { it.toInt() }

private fun mmss(s: Int) = if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60) else "%d:%02d".format(s / 60, s % 60)

@Composable
fun IntervalsScreen() {
    storeVersion()
    val ctx = LocalContext.current
    val run by IntervalService.state.collectAsState()
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    BackHandler(enabled = editing != null) { editing = null }

    run?.let { RunView(it); return }
    editing?.let { ProgramEditor(it) { editing = null }; return }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Intervalos", "Nuevo") {
            val id = (Store.programs().maxOfOrNull { it.id } ?: 0) + 1
            Store.saveProgram(IntervalProgram(id, "Nuevo programa", 0, listOf(IntervalStep("Trabajo", 40, STEP_COLORS[0]), IntervalStep("Descanso", 20, STEP_COLORS[1])), 8, 0))
            editing = id
        }
        Text("Secuencias de trabajo y descanso con aviso sonoro en cada cambio. Siguen sonando con la pantalla apagada.",
            style = T.small, modifier = Modifier.padding(horizontal = 20.dp))
        for (p in Store.programs()) {
            CardBox {
                Column(Modifier.padding(16.dp)) {
                    Text(p.name, style = T.head)
                    Text("${p.rounds} × (" + p.steps.joinToString(" · ") { "${it.name} ${mmss(it.seconds)}" } + ")", style = T.small)
                    Text("Total ${mmss(p.totalSec)}", style = T.small)
                    // vista previa de la secuencia como barra de colores
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(10.dp).clip(RoundedCornerShape(5.dp))) {
                        for (ph in IntervalService.build(p)) Box(Modifier.weight(ph.seconds.toFloat().coerceAtLeast(1f)).fillMaxSize().background(Color(ph.color)))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Empezar", filled = true, color = C.Orange) { IntervalService.start(ctx, p.id) }
                        Pill("Editar") { editing = p.id }
                    }
                }
            }
        }
        Gap(30.dp)
    }
}

@Composable
private fun RunView(s: IntervalService.RunState) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(100); now = System.currentTimeMillis() } }
    val ph = s.current ?: return
    val left = s.left(now)
    val frac = (left / 1000f / ph.seconds).coerceIn(0f, 1f)
    val color = Color(ph.color)
    val next = s.phases.getOrNull(s.index + 1)
    val doneSec = s.phases.take(s.index).sumOf { it.seconds } + (ph.seconds - left / 1000f)
    val totalSec = s.phases.sumOf { it.seconds }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Header(s.program.name)
        Text(if (s.finished) "Terminado" else ph.name, style = T.huge.copy(fontSize = 52.sp))
        if (ph.round > 0) Text("Ronda ${ph.round} de ${s.program.rounds}", style = T.body)
        Box(Modifier.fillMaxWidth().padding(28.dp).aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(C.Card)
                drawArc(color, -90f, 360f * frac, true, Offset.Zero, size)
                drawCircle(C.Ink, size.minDimension / 2, style = Stroke(3f))
            }
            Box(Modifier.size(180.dp).clip(CircleShape).background(C.Paper), contentAlignment = Alignment.Center) {
                Text(TimerNotifier.fmt(left), style = T.huge)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(C.Line)) {
            Box(Modifier.weight((doneSec / totalSec).coerceIn(0.001f, 1f)).fillMaxSize().background(C.Ink))
            if (doneSec < totalSec) Box(Modifier.weight((1f - doneSec / totalSec).coerceIn(0.001f, 1f)).fillMaxSize())
        }
        next?.let { Text("Después: ${it.name} · ${mmss(it.seconds)}", style = T.body, modifier = Modifier.padding(12.dp)) }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(if (s.paused) "Seguir" else "Pausa", C.Orange) { IntervalService.toggle(ctx) }
            RoundButton("Saltar", C.Card) { IntervalService.skip(ctx) }
            RoundButton("Fin", C.Ink, textColor = C.Paper) { IntervalService.stop(ctx) }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, step: Int, min: Int, display: (Int) -> String, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.body, modifier = Modifier.weight(1f))
        Box(Modifier.size(36.dp).clip(CircleShape).background(C.Paper).clickable { onChange((value - step).coerceAtLeast(min)) }, contentAlignment = Alignment.Center) { Text("−", style = T.head) }
        Text(display(value), style = T.head, modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.size(36.dp).clip(CircleShape).background(C.Paper).clickable { onChange(value + step) }, contentAlignment = Alignment.Center) { Text("+", style = T.head) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgramEditor(id: Int, onDone: () -> Unit) {
    val orig = Store.programs().firstOrNull { it.id == id } ?: run { LaunchedEffect(Unit) { onDone() }; return }
    var p by remember(id) { mutableStateOf(orig) }
    fun secStep(v: Int) = if (v >= 300) 60 else if (v >= 60) 15 else 5

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Programa", "Guardar") { Store.saveProgram(p); onDone() }
        OutlinedTextField(p.name, { p = p.copy(name = it) }, singleLine = true, label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        SectionLabel("Estructura")
        CardBox {
            Column(Modifier.padding(vertical = 6.dp)) {
                Stepper("Calentamiento", p.warmup, secStep(p.warmup), 0, ::mmss) { p = p.copy(warmup = it) }
                Stepper("Rondas", p.rounds, 1, 1, { it.toString() }) { p = p.copy(rounds = it) }
                Stepper("Vuelta a la calma", p.cooldown, secStep(p.cooldown), 0, ::mmss) { p = p.copy(cooldown = it) }
            }
        }
        SectionLabel("Fases de cada ronda")
        p.steps.forEachIndexed { i, st ->
            CardBox {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Color(st.color)).clickable {
                            val next = STEP_COLORS[(STEP_COLORS.indexOf(st.color) + 1).mod(STEP_COLORS.size)]
                            p = p.copy(steps = p.steps.toMutableList().also { it[i] = st.copy(color = next) })
                        })
                        HGap(10.dp)
                        OutlinedTextField(st.name, { v -> p = p.copy(steps = p.steps.toMutableList().also { it[i] = st.copy(name = v) }) },
                            singleLine = true, modifier = Modifier.weight(1f))
                        if (p.steps.size > 1) Text("Quitar", style = T.small.copy(color = C.Red, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.clickable { p = p.copy(steps = p.steps.filterIndexed { j, _ -> j != i }) }.padding(10.dp))
                    }
                    Stepper("Duración", st.seconds, secStep(st.seconds), 5, ::mmss) { v ->
                        p = p.copy(steps = p.steps.toMutableList().also { it[i] = st.copy(seconds = v) })
                    }
                }
            }
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Pill("Añadir fase") { p = p.copy(steps = p.steps + IntervalStep("Fase ${p.steps.size + 1}", 30, STEP_COLORS[p.steps.size % STEP_COLORS.size])) }
        }
        SectionLabel("Aviso entre fases")
        var pickSound by remember { mutableStateOf(false) }
        CardBox {
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth().clickable { pickSound = !pickSound }.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.soundTitle.ifBlank { "Notificación predeterminada" }, style = T.head, modifier = Modifier.weight(1f))
                    Text(if (pickSound) "Cerrar" else "Cambiar", style = T.body.copy(fontWeight = FontWeight.SemiBold))
                }
                if (pickSound) SoundPicker(p.soundUri, listOf(com.canopy.clock.audio.Sounds.Kind.NOTIFICATION, com.canopy.clock.audio.Sounds.Kind.ALARM),
                    "Notificación predeterminada", com.canopy.clock.audio.Sounds.defaultNotification()) { uri, title ->
                    p = p.copy(soundUri = uri, soundTitle = title)
                }
                Text("En los 3 últimos segundos de cada fase suena un pitido corto del sistema.", style = T.small, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
            }
        }
        Text("Total: ${mmss(p.totalSec)}", style = T.head, modifier = Modifier.padding(20.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill("Guardar", filled = true) { Store.saveProgram(p); onDone() }
            Pill("Borrar", color = C.Red) { Store.deleteProgram(id); onDone() }
        }
        Gap(30.dp)
    }
}
