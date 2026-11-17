package com.rinfix.app.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import kotlin.math.cos
import kotlin.math.sin

/**
 * Logo plano de RinFix: disco tinta, aro y cinco rayos crema, centro menta.
 * Es el mismo dibujo del ícono de la app y del final de la animación de apertura.
 */
@Composable
fun Logo(
    tamano: Dp,
    modifier: Modifier = Modifier,
    fondo: Color = Colores.Tinta,
    trazo: Color = Colores.Crema,
    centro: Color = Colores.Menta,
) {
    Canvas(
        modifier = modifier
            .size(tamano)
            .semantics { contentDescription = "RinFix" },
    ) {
        val u = size.minDimension / 100f
        val c = center
        drawCircle(color = fondo, radius = 48f * u, center = c)
        drawCircle(color = trazo, radius = 36f * u, center = c, style = Stroke(width = 6f * u))
        for (i in 0 until 5) {
            val angulo = Math.toRadians(i * 72.0)
            val dx = sin(angulo).toFloat()
            val dy = -cos(angulo).toFloat()
            drawLine(
                color = trazo,
                start = Offset(c.x + 13f * u * dx, c.y + 13f * u * dy),
                end = Offset(c.x + 31f * u * dx, c.y + 31f * u * dy),
                strokeWidth = 7f * u,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(color = centro, radius = 9f * u, center = c)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaLogo() {
    Logo(tamano = 96.dp)
}
