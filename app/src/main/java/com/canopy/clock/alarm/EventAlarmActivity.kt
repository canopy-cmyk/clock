package com.canopy.clock.alarm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.canopy.clock.data.Alarm
import com.canopy.clock.data.Store
import com.canopy.clock.widget.WidgetUpdater
import java.time.Instant
import java.time.ZoneId

/**
 * Alarmas pedidas por la app Calendario para un evento concreto.
 *  - extra "key": identificador del evento (una alarma por evento; si ya existe se sustituye)
 *  - extra "at": instante en milisegundos
 *  - extra "label": texto de la alarma
 *  - extra "remove": true para quitarla
 * Es una actividad invisible que se abre con startActivityForResult: así Clock sabe qué app la llama
 * y solo acepta a Calendario (com.pemau.calendario), sin depender del orden de instalación.
 */
class EventAlarmActivity : Activity() {
    companion object {
        const val ACTION = "com.canopy.clock.EVENT_ALARM"
        private val ALLOWED = setOf("com.pemau.calendario")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ok = callingPackage in ALLOWED && intent?.action == ACTION &&
            runCatching { apply(applicationContext, intent) }.getOrDefault(false)
        setResult(if (ok) RESULT_OK else RESULT_CANCELED)
        finish()
    }

    private fun apply(ctx: Context, i: Intent): Boolean {
        Store.init(ctx)
        val key = i.getStringExtra("key") ?: return false
        val existing = Store.alarms().firstOrNull { it.eventKey == key }
        if (i.getBooleanExtra("remove", false)) {
            if (existing != null) {
                Scheduler.cancel(ctx, existing.id)
                Store.deleteAlarm(existing.id)
            }
        } else {
            val at = i.getLongExtra("at", 0L)
            if (at <= System.currentTimeMillis()) return false
            val t = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDateTime()
            // mismo tono y posposición que la alarma más reciente del usuario
            val model = existing ?: Store.alarms().maxByOrNull { it.id }
            val base = existing ?: Alarm(Store.nextId(), t.hour, t.minute)
            val a = base.copy(
                hour = t.hour, minute = t.minute, days = emptySet(), enabled = true,
                label = (i.getStringExtra("label") ?: "").take(60),
                tone = model?.tone ?: base.tone, snoozeMin = model?.snoozeMin ?: base.snoozeMin,
                date = t.toLocalDate().toString(), eventKey = key, skipDate = null,
            )
            Store.saveAlarm(a)
            Scheduler.schedule(ctx, a)
        }
        runCatching { WidgetUpdater.updateAll(ctx) }
        return true
    }
}
