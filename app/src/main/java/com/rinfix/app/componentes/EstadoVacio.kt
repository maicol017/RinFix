package com.rinfix.app.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix

/**
 * Pantalla o sección sin contenido todavía. Dice qué falta y ofrece la acción para empezar,
 * en lugar de dejar un hueco.
 */
@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    textoAccion: String? = null,
    iconoAccion: ImageVector? = null,
    alPulsarAccion: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 28.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Colores.Arena),
        ) {
            LineasTecnicas(tamano = 96.dp, color = Colores.Tinta.copy(alpha = 0.08f))
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = Colores.Tinta2,
                modifier = Modifier.size(40.dp),
            )
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            color = Colores.Tinta,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.Tinta2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(top = 6.dp),
        )
        if (textoAccion != null && alPulsarAccion != null) {
            Boton(
                texto = textoAccion,
                alPulsar = alPulsarAccion,
                icono = iconoAccion,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaEstadoVacio() {
    TemaRinFix {
        EstadoVacio(
            icono = Iconos.Carro,
            titulo = "Aún no tienes vehículos",
            mensaje = "Agrega tu carro para que el análisis tenga en cuenta el tamaño y el material del rin.",
            textoAccion = "Agregar vehículo",
            iconoAccion = Iconos.Agregar,
            alPulsarAccion = {},
        )
    }
}
