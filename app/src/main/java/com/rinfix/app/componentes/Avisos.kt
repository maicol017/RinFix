package com.rinfix.app.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.Notificacion
import com.rinfix.app.datos.TipoNotificacion
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas

/** Campana de notificaciones con el número de avisos sin leer. La usan el Inicio y el Panel del taller. */
@Composable
fun CampanaAvisos(sinLeer: Int, alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        BotonIcono(
            icono = Iconos.Campana,
            descripcion = if (sinLeer > 0) "Notificaciones, $sinLeer sin leer" else "Notificaciones",
            alPulsar = alPulsar,
        )
        if (sinLeer > 0) {
            Insignia(
                cantidad = sinLeer,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp),
            )
        }
    }
}

/** Número rojo sobre la campana. Lo anuncia el botón, así que el lector de pantalla lo salta. */
@Composable
private fun Insignia(cantidad: Int, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clearAndSetSemantics { }
            .background(Colores.Crema, Formas.Pildora)
            .padding(2.dp)
            .defaultMinSize(minWidth = 14.dp, minHeight = 14.dp)
            .background(Colores.Freno, Formas.Pildora)
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = if (cantidad > 9) "9+" else cantidad.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/**
 * Contenido de la hoja de notificaciones: título, «Marcar como leídas» y la lista.
 * Se usa dentro de una [Hoja]. Tocar un aviso con orden llama a `alAbrirOrden`.
 */
@Composable
fun ListaAvisos(
    avisos: List<Notificacion>,
    alMarcarLeidas: () -> Unit,
    alAbrirOrden: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Notificaciones",
            style = MaterialTheme.typography.headlineSmall,
            color = Colores.Tinta,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (avisos.any { !it.leida }) {
            Enlace(
                texto = "Marcar como leídas",
                alPulsar = alMarcarLeidas,
                estilo = MaterialTheme.typography.bodyMedium,
                color = Colores.Tinta2,
            )
        }
    }
    if (avisos.isEmpty()) {
        Text(
            text = "No tienes notificaciones por ahora.",
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.Tinta2,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    avisos.forEach { aviso ->
        val idOrden = aviso.ordenId
        FilaAviso(
            aviso = aviso,
            alPulsar = if (idOrden != null) ({ alAbrirOrden(idOrden) }) else null,
        )
    }
}

@Composable
private fun FilaAviso(aviso: Notificacion, alPulsar: (() -> Unit)?) {
    val (icono, fondo, color) = when (aviso.tipo) {
        TipoNotificacion.Avance -> Triple(Iconos.Herramienta, Colores.MentaSuave, Colores.MentaTexto)
        TipoNotificacion.Fotos -> Triple(Iconos.Imagen, Colores.LavandaSuave, Colores.LavandaTexto)
        TipoNotificacion.Cotizacion -> Triple(Iconos.Recibo, Colores.Mantequilla, Colores.MantequillaTexto)
        TipoNotificacion.Pago -> Triple(Iconos.TarjetaPago, Colores.MentaSuave, Colores.MentaTexto)
        TipoNotificacion.Entrega -> Triple(Iconos.Hecho, Colores.MentaSuave, Colores.MentaTexto)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(Formas.Campo)
            .then(if (alPulsar != null) Modifier.clickable(role = Role.Button, onClick = alPulsar) else Modifier)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(fondo, CircleShape),
        ) {
            Icon(imageVector = icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = aviso.titulo, style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
            Text(
                text = "${aviso.detalle}, ${Formato.haceCuanto(aviso.fecha)}",
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Tinta3,
            )
        }
        if (!aviso.leida) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Colores.Freno, CircleShape)
                    .semantics { contentDescription = "Sin leer" },
            )
        }
    }
}
