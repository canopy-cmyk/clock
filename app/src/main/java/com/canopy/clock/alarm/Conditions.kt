package com.canopy.clock.alarm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.canopy.clock.data.Alarm
import com.canopy.clock.data.Store
import java.time.LocalDate
import java.time.ZoneId

/** Festivos calculados en el propio móvil. Se pueden añadir o quitar días a mano. */
object Holidays {
    val REGIONS = linkedMapOf(
        "nacional" to "España (solo nacionales)",
        "comunidad" to "Comunidad de Madrid",
        "madrid" to "Madrid ciudad",
    )

    fun easter(year: Int): LocalDate {
        val a = year % 19; val b = year / 100; val c = year % 100
        val d = b / 4; val e = b % 4; val f = (b + 8) / 25; val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30; val i = c / 4; val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7; val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31; val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    /** Lista de festivos del año con su nombre. */
    fun forYear(year: Int, region: String = Store.holidayRegion): List<Pair<LocalDate, String>> {
        val e = easter(year)
        val l = mutableListOf(
            LocalDate.of(year, 1, 1) to "Año Nuevo",
            LocalDate.of(year, 1, 6) to "Reyes",
            e.minusDays(2) to "Viernes Santo",
            LocalDate.of(year, 5, 1) to "Día del Trabajo",
            LocalDate.of(year, 8, 15) to "Asunción",
            LocalDate.of(year, 10, 12) to "Fiesta Nacional",
            LocalDate.of(year, 11, 1) to "Todos los Santos",
            LocalDate.of(year, 12, 6) to "Constitución",
            LocalDate.of(year, 12, 8) to "Inmaculada",
            LocalDate.of(year, 12, 25) to "Navidad",
        )
        if (region == "comunidad" || region == "madrid") {
            l += e.minusDays(3) to "Jueves Santo"
            l += LocalDate.of(year, 5, 2) to "Comunidad de Madrid"
        }
        if (region == "madrid") {
            l += LocalDate.of(year, 5, 15) to "San Isidro"
            l += LocalDate.of(year, 11, 9) to "La Almudena"
        }
        val removed = Store.removedHolidays().toSet()
        val custom = Store.customHolidays().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .filter { it.year == year }.map { it to "Añadido por ti" }
        return (l.filter { it.first.toString() !in removed } + custom).sortedBy { it.first }
    }

    fun isHoliday(d: LocalDate): Boolean = forYear(d.year).any { it.first == d }
}

object CalendarCheck {
    fun hasPermission(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    data class Cal(val id: Long, val name: String, val account: String)

    fun calendars(ctx: Context): List<Cal> {
        if (!hasPermission(ctx)) return emptyList()
        val out = mutableListOf<Cal>()
        runCatching {
            ctx.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME),
                null, null, null
            )?.use { c -> while (c.moveToNext()) out += Cal(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "") }
        }
        return out
    }

    /** Títulos de los eventos de ese día (en los calendarios indicados, o en todos). */
    fun titles(ctx: Context, day: LocalDate, calendarIds: List<Long>): List<String>? {
        if (!hasPermission(ctx)) return null
        val zone = ZoneId.systemDefault()
        val begin = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            android.content.ContentUris.appendId(it, begin); android.content.ContentUris.appendId(it, end)
        }.build()
        val out = mutableListOf<String>()
        runCatching {
            ctx.contentResolver.query(uri, arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.CALENDAR_ID), null, null, null)
                ?.use { c ->
                    while (c.moveToNext()) {
                        val cal = c.getLong(1)
                        if (calendarIds.isEmpty() || cal in calendarIds) out += (c.getString(0) ?: "")
                    }
                }
        }.onFailure { return null }
        return out
    }
}

object Conditions {
    /** Devuelve null si la alarma debe sonar ese día, o el motivo por el que se salta. */
    fun skipReason(ctx: Context, a: Alarm, day: LocalDate): String? {
        val c = a.conditions
        if (a.skipDate == day.toString()) return "Omitida una vez"
        if (c.skipHolidays && Holidays.isHoliday(day)) return "Festivo"
        if (c.onlyIfEvent || c.skipKeyword.isNotBlank()) {
            // Sin permiso de calendario no se puede comprobar: la alarma suena (es lo más seguro).
            val titles = CalendarCheck.titles(ctx, day, c.calendarIds) ?: return null
            if (c.skipKeyword.isNotBlank() && titles.any { it.contains(c.skipKeyword, ignoreCase = true) })
                return "Evento «${c.skipKeyword}»"
            if (c.onlyIfEvent) {
                val match = if (c.eventKeyword.isBlank()) titles.isNotEmpty()
                else titles.any { it.contains(c.eventKeyword, ignoreCase = true) }
                if (!match) return if (c.eventKeyword.isBlank()) "Sin eventos" else "Sin «${c.eventKeyword}»"
            }
        }
        return null
    }
}
