package com.canopy.clock.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.canopy.clock.alarm.CalendarCheck
import com.canopy.clock.alarm.Scheduler
import com.canopy.clock.data.Alarm
import com.canopy.clock.data.AlarmConditions
import com.canopy.clock.data.Store
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val DAYS = listOf("L", "M", "X", "J", "V", "S", "D")
private val ES = Locale("es", "ES")

fun daysText(days: Set<Int>): String = when {
    days.isEmpty() -> "Una vez"
    days.size == 7 -> "Todos los días"
    days == setOf(1, 2, 3, 4, 5) -> "De lunes a viernes"
    days == setOf(6, 7) -> "Fines de semana"
    else -> days.sorted().joinToString(" ") { DAYS[it - 1] }
}

@Composable
fun AlarmsScreen() {
    storeVersion()
    val ctx = LocalContext.current
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    BackHandler(enabled = editing != null) { editing = null }
    editing?.let { id -> AlarmEditor(id, onDone = { editing = null }); return }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Alarmas", "Nueva") {
            val n = LocalDateTime.now().plusHours(1)
            val a = Alarm(Store.nextId(), n.hour, 0, setOf(1, 2, 3, 4, 5))
            Store.saveAlarm(a); Scheduler.schedule(ctx, a); editing = a.id
        }
        PermissionBanners()
        val alarms = Store.alarms()
        if (alarms.isEmpty()) Text("Sin alarmas. Pulsa «Nueva» para crear la primera.", style = T.body, modifier = Modifier.padding(20.dp))
        val now = LocalDateTime.now()
        for (a in alarms) {
            val (next, skipped) = Scheduler.next(ctx, a, now)
            val snooze = Store.snoozeUntil(a.id)
            CardBox(onClick = { editing = a.id }) {
                Row(Modifier.padding(start = 18.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("%02d:%02d".format(a.hour, a.minute), style = T.huge.copy(color = if (a.enabled) C.Ink else C.Muted))
                        Text(listOf(a.label.ifBlank { null }, daysText(a.days)).filterNotNull().joinToString(" · "), style = T.small)
                        if (a.enabled) {
                            if (snooze > 0) Text("Pospuesta hasta las " + java.time.Instant.ofEpochMilli(snooze).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")), style = T.small.copy(color = C.Orange))
                            if (next != null) Text("Sonará ${relative(now, next)}", style = T.small.copy(color = C.Ink))
                            else Text("No sonará en los próximos 60 días", style = T.small.copy(color = C.Red))
                            skipped.firstOrNull()?.let { (d, why) ->
                                Text("Se salta el ${d.dayOfWeek.getDisplayName(TextStyle.FULL, ES)} ${d.dayOfMonth}: $why", style = T.small.copy(color = C.Orange))
                            }
                            if (a.conditions.any) Text("Con condiciones", style = T.small)
                        }
                    }
                    Toggle(a.enabled) { on ->
                        val n = a.copy(enabled = on)
                        Store.saveAlarm(n); Scheduler.schedule(ctx, n)
                        if (!on) Scheduler.cancelSnooze(ctx, a.id)
                    }
                }
            }
        }
        Gap(24.dp)
    }
}

fun relative(now: LocalDateTime, at: LocalDateTime): String {
    val d = Duration.between(now, at)
    val h = d.toHours(); val m = d.toMinutes() % 60
    val dayDiff = Duration.between(now.toLocalDate().atStartOfDay(), at.toLocalDate().atStartOfDay()).toDays()
    val day = when (dayDiff) {
        0L -> "hoy"; 1L -> "mañana"
        else -> "el " + at.dayOfWeek.getDisplayName(TextStyle.FULL, ES) + " " + at.dayOfMonth
    }
    return "$day · dentro de " + (if (h >= 24) "${h / 24} d ${h % 24} h" else if (h > 0) "$h h $m min" else "$m min")
}

@Composable
private fun PermissionBanners() {
    val ctx = LocalContext.current
    if (!Scheduler.canExact(ctx)) {
        CardBox(onClick = { ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName))) }) {
            Text("Falta el permiso de alarmas exactas. Toca para concederlo.", style = T.body.copy(color = C.Red), modifier = Modifier.padding(16.dp))
        }
    }
    if (Build.VERSION.SDK_INT >= 34 && !ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()) {
        CardBox(onClick = { ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + ctx.packageName))) }) {
            Text("Para que la alarma aparezca a pantalla completa con el móvil bloqueado, toca y activa el permiso.", style = T.body.copy(color = C.Red), modifier = Modifier.padding(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AlarmEditor(id: Int, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val orig = Store.alarm(id) ?: run { LaunchedEffect(Unit) { onDone() }; return }
    var a by remember(id) { mutableStateOf(orig) }
    val tp = rememberTimePickerState(orig.hour, orig.minute, is24Hour = true)
    var calVersion by remember { mutableIntStateOf(0) }
    val calPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { calVersion++ }

    fun commit() {
        val n = a.copy(hour = tp.hour, minute = tp.minute, enabled = true)
        Store.saveAlarm(n); Scheduler.cancelSnooze(ctx, n.id); Scheduler.schedule(ctx, n)
        onDone()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Alarma", "Guardar") { commit() }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimeInput(
                state = tp,
                colors = TimePickerDefaults.colors(
                    timeSelectorSelectedContainerColor = C.Orange, timeSelectorUnselectedContainerColor = C.Card,
                    timeSelectorSelectedContentColor = C.Ink, timeSelectorUnselectedContentColor = C.Ink
                )
            )
        }
        val preview = a.copy(hour = tp.hour, minute = tp.minute)
        Scheduler.next(ctx, preview).first?.let {
            Text("Sonará " + relative(LocalDateTime.now(), it), style = T.small.copy(color = C.Ink), modifier = Modifier.padding(horizontal = 20.dp))
        }

        SectionLabel("Repetir")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (d in 1..7) Chip(DAYS[d - 1], d in a.days) { a = a.copy(days = if (d in a.days) a.days - d else a.days + d) }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("Laborables", a.days == setOf(1, 2, 3, 4, 5)) { a = a.copy(days = setOf(1, 2, 3, 4, 5)) }
            Chip("Diario", a.days.size == 7) { a = a.copy(days = (1..7).toSet()) }
            Chip("Una vez", a.days.isEmpty()) { a = a.copy(days = emptySet()) }
        }

        SectionLabel("Nombre")
        OutlinedTextField(a.label, { a = a.copy(label = it) }, singleLine = true, placeholder = { Text("Por ejemplo: gimnasio") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

        // ---------- condiciones ----------
        SectionLabel("Condiciones")
        val c = a.conditions
        fun setC(n: AlarmConditions) { a = a.copy(conditions = n) }
        CardBox {
            Column(Modifier.padding(vertical = 6.dp)) {
                SwitchRow("Saltar en festivos", "Festivos de ${com.canopy.clock.alarm.Holidays.REGIONS[Store.holidayRegion]}. Se editan en Ajustes.", c.skipHolidays) { setC(c.copy(skipHolidays = it)) }
                SwitchRow("Solo si hay un evento ese día", "Mira tu calendario (el que sincroniza DAVx5)", c.onlyIfEvent) { setC(c.copy(onlyIfEvent = it)) }
                if (c.onlyIfEvent) OutlinedTextField(
                    c.eventKeyword, { setC(c.copy(eventKeyword = it)) }, singleLine = true,
                    label = { Text("…que contenga (vacío = cualquiera)") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Text("Saltar si hay un evento que contenga:", style = T.body, modifier = Modifier.padding(start = 18.dp, top = 10.dp))
                OutlinedTextField(
                    c.skipKeyword, { setC(c.copy(skipKeyword = it)) }, singleLine = true, placeholder = { Text("Vacaciones") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                if (c.onlyIfEvent || c.skipKeyword.isNotBlank()) {
                    val calV = calVersion; if (calV < 0) return@CardBox
                    if (!CalendarCheck.hasPermission(ctx)) {
                        Box(Modifier.padding(16.dp)) { Pill("Permitir leer el calendario", filled = true) { calPerm.launch(Manifest.permission.READ_CALENDAR) } }
                    } else {
                        val cals = CalendarCheck.calendars(ctx)
                        Text("Calendarios a consultar (ninguno marcado = todos):", style = T.small, modifier = Modifier.padding(start = 18.dp, top = 10.dp))
                        FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (cal in cals) {
                                val on = cal.id in c.calendarIds
                                Chip(cal.name.ifBlank { cal.account }, on) { setC(c.copy(calendarIds = if (on) c.calendarIds - cal.id else c.calendarIds + cal.id)) }
                            }
                        }
                    }
                }
            }
        }
        val nextRaw = Scheduler.next(ctx, preview.copy(skipDate = null)).first
        if (nextRaw != null && a.days.isNotEmpty()) {
            val skipOn = a.skipDate == nextRaw.toLocalDate().toString()
            CardBox {
                SwitchRow(
                    "Omitir la próxima", "Solo la del " + nextRaw.dayOfWeek.getDisplayName(TextStyle.FULL, ES) + " " + nextRaw.dayOfMonth, skipOn
                ) { a = a.copy(skipDate = if (it) nextRaw.toLocalDate().toString() else null) }
            }
        }

        // ---------- sonido ----------
        SectionLabel("Sonido")
        ToneEditor(a.tone) { a = a.copy(tone = it) }

        SectionLabel("Posponer")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (m in listOf(5, 9, 10, 15, 20)) Chip("$m min", a.snoozeMin == m) { a = a.copy(snoozeMin = m) }
        }

        Gap(20.dp)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill("Guardar", filled = true) { commit() }
            Pill("Borrar", color = C.Red) { Scheduler.cancel(ctx, id); Store.deleteAlarm(id); onDone() }
        }
        Gap(30.dp)
    }
}

