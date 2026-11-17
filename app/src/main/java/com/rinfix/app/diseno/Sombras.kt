package com.rinfix.app.diseno

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Sombras de RinFix, iguales a las del prototipo.
 *
 * Cada nivel tiene dos capas: una sombra corta y nítida pegada al borde
 * y otra larga y difusa que da la sensación de altura. Usan el tono de la
 * tinta en lugar de negro puro, para que se vean cálidas sobre el fondo crema.
 */
enum class Sombra(internal val capas: List<Shadow>) {

    /** Sin sombra. */
    Ninguna(emptyList()),

    /** Tarjetas apoyadas sobre el fondo. */
    Baja(
        listOf(
            capa(radio = 1, desplazamiento = 1, opacidad = 0.03f),
            capa(radio = 8, desplazamiento = 2, opacidad = 0.05f),
        ),
    ),

    /** Tarjetas destacadas y botones flotantes. */
    Media(
        listOf(
            capa(radio = 2, desplazamiento = 1, opacidad = 0.05f),
            capa(radio = 28, desplazamiento = 14, opacidad = 0.22f, expansion = -14),
        ),
    ),

    /** Elementos que flotan sobre todo: la barra inferior, hojas y diálogos. */
    Alta(
        listOf(
            capa(radio = 4, desplazamiento = 2, opacidad = 0.08f),
            capa(radio = 50, desplazamiento = 28, opacidad = 0.55f, expansion = -22),
        ),
    ),
}

/** Crea una capa de sombra. Las medidas están en dp. */
private fun capa(radio: Int, desplazamiento: Int, opacidad: Float, expansion: Int = 0) = Shadow(
    radius = radio.dp,
    spread = expansion.dp,
    color = Colores.Tinta.copy(alpha = opacidad),
    offset = DpOffset(x = 0.dp, y = desplazamiento.dp),
)

/**
 * Dibuja la sombra detrás del elemento con su forma.
 * Va antes del fondo en la cadena de modificadores, por ejemplo:
 * `Modifier.sombra(Sombra.Media, Formas.Tarjeta).background(Colores.Papel, Formas.Tarjeta)`.
 */
fun Modifier.sombra(sombra: Sombra, forma: Shape): Modifier =
    sombra.capas.fold(this) { modificador, capa -> modificador.dropShadow(forma, capa) }
