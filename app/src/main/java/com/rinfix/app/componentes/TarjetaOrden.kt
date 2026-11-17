package com.rinfix.app.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.datos.EtapaOrden
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.OrdenesDeEjemplo
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.Movimiento
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.recordarEntrada
import java.time.LocalDateTime

/**
 * Tarjeta de una orden: foto, estado, barra de etapas, entrega estimada y técnico.
 * Toda la tarjeta abre el seguimiento.
 */
@Composable
fun TarjetaOrden(
    orden: Orden,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Tarjeta(
        alPulsar = alPulsar,
        descripcionAccion = "Ver el seguimiento de la orden ${orden.codigo}",
        relleno = PaddingValues(16.dp),
        espacio = 0,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FotoRin(ancho = 78.dp, alto = 78.dp, tamanoRin = 70.dp, tono = TonoFoto.Clara, inclinacion = 30f)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Orden ${orden.codigo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.Tinta3,
                        modifier = Modifier.weight(1f),
                    )
                    Etiqueta(
                        texto = orden.etapa.estado,
                        tipo = tipoEtiqueta(orden.etapa),
                        conPunto = true,
                        pequena = true,
                    )
                }
                Text(
                    text = "${orden.vehiculo}, ${orden.rin}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Colores.Tinta,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        BarraEtapas(
            completadas = orden.etapa.numero,
            modifier = Modifier.padding(top = 14.dp),
        )
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                text = orden.detalleEtapa ?: orden.etapa.titulo,
                style = MaterialTheme.typography.bodySmall,
                color = Colores.Tinta,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Etapa ${orden.etapa.numero} de ${EtapaOrden.total}",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.Tinta3,
            )
        }

        // Pie con línea punteada: entrega y técnico.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .lineaPunteadaArriba()
                .padding(top = 12.dp),
        ) {
            Icon(
                imageVector = Iconos.Reloj,
                contentDescription = null,
                tint = Colores.Tinta,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = orden.entregaEstimada?.let { "Entrega ${Formato.diaYHora(it).replaceFirstChar { c -> c.lowercase() }}" }
                    ?: "Entrega por confirmar",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.Tinta,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f),
            )
            if (orden.tecnico != null) {
                Avatar(
                    nombre = orden.tecnico,
                    tamano = 22.dp,
                    fondo = Colores.Menta,
                    colorTexto = Colores.MentaTexto,
                )
                Text(
                    text = orden.tecnico,
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.Tinta3,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

/**
 * Barra de etapas: un segmento por etapa. Los completados se llenan de izquierda a derecha
 * con un pequeño escalón entre uno y otro.
 */
@Composable
fun BarraEtapas(
    completadas: Int,
    modifier: Modifier = Modifier,
    total: Int = EtapaOrden.total,
    color: Color = Colores.Menta,
    fondo: Color = Colores.Tinta.copy(alpha = 0.1f),
) {
    val visible = recordarEntrada()
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
            .clearAndSetSemantics { contentDescription = "Etapa $completadas de $total" },
    ) {
        repeat(total) { indice ->
            val llena = indice < completadas
            val progreso by animateFloatAsState(
                targetValue = if (visible && llena) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 500,
                    delayMillis = if (llena) indice * 90 + 200 else 0,
                    easing = Movimiento.Suave,
                ),
                label = "segmento $indice",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(fondo, Formas.Pildora),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = progreso
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                        .background(color, Formas.Pildora),
                )
            }
        }
    }
}

/** Color de la etiqueta según la etapa. Siempre igual en toda la app. */
fun tipoEtiqueta(etapa: EtapaOrden): TipoEtiqueta = when (etapa) {
    EtapaOrden.Solicitud, EtapaOrden.Inspeccion -> TipoEtiqueta.Neutra
    EtapaOrden.Cotizacion, EtapaOrden.Reparacion, EtapaOrden.Calidad -> TipoEtiqueta.EnProceso
    EtapaOrden.Lista, EtapaOrden.Entregada -> TipoEtiqueta.Listo
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaTarjetaOrden() {
    TemaRinFix {
        TarjetaOrden(
            orden = OrdenesDeEjemplo.ordenesIniciales(LocalDateTime.now()).first(),
            alPulsar = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
