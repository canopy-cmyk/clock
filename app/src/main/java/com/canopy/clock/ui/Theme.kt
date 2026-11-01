package com.canopy.clock.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canopy.clock.R

object C {
    val Paper = Color(0xFFECEAE4)
    val Card = Color(0xFFF7F5F0)
    val Ink = Color(0xFF151515)
    val Muted = Color(0xFF7A7872)
    val Line = Color(0xFFD6D3CB)
    val Orange = Color(0xFFF08A1C)
    val Yellow = Color(0xFFF4D40B)
    val Green = Color(0xFF3E9B5E)
    val Red = Color(0xFFE4472B)
    val Blue = Color(0xFF2E5FA8)
}

val Grotesk = FontFamily(
    Font(R.font.grotesk_400, FontWeight.Normal),
    Font(R.font.grotesk_600, FontWeight.SemiBold),
    Font(R.font.grotesk_800, FontWeight.ExtraBold),
)

object T {
    val huge = TextStyle(fontFamily = Grotesk, fontFeatureSettings = "tnum", fontWeight = FontWeight.ExtraBold, fontSize = 64.sp, letterSpacing = (-2).sp, color = C.Ink)
    val big = TextStyle(fontFamily = Grotesk, fontFeatureSettings = "tnum", fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, letterSpacing = (-1).sp, color = C.Ink)
    val title = TextStyle(fontFamily = Grotesk, fontFeatureSettings = "tnum", fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = (-0.5).sp, color = C.Ink)
    val head = TextStyle(fontFamily = Grotesk, fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = C.Ink)
    val body = TextStyle(fontFamily = Grotesk, fontFeatureSettings = "tnum", fontWeight = FontWeight.Normal, fontSize = 15.sp, color = C.Ink)
    val small = TextStyle(fontFamily = Grotesk, fontFeatureSettings = "tnum", fontWeight = FontWeight.Normal, fontSize = 13.sp, color = C.Muted)
    val label = TextStyle(fontFamily = Grotesk, fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.6.sp, color = C.Muted)
}

@Composable
fun ClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Ink, onPrimary = C.Paper, secondary = C.Orange, background = C.Paper, surface = C.Card,
            onSurface = C.Ink, onBackground = C.Ink, surfaceVariant = C.Card, primaryContainer = C.Orange,
            onPrimaryContainer = C.Ink, secondaryContainer = C.Orange, tertiaryContainer = C.Yellow,
            surfaceContainerHighest = C.Card, surfaceContainerHigh = C.Card, surfaceContainer = C.Card,
            outline = C.Line,
        ),
        content = content
    )
}

/** Encabezado de pantalla: título grande a la izquierda y acción opcional a la derecha. */
@Composable
fun Header(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = T.title, modifier = Modifier.weight(1f))
        if (action != null) Pill(action, onClick = onAction)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = T.label, modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp))
}

@Composable
fun CardBox(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val m = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clip(RoundedCornerShape(18.dp)).background(C.Card)
    Box(if (onClick != null) m.clickable(onClick = onClick) else m) { content() }
}

/** Botón en forma de píldora: relleno (tinta) o con borde. */
@Composable
fun Pill(text: String, filled: Boolean = false, color: Color = C.Ink, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val base = modifier.clip(shape)
    val m = if (filled) base.background(color) else base.border(BorderStroke(1.5.dp, color), shape)
    Box(m.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
        Text(text, style = T.body.copy(fontWeight = FontWeight.SemiBold, color = if (filled) (if (color == C.Yellow || color == C.Orange) C.Ink else C.Paper) else color))
    }
}

/** Botón redondo grande, como los de un aparato. */
@Composable
fun RoundButton(text: String, color: Color, size: Dp = 76.dp, textColor: Color = C.Ink, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(color).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = T.body.copy(fontWeight = FontWeight.SemiBold, color = textColor))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier.clip(shape).then(if (selected) Modifier.background(C.Ink) else Modifier.border(1.dp, C.Line, shape))
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 7.dp)
    ) { Text(text, style = T.small.copy(color = if (selected) C.Paper else C.Ink, fontWeight = FontWeight.SemiBold)) }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.body)
            if (subtitle != null) Text(subtitle, style = T.small)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = C.Orange, checkedThumbColor = C.Ink, uncheckedTrackColor = C.Paper, uncheckedBorderColor = C.Line, uncheckedThumbColor = C.Muted)
        )
    }
}

@Composable
fun SliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, valueText: String, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) {
        Row { Text(title, style = T.body, modifier = Modifier.weight(1f)); Text(valueText, style = T.small) }
        Slider(
            value = value, onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = C.Ink, activeTrackColor = C.Orange, inactiveTrackColor = C.Line)
        )
    }
}

@Composable
fun Dot(color: Color, size: Dp = 10.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

@Composable
fun Gap(h: Dp = 12.dp) = Spacer(Modifier.height(h))

@Composable
fun HGap(w: Dp = 8.dp) = Spacer(Modifier.width(w))

@Composable
fun KeyValue(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, style = T.small)
        Text(v, style = T.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Lee la versión del almacén para que la pantalla se redibuje cuando cambian los datos. */
@Composable
fun storeVersion(): Int = com.canopy.clock.data.Store.version.collectAsState().value

@Composable
fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked, onCheckedChange = onChange, modifier = Modifier.padding(horizontal = 14.dp),
        colors = SwitchDefaults.colors(checkedTrackColor = C.Orange, checkedThumbColor = C.Ink, uncheckedTrackColor = C.Paper, uncheckedBorderColor = C.Line, uncheckedThumbColor = C.Muted)
    )
}
