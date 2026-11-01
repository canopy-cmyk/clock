package com.canopy.clock.data

import org.json.JSONArray
import org.json.JSONObject

/** Tipos de tono sintetizado. */
enum class ToneType(val label: String) {
    QUARTZ("Pitido de cuarzo"),
    BELL("Doble campana"),
    CHIRP("Chirrido digital"),
    GONG("Gong suave"),
    PULSE("Solo vibración"),
}

data class ToneSettings(
    val type: ToneType = ToneType.QUARTZ,
    val pitch: Float = 1f,        // multiplicador de frecuencia 0.5–2
    val tempo: Float = 1f,        // velocidad de la cadencia 0.5–2
    val crescendoSec: Int = 30,   // segundos hasta volumen máximo (0 = directo)
    val volume: Float = 0.9f,
    val vibrate: Boolean = true,
) {
    fun toJson() = JSONObject().put("type", type.name).put("pitch", pitch.toDouble()).put("tempo", tempo.toDouble())
        .put("cres", crescendoSec).put("vol", volume.toDouble()).put("vib", vibrate)

    companion object {
        fun from(j: JSONObject?): ToneSettings {
            if (j == null) return ToneSettings()
            return ToneSettings(
                runCatching { ToneType.valueOf(j.optString("type")) }.getOrDefault(ToneType.QUARTZ),
                j.optDouble("pitch", 1.0).toFloat(), j.optDouble("tempo", 1.0).toFloat(),
                j.optInt("cres", 30), j.optDouble("vol", 0.9).toFloat(), j.optBoolean("vib", true)
            )
        }
    }
}

/** Condiciones que deciden si una alarma suena o se salta ese día. */
data class AlarmConditions(
    val onlyIfEvent: Boolean = false,       // solo si hay algún evento ese día
    val eventKeyword: String = "",          // ...que contenga esta palabra (vacío = cualquiera)
    val skipHolidays: Boolean = false,      // saltar en festivos
    val skipKeyword: String = "",           // saltar si hay un evento con esta palabra (p. ej. "Vacaciones")
    val calendarIds: List<Long> = emptyList(), // calendarios a consultar (vacío = todos)
) {
    val any get() = onlyIfEvent || skipHolidays || skipKeyword.isNotBlank()

    fun toJson() = JSONObject().put("onlyEv", onlyIfEvent).put("evKw", eventKeyword).put("hol", skipHolidays)
        .put("skipKw", skipKeyword).put("cals", JSONArray(calendarIds))

    companion object {
        fun from(j: JSONObject?): AlarmConditions {
            if (j == null) return AlarmConditions()
            val a = j.optJSONArray("cals") ?: JSONArray()
            return AlarmConditions(
                j.optBoolean("onlyEv"), j.optString("evKw"), j.optBoolean("hol"), j.optString("skipKw"),
                (0 until a.length()).map { a.getLong(it) }
            )
        }
    }
}

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val days: Set<Int> = emptySet(),   // 1 = lunes … 7 = domingo; vacío = una sola vez
    val enabled: Boolean = true,
    val label: String = "",
    val tone: ToneSettings = ToneSettings(),
    val snoozeMin: Int = 9,
    val conditions: AlarmConditions = AlarmConditions(),
    val skipDate: String? = null,       // fecha ISO de la próxima repetición que se omite una vez
) {
    fun toJson() = JSONObject().put("id", id).put("h", hour).put("m", minute).put("days", JSONArray(days.toList()))
        .put("on", enabled).put("label", label).put("tone", tone.toJson()).put("snooze", snoozeMin)
        .put("cond", conditions.toJson()).put("skip", skipDate ?: JSONObject.NULL)

    companion object {
        fun from(j: JSONObject): Alarm {
            val d = j.optJSONArray("days") ?: JSONArray()
            return Alarm(
                j.getInt("id"), j.getInt("h"), j.getInt("m"), (0 until d.length()).map { d.getInt(it) }.toSet(),
                j.optBoolean("on", true), j.optString("label"), ToneSettings.from(j.optJSONObject("tone")),
                j.optInt("snooze", 9), AlarmConditions.from(j.optJSONObject("cond")),
                if (j.isNull("skip")) null else j.optString("skip").ifBlank { null }
            )
        }
    }
}

data class TimerPreset(val name: String, val seconds: Int) {
    fun toJson() = JSONObject().put("n", name).put("s", seconds)
    companion object { fun from(j: JSONObject) = TimerPreset(j.getString("n"), j.getInt("s")) }
}

/** Temporizador activo. Si está en marcha, endAt es el instante final; si está pausado, remainingMs. */
data class RunningTimer(
    val id: Int,
    val label: String,
    val totalMs: Long,
    val endAt: Long,
    val remainingMs: Long,
    val running: Boolean,
) {
    fun left(now: Long) = if (running) (endAt - now).coerceAtLeast(0) else remainingMs
    fun toJson() = JSONObject().put("id", id).put("l", label).put("t", totalMs).put("e", endAt).put("r", remainingMs).put("run", running)
    companion object {
        fun from(j: JSONObject) = RunningTimer(j.getInt("id"), j.optString("l"), j.getLong("t"), j.getLong("e"), j.getLong("r"), j.getBoolean("run"))
    }
}

data class IntervalStep(val name: String, val seconds: Int, val color: Int) {
    fun toJson() = JSONObject().put("n", name).put("s", seconds).put("c", color)
    companion object { fun from(j: JSONObject) = IntervalStep(j.getString("n"), j.getInt("s"), j.getInt("c")) }
}

data class IntervalProgram(
    val id: Int,
    val name: String,
    val warmup: Int,
    val steps: List<IntervalStep>,
    val rounds: Int,
    val cooldown: Int,
    val tone: ToneType = ToneType.QUARTZ,
) {
    val totalSec get() = warmup + cooldown + steps.sumOf { it.seconds } * rounds

    fun toJson() = JSONObject().put("id", id).put("n", name).put("w", warmup).put("r", rounds).put("cd", cooldown)
        .put("tone", tone.name).put("steps", JSONArray(steps.map { it.toJson() }))

    companion object {
        fun from(j: JSONObject): IntervalProgram {
            val a = j.getJSONArray("steps")
            return IntervalProgram(
                j.getInt("id"), j.getString("n"), j.optInt("w"), (0 until a.length()).map { IntervalStep.from(a.getJSONObject(it)) },
                j.optInt("r", 1), j.optInt("cd"), runCatching { ToneType.valueOf(j.optString("tone")) }.getOrDefault(ToneType.QUARTZ)
            )
        }
    }
}

data class Place(val name: String, val lat: Double, val lon: Double) {
    fun toJson() = JSONObject().put("n", name).put("la", lat).put("lo", lon)
    companion object { fun from(j: JSONObject) = Place(j.getString("n"), j.getDouble("la"), j.getDouble("lo")) }
}
