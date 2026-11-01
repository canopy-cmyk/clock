package com.canopy.clock.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.canopy.clock.faces.FaceOptions
import com.canopy.clock.faces.Faces
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Todo se guarda en el propio móvil (SharedPreferences). La app no tiene permiso
 * de Internet, así que nada puede salir del dispositivo.
 */
object Store {
    private lateinit var sp: SharedPreferences
    val version = MutableStateFlow(0)

    fun init(ctx: Context) {
        if (::sp.isInitialized) return
        sp = ctx.applicationContext.getSharedPreferences("clock", Context.MODE_PRIVATE)
        if (!sp.getBoolean("seeded", false)) seed()
    }

    private fun bump() { version.value = version.value + 1 }

    private fun arr(key: String): JSONArray = runCatching { JSONArray(sp.getString(key, "[]")) }.getOrDefault(JSONArray())
    private fun putArr(key: String, a: JSONArray) { sp.edit().putString(key, a.toString()).apply(); bump() }

    // ---------- alarmas ----------
    fun alarms(): List<Alarm> = arr("alarms").let { a -> (0 until a.length()).map { Alarm.from(a.getJSONObject(it)) } }
        .sortedWith(compareBy({ it.hour }, { it.minute }))

    fun alarm(id: Int) = alarms().firstOrNull { it.id == id }

    fun saveAlarm(al: Alarm) {
        val list = alarms().filter { it.id != al.id } + al
        putArr("alarms", JSONArray(list.map { it.toJson() }))
    }

    fun deleteAlarm(id: Int) = putArr("alarms", JSONArray(alarms().filter { it.id != id }.map { it.toJson() }))

    fun setSnooze(id: Int, at: Long) { sp.edit().putLong("snz_$id", at).apply(); bump() }
    fun snoozeUntil(id: Int): Long = sp.getLong("snz_$id", 0).let { if (it < System.currentTimeMillis()) 0 else it }

    fun nextId(): Int { val n = sp.getInt("nextId", 100) + 1; sp.edit().putInt("nextId", n).apply(); return n }

    // ---------- temporizadores ----------
    fun presets(): List<TimerPreset> = arr("presets").let { a -> (0 until a.length()).map { TimerPreset.from(a.getJSONObject(it)) } }
    fun savePresets(l: List<TimerPreset>) = putArr("presets", JSONArray(l.map { it.toJson() }))

    fun timers(): List<RunningTimer> = arr("timers").let { a -> (0 until a.length()).map { RunningTimer.from(a.getJSONObject(it)) } }
    fun saveTimer(t: RunningTimer) = putArr("timers", JSONArray((timers().filter { it.id != t.id } + t).sortedBy { it.id }.map { it.toJson() }))
    fun deleteTimer(id: Int) = putArr("timers", JSONArray(timers().filter { it.id != id }.map { it.toJson() }))
    var timerTone: ToneSettings
        get() = ToneSettings.from(runCatching { JSONObject(sp.getString("timerTone", "{}")!!) }.getOrNull())
        set(v) { sp.edit().putString("timerTone", v.toJson().toString()).apply(); bump() }

    // ---------- sonidos propios (archivos elegidos por el usuario) ----------
    fun customSounds(): List<Pair<String, String>> = arr("customSounds").let { a ->
        (0 until a.length()).map { val o = a.getJSONObject(it); o.getString("u") to o.getString("t") }
    }
    fun addCustomSound(uri: String, title: String) {
        val l = listOf(uri to title) + customSounds().filter { it.first != uri }
        putArr("customSounds", JSONArray(l.map { JSONObject().put("u", it.first).put("t", it.second) }))
    }
    fun removeCustomSound(uri: String) =
        putArr("customSounds", JSONArray(customSounds().filter { it.first != uri }.map { JSONObject().put("u", it.first).put("t", it.second) }))

    // ---------- intervalos ----------
    fun programs(): List<IntervalProgram> = arr("programs").let { a -> (0 until a.length()).map { IntervalProgram.from(a.getJSONObject(it)) } }
    fun saveProgram(p: IntervalProgram) = putArr("programs", JSONArray((programs().filter { it.id != p.id } + p).sortedBy { it.id }.map { it.toJson() }))
    fun deleteProgram(id: Int) = putArr("programs", JSONArray(programs().filter { it.id != id }.map { it.toJson() }))

    // ---------- cronómetro ----------
    var swRunning: Boolean
        get() = sp.getBoolean("swRun", false)
        set(v) { sp.edit().putBoolean("swRun", v).apply(); bump() }
    var swStart: Long
        get() = sp.getLong("swStart", 0)
        set(v) { sp.edit().putLong("swStart", v).apply() }
    var swAccum: Long
        get() = sp.getLong("swAcc", 0)
        set(v) { sp.edit().putLong("swAcc", v).apply(); bump() }
    fun laps(): List<Long> = arr("laps").let { a -> (0 until a.length()).map { a.getLong(it) } }
    fun saveLaps(l: List<Long>) = putArr("laps", JSONArray(l))

    // ---------- mundo ----------
    fun zones(): List<String> = arr("zones").let { a -> (0 until a.length()).map { a.getString(it) } }
    fun saveZones(l: List<String>) = putArr("zones", JSONArray(l))

    // ---------- sol ----------
    var place: Place?
        get() = sp.getString("place", null)?.let { runCatching { Place.from(JSONObject(it)) }.getOrNull() }
        set(v) { sp.edit().putString("place", v?.toJson()?.toString()).apply(); bump() }

    // ---------- esferas ----------
    var faceId: String
        get() = sp.getString("face", "funcional")!!
        set(v) { sp.edit().putString("face", v).apply(); bump() }
    var nightFaceId: String
        get() = sp.getString("nightFace", "vfd")!!
        set(v) { sp.edit().putString("nightFace", v).apply(); bump() }

    fun faceOptions(id: String): FaceOptions {
        val d = Faces.byId(id).defaults
        val s = sp.getString("fo_$id", null) ?: return d
        return runCatching { FaceOptions.fromJson(JSONObject(s), d) }.getOrDefault(d)
    }
    fun saveFaceOptions(id: String, o: FaceOptions) { sp.edit().putString("fo_$id", o.toJson().toString()).apply(); bump() }
    fun resetFaceOptions(id: String) { sp.edit().remove("fo_$id").apply(); bump() }

    // ---------- modo reloj ----------
    var nightDim: Float
        get() = sp.getFloat("nightDim", 0.35f)
        set(v) { sp.edit().putFloat("nightDim", v).apply(); bump() }
    var nightRedShift: Boolean
        get() = sp.getBoolean("nightRed", false)
        set(v) { sp.edit().putBoolean("nightRed", v).apply(); bump() }

    // ---------- festivos ----------
    var holidayRegion: String
        get() = sp.getString("holReg", "madrid")!!
        set(v) { sp.edit().putString("holReg", v).apply(); bump() }
    fun customHolidays(): List<String> = arr("customHol").let { a -> (0 until a.length()).map { a.getString(it) } }.sorted()
    fun saveCustomHolidays(l: List<String>) = putArr("customHol", JSONArray(l.distinct().sorted()))
    fun removedHolidays(): List<String> = arr("removedHol").let { a -> (0 until a.length()).map { a.getString(it) } }
    fun saveRemovedHolidays(l: List<String>) = putArr("removedHol", JSONArray(l.distinct()))

    private fun seed() {
        savePresets(
            listOf(
                TimerPreset("Huevo pasado por agua", 6 * 60), TimerPreset("Huevo duro", 10 * 60),
                TimerPreset("Té verde", 3 * 60), TimerPreset("Pasta", 9 * 60),
                TimerPreset("Siesta", 20 * 60), TimerPreset("Concentración", 25 * 60),
            )
        )
        saveProgram(
            IntervalProgram(1, "Pomodoro", 0,
                listOf(IntervalStep("Trabajo", 25 * 60, Color.parseColor("#F08A1C")), IntervalStep("Pausa", 5 * 60, Color.parseColor("#3E9B5E"))),
                4, 0)
        )
        saveProgram(
            IntervalProgram(2, "HIIT 30/30", 120,
                listOf(IntervalStep("Esfuerzo", 30, Color.parseColor("#E4472B")), IntervalStep("Descanso", 30, Color.parseColor("#2E5FA8"))),
                10, 120)
        )
        saveProgram(
            IntervalProgram(3, "Fuerza: series", 0,
                listOf(IntervalStep("Serie", 45, Color.parseColor("#F4D40B")), IntervalStep("Descanso", 90, Color.parseColor("#8C8A84"))),
                5, 0)
        )
        saveZones(listOf("America/New_York", "Asia/Tokyo"))
        place = Place("Madrid", 40.4168, -3.7038)
        sp.edit().putBoolean("seeded", true).apply()
    }
}
