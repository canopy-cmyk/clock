package com.canopy.clock.widget

class WidgetFuncional : FaceWidget("funcional")
class WidgetInstrumento : FaceWidget("instrumento")
class WidgetBloque : FaceWidget("bloque")
class WidgetPedestal : FaceWidget("pedestal")
class WidgetMadera : FaceWidget("madera")
class WidgetLcd : FaceWidget("lcd")
class WidgetTubos : FaceWidget("tubos")
class WidgetMatriz : FaceWidget("matriz")
class WidgetCubo : FaceWidget("cubo")
class WidgetRadio : FaceWidget("radio")
class WidgetTorre : FaceWidget("torre")
class WidgetTelevisor : FaceWidget("televisor")

object WidgetRegistry {
    val PROVIDERS: List<Pair<String, Class<*>>> = listOf(
        "funcional" to WidgetFuncional::class.java,
        "instrumento" to WidgetInstrumento::class.java,
        "bloque" to WidgetBloque::class.java,
        "pedestal" to WidgetPedestal::class.java,
        "madera" to WidgetMadera::class.java,
        "lcd" to WidgetLcd::class.java,
        "tubos" to WidgetTubos::class.java,
        "matriz" to WidgetMatriz::class.java,
        "cubo" to WidgetCubo::class.java,
        "radio" to WidgetRadio::class.java,
        "torre" to WidgetTorre::class.java,
        "televisor" to WidgetTelevisor::class.java
    )
}
