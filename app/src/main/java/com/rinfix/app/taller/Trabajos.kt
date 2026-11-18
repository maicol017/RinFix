package com.rinfix.app.taller

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.EstadoVacio
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.SelectorFichas
import com.rinfix.app.datos.OrdenesDeEjemplo
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import java.time.LocalDateTime

/** Trabajos del taller con filtros por etapa. Cada tarjeta abre el detalle del trabajo. */
@Composable
fun PantallaTrabajos(
    alVerTrabajo: (idOrden: String) -> Unit,
    modifier: Modifier = Modifier,
    logica: TrabajosLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    ContenidoTrabajos(estado = estado, alVerTrabajo = alVerTrabajo, modifier = modifier)
}

@Composable
private fun ContenidoTrabajos(
    estado: TrabajosLogica.Estado,
    alVerTrabajo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = recordarEntrada()
    var filtro by rememberSaveable { mutableStateOf(TrabajosLogica.Filtro.Activos) }
    val activos = estado.de(TrabajosLogica.Filtro.Activos).size

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 14.dp, bottom = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .aparecer(visible, orden = 0),
        ) {
            Text(
                text = "Trabajos",
                style = MaterialTheme.typography.headlineLarge,
                color = Colores.Tinta,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = when {
                    estado.cargando -> " "
                    activos == 0 -> "No hay trabajos activos en ${estado.taller}."
                    activos == 1 -> "1 trabajo activo en ${estado.taller}."
                    else -> "$activos trabajos activos en ${estado.taller}."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Colores.Tinta2,
            )
        }

        SelectorFichas(
            opciones = TrabajosLogica.Filtro.entries,
            seleccionada = filtro,
            alElegir = { filtro = it },
            texto = { "${it.nombre} ${estado.de(it).size}" },
            descripcion = { "${it.nombre}, ${estado.de(it).size} trabajos" },
            enUnaLinea = true,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .aparecer(visible, orden = 1),
        )

        if (!estado.cargando) {
            AnimatedContent(
                targetState = filtro,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "filtro de trabajos",
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .aparecer(visible, orden = 2),
            ) { actual ->
                val lista = estado.de(actual)
                if (lista.isEmpty()) {
                    EstadoVacio(
                        icono = if (actual == TrabajosLogica.Filtro.Entregados) Iconos.Hecho else Iconos.Herramienta,
                        titulo = vacioTitulo(actual),
                        mensaje = vacioMensaje(actual),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val ahora = LocalDateTime.now()
                        lista.forEach { orden ->
                            TarjetaTrabajo(orden = orden, alPulsar = { alVerTrabajo(orden.id) }, ahora = ahora)
                        }
                    }
                }
            }
        }
    }
}

private fun vacioTitulo(filtro: TrabajosLogica.Filtro): String = when (filtro) {
    TrabajosLogica.Filtro.Activos -> "Sin trabajos activos"
    TrabajosLogica.Filtro.Nuevos -> "Sin solicitudes nuevas"
    TrabajosLogica.Filtro.EnTaller -> "Nada en el taller ahora"
    TrabajosLogica.Filtro.Listos -> "Nada listo para entregar"
    TrabajosLogica.Filtro.Entregados -> "Todavía no hay entregas"
}

private fun vacioMensaje(filtro: TrabajosLogica.Filtro): String = when (filtro) {
    TrabajosLogica.Filtro.Activos, TrabajosLogica.Filtro.Nuevos ->
        "Cuando un cliente pida una reparación desde la app, aparece aquí."
    TrabajosLogica.Filtro.EnTaller -> "Los rines que recibas y empieces a revisar se ven aquí."
    TrabajosLogica.Filtro.Listos -> "Cuando termines un trabajo y pase el control de calidad, queda aquí."
    TrabajosLogica.Filtro.Entregados -> "Los trabajos que entregues quedan aquí como historial."
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 1400)
@Composable
private fun VistaPreviaTrabajos() {
    val ordenes = OrdenesDeEjemplo.ordenesIniciales(LocalDateTime.now())
    TemaRinFix {
        ContenidoTrabajos(
            estado = TrabajosLogica.Estado(
                cargando = false,
                taller = "Taller RinFix Cabecera",
                porFiltro = TrabajosLogica.Filtro.entries.associateWith { filtro -> ordenes.filter(filtro::incluye) },
            ),
            alVerTrabajo = {},
        )
    }
}
