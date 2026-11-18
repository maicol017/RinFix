package com.rinfix.app.taller

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.Avatar
import com.rinfix.app.componentes.CampanaAvisos
import com.rinfix.app.componentes.EncabezadoSeccion
import com.rinfix.app.componentes.Enlace
import com.rinfix.app.componentes.Hoja
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.ListaAvisos
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.OrdenesDeEjemplo
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Movimiento
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import com.rinfix.app.diseno.sombra
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Panel del taller: cómo va el día, solicitudes por revisar, quién entra y quién sale
 * hoy y mañana, y estadísticas básicas.
 */
@Composable
fun PantallaPanel(
    alVerTrabajo: (idOrden: String) -> Unit,
    alVerTrabajos: () -> Unit,
    alVerAgenda: () -> Unit,
    modifier: Modifier = Modifier,
    logica: PanelLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    ContenidoPanel(
        estado = estado,
        alVerTrabajo = alVerTrabajo,
        alVerTrabajos = alVerTrabajos,
        alVerAgenda = alVerAgenda,
        alMarcarLeidas = logica::marcarLeidas,
        modifier = modifier,
    )
}

@Composable
private fun ContenidoPanel(
    estado: PanelLogica.Estado,
    alVerTrabajo: (String) -> Unit,
    alVerTrabajos: () -> Unit,
    alVerAgenda: () -> Unit,
    alMarcarLeidas: () -> Unit,
    modifier: Modifier = Modifier,
    hoy: LocalDate = LocalDate.now(),
) {
    val visible = recordarEntrada()
    var verAvisos by rememberSaveable { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
    ) {
        Encabezado(
            nombre = estado.nombre,
            taller = estado.taller,
            sinLeer = estado.sinLeer,
            alAbrirAvisos = { verAvisos = true },
            modifier = Modifier.aparecer(visible, orden = 0),
        )

        ResumenDelDia(
            fecha = Formato.fechaLarga(hoy),
            enTaller = estado.enTaller,
            listas = estado.listas,
            nuevas = estado.nuevas.size,
            atrasadas = estado.atrasadas,
            animar = visible && !estado.cargando,
            alVerTrabajos = alVerTrabajos,
            modifier = Modifier.aparecer(visible, orden = 1),
        )

        if (!estado.cargando) {
            Column(modifier = Modifier.aparecer(visible, orden = 2)) {
                EncabezadoSeccion(
                    titulo = "Solicitudes nuevas",
                    accion = if (estado.nuevas.size > MaximoSolicitudes) "Ver todas" else null,
                    alPulsarAccion = alVerTrabajos,
                )
                if (estado.nuevas.isEmpty()) {
                    TextoVacio("No hay solicitudes por revisar.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                        estado.nuevas.take(MaximoSolicitudes).forEach { orden ->
                            TarjetaSolicitud(orden = orden, alPulsar = { alVerTrabajo(orden.id) })
                        }
                    }
                }
            }

            Column(modifier = Modifier.aparecer(visible, orden = 3)) {
                EncabezadoSeccion(titulo = "Hoy y mañana", accion = "Ver agenda", alPulsarAccion = alVerAgenda)
                if (estado.proximos.isEmpty()) {
                    TextoVacio("Sin ingresos ni entregas para hoy ni mañana.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                        estado.proximos.groupBy { it.fecha.toLocalDate() }.forEach { (dia, eventos) ->
                            Text(
                                text = if (dia == hoy) "Hoy" else "Mañana",
                                style = MaterialTheme.typography.titleSmall,
                                color = Colores.Tinta3,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            eventos.forEach { evento ->
                                FilaEvento(evento = evento, alPulsar = { alVerTrabajo(evento.orden.id) })
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.aparecer(visible, orden = 4)) {
                EncabezadoSeccion(titulo = "Últimos ${PanelLogica.DiasEstadisticas} días")
                Estadisticas(datos = estado.estadisticas, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }

    if (verAvisos) {
        Hoja(alCerrar = { verAvisos = false }) { cerrar ->
            ListaAvisos(
                avisos = estado.notificaciones,
                alMarcarLeidas = alMarcarLeidas,
                alAbrirOrden = { idOrden ->
                    cerrar()
                    alVerTrabajo(idOrden)
                },
            )
        }
    }
}

private const val MaximoSolicitudes = 3

@Composable
private fun Encabezado(
    nombre: String,
    taller: String,
    sinLeer: Int,
    alAbrirAvisos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primerNombre = nombre.trim().substringBefore(' ')
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
    ) {
        Avatar(nombre = nombre, tamano = 46.dp, fondo = Colores.Mantequilla, colorTexto = Colores.MantequillaTexto)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = taller, style = MaterialTheme.typography.bodySmall, color = Colores.Tinta3)
            Text(
                text = if (primerNombre.isEmpty()) "Hola" else "Hola, $primerNombre",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 28.sp),
                color = Colores.Tinta,
                modifier = Modifier.semantics { heading() },
            )
        }
        CampanaAvisos(sinLeer = sinLeer, alPulsar = alAbrirAvisos)
    }
}

private val FormaResumen = RoundedCornerShape(28.dp)

/** Tarjeta oscura con los tres números del día. Los números suben desde cero al abrir. */
@Composable
private fun ResumenDelDia(
    fecha: String,
    enTaller: Int,
    listas: Int,
    nuevas: Int,
    atrasadas: Int,
    animar: Boolean,
    alVerTrabajos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sombra(Sombra.Media, FormaResumen)
            .background(Colores.Tinta, FormaResumen)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = fecha,
                style = MaterialTheme.typography.labelLarge,
                color = Colores.Mantequilla,
                modifier = Modifier.weight(1f),
            )
            if (atrasadas > 0) {
                Text(
                    text = if (atrasadas == 1) "1 atrasado" else "$atrasadas atrasados",
                    style = MaterialTheme.typography.labelMedium,
                    color = Colores.FrenoTexto,
                    modifier = Modifier
                        .background(Colores.FrenoSuave, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Text(
            text = "Así va el taller",
            style = MaterialTheme.typography.headlineSmall,
            color = Colores.Crema,
            modifier = Modifier
                .padding(top = 4.dp)
                .semantics { heading() },
        )
        Row(
            modifier = Modifier
                .padding(top = 18.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            DatoDelDia(numero = enTaller, texto = "en el taller", animar = animar, modifier = Modifier.weight(1f))
            SeparadorVertical()
            DatoDelDia(
                numero = listas,
                texto = "para entregar",
                animar = animar,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            )
            SeparadorVertical()
            DatoDelDia(
                numero = nuevas,
                texto = if (nuevas == 1) "solicitud nueva" else "solicitudes nuevas",
                animar = animar,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            )
        }
        Enlace(
            texto = "Ver todos los trabajos",
            alPulsar = alVerTrabajos,
            color = Colores.Mantequilla,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

@Composable
private fun DatoDelDia(numero: Int, texto: String, animar: Boolean, modifier: Modifier = Modifier) {
    val mostrado by animateIntAsState(
        targetValue = if (animar) numero else 0,
        animationSpec = tween(durationMillis = 700, easing = Movimiento.Suave),
        label = "número del día",
    )
    Column(modifier = modifier.clearAndSetSemantics { contentDescription = "$numero $texto" }) {
        Text(
            text = mostrado.toString(),
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
            color = Colores.Crema,
        )
        Text(text = texto, style = MaterialTheme.typography.bodySmall, color = Colores.Crema.copy(alpha = 0.7f))
    }
}

@Composable
private fun SeparadorVertical() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(Colores.Crema.copy(alpha = 0.16f)),
    )
}

@Composable
private fun TarjetaSolicitud(orden: Orden, alPulsar: () -> Unit) {
    Tarjeta(
        fondo = Colores.MantequillaSuave,
        alPulsar = alPulsar,
        descripcionAccion = "Revisar la solicitud ${orden.codigo}",
        relleno = PaddingValues(16.dp),
        espacio = 0,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(nombre = orden.cliente, tamano = 40.dp, fondo = Colores.Mantequilla, colorTexto = Colores.MantequillaTexto)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = orden.cliente, style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
                Text(
                    text = "${orden.vehiculo}, ${orden.rin}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.Tinta2,
                )
            }
            Text(text = orden.codigo, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Icon(Iconos.Calendario, contentDescription = null, tint = Colores.Tinta, modifier = Modifier.size(16.dp))
            Text(
                text = orden.cita?.let { "Ingresa ${Formato.diaYHora(it).replaceFirstChar { c -> c.lowercase() }}" }
                    ?: "Ingreso sin agendar",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.Tinta,
            )
        }
        orden.notas?.let { notas ->
            Text(
                text = "«$notas»",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.Tinta2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = orden.servicio,
                style = MaterialTheme.typography.labelMedium,
                color = Colores.MantequillaTexto,
                modifier = Modifier.weight(1f),
            )
            Text(text = "Revisar", style = MaterialTheme.typography.labelLarge, color = Colores.Tinta)
            Icon(Iconos.Siguiente, contentDescription = null, tint = Colores.Tinta, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun FilaEvento(evento: PanelLogica.Evento, alPulsar: () -> Unit) {
    val ingreso = evento.tipo == PanelLogica.TipoEvento.Ingreso
    val orden = evento.orden
    val paso = evento.fecha.isBefore(LocalDateTime.now())
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = Formato.hora(evento.fecha),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = if (paso) Colores.Tinta3 else Colores.Tinta,
            modifier = Modifier
                .width(72.dp)
                .padding(top = 16.dp),
        )
        Tarjeta(
            alPulsar = alPulsar,
            descripcionAccion = "Ver el trabajo ${orden.codigo}",
            relleno = PaddingValues(14.dp),
            espacio = 0,
            modifier = Modifier.weight(1f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconoEvento(ingreso = ingreso)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (ingreso) "Recibir ${orden.codigo}" else "Entregar ${orden.codigo}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Colores.Tinta,
                    )
                    Text(
                        text = "${orden.vehiculo}, ${orden.cliente}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.Tinta3,
                    )
                }
            }
            if (!ingreso) {
                EstadoCobro(orden = orden, modifier = Modifier.padding(start = 42.dp, top = 8.dp))
            }
        }
    }
}

@Composable
private fun IconoEvento(ingreso: Boolean) {
    val (fondo, color) = if (ingreso) Colores.MantequillaSuave to Colores.MantequillaTexto else Colores.MentaSuave to Colores.MentaTexto
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .background(fondo, RoundedCornerShape(10.dp)),
    ) {
        Icon(
            imageVector = if (ingreso) Iconos.Calendario else Iconos.Carro,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun Estadisticas(datos: PanelLogica.Estadisticas, modifier: Modifier = Modifier) {
    if (datos.entregados == 0) {
        TextoVacio("Todavía no hay entregas en este periodo.", modifier = modifier)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.fillMaxWidth()) {
        CuadroDato(
            valor = datos.entregados.toString(),
            texto = if (datos.entregados == 1) "trabajo entregado" else "trabajos entregados",
            fondo = Colores.MentaSuave,
            modifier = Modifier.weight(1f),
        )
        CuadroDato(
            valor = Formato.pesos(datos.cobrado),
            texto = "cobrado",
            fondo = Colores.MantequillaSuave,
            modifier = Modifier.weight(1f),
        )
        CuadroDato(
            valor = datos.diasPromedio?.let { if (it == 1) "1 día" else "$it días" } ?: "Sin datos",
            texto = "en promedio",
            fondo = Colores.Crema2,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CuadroDato(valor: String, texto: String, fondo: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(fondo, RoundedCornerShape(18.dp))
            .padding(14.dp)
            .clearAndSetSemantics { contentDescription = "$valor $texto" },
    ) {
        Text(
            text = valor,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = Colores.Tinta,
            maxLines = 1,
        )
        Text(text = texto, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta2)
    }
}

@Composable
private fun TextoVacio(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = Colores.Tinta3,
        modifier = modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .background(Colores.Crema2, RoundedCornerShape(18.dp))
            .padding(16.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 1400)
@Composable
private fun VistaPreviaPanel() {
    val ahora = LocalDateTime.now()
    val ordenes = OrdenesDeEjemplo.ordenesIniciales(ahora)
    TemaRinFix {
        ContenidoPanel(
            estado = PanelLogica.Estado(
                cargando = false,
                nombre = "Carlos Rueda",
                taller = "Taller RinFix Cabecera",
                nuevas = ordenes.filter { it.etapa == com.rinfix.app.datos.EtapaOrden.Solicitud },
                enTaller = 2,
                listas = 1,
                atrasadas = 0,
                proximos = ordenes.filter { it.cita != null }.map {
                    PanelLogica.Evento(it.cita!!, PanelLogica.TipoEvento.Ingreso, it)
                },
                estadisticas = PanelLogica.Estadisticas(entregados = 1, cobrado = 95_000, diasPromedio = 2),
            ),
            alVerTrabajo = {},
            alVerTrabajos = {},
            alVerAgenda = {},
            alMarcarLeidas = {},
        )
    }
}
