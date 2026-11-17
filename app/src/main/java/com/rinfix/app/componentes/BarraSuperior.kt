package com.rinfix.app.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores

/**
 * Barra de arriba de las pantallas internas: botón para volver, título centrado
 * y un espacio opcional a la derecha para una acción (por ejemplo, compartir).
 * Sin `alVolver` no se muestra el botón de volver.
 */
@Composable
fun BarraSuperior(
    modifier: Modifier = Modifier,
    titulo: String = "",
    alVolver: (() -> Unit)? = null,
    accion: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp),
    ) {
        if (alVolver != null) {
            BotonIcono(icono = Iconos.Atras, descripcion = "Volver", alPulsar = alVolver)
        } else {
            Box(modifier = Modifier.size(44.dp))
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = Colores.Tinta,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            accion?.invoke()
        }
    }
}
