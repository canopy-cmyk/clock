package com.canopy.clock.ui

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.canopy.clock.alarm.Holidays
import com.canopy.clock.alarm.Scheduler
import com.canopy.clock.data.Store
import com.canopy.clock.faces.Faces
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es", "ES")

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    storeVersion()
    val ctx = LocalContext.current
    var year by remember { mutableIntStateOf(LocalDate.now().year) }
    var adding by remember { mutableStateOf(false) }

    fun refreshAlarms() = Scheduler.scheduleAll(ctx)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Ajustes", "Cerrar", onBack)

        // ---------- modo reloj ----------
        SectionLabel("Modo reloj")
        CardBox {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text("Reloj que se muestra", style = T.body, modifier = Modifier.padding(horizontal = 18.dp))
                FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (f in Faces.ALL) Chip(f.name, Store.nightFaceId == f.id) { Store.nightFaceId = f.id }
                }
                SliderRow("Brillo de noche", Store.nightDim, 0.05f..1f, "${(Store.nightDim * 100).toInt()} %") { Store.nightDim = it }
                SwitchRow("Filtro rojo", "Tiñe el reloj de rojo para no deslumbrar de noche", Store.nightRedShift) { Store.nightRedShift = it }
                Text(
                    "La pantalla no se apaga mientras el modo reloj está abierto. El reloj se desplaza un poco cada minuto para no marcar la pantalla.",
                    style = T.small, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )
                Text(
                    "Para que aparezca solo al cargar el móvil: en Salvapantallas elige «Clock · modo reloj» y actívalo al cargar.",
                    style = T.small, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )
                Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Pill("Abrir ajustes de salvapantallas") {
                        runCatching { ctx.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS)) }
                            .onFailure { ctx.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
                    }
                }
            }
        }

        // ---------- festivos ----------
        SectionLabel("Festivos")
        CardBox {
            Column(Modifier.padding(vertical = 8.dp)) {
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((id, name) in Holidays.REGIONS) Chip(name, Store.holidayRegion == id) { Store.holidayRegion = id; refreshAlarms() }
                }
                Text(
                    "Cuando un festivo cae en domingo, la Comunidad a veces lo pasa al lunes. Revisa la lista y añade o quita días si hace falta.",
                    style = T.small, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Chip("‹", false) { year-- }
                    Text("$year", style = T.head, modifier = Modifier.padding(horizontal = 12.dp))
                    Chip("›", false) { year++ }
                }
                val removed = Store.removedHolidays()
                val all = Holidays.forYear(year).map { it.first to it.second } +
                    removed.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.filter { it.year == year }.map { it to "Quitado" }
                for ((d, name) in all.sortedBy { it.first }) {
                    val isRemoved = d.toString() in removed
                    val isCustom = d.toString() in Store.customHolidays()
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.format(DateTimeFormatter.ofPattern("d MMM", ES)) + " · " + d.dayOfWeek.getDisplayName(TextStyle.SHORT, ES),
                                style = T.body.copy(color = if (isRemoved) C.Muted else C.Ink))
                            Text(name, style = T.small)
                        }
                        Text(
                            if (isRemoved) "Recuperar" else "Quitar",
                            style = T.small.copy(color = if (isRemoved) C.Ink else C.Red, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.clickable {
                                when {
                                    isRemoved -> Store.saveRemovedHolidays(removed - d.toString())
                                    isCustom -> Store.saveCustomHolidays(Store.customHolidays() - d.toString())
                                    else -> Store.saveRemovedHolidays(removed + d.toString())
                                }
                                refreshAlarms()
                            }.padding(8.dp)
                        )
                    }
                }
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { Pill("Añadir día") { adding = true } }
            }
        }

        // ---------- permisos ----------
        SectionLabel("Permisos")
        CardBox {
            Column(Modifier.padding(vertical = 8.dp)) {
                PermRow("Alarmas exactas", Scheduler.canExact(ctx)) {
                    ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName)))
                }
                if (Build.VERSION.SDK_INT >= 34) PermRow("Alarma a pantalla completa", ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()) {
                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + ctx.packageName)))
                }
                PermRow("Notificaciones", ctx.getSystemService(NotificationManager::class.java).areNotificationsEnabled()) {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }
                PermRow("Sin restricciones de batería", ctx.getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)) {
                    ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }

        // ---------- privacidad ----------
        SectionLabel("Privacidad")
        CardBox {
            Text(
                "Esta app no tiene permiso de Internet: es técnicamente imposible que envíe o reciba datos. Alarmas, ajustes y cálculos del sol se guardan y se hacen solo en el móvil. El calendario y la ubicación se leen solo si les das permiso, y nunca salen del dispositivo.",
                style = T.body, modifier = Modifier.padding(16.dp)
            )
        }
        Text("Tipografía Inter Tight (licencia SIL Open Font). Tonos sintetizados en la propia app.",
            style = T.small, modifier = Modifier.padding(20.dp))
    }

    if (adding) {
        val st = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { adding = false },
            confirmButton = {
                Pill("Añadir", filled = true) {
                    st.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        Store.saveCustomHolidays(Store.customHolidays() + d.toString())
                        refreshAlarms()
                    }
                    adding = false
                }
            },
            dismissButton = { Pill("Cancelar") { adding = false } }
        ) { DatePicker(st) }
    }
}

@Composable
private fun PermRow(title: String, ok: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = !ok, onClick = onFix).padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = T.body, modifier = Modifier.weight(1f))
        Text(if (ok) "Concedido" else "Activar", style = T.small.copy(color = if (ok) C.Green else C.Red, fontWeight = FontWeight.SemiBold))
    }
}
