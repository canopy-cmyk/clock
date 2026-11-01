package com.canopy.clock.sun

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cálculo astronómico del sol (ecuación del amanecer), sin conexión.
 * Precisión de un par de minutos, de sobra para un reloj.
 */
object Sun {
    private fun rad(d: Double) = d * PI / 180
    private fun deg(r: Double) = r * 180 / PI

    private class Base(val transit: Double, val decl: Double)

    private fun base(date: LocalDate, lon: Double): Base {
        val n = date.toEpochDay() + 2440588 - 2451545 + 0.0008
        val jStar = n - lon / 360.0
        val m = (357.5291 + 0.98560028 * jStar) % 360
        val c = 1.9148 * sin(rad(m)) + 0.02 * sin(rad(2 * m)) + 0.0003 * sin(rad(3 * m))
        val lambda = (m + c + 180 + 102.9372) % 360
        val transit = 2451545.0 + jStar + 0.0053 * sin(rad(m)) - 0.0069 * sin(rad(2 * lambda))
        val decl = asin(sin(rad(lambda)) * sin(rad(23.4397)))
        return Base(transit, decl)
    }

    private fun jdToZdt(j: Double, zone: ZoneId): ZonedDateTime =
        Instant.ofEpochMilli(((j - 2440587.5) * 86_400_000).toLong()).atZone(zone)

    /** Instante en que el sol cruza una elevación dada, subiendo o bajando. Null si no ocurre (verano/invierno polar). */
    fun at(date: LocalDate, lat: Double, lon: Double, elevation: Double, rising: Boolean, zone: ZoneId): ZonedDateTime? {
        val b = base(date, lon)
        val cosW = (sin(rad(elevation)) - sin(rad(lat)) * sin(b.decl)) / (cos(rad(lat)) * cos(b.decl))
        if (cosW < -1 || cosW > 1) return null
        val w = deg(acos(cosW))
        return jdToZdt(if (rising) b.transit - w / 360 else b.transit + w / 360, zone)
    }

    fun noon(date: LocalDate, lon: Double, zone: ZoneId) = jdToZdt(base(date, lon).transit, zone)

    data class Day(
        val dawn: ZonedDateTime?,          // amanecer civil (-6°)
        val blueMorningEnd: ZonedDateTime?,// fin de hora azul de mañana (-4°)
        val sunrise: ZonedDateTime?,
        val goldenMorningEnd: ZonedDateTime?,
        val noon: ZonedDateTime,
        val goldenEveningStart: ZonedDateTime?,
        val sunset: ZonedDateTime?,
        val blueEveningStart: ZonedDateTime?,
        val dusk: ZonedDateTime?,          // anochecer civil (-6°)
    ) {
        val dayLengthMin: Long? get() = if (sunrise != null && sunset != null) java.time.Duration.between(sunrise, sunset).toMinutes() else null
    }

    fun day(date: LocalDate, lat: Double, lon: Double, zone: ZoneId = ZoneId.systemDefault()) = Day(
        dawn = at(date, lat, lon, -6.0, true, zone),
        blueMorningEnd = at(date, lat, lon, -4.0, true, zone),
        sunrise = at(date, lat, lon, -0.833, true, zone),
        goldenMorningEnd = at(date, lat, lon, 6.0, true, zone),
        noon = noon(date, lon, zone),
        goldenEveningStart = at(date, lat, lon, 6.0, false, zone),
        sunset = at(date, lat, lon, -0.833, false, zone),
        blueEveningStart = at(date, lat, lon, -4.0, false, zone),
        dusk = at(date, lat, lon, -6.0, false, zone),
    )
}
