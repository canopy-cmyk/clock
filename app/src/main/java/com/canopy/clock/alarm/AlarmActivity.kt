package com.canopy.clock.alarm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canopy.clock.data.Store
import com.canopy.clock.faces.Faces
import com.canopy.clock.faces.Fonts
import com.canopy.clock.ui.C
import com.canopy.clock.ui.ClockTheme
import com.canopy.clock.ui.LiveFace
import com.canopy.clock.ui.RoundButton
import com.canopy.clock.ui.T

/** Pantalla que aparece cuando suena una alarma o termina un temporizador. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this); Fonts.init(this)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            ClockTheme {
                val ring by RingService.state.collectAsState()
                var seen = ring != null
                LaunchedEffect(ring) { if (ring == null) finish() }
                val r = ring
                Box(Modifier.fillMaxSize().background(C.Paper).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        val spec = Faces.byId(Store.faceId)
                        LiveFace(spec, Store.faceOptions(spec.id), Modifier.fillMaxWidth(0.8f).aspectRatio(1f))
                        Text(r?.subtitle ?: "", style = T.title)
                        Text(r?.title ?: "", style = T.head.copy(color = C.Muted))
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.padding(top = 24.dp)) {
                            if (r?.canSnooze == true) RoundButton("Posponer", C.Card, 120.dp) { RingService.snooze(this@AlarmActivity) }
                            RoundButton("Detener", C.Orange, 120.dp) { RingService.dismiss(this@AlarmActivity) }
                        }
                    }
                }
                if (!seen) seen = true
            }
        }
    }
}
