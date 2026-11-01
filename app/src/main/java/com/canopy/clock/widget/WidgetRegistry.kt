package com.canopy.clock.widget


class WidgetFuncional : FaceWidget("funcional")
class WidgetIndices : FaceWidget("indices")
class WidgetGeometrico : FaceWidget("geometrico")
class WidgetDiscos : FaceWidget("discos")
class WidgetInstrumento : FaceWidget("instrumento")
class WidgetHerramienta : FaceWidget("herramienta")
class WidgetBloque : FaceWidget("bloque")
class WidgetLcd : FaceWidget("lcd")
class WidgetPersiana : FaceWidget("persiana")
class WidgetTubos : FaceWidget("tubos")
class WidgetVfd : FaceWidget("vfd")
class WidgetMatriz : FaceWidget("matriz")
class WidgetTipografico : FaceWidget("tipografico")
class WidgetPalabras : FaceWidget("palabras")
class WidgetBinario : FaceWidget("binario")

object WidgetRegistry {
    val PROVIDERS: List<Pair<String, Class<*>>> = listOf(
        "funcional" to WidgetFuncional::class.java,
        "indices" to WidgetIndices::class.java,
        "geometrico" to WidgetGeometrico::class.java,
        "discos" to WidgetDiscos::class.java,
        "instrumento" to WidgetInstrumento::class.java,
        "herramienta" to WidgetHerramienta::class.java,
        "bloque" to WidgetBloque::class.java,
        "lcd" to WidgetLcd::class.java,
        "persiana" to WidgetPersiana::class.java,
        "tubos" to WidgetTubos::class.java,
        "vfd" to WidgetVfd::class.java,
        "matriz" to WidgetMatriz::class.java,
        "tipografico" to WidgetTipografico::class.java,
        "palabras" to WidgetPalabras::class.java,
        "binario" to WidgetBinario::class.java
    )
}
