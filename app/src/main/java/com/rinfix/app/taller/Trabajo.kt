package com.rinfix.app.taller

import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.EncabezadoSeccion
import com.rinfix.app.componentes.Enlace
import com.rinfix.app.componentes.EstadoVacio
import com.rinfix.app.componentes.EsqueletoDetalle
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.LineaTiempo
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.VisorFotoAvance
import com.rinfix.app.componentes.lineaPunteadaArriba
import com.rinfix.app.datos.Avance
import com.rinfix.app.datos.EtapaOrden
import com.rinfix.app.datos.FotoAvance
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.NuevoAvance
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.OrdenesDeEjemplo
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import com.rinfix.app.diseno.sombra
import java.time.LocalDateTime

/** Hojas que se abren desde el detalle del trabajo. */
private enum class HojaTrabajo { CambiarEtapa, Cotizar }

/**
 * Detalle de un trabajo para el taller: datos de la orden, el siguiente paso con su confirmación,
 * lo que pidió el cliente, la cotización y la misma línea de tiempo que ve el cliente.
 */
@Composable
fun PantallaTrabajo(
    alVolver: () -> Unit,
    alRegistrarAvance: (idOrden: String) -> Unit,
    modifier: Modifier = Modifier,
    logica: TrabajoLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    val contexto = LocalContext.current
    val vista = LocalView.current
    val orden = estado.orden
    var hoja by rememberSaveable { mutableStateOf<HojaTrabajo?>(null) }
    // Etapa al abrir la hoja: si la orden cambia mientras la hoja baja, la hoja no cambia de texto.
    var etapaHoja by rememberSaveable { mutableStateOf<EtapaOrden?>(null) }

    fun alGuardar(cerrar: () -> Unit, mensaje: String) {
        cerrar()
        vista.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK,
        )
        Toast.makeText(contexto, mensaje, Toast.LENGTH_SHORT).show()
    }

    when {
        estado.cargando -> Column(modifier = modifier.fillMaxSize()) {
            BarraSuperior(titulo = "Trabajo", alVolver = alVolver)
            EsqueletoDetalle()
        }
        orden == null -> Column(modifier = modifier.fillMaxSize()) {
            BarraSuperior(titulo = "Trabajo", alVolver = alVolver)
            EstadoVacio(
                icono = Iconos.Herramienta,
                titulo = "No encontramos este trabajo",
                mensaje = "Puede que sea de otro taller. Vuelve a la lista de trabajos.",
                textoAccion = "Volver",
                alPulsarAccion = alVolver,
            )
        }
        else -> ContenidoTrabajo(
            orden = orden,
            alVolver = alVolver,
            alSiguientePaso = {
                logica.limpiarError()
                etapaHoja = orden.etapa
                hoja = if (orden.etapa == EtapaOrden.Inspeccion) HojaTrabajo.Cotizar else HojaTrabajo.CambiarEtapa
            },
            alRegistrarAvance = { alRegistrarAvance(orden.id) },
            modifier = modifier,
        )
    }

    if (orden == null) return
    val desde = etapaHoja ?: orden.etapa
    val siguiente = desde.siguiente
    val paso = pasoDesde(desde)
    val cerrarHoja = {
        hoja = null
        etapaHoja = null
    }
    when (hoja) {
        HojaTrabajo.CambiarEtapa -> if (paso != null && siguiente != null) {
            HojaCambiarEtapa(
                orden = orden,
                actual = desde,
                paso = paso,
                siguiente = siguiente,
                guardando = estado.guardando,
                error = estado.error,
                alConfirmar = { nota, cobrado, cerrar ->
                    logica.registrar(NuevoAvance(etapa = siguiente, nota = nota, cobradoEnTaller = cobrado)) {
                        alGuardar(cerrar, "Ahora: ${siguiente.titulo.lowercase()}")
                    }
                },
                alCerrar = cerrarHoja,
            )
        }
        HojaTrabajo.Cotizar -> HojaCotizar(
            orden = orden,
            guardando = estado.guardando,
            error = estado.error,
            alConfirmar = { lineas, entrega, nota, cerrar ->
                logica.registrar(
                    NuevoAvance(etapa = EtapaOrden.Cotizacion, nota = nota, cotizacion = lineas, entregaEstimada = entrega),
                ) {
                    alGuardar(cerrar, "Cotización enviada al cliente")
                }
            },
            alCerrar = cerrarHoja,
        )
        null -> Unit
    }
}

@Composable
private fun ContenidoTrabajo(
    orden: Orden,
    alVolver: () -> Unit,
    alSiguientePaso: () -> Unit,
    alRegistrarAvance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = recordarEntrada()
    var fotoAbierta by remember { mutableStateOf<Pair<FotoAvance, Avance>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        BarraSuperior(titulo = "Trabajo ${orden.codigo}", alVolver = alVolver)
        Column(
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        ) {
            TarjetaTrabajo(orden = orden, alPulsar = null, modifier = Modifier.aparecer(visible, orden = 0))

            SiguientePaso(
                orden = orden,
                alSiguientePaso = alSiguientePaso,
                alRegistrarAvance = alRegistrarAvance,
                modifier = Modifier.aparecer(visible, orden = 1),
            )

            orden.notas?.let { notas ->
                Column(modifier = Modifier.aparecer(visible, orden = 2)) {
                    EncabezadoSeccion(titulo = "Lo que contó el cliente")
                    Text(
                        text = "«$notas»",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Colores.Tinta2,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .fillMaxWidth()
                            .background(Colores.Crema2, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                    )
                }
            }

            if (orden.detalleTotal.isNotEmpty()) {
                Column(modifier = Modifier.aparecer(visible, orden = 3)) {
                    EncabezadoSeccion(titulo = "Cotización")
                    ResumenCotizacion(orden = orden, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Column(modifier = Modifier.aparecer(visible, orden = 4)) {
                EncabezadoSeccion(titulo = "Línea de tiempo")
                LineaTiempo(
                    orden = orden,
                    alVerFoto = { foto, avance -> fotoAbierta = foto to avance },
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }

    fotoAbierta?.let { (foto, avance) ->
        VisorFotoAvance(foto = foto, avance = avance, alCerrar = { fotoAbierta = null })
    }
}

private val FormaPaso = RoundedCornerShape(28.dp)

/** Tarjeta oscura con lo que toca hacer ahora y el acceso a fotos y notas. */
@Composable
private fun SiguientePaso(
    orden: Orden,
    alSiguientePaso: () -> Unit,
    alRegistrarAvance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paso = pasoDesde(orden.etapa)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sombra(Sombra.Media, FormaPaso)
            .background(Colores.Tinta, FormaPaso)
            .padding(20.dp),
    ) {
        if (paso == null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Iconos.Hecho, contentDescription = null, tint = Colores.Menta, modifier = Modifier.size(28.dp))
                Column {
                    Text(
                        text = "Trabajo entregado",
                        style = MaterialTheme.typography.titleLarge,
                        color = Colores.Crema,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = "El ${Formato.fechaCorta(orden.ultimaActualizacion)}. Queda en el historial del taller y del cliente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.Crema.copy(alpha = 0.7f),
                    )
                }
            }
            return@Column
        }
        Text(text = "Siguiente paso", style = MaterialTheme.typography.labelLarge, color = Colores.Mantequilla)
        Text(
            text = paso.titulo,
            style = MaterialTheme.typography.headlineSmall,
            color = Colores.Crema,
            modifier = Modifier
                .padding(top = 4.dp)
                .semantics { heading() },
        )
        Text(
            text = paso.descripcion,
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.Crema.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = 4.dp),
        )
        Boton(
            texto = paso.boton,
            alPulsar = alSiguientePaso,
            estilo = EstiloBoton.Menta,
            icono = paso.icono,
            modifier = Modifier
                .padding(top = 18.dp)
                .fillMaxWidth(),
        )
        if (orden.etapa != EtapaOrden.Solicitud) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth(),
            ) {
                Enlace(
                    texto = "Agregar fotos o una nota",
                    alPulsar = alRegistrarAvance,
                    color = Colores.Mantequilla,
                )
            }
        }
    }
}

@Composable
private fun ResumenCotizacion(orden: Orden, modifier: Modifier = Modifier) {
    Tarjeta(espacio = 0, modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            orden.detalleTotal.forEach { linea ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = linea.servicio, style = MaterialTheme.typography.bodyMedium, color = Colores.Tinta)
                        linea.detalle?.let {
                            Text(text = it, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
                        }
                    }
                    Text(
                        text = if (linea.incluido) "Incluido" else Formato.pesos(linea.minimo),
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        color = Colores.Tinta,
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .lineaPunteadaArriba()
                .padding(top = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Total", style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
                EstadoCobro(orden = orden)
            }
            Text(
                text = Formato.pesos(orden.total ?: 0),
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                color = Colores.Tinta,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 1600)
@Composable
private fun VistaPreviaTrabajo() {
    val orden = OrdenesDeEjemplo.ordenesIniciales(LocalDateTime.now()).first()
    TemaRinFix {
        ContenidoTrabajo(orden = orden, alVolver = {}, alSiguientePaso = {}, alRegistrarAvance = {})
    }
}
