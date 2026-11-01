package com.canopy.clock

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canopy.clock.alarm.Scheduler
import com.canopy.clock.alarm.TimerNotifier
import com.canopy.clock.data.Store
import com.canopy.clock.faces.Fonts
import com.canopy.clock.ui.AlarmsScreen
import com.canopy.clock.ui.C
import com.canopy.clock.ui.ClockScreen
import com.canopy.clock.ui.ClockTheme
import com.canopy.clock.ui.Dot
import com.canopy.clock.ui.GalleryScreen
import com.canopy.clock.ui.IntervalsScreen
import com.canopy.clock.ui.SettingsScreen
import com.canopy.clock.ui.StopwatchScreen
import com.canopy.clock.ui.T
import com.canopy.clock.ui.TimerScreen
import com.canopy.clock.widget.WidgetUpdater

class ClockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Fonts.init(this)
        Scheduler.channels(this)
    }
}

class MainActivity : ComponentActivity() {
    private var startTab by mutableStateOf("clock")

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startTab = intent?.getStringExtra("tab") ?: "clock"
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { ClockTheme { Root(startTab) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("tab")?.let { startTab = it }
    }

    override fun onResume() {
        super.onResume()
        // cada vez que se abre la app se recalculan alarmas (por si cambió el calendario) y widgets
        Scheduler.scheduleAll(this)
        Scheduler.rescheduleTimers(this)
        TimerNotifier.update(this)
        WidgetUpdater.updateAll(this)
        WidgetUpdater.scheduleTick(this)
    }
}

private val TABS = listOf(
    "clock" to "Reloj", "alarms" to "Alarmas", "timer" to "Temporizador", "intervals" to "Intervalos", "stopwatch" to "Crono"
)

@Composable
private fun Root(startTab: String) {
    var tab by rememberSaveable(startTab) { mutableStateOf(startTab) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = overlay != null) { overlay = null }

    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (overlay) {
                "gallery" -> GalleryScreen(onBack = { overlay = null })
                "settings" -> SettingsScreen(onBack = { overlay = null })
                else -> when (tab) {
                    "alarms" -> AlarmsScreen()
                    "timer" -> TimerScreen()
                    "intervals" -> IntervalsScreen()
                    "stopwatch" -> StopwatchScreen()
                    else -> ClockScreen(onGallery = { overlay = "gallery" }, onSettings = { overlay = "settings" })
                }
            }
        }
        if (overlay == null) {
            HorizontalDivider(color = C.Line)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                for ((id, label) in TABS) {
                    val sel = id == tab
                    Column(
                        Modifier.weight(1f).clickable { tab = id }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Dot(if (sel) C.Orange else C.Paper, 7.dp)
                        Text(
                            label, maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
                            style = T.small.copy(fontSize = 11.5.sp, color = if (sel) C.Ink else C.Muted, fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
