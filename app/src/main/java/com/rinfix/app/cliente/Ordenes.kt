package com.rinfix.app.cliente

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.BotonIcono
import com.rinfix.app.componentes.EstadoVacio
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.SelectorSegmentos
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TarjetaOrden
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.OrdenesDeEjemplo
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import java.time.LocalDateTime

private enum class Grupo { EnCurso, Terminadas }

/** Mis órdenes: reparaciones en curso con su etapa, y las terminadas con lo que se pagó. */
@Composable
fun PantallaOrdenes(
    alVerOrden: (idOrden: String) -> Unit,
    alAnalizar: () -> Unit,
    alAgendar: () -> Unit,
    modifier: Modifier = Modifier,
    logica: OrdenesLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    ContenidoOrdenes(
        estado = estado,
        alVerOrden = alVerOrden,
        alAnalizar = alAnalizar,
        alAgendar = alAgendar,
        modifier = modifier,
    )
}

@Composable
private fun ContenidoOrdenes(
    estado: OrdenesLogica.Estado,
    alVerOrden: (String) -> Unit,
    alAnalizar: () -> Unit,
    alAgendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = recordarEntrada()
    var grupo by rememberSaveable { mutableStateOf(Grupo.EnCurso) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.aparecer(visible, orden = 0)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mis órdenes",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Colores.Tinta,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "Sigue cada reparación etapa por etapa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Colores.Tinta2,
                )
            }
            BotonIcono(icono = Iconos.Calendario, descripcion = "Agendar una revisión", alPulsar = alAgendar)
        }

        SelectorSegmentos(
            opciones = Grupo.entries,
            seleccionada = grupo,
            alElegir = { grupo = it },
            texto = {
                when (it) {
                    Grupo.EnCurso -> "En curso (${estado.enCurso.size})"
                    Grupo.Terminadas -> "Terminadas (${estado.terminadas.size})"
                }
            },
            modifier = Modifier.aparecer(visible, orden = 1),
        )

        if (!estado.cargando) {
            AnimatedContent(
                targetState = grupo,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "grupo de órdenes",
                modifier = Modifier.aparecer(visible, orden = 2),
            ) { actual ->
                when (actual) {
                    Grupo.EnCurso -> ListaEnCurso(estado.enCurso, alVerOrden, alAnalizar)
                    Grupo.Terminadas -> ListaTerminadas(estado.terminadas, alVerOrden)
                }
            }
        }
    }
}

@Composable
private fun ListaEnCurso(ordenes: List<Orden>, alVerOrden: (String) -> Unit, alAnalizar: () -> Unit) {
    if (ordenes.isEmpty()) {
        EstadoVacio(
            icono = Iconos.Lista,
            titulo = "No tienes reparaciones en curso",
            mensaje = "Analiza un rin para recibir una cotización y solicitar la reparación.",
            textoAccion = "Analizar un rin",
            iconoAccion = Iconos.Camara,
            alPulsarAccion = alAnalizar,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ordenes.forEach { orden ->
            key(orden.id) {
                TarjetaOrden(orden = orden, alPulsar = { alVerOrden(orden.id) })
            }
        }
    }
}

@Composable
private fun ListaTerminadas(ordenes: List<Orden>, alVerOrden: (String) -> Unit) {
    if (ordenes.isEmpty()) {
        EstadoVacio(
            icono = Iconos.Hecho,
            titulo = "Todavía no hay reparaciones terminadas",
            mensaje = "Cuando recojas un rin reparado, aquí queda el registro con lo que pagaste.",
            fondoIcono = Colores.Menta,
            colorIcono = Colores.MentaTexto,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ordenes.forEach { orden ->
            key(orden.id) {
                Tarjeta(
                    alPulsar = { alVerOrden(orden.id) },
                    descripcionAccion = "Ver la orden ${orden.codigo}",
                    espacio = 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Colores.MentaSuave, RoundedCornerShape(14.dp)),
                        ) {
                            Icon(Iconos.Hecho, contentDescription = null, tint = Colores.MentaTexto, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${orden.servicio} entregado",
                                style = MaterialTheme.typography.titleSmall,
                                color = Colores.Tinta,
                            )
                            Text(
                                text = "${Formato.fechaCorta(orden.ultimaActualizacion)}, ${orden.vehiculo}, ${orden.rin}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Colores.Tinta3,
                            )
                        }
                        orden.total?.let {
                            Text(
                                text = Formato.pesos(it),
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                color = Colores.Tinta,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 800)
@Composable
private fun VistaPreviaOrdenes() {
    val lista = OrdenesDeEjemplo.ordenesIniciales(LocalDateTime.now()).filter { it.clienteId == "cliente-1" }
    TemaRinFix {
        ContenidoOrdenes(
            estado = OrdenesLogica.Estado(
                cargando = false,
                enCurso = lista.filter { it.activa },
                terminadas = lista.filterNot { it.activa },
            ),
            alVerOrden = {},
            alAnalizar = {},
            alAgendar = {},
        )
    }
}
