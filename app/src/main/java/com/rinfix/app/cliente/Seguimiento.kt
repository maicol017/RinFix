package com.rinfix.app.cliente

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.BarraEtapas
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.BotonIcono
import com.rinfix.app.componentes.EstadoVacio
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.FotoRin
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.LineaTiempo
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TonoFoto
import com.rinfix.app.componentes.VisorFotoAvance
import com.rinfix.app.componentes.tipoEtiqueta
import com.rinfix.app.datos.Avance
import com.rinfix.app.datos.FotoAvance
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.OrdenesDeEjemplo
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Seguimiento de una orden, como el de un pedido: etapa actual, entrega, total, línea de tiempo
 * con fotos del taller, datos del taller y el pago cuando hay un total aprobado.
 * Se actualiza sola cuando el taller registra un avance.
 */
@Composable
fun PantallaSeguimiento(
    alVolver: () -> Unit,
    alPagar: (idOrden: String) -> Unit,
    alVerDiagnostico: (idDiagnostico: String) -> Unit,
    modifier: Modifier = Modifier,
    logica: SeguimientoLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    val orden = estado.orden
    when {
        estado.cargando -> Column(modifier = modifier.fillMaxSize()) {
            BarraSuperior(titulo = "Seguimiento", alVolver = alVolver)
        }
        orden == null -> Column(modifier = modifier.fillMaxSize()) {
            BarraSuperior(titulo = "Seguimiento", alVolver = alVolver)
            EstadoVacio(
                icono = Iconos.Lista,
                titulo = "No encontramos esta orden",
                mensaje = "Puede que ya no esté disponible. Revisa tus órdenes en «Mis órdenes».",
            )
        }
        else -> ContenidoSeguimiento(
            orden = orden,
            alVolver = alVolver,
            alPagar = { alPagar(orden.id) },
            alVerDiagnostico = alVerDiagnostico,
            modifier = modifier,
        )
    }
}

@Composable
private fun ContenidoSeguimiento(
    orden: Orden,
    alVolver: () -> Unit,
    alPagar: () -> Unit,
    alVerDiagnostico: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = recordarEntrada()
    var fotoAbierta by remember { mutableStateOf<Pair<FotoAvance, Avance>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        BarraSuperior(titulo = "Seguimiento", alVolver = alVolver)
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        ) {
            CabeceraOrden(orden = orden, modifier = Modifier.aparecer(visible, orden = 0))
            EstadoEnVivo(orden = orden, modifier = Modifier.aparecer(visible, orden = 1))

            Tarjeta(
                relleno = PaddingValues(start = 12.dp, end = 14.dp, top = 6.dp, bottom = 4.dp),
                espacio = 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .aparecer(visible, orden = 2),
            ) {
                LineaTiempo(orden = orden, alVerFoto = { foto, avance -> fotoAbierta = foto to avance })
            }

            orden.notas?.let { notas ->
                Tarjeta(espacio = 4, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Tus notas para el taller", style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
                    Text(text = notas, style = MaterialTheme.typography.bodyMedium, color = Colores.Tinta)
                }
            }

            TarjetaTallerSeguimiento(taller = orden.taller, modifier = Modifier.aparecer(visible, orden = 3))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val total = orden.total
                if (orden.pagoPendiente && total != null) {
                    Boton(
                        texto = "Pagar ${Formato.pesos(total)}",
                        alPulsar = alPagar,
                        icono = Iconos.TarjetaPago,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "También puedes pagar al recoger tu rin.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.Tinta3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                orden.diagnosticoId?.let { id ->
                    Boton(
                        texto = "Ver diagnóstico",
                        alPulsar = { alVerDiagnostico(id) },
                        estilo = EstiloBoton.Blanco,
                        icono = Iconos.Ia,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    fotoAbierta?.let { (foto, avance) ->
        VisorFotoAvance(foto = foto, avance = avance, alCerrar = { fotoAbierta = null })
    }
}

@Composable
private fun CabeceraOrden(orden: Orden, modifier: Modifier = Modifier) {
    Tarjeta(espacio = 0, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FotoRin(ancho = 64.dp, alto = 64.dp, tamanoRin = 58.dp, tono = TonoFoto.Clara, inclinacion = 30f, radio = 16.dp)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Orden ${orden.codigo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.Tinta3,
                        modifier = Modifier.weight(1f),
                    )
                    Etiqueta(texto = orden.etapa.estado, tipo = tipoEtiqueta(orden.etapa), conPunto = true, pequena = true)
                }
                Text(
                    text = "${orden.vehiculo}, ${orden.rin}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Colores.Tinta,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .semantics { heading() },
                )
            }
        }
        BarraEtapas(completadas = orden.etapa.numero, modifier = Modifier.padding(top = 14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth(),
        ) {
            val entrega = orden.entregaEstimada
            val cita = orden.cita
            when {
                entrega != null -> DatoCorto("Entrega estimada", fechaCorta(entrega), Formato.hora(entrega), Modifier.weight(1f))
                cita != null -> DatoCorto("Ingreso al taller", fechaCorta(cita), Formato.hora(cita), Modifier.weight(1f))
                else -> DatoCorto("Entrega estimada", "Por confirmar", "Tras la inspección", Modifier.weight(1f))
            }
            val total = orden.total
            if (total != null) {
                DatoCorto(
                    etiqueta = "Total aprobado",
                    valor = Formato.pesos(total),
                    detalle = if (orden.pagada) "Pagado" else "Pago pendiente",
                    modifier = Modifier.weight(1f),
                )
            } else {
                DatoCorto("Cotización", "Por confirmar", "Tras la inspección", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DatoCorto(etiqueta: String, valor: String, detalle: String, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = modifier
            .background(Colores.Crema2, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Text(text = etiqueta, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
        Text(
            text = valor,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            color = Colores.Tinta,
        )
        Text(text = detalle, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta2)
    }
}

/** «En vivo» con un punto que late mientras la orden está activa; si ya se entregó, la fecha. */
@Composable
private fun EstadoEnVivo(orden: Orden, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (orden.activa) {
            val transicion = rememberInfiniteTransition(label = "en vivo")
            val pulso by transicion.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    keyframes {
                        durationMillis = 1600
                        0f at 0
                        1f at 1280
                    },
                ),
                label = "pulso en vivo",
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .drawBehind {
                        drawCircle(
                            color = Colores.Menta.copy(alpha = 0.65f * (1f - pulso)),
                            radius = size.minDimension / 2 + 7.dp.toPx() * pulso,
                        )
                    }
                    .background(Colores.MentaTexto, CircleShape),
            )
            Text(
                text = "En vivo",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Colores.Tinta,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Último avance ${Formato.haceCuanto(orden.ultimaActualizacion)}",
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Tinta3,
            )
        } else {
            Icon(Iconos.Hecho, contentDescription = null, tint = Colores.MentaTexto, modifier = Modifier.size(16.dp))
            Text(
                text = "Entregada el ${Formato.fechaCorta(orden.ultimaActualizacion)}",
                style = MaterialTheme.typography.labelLarge,
                color = Colores.Tinta,
            )
        }
    }
}

@Composable
private fun TarjetaTallerSeguimiento(taller: String, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    fun abrirMapa() {
        val intento = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode("Cabecera del Llano, Bucaramanga")))
        try {
            contexto.startActivity(intento)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(contexto, "No encontramos una app de mapas en el teléfono.", Toast.LENGTH_SHORT).show()
        }
    }
    Tarjeta(espacio = 0, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(Colores.MentaSuave, RoundedCornerShape(14.dp)),
            ) {
                Icon(Iconos.Ubicacion, contentDescription = null, tint = Colores.MentaTexto, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = taller, style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
                Text(
                    text = "Cabecera, Bucaramanga. Abierto hasta las 6:00 p. m.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.Tinta3,
                )
            }
            BotonIcono(icono = Iconos.Flecha, descripcion = "Cómo llegar al taller", alPulsar = ::abrirMapa)
        }
    }
}

/** «Jue 19 nov» */
private fun fechaCorta(fecha: LocalDateTime): String {
    val dia: LocalDate = fecha.toLocalDate()
    return "${Formato.diaSemanaCorto(dia)} ${dia.dayOfMonth} ${Formato.mesCorto(dia)}"
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 1500)
@Composable
private fun VistaPreviaSeguimiento() {
    TemaRinFix {
        ContenidoSeguimiento(
            orden = OrdenesDeEjemplo.ordenesIniciales(LocalDateTime.now()).first(),
            alVolver = {},
            alPagar = {},
            alVerDiagnostico = {},
        )
    }
}
