package com.canopy.clock.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.canopy.clock.alarm.Scheduler
import com.canopy.clock.data.Place
import com.canopy.clock.data.Store
import com.canopy.clock.faces.Faces
import com.canopy.clock.nightstand.NightstandActivity
import com.canopy.clock.sun.Cities
import com.canopy.clock.sun.Sun
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val ES = Locale("es", "ES")
private val HM = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ClockScreen(onGallery: () -> Unit, onSettings: () -> Unit) {
    storeVersion()
    val ctx = LocalContext.current
    val spec = Faces.byId(Store.faceId)
    val opts = Store.faceOptions(spec.id)
    var minuteTick by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(15_000); minuteTick = LocalDateTime.now() } }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Header("Reloj", "Ajustes", onSettings)
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).aspectRatio(if (spec.wide) 1.25f else 1f)
                .clickable(onClick = onGallery)
        ) { LiveFace(spec, opts, Modifier.fillMaxWidth().aspectRatio(if (spec.wide) 1.25f else 1f), sound = true) }

        val today = minuteTick.toLocalDate()
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(today.dayOfWeek.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() }, style = T.head)
                Text("${today.dayOfMonth} de ${today.month.getDisplayName(TextStyle.FULL, ES)} · ${spec.name}", style = T.small)
                nextAlarmText(ctx)?.let { Text(it, style = T.small.copy(color = C.Ink)) }
            }
            Pill("Esferas", onClick = onGallery)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Pill("Modo reloj", filled = true, modifier = Modifier.weight(1f)) {
                ctx.startActivity(Intent(ctx, NightstandActivity::class.java))
            }
        }

        SunSection(minuteTick)
        WorldSection(minuteTick)
        Gap(24.dp)
    }
}

private fun nextAlarmText(ctx: Context): String? {
    val now = LocalDateTime.now()
    val next = Store.alarms().filter { it.enabled }.mapNotNull { Scheduler.next(ctx, it, now).first }.minOrNull() ?: return null
    val d = Duration.between(now, next)
    val h = d.toHours(); val m = d.toMinutes() % 60
    return "Próxima alarma: ${next.format(HM)} · dentro de " + (if (h > 0) "$h h $m min" else "$m min")
}

// ---------------- sol ----------------

@Composable
private fun SunSection(now: LocalDateTime) {
    val place = Store.place ?: Place("Madrid", 40.4168, -3.7038)
    var picking by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val day = Sun.day(now.toLocalDate(), place.lat, place.lon, zone)
    val yesterday = Sun.day(now.toLocalDate().minusDays(1), place.lat, place.lon, zone)

    SectionLabel("Sol · ${place.name}")
    CardBox(onClick = { picking = true }) {
        Column(Modifier.padding(16.dp)) {
            SunBar(day, now)
            Gap(10.dp)
            Row {
                SunBig("Sale", day.sunrise, Modifier.weight(1f))
                SunBig("Se pone", day.sunset, Modifier.weight(1f))
            }
            Gap(8.dp)
            KeyValue("Hora azul de mañana", range(day.dawn, day.blueMorningEnd))
            KeyValue("Hora dorada de mañana", range(day.sunrise, day.goldenMorningEnd))
            KeyValue("Mediodía solar", day.noon.format(HM))
            KeyValue("Hora dorada de tarde", range(day.goldenEveningStart, day.sunset))
            KeyValue("Hora azul de tarde", range(day.blueEveningStart, day.dusk))
            val len = day.dayLengthMin
            val diff = if (len != null && yesterday.dayLengthMin != null) len - yesterday.dayLengthMin!! else null
            if (len != null) KeyValue(
                "Duración del día",
                "${len / 60} h ${len % 60} min" + (diff?.let { if (it == 0L) " · igual que ayer" else " · ${if (it > 0) "+" else "−"}${abs(it)} min" } ?: "")
            )
            Text("Toca para cambiar el lugar", style = T.small, modifier = Modifier.padding(top = 6.dp))
        }
    }
    if (picking) PlacePicker(onDismiss = { picking = false })
}

private fun range(a: ZonedDateTime?, b: ZonedDateTime?) = if (a == null || b == null) "—" else "${a.format(HM)} – ${b.format(HM)}"

@Composable
private fun SunBig(label: String, t: ZonedDateTime?, modifier: Modifier) {
    Column(modifier) {
        Text(label.uppercase(), style = T.label)
        Text(t?.format(HM) ?: "—", style = T.big)
    }
}

/** Barra de 24 horas: noche, hora azul, hora dorada y día, con la hora actual marcada. */
@Composable
private fun SunBar(d: Sun.Day, now: LocalDateTime) {
    fun f(z: ZonedDateTime?): Float? = z?.let { (it.hour * 60 + it.minute) / 1440f }
    val night = Color(0xFF1D2433); val blue = Color(0xFF3F5E9C); val gold = Color(0xFFF2B33D); val dayC = Color(0xFFF4E3A1)
    Canvas(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp))) {
        val w = size.width; val h = size.height
        drawRect(night)
        fun seg(a: Float?, b: Float?, c: Color) { if (a != null && b != null && b > a) drawRect(c, Offset(a * w, 0f), Size((b - a) * w, h)) }
        seg(f(d.dawn), f(d.dusk), blue)
        seg(f(d.blueMorningEnd), f(d.blueEveningStart), gold)
        seg(f(d.goldenMorningEnd), f(d.goldenEveningStart), dayC)
        val x = (now.hour * 60 + now.minute) / 1440f * w
        drawRoundRect(Color(0xFF151515), Offset(x - 2f, 0f), Size(4f, h), CornerRadius(2f))
        drawCircle(Color(0xFFF08A1C), 5f, Offset(x, h / 2))
    }
    Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        for (s in listOf("0", "6", "12", "18", "24")) Text(s, style = T.small)
    }
}

@SuppressLint("MissingPermission")
private fun lastLocation(ctx: Context): android.location.Location? {
    val lm = ctx.getSystemService(LocationManager::class.java)
    return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
}

@Composable
private fun PlacePicker(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    fun useLocation() {
        val l = lastLocation(ctx)
        if (l == null) msg = "El móvil no tiene una ubicación reciente. Elige una ciudad."
        else { Store.place = Place("Mi ubicación", l.latitude, l.longitude); onDismiss() }
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) useLocation() else msg = "Sin permiso de ubicación. Elige una ciudad."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Pill("Cerrar", onClick = onDismiss) },
        title = { Text("Lugar para el sol", style = T.head) },
        text = {
            Column {
                Pill("Usar mi ubicación", filled = true, modifier = Modifier.fillMaxWidth()) {
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) useLocation()
                    else perm.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                msg?.let { Text(it, style = T.small, modifier = Modifier.padding(top = 6.dp)) }
                Gap(8.dp)
                OutlinedTextField(q, { q = it }, label = { Text("Buscar ciudad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(Cities.ALL.filter { q.isBlank() || it.name.contains(q, true) || it.country.contains(q, true) }) { c ->
                        Text("${c.name} · ${c.country}", style = T.body, modifier = Modifier.fillMaxWidth().clickable {
                            Store.place = Place(c.name, c.lat, c.lon); onDismiss()
                        }.padding(vertical = 10.dp))
                    }
                }
            }
        },
        containerColor = C.Card,
    )
}

// ---------------- mundo ----------------

/** Diferencia legible: "+6 h · mañana", "−1 h 30 min · hoy", "misma hora". */
fun diffText(zone: ZoneId, now: ZonedDateTime = ZonedDateTime.now()): String {
    val there = now.withZoneSameInstant(zone)
    val mins = (there.offset.totalSeconds - now.offset.totalSeconds) / 60
    val sign = if (mins >= 0) "+" else "−"
    val h = abs(mins) / 60; val m = abs(mins) % 60
    val off = when {
        mins == 0 -> "misma hora"
        m == 0 -> "$sign$h h"
        h == 0 -> "$sign$m min"
        else -> "$sign$h h $m min"
    }
    val dd = there.toLocalDate().compareTo(now.toLocalDate())
    val day = when { dd > 0 -> "mañana"; dd < 0 -> "ayer"; else -> "hoy" }
    return "$off · $day"
}

@Composable
private fun WorldSection(now: LocalDateTime) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val zones = Store.zones()
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel("Mundo", Modifier.weight(1f))
        if (zones.isNotEmpty()) Text(if (editing) "Listo" else "Editar", style = T.small.copy(color = C.Ink, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.clickable { editing = !editing }.padding(horizontal = 20.dp, vertical = 8.dp))
    }
    val local = ZonedDateTime.now()
    for (key in zones) {
        val (zid, name) = Cities.parse(key)
        val zone = runCatching { ZoneId.of(zid) }.getOrNull() ?: continue
        val there = local.withZoneSameInstant(zone)
        val nightThere = there.hour < 7 || there.hour >= 22
        CardBox {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = T.head)
                    Text(diffText(zone, local), style = T.small)
                    // a qué hora de aquí empieza su jornada: útil para llamar o quedar
                    val nine = there.toLocalDate().atTime(9, 0).atZone(zone).withZoneSameInstant(local.zone)
                    Text("Sus 9:00 son tus ${nine.format(HM)}", style = T.small)
                }
                if (editing) Pill("Quitar", color = C.Red) { Store.saveZones(zones - key) }
                else Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(if (nightThere) Color(0xFF1D2433) else C.Yellow, 10.dp)
                    HGap(10.dp)
                    Text(there.format(HM), style = T.big)
                }
            }
        }
    }
    Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { Pill("Añadir ciudad") { adding = true } }
    if (adding) CityPicker(onPick = { Store.saveZones((zones + it).distinct()); adding = false }, onDismiss = { adding = false })
}

@Composable
fun CityPicker(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val extra = remember { ZoneId.getAvailableZoneIds().filter { it.contains('/') && !it.startsWith("Etc") && !it.startsWith("SystemV") }.sorted() }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Pill("Cerrar", onClick = onDismiss) },
        title = { Text("Añadir ciudad", style = T.head) },
        text = {
            Column {
                OutlinedTextField(q, { q = it }, label = { Text("Buscar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                val cities = Cities.ALL.filter { q.isBlank() || it.name.contains(q, true) || it.country.contains(q, true) }
                val zonesQ = if (q.length < 2) emptyList() else extra.filter { it.replace('_', ' ').contains(q, true) }.take(40)
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(cities) { c ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(Cities.key(c)) }.padding(vertical = 10.dp)) {
                            Text(c.name, style = T.body, modifier = Modifier.weight(1f))
                            Text(diffText(ZoneId.of(c.zone)).substringBefore(" ·"), style = T.small)
                        }
                    }
                    items(zonesQ) { z ->
                        Text(z.replace('_', ' '), style = T.body, modifier = Modifier.fillMaxWidth().clickable { onPick(z) }.padding(vertical = 10.dp))
                    }
                }
            }
        },
        containerColor = C.Card,
    )
}

