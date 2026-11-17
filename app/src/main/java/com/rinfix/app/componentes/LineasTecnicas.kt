package com.rinfix.app.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores

/**
 * Trazos de plano técnico: circunferencias de medida, ejes y una cota abajo.
 * Es un fondo decorativo para bloques de color (por ejemplo, la tarjeta lavanda de bienvenida);
 * no lo anuncia el lector de pantalla.
 */
@Composable
fun LineasTecnicas(
    tamano: Dp,
    modifier: Modifier = Modifier,
    color: Color = Colores.LavandaTexto.copy(alpha = 0.22f),
) {
    Canvas(modifier = modifier.size(tamano)) {
        val lado = size.minDimension
        val c = lado / 2f
        val centro = Offset(c, c)
        val trazo = 1.dp.toPx()
        val punteadoLargo = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx()))
        val punteadoCorto = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))

        // Circunferencias de medida
        drawCircle(color, radius = c * 0.96f, center = centro, style = Stroke(trazo, pathEffect = punteadoLargo))
        drawCircle(color, radius = c * 0.78f, center = centro, style = Stroke(trazo))
        drawCircle(color, radius = c * 0.5f, center = centro, style = Stroke(trazo, pathEffect = punteadoCorto))

        // Ejes
        val borde = 4.dp.toPx()
        drawLine(color, Offset(c, borde), Offset(c, lado - borde), strokeWidth = trazo)
        drawLine(color, Offset(borde, c), Offset(lado - borde, c), strokeWidth = trazo)

        // Cota del diámetro, abajo
        val izquierda = c - c * 0.78f
        val derecha = c + c * 0.78f
        val linea = lado - 12.dp.toPx()
        drawLine(color, Offset(izquierda, linea), Offset(derecha, linea), strokeWidth = trazo)
        drawLine(color, Offset(izquierda, lado - 18.dp.toPx()), Offset(izquierda, lado - 6.dp.toPx()), strokeWidth = trazo)
        drawLine(color, Offset(derecha, lado - 18.dp.toPx()), Offset(derecha, lado - 6.dp.toPx()), strokeWidth = trazo)
    }
}

@Preview
@Composable
private fun VistaPreviaLineas() {
    Box(modifier = Modifier.background(Colores.Lavanda).padding(16.dp)) {
        LineasTecnicas(tamano = 260.dp)
    }
}
