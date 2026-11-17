package com.rinfix.app.componentes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.TemaRinFix

/** Fondo de la foto: dónde parece que se tomó. */
enum class TonoFoto(
    internal val colores: Array<Pair<Float, Color>>,
    internal val luzX: Float,
    internal val luzY: Float,
) {
    /** Garaje con poca luz, como las fotos que toma el cliente. */
    Taller(arrayOf(0f to Color(0xFF7A7166), 0.45f to Color(0xFF423C35), 1f to Color(0xFF1C1916)), 0.28f, 0.12f),

    /** Banco de trabajo del taller. */
    Banco(arrayOf(0f to Color(0xFF8F8475), 0.52f to Color(0xFF4C453D), 1f to Color(0xFF24201C)), 0.72f, 0.08f),

    /** Fondo claro, como un rin ya reparado. */
    Clara(arrayOf(0f to Color(0xFFF4EEE6), 0.58f to Color(0xFFD3CABD), 1f to Color(0xFFACA295)), 0.5f, 0f),

    /** Estudio blanco cálido. */
    Estudio(arrayOf(0f to Color(0xFFFFFFFF), 0.7f to Color(0xFFF3E6D6), 1f to Color(0xFFE9D7C2)), 0.5f, 0.35f),
}

/**
 * «Foto» de un rin dibujada: el rin ilustrado sobre un fondo con luz, piso y viñeta.
 * Se usa mientras no hay fotos reales (datos de ejemplo); cuando las haya, mostrará la imagen.
 *
 * Si `descripcion` es `null`, es decorativa y el lector de pantalla la salta.
 */
@Composable
fun FotoRin(
    modifier: Modifier = Modifier,
    ancho: Dp = 78.dp,
    alto: Dp = 78.dp,
    tamanoRin: Dp = 70.dp,
    tono: TonoFoto = TonoFoto.Clara,
    inclinacion: Float = 0f,
    conDano: Boolean = false,
    radio: Dp = 18.dp,
    descripcion: String? = null,
) {
    val sombraRin = Shadow(
        radius = tamanoRin * 0.09f,
        color = Color.Black.copy(alpha = 0.42f),
        offset = DpOffset(x = 0.dp, y = tamanoRin * 0.075f),
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(ancho, alto)
            .clip(RoundedCornerShape(radio))
            .drawBehind {
                val lado = maxOf(size.width, size.height)
                // Luz de fondo
                drawRect(
                    Brush.radialGradient(
                        *tono.colores,
                        center = Offset(size.width * tono.luzX, size.height * tono.luzY),
                        radius = lado * 1.15f,
                    ),
                )
                // Sombra del piso bajo el rin
                drawRect(
                    Brush.radialGradient(
                        0f to Color.Black.copy(alpha = 0.32f),
                        1f to Color.Transparent,
                        center = Offset(size.width / 2, size.height * 0.98f),
                        radius = size.width * 0.45f,
                    ),
                )
            }
            .then(
                if (descripcion == null) {
                    Modifier.clearAndSetSemantics { }
                } else {
                    Modifier.clearAndSetSemantics {
                        contentDescription = descripcion
                        role = Role.Image
                    }
                },
            ),
    ) {
        RinIlustrado(
            tamano = tamanoRin,
            conDano = conDano,
            inclinacion = inclinacion,
            modifier = Modifier.dropShadow(CircleShape, sombraRin),
        )
        // Viñeta: bordes un poco más oscuros, como una foto real.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.3f),
                            center = center,
                            radius = maxOf(size.width, size.height) * 0.75f,
                        ),
                    )
                },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaFotoRin() {
    TemaRinFix {
        Row(modifier = Modifier.padding(16.dp)) {
            FotoRin(tono = TonoFoto.Clara, inclinacion = 30f)
            FotoRin(
                tono = TonoFoto.Taller,
                conDano = true,
                inclinacion = 20f,
                modifier = Modifier.padding(start = 10.dp),
            )
            FotoRin(
                tono = TonoFoto.Banco,
                conDano = true,
                inclinacion = 10f,
                modifier = Modifier.padding(start = 10.dp),
            )
            FotoRin(tono = TonoFoto.Estudio, modifier = Modifier.padding(start = 10.dp))
        }
    }
}
