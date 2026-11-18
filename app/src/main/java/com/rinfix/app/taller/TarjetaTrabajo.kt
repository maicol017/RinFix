package com.rinfix.app.taller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.componentes.Avatar
import com.rinfix.app.componentes.BarraEtapas
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TipoEtiqueta
import com.rinfix.app.componentes.lineaPunteadaArriba
import com.rinfix.app.componentes.tipoEtiqueta
import com.rinfix.app.datos.EtapaOrden
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.OrdenesDeEjemplo
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix
import java.time.LocalDateTime

/** `true` si la entrega prometida ya pasó y el rin todavía no está listo. */
fun Orden.atrasada(ahora: LocalDateTime = LocalDateTime.now()): Boolean {
    val entrega = entregaEstimada ?: return false
    return etapa < EtapaOrden.Lista && entrega.isBefore(ahora)
}

/**
 * La fecha que más le importa al taller: la cita de ingreso si aún no llega el rin,
 * la entrega prometida mientras se repara, y la última novedad si ya terminó.
 */
fun Orden.fechaClave(): LocalDateTime? = when (etapa) {
    EtapaOrden.Solicitud -> cita
    EtapaOrden.Lista, EtapaOrden.Entregada -> ultimaActualizacion
    else -> entregaEstimada
}

/**
 * Tarjeta de un trabajo para el taller: qué rin, de quién, en qué etapa va,
 * qué fecha importa y si ya se cobró.
 */
@Composable
fun TarjetaTrabajo(
    orden: Orden,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    ahora: LocalDateTime = LocalDateTime.now(),
) {
    val atrasada = orden.atrasada(ahora)
    Tarjeta(
        alPulsar = alPulsar,
        descripcionAccion = "Ver el trabajo ${orden.codigo}",
        relleno = PaddingValues(16.dp),
        espacio = 0,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = orden.codigo, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
                Text(
                    text = "${orden.vehiculo}, ${orden.rin}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Colores.Tinta,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = orden.detalleEtapa ?: orden.servicio,
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.Tinta2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Etiqueta(
                texto = if (atrasada) "Atrasado" else orden.etapa.estado,
                tipo = if (atrasada) TipoEtiqueta.Atencion else tipoEtiqueta(orden.etapa),
                conPunto = true,
                pequena = true,
            )
        }

        if (orden.etapa != EtapaOrden.Entregada) {
            BarraEtapas(completadas = orden.etapa.numero, modifier = Modifier.padding(top = 12.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Avatar(nombre = orden.cliente, tamano = 24.dp, fondo = Colores.Crema2, colorTexto = Colores.Tinta2)
            Text(
                text = orden.cliente,
                style = MaterialTheme.typography.bodySmall,
                color = Colores.Tinta2,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = orden.tecnico?.let { "Técnico: $it" } ?: "Sin técnico",
                style = MaterialTheme.typography.labelSmall,
                color = if (orden.tecnico == null && orden.activa) Colores.FrenoTexto else Colores.Tinta3,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .lineaPunteadaArriba()
                .padding(top = 12.dp),
        ) {
            val (icono, texto) = momentoClave(orden, atrasada)
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = if (atrasada) Colores.FrenoTexto else Colores.Tinta,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = texto,
                style = MaterialTheme.typography.bodySmall,
                color = if (atrasada) Colores.FrenoTexto else Colores.Tinta,
                modifier = Modifier.weight(1f),
            )
            EstadoCobro(orden = orden)
        }
    }
}

private fun momentoClave(orden: Orden, atrasada: Boolean): Pair<ImageVector, String> {
    fun en(fecha: LocalDateTime) = Formato.diaYHora(fecha).replaceFirstChar { it.lowercase() }
    return when (orden.etapa) {
        EtapaOrden.Solicitud -> Iconos.Calendario to (orden.cita?.let { "Ingresa ${en(it)}" } ?: "Ingreso sin agendar")
        EtapaOrden.Lista -> Iconos.Hecho to "Listo, esperando al cliente"
        EtapaOrden.Entregada -> Iconos.Hecho to "Entregado el ${Formato.fechaCorta(orden.ultimaActualizacion)}"
        else -> {
            val entrega = orden.entregaEstimada
            when {
                entrega == null -> Iconos.Reloj to "Entrega por definir"
                atrasada -> Iconos.Alerta to "Prometido para ${en(entrega)}"
                else -> Iconos.Reloj to "Entrega ${en(entrega)}"
            }
        }
    }
}

/** Sin cotizar, por cobrar o pagado. */
@Composable
fun EstadoCobro(orden: Orden, modifier: Modifier = Modifier) {
    val total = orden.total
    val (texto, color) = when {
        total == null -> "Sin cotizar" to Colores.Tinta3
        orden.pagada -> "Pagado" to Colores.MentaTexto
        else -> "Por cobrar ${Formato.pesos(total)}" to Colores.MantequillaTexto
    }
    Text(text = texto, style = MaterialTheme.typography.labelMedium, color = color, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaTarjetaTrabajo() {
    val ordenes = OrdenesDeEjemplo.ordenesIniciales(LocalDateTime.now())
    TemaRinFix {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            ordenes.take(3).forEach { TarjetaTrabajo(orden = it, alPulsar = {}) }
        }
    }
}
