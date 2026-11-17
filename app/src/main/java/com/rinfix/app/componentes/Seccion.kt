package com.rinfix.app.componentes

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores

/**
 * Línea punteada en el borde de arriba, para separar el pie de una tarjeta
 * (entrega, acciones) del contenido principal.
 */
fun Modifier.lineaPunteadaArriba(color: Color = Colores.Linea2): Modifier = drawBehind {
    drawLine(
        color = color,
        start = Offset.Zero,
        end = Offset(size.width, 0f),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
    )
}

/**
 * Título de una sección con una acción opcional a la derecha, por ejemplo
 * «Reparación en curso» con «Ver todas». El lector de pantalla lo anuncia como encabezado.
 */
@Composable
fun EncabezadoSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    alPulsarAccion: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(horizontal = 2.dp),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            color = Colores.Tinta,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (accion != null && alPulsarAccion != null) {
            Enlace(
                texto = accion,
                alPulsar = alPulsarAccion,
                estilo = MaterialTheme.typography.bodyMedium,
                color = Colores.Tinta2,
            )
        }
    }
}
