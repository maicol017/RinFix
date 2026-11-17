package com.rinfix.app.cliente

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.Campo
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.FotoRin
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.MiniaturaFoto
import com.rinfix.app.componentes.SelectorFichas
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.componentes.TipoEtiqueta
import com.rinfix.app.componentes.TonoFoto
import com.rinfix.app.componentes.tipoEtiqueta
import com.rinfix.app.datos.AnalisisDeEjemplo
import com.rinfix.app.datos.Catalogo
import com.rinfix.app.datos.Diagnostico
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.GarajeDeEjemplo
import com.rinfix.app.datos.Horario
import com.rinfix.app.datos.OrdenesDeEjemplo
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.PosicionRin
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Solicitar la reparación de un diagnóstico, o agendar una revisión sin diagnóstico.
 * La persona elige qué rin lleva, el día y la hora; al confirmar se crea la orden y se ve
 * la confirmación con el acceso al seguimiento.
 */
@Composable
fun PantallaAgendar(
    alVolver: () -> Unit,
    alVerSeguimiento: (idOrden: String) -> Unit,
    alIrAlInicio: () -> Unit,
    modifier: Modifier = Modifier,
    logica: AgendarLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()

    AnimatedContent(
        targetState = estado.ordenCreada,
        contentKey = { it != null },
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        label = "solicitud",
        modifier = modifier,
    ) { creada ->
        if (creada != null) {
            Confirmacion(orden = creada, alVerSeguimiento = alVerSeguimiento, alIrAlInicio = alIrAlInicio)
        } else {
            Formulario(
                estado = estado,
                alVolver = alVolver,
                alElegirVehiculo = logica::elegirVehiculo,
                alElegirPosicion = logica::elegirPosicion,
                alElegirDia = logica::elegirDia,
                alElegirHora = logica::elegirHora,
                alCambiarNotas = logica::cambiarNotas,
                alEnviar = logica::enviar,
            )
        }
    }
}

@Composable
private fun Formulario(
    estado: AgendarLogica.Estado,
    alVolver: () -> Unit,
    alElegirVehiculo: (String?) -> Unit,
    alElegirPosicion: (PosicionRin) -> Unit,
    alElegirDia: (LocalDate) -> Unit,
    alElegirHora: (java.time.LocalTime) -> Unit,
    alCambiarNotas: (String) -> Unit,
    alEnviar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = recordarEntrada()
    val diagnostico = estado.diagnostico

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        BarraSuperior(
            titulo = if (diagnostico != null) "Solicitar reparación" else "Agendar revisión",
            alVolver = alVolver,
        )
        if (estado.cargando) return@Column

        Column(
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        ) {
            Box(modifier = Modifier.aparecer(visible, orden = 0)) {
                if (diagnostico != null) ResumenDiagnostico(diagnostico) else ResumenRevision()
            }

            if (estado.vehiculos.isNotEmpty()) {
                Seccion(titulo = "Vehículo", modifier = Modifier.aparecer(visible, orden = 1)) {
                    val opciones: List<String?> = estado.vehiculos.map { it.id } + listOf(null)
                    SelectorFichas(
                        opciones = opciones,
                        seleccionada = estado.idVehiculo,
                        alElegir = alElegirVehiculo,
                        texto = { id -> estado.vehiculos.firstOrNull { it.id == id }?.titulo ?: "Otro carro" },
                        enUnaLinea = true,
                    )
                }
            }

            Seccion(titulo = "¿Qué rin llevas?", modifier = Modifier.aparecer(visible, orden = 2)) {
                SelectorFichas(
                    opciones = PosicionRin.entries,
                    seleccionada = estado.posicion,
                    alElegir = alElegirPosicion,
                    texto = { it.nombre },
                )
            }

            Seccion(titulo = "Taller", modifier = Modifier.aparecer(visible, orden = 3)) {
                TarjetaTaller()
            }

            Seccion(titulo = "Día de ingreso", modifier = Modifier.aparecer(visible, orden = 4)) {
                FilaDias(dias = estado.dias, elegido = estado.dia, alElegir = alElegirDia)
            }

            Seccion(titulo = "Hora", modifier = Modifier.aparecer(visible, orden = 5)) {
                RejillaHoras(
                    horas = estado.horas,
                    cargando = estado.cargandoHoras,
                    elegida = estado.hora,
                    alElegir = alElegirHora,
                )
            }

            Campo(
                valor = estado.notas,
                alCambiar = alCambiarNotas,
                etiqueta = "Notas para el taller (opcional)",
                marcador = "Ej.: el carro vibra a más de 80 km/h",
                ayuda = "${estado.notas.length} de ${AgendarLogica.MaximoNotas}",
                lineas = 3,
                habilitado = !estado.enviando,
            )

            AnimatedVisibility(visible = estado.error != null) {
                Aviso(titulo = "Falta un dato.", mensaje = estado.error.orEmpty(), tipo = TipoAviso.Error)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Boton(
                    texto = "Confirmar solicitud",
                    alPulsar = alEnviar,
                    ocupado = estado.enviando,
                    habilitado = estado.listo,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "No pagas nada ahora. El taller confirma el precio después de revisar el rin.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.Tinta3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = Colores.Tinta,
            modifier = Modifier.semantics { heading() },
        )
        contenido()
    }
}

// ---------- Resúmenes ----------

@Composable
private fun ResumenDiagnostico(diagnostico: Diagnostico) {
    Tarjeta(espacio = 0, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val foto = diagnostico.fotos.firstOrNull()
            if (foto != null) {
                MiniaturaFoto(ruta = foto, tamano = 60.dp, radio = 16.dp)
            } else {
                FotoRin(ancho = 60.dp, alto = 60.dp, tamanoRin = 54.dp, tono = TonoFoto.Taller, conDano = true, inclinacion = 20f, radio = 16.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = "Diagnóstico del ${Formato.fechaCorta(diagnostico.fecha)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.Tinta3,
                )
                Text(
                    text = diagnostico.resumen.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    color = Colores.Tinta,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Etiqueta(
                        texto = Formato.rangoPesos(diagnostico.precioMinimo, diagnostico.precioMaximo, corto = true),
                        tipo = TipoEtiqueta.EnProceso,
                        pequena = true,
                    )
                    if (diagnostico.deEjemplo) Etiqueta(texto = "Ejemplo", tipo = TipoEtiqueta.Neutra, pequena = true)
                }
            }
        }
    }
}

/** Sin diagnóstico: se pide una revisión y el taller da el precio exacto. */
@Composable
private fun ResumenRevision() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Colores.LavandaSuave, Formas.Tarjeta)
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(Colores.Lavanda, CircleShape),
        ) {
            Icon(Iconos.Herramienta, contentDescription = null, tint = Colores.LavandaTexto, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(text = "Revisión en el taller", style = MaterialTheme.typography.titleSmall, color = Colores.LavandaTexto)
            Text(
                text = "El taller revisa el rin y te da el precio exacto. Si quieres un estimado antes, analiza una foto.",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.LavandaTexto,
            )
        }
    }
}

@Composable
private fun TarjetaTaller() {
    Tarjeta(espacio = 0, modifier = Modifier.fillMaxWidth()) {
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
                Text(text = Catalogo.Taller, style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
                Text(
                    text = "Cabecera, Bucaramanga. Lunes a viernes de 8 a. m. a 6 p. m.; sábados hasta el mediodía.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.Tinta3,
                )
            }
        }
    }
}

// ---------- Día y hora ----------

@Composable
private fun FilaDias(dias: List<LocalDate>, elegido: LocalDate?, alElegir: (LocalDate) -> Unit) {
    val hoy = LocalDate.now()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
    ) {
        dias.forEach { dia ->
            DiaCita(dia = dia, elegido = dia == elegido, esHoy = dia == hoy, alPulsar = { alElegir(dia) })
        }
    }
}

@Composable
private fun DiaCita(dia: LocalDate, elegido: Boolean, esHoy: Boolean, alPulsar: () -> Unit) {
    val fondo by animateColorAsState(if (elegido) Colores.Tinta else Colores.Papel, label = "fondo del día")
    val texto by animateColorAsState(if (elegido) Colores.Crema else Colores.Tinta, label = "texto del día")
    val forma = RoundedCornerShape(18.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier
            .width(60.dp)
            .clip(forma)
            .background(fondo)
            .border(1.dp, if (elegido) Colores.Tinta else Colores.Linea2, forma)
            .selectable(selected = elegido, role = Role.RadioButton, onClick = alPulsar)
            .semantics(mergeDescendants = true) { contentDescription = Formato.fechaLarga(dia) }
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = if (esHoy) "Hoy" else Formato.diaSemanaCorto(dia),
            style = MaterialTheme.typography.labelSmall,
            color = texto.copy(alpha = 0.8f),
        )
        Text(
            text = dia.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            color = texto,
        )
        Text(text = Formato.mesCorto(dia), style = MaterialTheme.typography.labelSmall, color = texto.copy(alpha = 0.8f))
    }
}

@Composable
private fun RejillaHoras(
    horas: List<Horario>,
    cargando: Boolean,
    elegida: java.time.LocalTime?,
    alElegir: (java.time.LocalTime) -> Unit,
) {
    when {
        cargando -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(96.dp)) {
            CircularProgressIndicator(color = Colores.Tinta, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
        }
        horas.none { it.disponible } -> Text(
            text = "No quedan horas libres este día. Elige otro.",
            style = MaterialTheme.typography.bodySmall,
            color = Colores.Tinta2,
        )
        else -> Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.selectableGroup(),
        ) {
            horas.chunked(4).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    fila.forEach { horario ->
                        FichaHora(
                            horario = horario,
                            elegida = horario.hora == elegida,
                            alPulsar = { alElegir(horario.hora) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - fila.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun FichaHora(horario: Horario, elegida: Boolean, alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    val fondo by animateColorAsState(
        targetValue = when {
            elegida -> Colores.Tinta
            !horario.disponible -> Colores.Crema2
            else -> Colores.Papel
        },
        label = "fondo de la hora",
    )
    val texto = when {
        elegida -> Colores.Crema
        !horario.disponible -> Colores.Tinta3
        else -> Colores.Tinta
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .clip(Formas.Pildora)
            .background(fondo)
            .border(
                1.dp,
                when {
                    elegida -> Colores.Tinta
                    !horario.disponible -> Color.Transparent
                    else -> Colores.Linea2
                },
                Formas.Pildora,
            )
            .selectable(selected = elegida, enabled = horario.disponible, role = Role.RadioButton, onClick = alPulsar)
            .semantics {
                contentDescription = if (horario.disponible) Formato.hora(horario.hora) else "${Formato.hora(horario.hora)}, ocupada"
            },
    ) {
        Text(
            text = Formato.hora(horario.hora),
            style = MaterialTheme.typography.labelMedium.copy(
                textDecoration = if (horario.disponible) null else TextDecoration.LineThrough,
            ),
            color = texto,
        )
    }
}

// ---------- Confirmación ----------

@Composable
private fun Confirmacion(
    orden: Orden,
    alVerSeguimiento: (String) -> Unit,
    alIrAlInicio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vista = LocalView.current
    val enVistaPrevia = LocalInspectionMode.current
    val escala = remember { Animatable(if (enVistaPrevia) 1f else 0.4f) }
    LaunchedEffect(orden.id) {
        vista.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK,
        )
        escala.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 32.dp)
                .size(96.dp)
                .graphicsLayer {
                    scaleX = escala.value
                    scaleY = escala.value
                }
                .background(Colores.Menta, CircleShape),
        ) {
            Icon(Iconos.Hecho, contentDescription = null, tint = Colores.MentaTexto, modifier = Modifier.size(46.dp))
        }
        Text(
            text = "Solicitud enviada",
            style = MaterialTheme.typography.headlineMedium,
            color = Colores.Tinta,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 20.dp)
                .semantics { heading() },
        )
        Text(
            text = textoCita(orden),
            style = MaterialTheme.typography.bodyLarge,
            color = Colores.Tinta2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )

        Tarjeta(
            espacio = 4,
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Orden ${orden.codigo}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Colores.Tinta,
                    modifier = Modifier.weight(1f),
                )
                Etiqueta(texto = orden.etapa.estado, tipo = tipoEtiqueta(orden.etapa), conPunto = true, pequena = true)
            }
            Text(text = "${orden.vehiculo}, ${orden.rin}", style = MaterialTheme.typography.bodySmall, color = Colores.Tinta2)
            Text(text = orden.servicio, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
        ) {
            Boton(texto = "Ver seguimiento", alPulsar = { alVerSeguimiento(orden.id) }, modifier = Modifier.fillMaxWidth())
            Boton(
                texto = "Volver al inicio",
                alPulsar = alIrAlInicio,
                estilo = EstiloBoton.Transparente,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = "Te avisaremos cuando el taller revise el rin y confirme el precio.",
            style = MaterialTheme.typography.labelSmall,
            color = Colores.Tinta3,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** «Te esperamos mañana a las 9:00 a. m. en Taller RinFix Cabecera.» */
private fun textoCita(orden: Orden): String {
    val cita = orden.cita ?: return "El taller te contactará para confirmar el ingreso."
    val hoy = LocalDate.now()
    val dia = when (cita.toLocalDate()) {
        hoy -> "hoy"
        hoy.plusDays(1) -> "mañana"
        else -> "el ${Formato.diaSemanaCorto(cita.toLocalDate()).lowercase()} ${cita.dayOfMonth}"
    }
    return "Te esperamos $dia a las ${Formato.hora(cita)} en ${orden.taller}."
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 1300)
@Composable
private fun VistaPreviaAgendar() {
    val ahora = LocalDateTime.now()
    val dias = AgendarLogica.proximosDias(ahora)
    TemaRinFix {
        Formulario(
            estado = AgendarLogica.Estado(
                cargando = false,
                diagnostico = AnalisisDeEjemplo.diagnosticosIniciales(ahora).getValue("cliente-1").first(),
                vehiculos = GarajeDeEjemplo.vehiculosIniciales().getValue("cliente-1"),
                idVehiculo = "vehiculo-1",
                posicion = PosicionRin.DelanteroIzquierdo,
                dias = dias,
                dia = dias.first(),
                horas = OrdenesDeEjemplo.HorasSemana.mapIndexed { i, hora -> Horario(hora, disponible = i % 3 != 0) },
                hora = OrdenesDeEjemplo.HorasSemana[1],
            ),
            alVolver = {},
            alElegirVehiculo = {},
            alElegirPosicion = {},
            alElegirDia = {},
            alElegirHora = {},
            alCambiarNotas = {},
            alEnviar = {},
        )
    }
}
