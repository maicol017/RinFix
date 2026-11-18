package com.rinfix.app.cliente

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.EstadoVacio
import com.rinfix.app.componentes.FotoRin
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.Logo
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.componentes.TonoFoto
import com.rinfix.app.componentes.lineaPunteadaArriba
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.MetodoPago
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.OrdenesDeEjemplo
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import com.rinfix.app.diseno.sombra
import java.time.LocalDateTime

/**
 * Pago SIMULADO de una orden: resumen del cobro, método de prueba y confirmación.
 * No se cobra dinero real ni se piden datos de tarjeta. Al aprobar, abre el comprobante.
 */
@Composable
fun PantallaPago(
    alVolver: () -> Unit,
    alVerComprobante: (idComprobante: String) -> Unit,
    modifier: Modifier = Modifier,
    logica: PagoLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()

    LaunchedEffect(estado.comprobante?.id) {
        estado.comprobante?.let { alVerComprobante(it.id) }
    }

    val orden = estado.orden
    val total = orden?.total
    when {
        estado.cargando -> Column(modifier = modifier.fillMaxSize()) {
            BarraSuperior(titulo = "Pagar servicio", alVolver = alVolver)
        }
        orden == null || total == null -> Column(modifier = modifier.fillMaxSize()) {
            BarraSuperior(titulo = "Pagar servicio", alVolver = alVolver)
            EstadoVacio(
                icono = Iconos.TarjetaPago,
                titulo = "Todavía no hay nada por pagar",
                mensaje = "El taller confirma el total después de revisar el rin. Te avisaremos cuando esté listo.",
                fondoIcono = Colores.Menta,
                colorIcono = Colores.MentaTexto,
            )
        }
        orden.pagada && estado.fase == PagoLogica.Fase.Eligiendo -> Column(modifier = modifier.fillMaxSize()) {
            BarraSuperior(titulo = "Pagar servicio", alVolver = alVolver)
            EstadoVacio(
                icono = Iconos.Hecho,
                titulo = "Esta orden ya está pagada",
                mensaje = "No tienes que hacer nada más. Puedes ver el estado del pago en el seguimiento de la orden.",
                textoAccion = "Volver",
                alPulsarAccion = alVolver,
                fondoIcono = Colores.Menta,
                colorIcono = Colores.MentaTexto,
            )
        }
        else -> ContenidoPago(
            orden = orden,
            total = total,
            metodo = estado.metodo,
            fase = estado.fase,
            error = estado.error,
            alVolver = alVolver,
            alElegirMetodo = logica::elegirMetodo,
            alPagar = logica::pagar,
            modifier = modifier,
        )
    }
}

@Composable
private fun ContenidoPago(
    orden: Orden,
    total: Long,
    metodo: MetodoPago,
    fase: PagoLogica.Fase,
    error: String?,
    alVolver: () -> Unit,
    alElegirMetodo: (MetodoPago) -> Unit,
    alPagar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = recordarEntrada()
    val procesando = fase != PagoLogica.Fase.Eligiendo

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            BarraSuperior(titulo = "Pagar servicio", alVolver = alVolver)
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 130.dp),
            ) {
                AvisoDemostracion(modifier = Modifier.aparecer(visible, orden = 0))
                ResumenCobro(orden = orden, total = total, modifier = Modifier.aparecer(visible, orden = 1))
                Text(
                    text = "Método de pago",
                    style = MaterialTheme.typography.titleLarge,
                    color = Colores.Tinta,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .semantics { heading() }
                        .aparecer(visible, orden = 2),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .selectableGroup()
                        .aparecer(visible, orden = 3),
                ) {
                    MetodoPago.entries.forEach { opcion ->
                        OpcionMetodo(
                            metodo = opcion,
                            elegido = opcion == metodo,
                            habilitado = !procesando,
                            alPulsar = { alElegirMetodo(opcion) },
                        )
                    }
                }
                AnimatedVisibility(visible = error != null) {
                    Aviso(titulo = "No se completó.", mensaje = error.orEmpty(), tipo = TipoAviso.Error)
                }
            }
        }

        BarraPagar(
            total = total,
            metodo = metodo,
            procesando = procesando,
            alPagar = alPagar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        AnimatedVisibility(visible = procesando, enter = fadeIn(), exit = fadeOut()) {
            CapaProcesando(fase = fase, total = total, metodo = metodo)
        }
    }
}

@Composable
private fun AvisoDemostracion(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(Colores.MantequillaSuave, RoundedCornerShape(18.dp))
            .border(1.dp, Colores.Mantequilla, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(Iconos.Informacion, contentDescription = null, tint = Colores.MantequillaTexto, modifier = Modifier.size(20.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Colores.Tinta)) { append("Modo demostración. ") }
                append("No se cobra dinero real ni se guardan datos de tarjetas.")
            },
            style = MaterialTheme.typography.bodySmall,
            color = Colores.MantequillaTexto,
        )
    }
}

@Composable
private fun ResumenCobro(orden: Orden, total: Long, modifier: Modifier = Modifier) {
    Tarjeta(espacio = 0, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FotoRin(ancho = 52.dp, alto = 52.dp, tamanoRin = 46.dp, tono = TonoFoto.Clara, inclinacion = 30f, radio = 14.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Orden ${orden.codigo}", style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
                Text(text = "${orden.vehiculo}, ${orden.rin}", style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 14.dp),
        ) {
            orden.detalleTotal.forEach { linea ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            Text(
                text = "Total, IVA incluido",
                style = MaterialTheme.typography.titleSmall,
                color = Colores.Tinta,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Formato.pesos(total),
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                color = Colores.Tinta,
            )
        }
    }
}

@Composable
private fun OpcionMetodo(metodo: MetodoPago, elegido: Boolean, habilitado: Boolean, alPulsar: () -> Unit) {
    val borde by animateColorAsState(if (elegido) Colores.Tinta else Colores.Linea, label = "borde del método")
    val fondoIcono by animateColorAsState(if (elegido) Colores.Mantequilla else Colores.Crema, label = "ícono del método")
    val punto by animateFloatAsState(
        targetValue = if (elegido) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "punto del método",
    )
    val forma = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Colores.Papel)
            .border(if (elegido) 2.dp else 1.5.dp, borde, forma)
            .selectable(selected = elegido, enabled = habilitado, role = Role.RadioButton, onClick = alPulsar)
            .padding(14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(fondoIcono, RoundedCornerShape(14.dp)),
        ) {
            Icon(iconoDe(metodo), contentDescription = null, tint = Colores.Tinta, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = metodo.nombre, style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
            Text(text = metodo.detalle, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .border(2.dp, if (elegido) Colores.Tinta else Colores.Linea2, CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = punto
                        scaleY = punto
                    }
                    .background(Colores.Tinta, CircleShape),
            )
        }
    }
}

private fun iconoDe(metodo: MetodoPago): ImageVector = when (metodo) {
    MetodoPago.Tarjeta -> Iconos.TarjetaPago
    MetodoPago.Pse -> Iconos.Banco
    MetodoPago.Billetera -> Iconos.Billetera
    MetodoPago.EnTaller -> Iconos.Taller
}

private val FormaBarra = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
private fun BarraPagar(
    total: Long,
    metodo: MetodoPago,
    procesando: Boolean,
    alPagar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .sombra(Sombra.Alta, FormaBarra)
            .clip(FormaBarra)
            .background(Colores.Papel)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp),
    ) {
        Column {
            Text(text = "Total", style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
            Text(
                text = Formato.pesos(total),
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                color = Colores.Tinta,
            )
        }
        Boton(
            texto = if (metodo == MetodoPago.EnTaller) "Pagar al recoger" else "Pagar ${Formato.pesos(total)}",
            alPulsar = alPagar,
            icono = if (metodo == MetodoPago.EnTaller) Iconos.Taller else Iconos.Candado,
            ocupado = procesando,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Mientras «procesa»: el logo gira, cambian los textos y termina con el chulito. */
@Composable
private fun CapaProcesando(fase: PagoLogica.Fase, total: Long, metodo: MetodoPago) {
    val enTaller = metodo == MetodoPago.EnTaller
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Colores.Tinta.copy(alpha = 0.45f))
            // Bloquea los toques mientras se procesa.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .fillMaxWidth(0.82f)
                .sombra(Sombra.Alta, RoundedCornerShape(28.dp))
                .background(Colores.Papel, RoundedCornerShape(28.dp))
                .padding(28.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            AnimatedContent(
                targetState = fase == PagoLogica.Fase.Aprobado,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.5f)) togetherWith fadeOut() },
                label = "icono del pago",
            ) { aprobado ->
                if (aprobado) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(66.dp)
                            .background(Colores.Menta, CircleShape),
                    ) {
                        Icon(Iconos.Hecho, contentDescription = null, tint = Colores.MentaTexto, modifier = Modifier.size(34.dp))
                    }
                } else {
                    val giro = rememberInfiniteTransition(label = "giro del logo")
                    val angulo by giro.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
                        label = "ángulo",
                    )
                    Logo(tamano = 58.dp, modifier = Modifier.graphicsLayer { rotationZ = angulo })
                }
            }
            Text(
                text = when (fase) {
                    PagoLogica.Fase.Validando -> if (enTaller) "Registrando tu elección" else fase.texto
                    PagoLogica.Fase.Autorizando -> if (enTaller) "Avisando al taller" else fase.texto
                    PagoLogica.Fase.Aprobado -> if (enTaller) "Listo" else fase.texto
                    PagoLogica.Fase.Eligiendo -> ""
                },
                style = MaterialTheme.typography.titleLarge,
                color = Colores.Tinta,
                textAlign = TextAlign.Center,
            )
            Text(
                text = when {
                    fase != PagoLogica.Fase.Aprobado -> "Pago simulado, sin cobro real"
                    enTaller -> "Pagarás ${Formato.pesos(total)} al recoger tu rin"
                    else -> "${Formato.pesos(total)} con ${metodo.nombre.replaceFirstChar { it.lowercase() }}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Colores.Tinta3,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 1000)
@Composable
private fun VistaPreviaPago() {
    val orden = OrdenesDeEjemplo.ordenesIniciales(LocalDateTime.now()).first()
    TemaRinFix {
        ContenidoPago(
            orden = orden,
            total = orden.total ?: 0,
            metodo = MetodoPago.Tarjeta,
            fase = PagoLogica.Fase.Eligiendo,
            error = null,
            alVolver = {},
            alElegirMetodo = {},
            alPagar = {},
        )
    }
}
