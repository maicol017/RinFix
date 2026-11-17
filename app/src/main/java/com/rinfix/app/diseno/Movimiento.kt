package com.rinfix.app.diseno

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp

/**
 * Curvas y tiempos de animación de RinFix, iguales a los del prototipo.
 * Si el teléfono tiene «Quitar animaciones», Android las reduce automáticamente.
 */
object Movimiento {

    /** Arranca rápido y frena suave al llegar. La curva por defecto de la app. */
    val Suave = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

    /** Se pasa un poco del destino y vuelve. Para cosas que «aparecen» con energía. */
    val Rebote = CubicBezierEasing(0.34f, 1.5f, 0.64f, 1f)

    const val Rapido = 150
    const val Normal = 250
    const val Lento = 450

    /** Retraso entre un elemento y el siguiente en una entrada escalonada. */
    const val Escalon = 70
}

/**
 * Devuelve `false` en el primer cuadro y `true` justo después.
 * Sirve para disparar la entrada escalonada de una pantalla al abrirla.
 * En la vista previa de Android Studio devuelve `true` de una vez, para que se vea el contenido.
 */
@Composable
fun recordarEntrada(): Boolean {
    if (LocalInspectionMode.current) return true
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    return visible
}

/**
 * Entrada escalonada: el elemento sube 16 dp y aparece.
 * `orden` define el turno: cada paso espera 70 ms más que el anterior.
 */
fun Modifier.aparecer(visible: Boolean, orden: Int = 0): Modifier = composed {
    val progreso by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = Movimiento.Lento,
            delayMillis = if (visible) orden * Movimiento.Escalon else 0,
            easing = Movimiento.Suave,
        ),
        label = "entrada",
    )
    graphicsLayer {
        alpha = progreso
        translationY = (1f - progreso) * 16.dp.toPx()
    }
}
