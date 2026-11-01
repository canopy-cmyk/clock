package com.canopy.clock.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.canopy.clock.data.Store
import com.canopy.clock.faces.FaceKind
import com.canopy.clock.faces.FaceOptions
import com.canopy.clock.faces.FaceSpec
import com.canopy.clock.faces.Faces
import com.canopy.clock.faces.Pal
import com.canopy.clock.widget.WidgetUpdater
import java.time.LocalDate
import java.time.LocalDateTime

private val FROZEN: LocalDateTime get() = LocalDate.now().atTime(10, 10, 30)

@Composable
fun GalleryScreen(onBack: () -> Unit) {
    storeVersion()
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = editing != null) { editing = null }
    val e = editing
    if (e != null) { FaceEditor(Faces.byId(e), onBack = { editing = null }); return }

    Column(Modifier.fillMaxSize()) {
        Header("Esferas", "Cerrar", onBack)
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
            for ((kind, title) in listOf(FaceKind.ANALOG to "Analógicos", FaceKind.DIGITAL to "Digitales")) {
                item(span = { GridItemSpan(2) }) { SectionLabel(title, Modifier.padding(start = 0.dp)) }
                items(Faces.ALL.filter { it.kind == kind }, key = { it.id }) { spec -> FaceTile(spec) { editing = spec.id } }
            }
        }
    }
}

@Composable
private fun FaceTile(spec: FaceSpec, onClick: () -> Unit) {
    val selected = Store.faceId == spec.id
    Column(
        Modifier.padding(6.dp).clip(RoundedCornerShape(18.dp)).background(C.Card)
            .then(if (selected) Modifier.border(2.dp, C.Orange, RoundedCornerShape(18.dp)) else Modifier)
            .clickable(onClick = onClick).padding(10.dp)
    ) {
        LiveFace(spec, Store.faceOptions(spec.id), Modifier.fillMaxWidth().aspectRatio(1f), frozen = FROZEN)
        Text(spec.name, style = T.head, modifier = Modifier.padding(top = 6.dp))
        Text(spec.blurb, style = T.small, maxLines = 2)
    }
}

@Composable
private fun FaceEditor(spec: FaceSpec, onBack: () -> Unit) {
    val ctx = LocalContext.current
    storeVersion()
    val o = Store.faceOptions(spec.id)
    fun save(n: FaceOptions) { Store.saveFaceOptions(spec.id, n); WidgetUpdater.updateAll(ctx) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header(spec.name, "Listo", onBack)
        Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
            LiveFace(spec, o, Modifier.fillMaxWidth().aspectRatio(if (spec.wide) 1.25f else 1f))
        }
        Text(spec.blurb, style = T.body, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(if (Store.faceId == spec.id) "En pantalla Reloj ✓" else "Usar en pantalla Reloj", filled = Store.faceId == spec.id) { Store.faceId = spec.id }
            Pill(if (Store.nightFaceId == spec.id) "En modo reloj ✓" else "Usar en modo reloj", filled = Store.nightFaceId == spec.id, color = C.Ink) { Store.nightFaceId = spec.id }
        }

        SectionLabel("Colores")
        if (spec.colorLabels[0].isNotBlank()) SwatchRow(spec.colorLabels[0], o.bg) { save(o.copy(bg = it)) }
        SwatchRow(spec.colorLabels[1], o.ink) { save(o.copy(ink = it)) }
        if (spec.colorLabels[2].isNotBlank()) SwatchRow(spec.colorLabels[2], o.accent) { save(o.copy(accent = it)) }

        SectionLabel("Funcionamiento")
        CardBox {
            Column {
                if (spec.hasSeconds) SwitchRow("Segundos", "Mostrar el segundero o los segundos", o.showSeconds) { save(o.copy(showSeconds = it)) }
                if (spec.hasSeconds && spec.kind == FaceKind.ANALOG)
                    SwitchRow("Movimiento continuo", "Desactivado: salta cada segundo, como un cuarzo", o.sweep) { save(o.copy(sweep = it)) }
                SwitchRow("Formato 24 horas", null, o.h24) { save(o.copy(h24 = it)) }
                SwitchRow("Tic-tac", "Suena cada segundo con la app abierta en Reloj", o.tick) { save(o.copy(tick = it)) }
            }
        }
        Text(
            "El widget «Reloj · ${spec.name}» usa estos mismos colores. Añádelo desde la pantalla de inicio.",
            style = T.small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        Box(Modifier.padding(horizontal = 16.dp)) { Pill("Restaurar colores originales") { Store.resetFaceOptions(spec.id); WidgetUpdater.updateAll(ctx) } }
        Gap(28.dp)
    }
}

@Composable
private fun SwatchRow(label: String, current: Int, onPick: (Int) -> Unit) {
    Text(label, style = T.body, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
        items(Pal.SWATCHES) { c ->
            val sel = c == current
            Box(
                Modifier.padding(4.dp).size(36.dp).clip(CircleShape).background(Color(c))
                    .border(if (sel) 3.dp else 1.dp, if (sel) C.Orange else C.Line, CircleShape)
                    .clickable { onPick(c) },
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}
