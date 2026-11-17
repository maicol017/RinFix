package com.rinfix.app.diseno

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Bordes redondeados de RinFix. El radio depende de la jerarquía:
 * entre más importante y grande es el bloque, más redondeado.
 */
object Formas {

    /** Tarjetas principales, como el servicio activo en Inicio. */
    val Principal = RoundedCornerShape(28.dp)

    /** Tarjetas normales. */
    val Tarjeta = RoundedCornerShape(22.dp)

    /** Campos de texto y bloques internos de una tarjeta. */
    val Campo = RoundedCornerShape(16.dp)

    /** Miniaturas de fotos y elementos pequeños. */
    val Pequena = RoundedCornerShape(12.dp)

    /** Botones, etiquetas de estado y la barra inferior. */
    val Pildora = RoundedCornerShape(percent = 50)

    /** Hojas que suben desde abajo: solo se redondean las esquinas de arriba. */
    val Hoja = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

/** Formas que usa el tema para los componentes de Material. */
val FormasRinFix = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = Formas.Pequena,
    medium = Formas.Campo,
    large = Formas.Tarjeta,
    extraLarge = Formas.Principal,
)
