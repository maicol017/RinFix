package com.rinfix.app.taller

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.EncabezadoSeccion
import com.rinfix.app.componentes.Enlace
import com.rinfix.app.componentes.EstadoVacio
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.datos.EtapaOrden
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
 * Agenda del taller: tira de días, las horas de ingreso del día elegido (con cita, libres o
 * bloqueadas) y las entregas. Bloquear una hora la quita de las que los clientes pueden agendar.
 */
@Composable
fun PantallaAgenda(
    alVerTrabajo: (idOrden: String) -> Unit,
    modifier: Modifier = Modifier,
    logica: AgendaLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    val contexto = LocalContext.current
    ContenidoAgenda(
        estado = estado,
        alElegirDia = logica::elegirDia,
        alVerTrabajo = alVerTrabajo,
        alCambiarBloqueo = { franja ->
            logica.cambiarBloqueo(franja) { bloqueada ->
                val hora = Formato.hora(franja.inicio)
                Toast.makeText(
                    contexto,
                    if (bloqueada) "$hora bloqueada: los clientes ya no pueden agendarla." else "$hora quedó libre para agendar.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
        alLimpiarError = logica::limpiarError,
        modifier = modifier,
    )
}

@Composable
private fun ContenidoAgenda(
    estado: AgendaLogica.Estado,
    alElegirDia: (LocalDate) -> Unit,
    alVerTrabajo: (String) -> Unit,
    alCambiarBloqueo: (AgendaLogica.Franja) -> Unit,
    alLimpiarError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = recordarEntrada()
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
                text = "Agenda",
                style = MaterialTheme.typography.headlineLarge,
                color = Colores.Tinta,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "Ingresos de rines y entregas de las próximas dos semanas.",
                style = MaterialTheme.typography.bodyMedium,
                color = Colores.Tinta2,
            )
        }

        TiraDias(
            dias = estado.dias,
            elegido = estado.elegido,
            hoy = estado.hoy,
            alElegir = alElegirDia,
            modifier = Modifier.aparecer(visible, orden = 1),
        )

        if (!estado.cargando) {
            AnimatedContent(
                targetState = estado.elegido,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "día de la agenda",
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .aparecer(visible, orden = 2),
            ) { dia ->
                // Mientras cambia de día, el contenido nuevo usa el estado actual.
                if (dia == estado.elegido) {
                    DiaAgenda(
                        estado = estado,
                        alVerTrabajo = alVerTrabajo,
                        alCambiarBloqueo = alCambiarBloqueo,
                        alLimpiarError = alLimpiarError,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp))
                }
            }
        }
    }
}

// ---------- Tira de días ----------

@Composable
private fun TiraDias(
    dias: List<AgendaLogica.ResumenDia>,
    elegido: LocalDate,
    hoy: LocalDate,
    alElegir: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        items(dias, key = { it.fecha.toEpochDay() }) { dia ->
            FichaDia(dia = dia, elegido = dia.fecha == elegido, esHoy = dia.fecha == hoy, alElegir = { alElegir(dia.fecha) })
        }
    }
}

private val FormaDia = RoundedCornerShape(18.dp)

@Composable
private fun FichaDia(dia: AgendaLogica.ResumenDia, elegido: Boolean, esHoy: Boolean, alElegir: () -> Unit) {
    val fondo by animateColorAsState(if (elegido) Colores.Tinta else Colores.Papel, label = "fondo del día")
    val texto by animateColorAsState(
        when {
            elegido -> Colores.Crema
            dia.cerrado -> Colores.Tinta3
            else -> Colores.Tinta
        },
        label = "texto del día",
    )
    val descripcion = buildString {
        append(if (esHoy) "Hoy, " else "")
        append(Formato.fechaLarga(dia.fecha))
        when {
            dia.cerrado -> append(", cerrado")
            else -> {
                append(", ${dia.ingresos} ${if (dia.ingresos == 1) "ingreso" else "ingresos"}")
                if (dia.entregas > 0) append(", ${dia.entregas} ${if (dia.entregas == 1) "entrega" else "entregas"}")
            }
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(58.dp)
            .clip(FormaDia)
            .background(fondo)
            .border(
                width = if (esHoy && !elegido) 1.5.dp else 1.dp,
                color = if (esHoy && !elegido) Colores.Mantequilla else Colores.Linea,
                shape = FormaDia,
            )
            .selectable(selected = elegido, role = Role.Tab, onClick = alElegir)
            .clearAndSetSemantics { contentDescription = descripcion }
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = if (esHoy) "Hoy" else Formato.diaSemanaCorto(dia.fecha),
            style = MaterialTheme.typography.labelMedium,
            color = texto,
        )
        Text(
            text = dia.fecha.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            color = texto,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 4.dp)
                .height(6.dp),
        ) {
            repeat(minOf(dia.ingresos, 3)) {
                Box(modifier = Modifier.size(6.dp).background(Colores.Mantequilla, CircleShape))
            }
            if (dia.entregas > 0) {
                Box(modifier = Modifier.size(6.dp).background(Colores.Menta, CircleShape))
            }
        }
    }
}

// ---------- Día elegido ----------

@Composable
private fun DiaAgenda(
    estado: AgendaLogica.Estado,
    alVerTrabajo: (String) -> Unit,
    alCambiarBloqueo: (AgendaLogica.Franja) -> Unit,
    alLimpiarError: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text(
                text = (if (estado.elegido == estado.hoy) "Hoy, " else "") + Formato.fechaLarga(estado.elegido),
                style = MaterialTheme.typography.titleLarge,
                color = Colores.Tinta,
                modifier = Modifier.semantics { heading() },
            )
            if (!estado.cerrado) {
                Text(
                    text = listOf(
                        cantidad(estado.ingresos, "ingreso", "ingresos"),
                        cantidad(estado.entregas.size, "entrega", "entregas"),
                        cantidad(estado.libres, "hora libre", "horas libres"),
                    ).joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Colores.Tinta2,
                )
            }
        }

        if (estado.cerrado) {
            EstadoVacio(
                icono = Iconos.Calendario,
                titulo = "Domingo: el taller no abre",
                mensaje = "Los clientes no pueden agendar ingresos este día.",
            )
            return@Column
        }

        AnimatedVisibility(visible = estado.error != null) {
            Aviso(
                titulo = "No se cambió.",
                mensaje = estado.error.orEmpty(),
                tipo = TipoAviso.Error,
                textoAccion = "Entendido",
                alPulsarAccion = alLimpiarError,
            )
        }

        Column {
            EncabezadoSeccion(titulo = "Ingresos")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                var anterior: LocalDateTime? = null
                estado.franjas.forEach { franja ->
                    // Entre la mañana y la tarde, la pausa del almuerzo.
                    val previa = anterior
                    if (previa != null && previa.hour < 12 && franja.inicio.hour >= 14) {
                        Almuerzo()
                    }
                    anterior = franja.inicio
                    FilaFranja(
                        franja = franja,
                        cambiando = estado.cambiando == franja.inicio,
                        ocupado = estado.cambiando != null,
                        alVerTrabajo = alVerTrabajo,
                        alCambiarBloqueo = { alCambiarBloqueo(franja) },
                    )
                }
            }
        }

        Column {
            EncabezadoSeccion(titulo = "Entregas")
            if (estado.entregas.isEmpty()) {
                Text(
                    text = "Sin entregas programadas este día.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Colores.Tinta3,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .background(Colores.Crema2, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    estado.entregas.forEach { orden ->
                        FilaEntrega(orden = orden, alPulsar = { alVerTrabajo(orden.id) })
                    }
                }
            }
        }
    }
}

private fun cantidad(numero: Int, uno: String, varios: String): String =
    if (numero == 1) "1 $uno" else "$numero $varios"

private val AnchoHora = 64.dp
private val FormaFranja = RoundedCornerShape(16.dp)

@Composable
private fun FilaFranja(
    franja: AgendaLogica.Franja,
    cambiando: Boolean,
    ocupado: Boolean,
    alVerTrabajo: (String) -> Unit,
    alCambiarBloqueo: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = Formato.hora(franja.inicio),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = if (franja.pasada) Colores.Tinta3 else Colores.Tinta,
            modifier = Modifier
                .width(AnchoHora)
                .padding(top = 14.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            franja.ingresos.forEach { orden ->
                TarjetaIngreso(orden = orden, alPulsar = { alVerTrabajo(orden.id) })
            }
            when {
                franja.ingresos.isNotEmpty() -> Unit
                franja.bloqueada -> HoraBloqueada(
                    pasada = franja.pasada,
                    cambiando = cambiando,
                    habilitado = !ocupado,
                    alLiberar = alCambiarBloqueo,
                )
                else -> HoraLibre(
                    pasada = franja.pasada,
                    cambiando = cambiando,
                    habilitado = !ocupado && franja.deAtencion,
                    alBloquear = alCambiarBloqueo,
                )
            }
        }
    }
}

@Composable
private fun TarjetaIngreso(orden: Orden, alPulsar: () -> Unit) {
    val recibido = orden.etapa > EtapaOrden.Solicitud
    Tarjeta(
        fondo = if (recibido) Colores.Papel else Colores.MantequillaSuave,
        alPulsar = alPulsar,
        descripcionAccion = "Ver el trabajo ${orden.codigo}",
        relleno = PaddingValues(14.dp),
        espacio = 0,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(if (recibido) Colores.MentaSuave else Colores.Mantequilla, RoundedCornerShape(10.dp)),
            ) {
                Icon(
                    imageVector = if (recibido) Iconos.Hecho else Iconos.Calendario,
                    contentDescription = null,
                    tint = if (recibido) Colores.MentaTexto else Colores.MantequillaTexto,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (recibido) "Recibido ${orden.codigo}" else "Recibir ${orden.codigo}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Colores.Tinta,
                )
                Text(
                    text = "${orden.vehiculo}, ${orden.cliente}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.Tinta2,
                )
                Text(text = orden.servicio, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
            }
        }
    }
}

@Composable
private fun HoraLibre(pasada: Boolean, cambiando: Boolean, habilitado: Boolean, alBloquear: () -> Unit) {
    val linea = Colores.Linea2
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .drawBehind {
                drawRoundRect(
                    color = linea,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                    ),
                )
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = if (pasada) "Sin cita" else "Libre",
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.Tinta3,
            modifier = Modifier.weight(1f),
        )
        if (!pasada) {
            AccionFranja(texto = "Bloquear", cambiando = cambiando, habilitado = habilitado, alPulsar = alBloquear)
        }
    }
}

/** Hora bloqueada: rayas suaves para que se distinga de una libre de un vistazo. */
@Composable
private fun HoraBloqueada(pasada: Boolean, cambiando: Boolean, habilitado: Boolean, alLiberar: () -> Unit) {
    val raya = Colores.Tinta.copy(alpha = 0.06f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(FormaFranja)
            .background(Colores.Crema2)
            .drawBehind {
                val paso = 12.dp.toPx()
                clipRect {
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(
                            color = raya,
                            start = Offset(x, size.height),
                            end = Offset(x + size.height, 0f),
                            strokeWidth = 3.dp.toPx(),
                        )
                        x += paso
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(Iconos.Candado, contentDescription = null, tint = Colores.Tinta2, modifier = Modifier.size(16.dp))
        Text(
            text = "Bloqueada",
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.Tinta2,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        )
        if (!pasada) {
            AccionFranja(texto = "Liberar", cambiando = cambiando, habilitado = habilitado, alPulsar = alLiberar)
        }
    }
}

@Composable
private fun AccionFranja(texto: String, cambiando: Boolean, habilitado: Boolean, alPulsar: () -> Unit) {
    if (cambiando) {
        CircularProgressIndicator(color = Colores.Tinta, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
    } else {
        Enlace(
            texto = texto,
            alPulsar = { if (habilitado) alPulsar() },
            color = if (habilitado) Colores.Tinta else Colores.Tinta3,
        )
    }
}

@Composable
private fun Almuerzo() {
    Text(
        text = "Almuerzo, 12:00 m. a 2:00 p. m.",
        style = MaterialTheme.typography.labelSmall,
        color = Colores.Tinta3,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = AnchoHora + 12.dp, top = 2.dp, bottom = 2.dp),
    )
}

@Composable
private fun FilaEntrega(orden: Orden, alPulsar: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = orden.entregaEstimada?.let(Formato::hora).orEmpty(),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = Colores.Tinta,
            modifier = Modifier
                .width(AnchoHora)
                .padding(top = 14.dp),
        )
        Tarjeta(
            alPulsar = alPulsar,
            descripcionAccion = "Ver el trabajo ${orden.codigo}",
            relleno = PaddingValues(14.dp),
            espacio = 0,
            modifier = Modifier.weight(1f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Colores.MentaSuave, RoundedCornerShape(10.dp)),
                ) {
                    Icon(Iconos.Carro, contentDescription = null, tint = Colores.MentaTexto, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Entregar ${orden.codigo}", style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
                    Text(
                        text = "${orden.vehiculo}, ${orden.cliente}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.Tinta2,
                    )
                }
                Text(
                    text = if (orden.atrasada()) "Atrasado" else orden.etapa.estado,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (orden.atrasada()) Colores.FrenoTexto else Colores.Tinta3,
                )
            }
            EstadoCobro(orden = orden, modifier = Modifier.padding(start = 42.dp, top = 8.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 1400)
@Composable
private fun VistaPreviaAgenda() {
    val ahora = LocalDateTime.now()
    val hoy = ahora.toLocalDate()
    val ordenes = OrdenesDeEjemplo.ordenesIniciales(ahora)
    val manana = hoy.plusDays(1)
    TemaRinFix {
        ContenidoAgenda(
            estado = AgendaLogica.Estado(
                cargando = false,
                hoy = hoy,
                dias = (0 until 7).map {
                    val fecha = hoy.plusDays(it.toLong())
                    AgendaLogica.ResumenDia(fecha, ingresos = it % 3, entregas = it % 2, cerrado = fecha.dayOfWeek.value == 7)
                },
                elegido = manana,
                franjas = listOf(8, 9, 10, 11, 14, 15, 16, 17).map { hora ->
                    AgendaLogica.Franja(
                        inicio = manana.atTime(hora, 0),
                        ingresos = if (hora == 9) ordenes.filter { it.etapa == EtapaOrden.Solicitud }.take(1) else emptyList(),
                        bloqueada = hora == 16,
                        pasada = false,
                    )
                },
                entregas = ordenes.filter { it.entregaEstimada != null && it.activa }.take(1),
            ),
            alElegirDia = {},
            alVerTrabajo = {},
            alCambiarBloqueo = {},
            alLimpiarError = {},
        )
    }
}
