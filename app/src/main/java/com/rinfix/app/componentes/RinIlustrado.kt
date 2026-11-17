package com.rinfix.app.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rin de aleación dibujado en código: llanta, aro metálico, cinco rayos, tuercas y centro menta.
 * Reemplaza a las fotos de ejemplo en bienvenida, análisis y seguimiento, así la app no depende
 * de imágenes de terceros y se ve nítida en cualquier pantalla.
 *
 * - `conDano` dibuja un rayón en el borde superior derecho, como el del prototipo.
 * - `inclinacion` gira los rayos unos grados para que cada rin se vea distinto.
 *
 * Se dibuja sobre una cuadrícula de 200 × 200 con el centro en (100, 100).
 */
@Composable
fun RinIlustrado(
    tamano: Dp,
    modifier: Modifier = Modifier,
    conDano: Boolean = false,
    inclinacion: Float = 0f,
    descripcion: String = "Rin de aleación",
) {
    Box(
        modifier = modifier
            .size(tamano)
            .semantics { contentDescription = descripcion }
            .drawWithCache {
                val piezas = PiezasRin()
                onDrawBehind {
                    // La cuadrícula del dibujo va de -6 a 206 para dejar margen a la llanta.
                    val escala = size.minDimension / 212f
                    withTransform({
                        translate(left = 6f * escala, top = 6f * escala)
                        scale(scaleX = escala, scaleY = escala, pivot = Offset.Zero)
                    }) {
                        dibujarRin(piezas, conDano, inclinacion)
                    }
                }
            },
    )
}

/** Punto sobre una circunferencia: el ángulo se mide desde las 12 en punto, en el sentido del reloj. */
private fun punto(radio: Float, angulo: Float): Offset {
    val rad = Math.toRadians(angulo.toDouble())
    return Offset((100 + radio * sin(rad)).toFloat(), (100 - radio * cos(rad)).toFloat())
}

private val Centro = Offset(100f, 100f)

/** Pinceles y caminos que no cambian entre cuadros; se crean una sola vez. */
private class PiezasRin {
    val llanta = Brush.radialGradient(
        0.76f to Color(0xFF2C2824),
        0.90f to Color(0xFF171513),
        1.00f to Color(0xFF0B0A09),
        center = Centro,
        radius = 98f,
    )
    val labio = Brush.linearGradient(
        0.00f to Color(0xFFFFFFFF),
        0.33f to Color(0xFFD8D3CB),
        0.55f to Color(0xFFF5F2ED),
        0.82f to Color(0xFF9F988E),
        1.00f to Color(0xFFD5CFC6),
        start = Offset(20f, 20f),
        end = Offset(180f, 180f),
    )
    val aroInterior = Brush.linearGradient(
        0.0f to Color(0xFF8B847A),
        0.5f to Color(0xFFD9D4CC),
        1.0f to Color(0xFF726B64),
        start = Offset(174.6f, 25.4f),
        end = Offset(25.4f, 174.6f),
    )
    val pozo = Brush.radialGradient(
        0.55f to Color(0xFF2D2A26),
        1.00f to Color(0xFF0E0D0B),
        center = Centro,
        radius = 70.2f,
    )
    val disco = Brush.radialGradient(
        0f to Color(0xFF958E85),
        1f to Color(0xFF4A4540),
        center = Offset(91.2f, 86.8f),
        radius = 68.2f,
    )
    val rayo = Brush.horizontalGradient(
        0.00f to Color(0xFF857E74),
        0.42f to Color(0xFFEBE7E1),
        0.50f to Color(0xFFFFFFFF),
        0.63f to Color(0xFFCDC7BE),
        1.00f to Color(0xFF8C857B),
        startX = 88f,
        endX = 112f,
    )
    val cubo = Brush.radialGradient(
        0f to Color(0xFFFFFFFF),
        1f to Color(0xFFAEA79D),
        center = Offset(94.24f, 91.36f),
        radius = 34.56f,
    )
    val formaRayo: Path = camino("M93 77.5 L88 33 Q100 28.6 112 33 L107 77.5 Q100 80 93 77.5Z")
    val ventana: Path = camino("M55.6 62.7 A58 58 0 0 1 85 44 L87.6 53.6 A48 48 0 0 0 63.2 69.1 Z")
    val rayon: Path = Path().apply {
        for (k in 0 until 15) {
            val p = punto(78.6f + if (k % 2 == 1) 1.5f else -1.3f, 22f + k * 4.1f)
            if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
    }
}

private fun camino(datos: String): Path = PathParser().parsePathString(datos).toPath()

private fun DrawScope.arco(radio: Float, desde: Float, hasta: Float, color: Color, grosor: Float) {
    drawArc(
        color = color,
        startAngle = desde - 90f,
        sweepAngle = hasta - desde,
        useCenter = false,
        topLeft = Offset(100f - radio, 100f - radio),
        size = androidx.compose.ui.geometry.Size(radio * 2, radio * 2),
        style = Stroke(width = grosor, cap = StrokeCap.Round),
    )
}

private fun DrawScope.dibujarRin(p: PiezasRin, conDano: Boolean, t: Float) {
    // Llanta
    drawCircle(brush = p.llanta, radius = 98f, center = Centro)
    drawCircle(color = Color(0xFF35312C), radius = 92.6f, center = Centro, style = Stroke(1.4f))
    arco(95.3f, -140f, -48f, Color.White.copy(alpha = 0.16f), 3f)

    // Aro, pozo y disco
    drawCircle(brush = p.labio, radius = 80f, center = Centro)
    drawCircle(brush = p.aroInterior, radius = 74.6f, center = Centro)
    drawCircle(brush = p.pozo, radius = 70.2f, center = Centro)
    drawCircle(brush = p.disco, radius = 55f, center = Centro)
    drawCircle(color = Color(0xFF3A3631), radius = 34f, center = Centro)
    for (i in 0 until 22) {
        drawCircle(
            color = Color(0xFF2A2723).copy(alpha = 0.7f),
            radius = 1.6f,
            center = punto(46f, i * 16.36f + t),
        )
    }
    drawPath(p.ventana, color = Color(0xFF26231F))
    drawPath(p.ventana, color = Color(0xFF4A453F), style = Stroke(0.6f))

    // Rayos
    for (i in 0 until 5) {
        rotate(degrees = i * 72f + t, pivot = Centro) {
            drawPath(p.formaRayo, brush = p.rayo)
            drawLine(Color.White.copy(alpha = 0.65f), Offset(100f, 76f), Offset(100f, 31f), strokeWidth = 1.1f)
            drawLine(Color(0xFF6F685F).copy(alpha = 0.55f), Offset(93f, 77.5f), Offset(88f, 33f), strokeWidth = 0.8f)
            drawLine(Color.White.copy(alpha = 0.4f), Offset(107f, 77.5f), Offset(112f, 33f), strokeWidth = 0.8f)
        }
    }

    // Brillo del aro
    drawCircle(color = Color(0xFFCFC9C0), radius = 69.6f, center = Centro, style = Stroke(1.6f))
    arco(77.6f, -152f, -62f, Color.White.copy(alpha = 0.9f), 2.2f)

    // Rayón en el borde
    if (conDano) {
        drawPath(
            p.rayon,
            color = Color(0xFF6A6056),
            style = Stroke(width = 3.4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            p.rayon,
            color = Color(0xFFFBF6EE),
            style = Stroke(
                width = 1f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.5f, 3.5f)),
            ),
        )
        for (a in listOf(30f, 44f, 58f, 71f)) {
            drawLine(
                color = Color(0xFF5D544B),
                start = punto(76.5f, a),
                end = punto(80.2f, a + 2.5f),
                strokeWidth = 1.2f,
                cap = StrokeCap.Round,
            )
        }
    }

    // Centro, tuercas y tapa menta
    drawCircle(brush = p.cubo, radius = 24f, center = Centro)
    drawCircle(color = Color(0xFF99928A), radius = 24f, center = Centro, style = Stroke(0.8f))
    for (i in 0 until 5) {
        val c = punto(15f, i * 72f + 36f + t)
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFFF4F1EC),
                1f to Color(0xFF7D766D),
                center = Offset(c.x - 1.02f, c.y - 1.36f),
                radius = 4.76f,
            ),
            radius = 3.4f,
            center = c,
        )
        drawCircle(color = Color(0xFF7A736A), radius = 3.4f, center = c, style = Stroke(0.5f))
    }
    drawCircle(color = Color(0xFF030302), radius = 9.5f, center = Centro)
    drawCircle(color = Color(0xFF9BD8A9), radius = 4.2f, center = Centro)
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaRin() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        RinIlustrado(tamano = 160.dp)
        RinIlustrado(tamano = 160.dp, conDano = true, inclinacion = 12f)
    }
}
