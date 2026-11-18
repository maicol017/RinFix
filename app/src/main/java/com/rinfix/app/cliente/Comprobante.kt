package com.rinfix.app.cliente

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.EstadoVacio
import com.rinfix.app.componentes.EsqueletoDetalle
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.datos.Comprobante
import com.rinfix.app.datos.EstadoPago
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.MetodoPago
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import com.rinfix.app.diseno.sombra
import java.time.LocalDateTime

/**
 * Comprobante DEMOSTRATIVO de un pago simulado. Lleva la marca «Simulado» y no vale como factura.
 */
@Composable
fun PantallaComprobante(
    alVolver: () -> Unit,
    alVerSeguimiento: (idOrden: String) -> Unit,
    modifier: Modifier = Modifier,
    logica: ComprobanteLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    val comprobante = estado.comprobante
    val contexto = LocalContext.current

    when {
        estado.cargando -> Column(modifier = modifier.fillMaxSize()) {
            BarraSuperior(titulo = "Comprobante", alVolver = alVolver)
            EsqueletoDetalle()
        }
        comprobante == null -> Column(modifier = modifier.fillMaxSize()) {
            BarraSuperior(titulo = "Comprobante", alVolver = alVolver)
            EstadoVacio(
                icono = Iconos.Recibo,
                titulo = "No encontramos este comprobante",
                mensaje = "Puede que sea de otra cuenta. Revisa el estado del pago en el seguimiento de tu orden.",
                textoAccion = "Volver",
                alPulsarAccion = alVolver,
                fondoIcono = Colores.Menta,
                colorIcono = Colores.MentaTexto,
            )
        }
        else -> ContenidoComprobante(
            comprobante = comprobante,
            alVolver = alVolver,
            alVerSeguimiento = { alVerSeguimiento(comprobante.idOrden) },
            alCompartir = {
                val envio = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Comprobante RinFix ${comprobante.referencia}")
                    putExtra(Intent.EXTRA_TEXT, textoParaCompartir(comprobante))
                }
                contexto.startActivity(Intent.createChooser(envio, "Compartir comprobante"))
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun ContenidoComprobante(
    comprobante: Comprobante,
    alVolver: () -> Unit,
    alVerSeguimiento: () -> Unit,
    alCompartir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = recordarEntrada()
    val enTaller = comprobante.estado == EstadoPago.EnTaller

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        BarraSuperior(titulo = "Comprobante", alVolver = alVolver)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        ) {
            ChuloAprobado()
            Text(
                text = if (enTaller) "Listo, pagas al recoger" else "Pago simulado aprobado",
                style = MaterialTheme.typography.headlineSmall,
                color = Colores.Tinta,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .semantics { heading() }
                    .aparecer(visible, orden = 1),
            )
            Text(
                text = if (enTaller) {
                    "El taller ya sabe que pagarás ${Formato.pesos(comprobante.monto)} cuando recojas tu rin."
                } else {
                    "Te avisaremos cuando tu rin esté listo para recoger."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Colores.Tinta3,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .aparecer(visible, orden = 1),
            )

            Tiquete(
                comprobante = comprobante,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .aparecer(visible, orden = 2),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(top = 24.dp)
                    .aparecer(visible, orden = 3),
            ) {
                Boton(
                    texto = "Volver al seguimiento",
                    alPulsar = alVerSeguimiento,
                    icono = Iconos.Lista,
                    modifier = Modifier.fillMaxWidth(),
                )
                Boton(
                    texto = "Compartir comprobante",
                    alPulsar = alCompartir,
                    estilo = EstiloBoton.Blanco,
                    icono = Iconos.Compartir,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Círculo menta que «salta» al entrar, con el chulito. */
@Composable
private fun ChuloAprobado() {
    val escala = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        escala.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(84.dp)
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
            }
            .background(Colores.Menta, CircleShape),
    ) {
        Icon(Iconos.Hecho, contentDescription = null, tint = Colores.MentaTexto, modifier = Modifier.size(40.dp))
    }
}

private val FormaTiquete = RoundedCornerShape(24.dp)

/** El comprobante como un tiquete: datos arriba, corte perforado y total abajo. */
@Composable
private fun Tiquete(comprobante: Comprobante, modifier: Modifier = Modifier) {
    val enTaller = comprobante.estado == EstadoPago.EnTaller
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sombra(Sombra.Media, FormaTiquete)
            .clip(FormaTiquete)
            .background(Colores.Papel),
    ) {
        Box {
            // Marca de agua: deja claro que no es un comprobante real.
            Text(
                text = "Simulado",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Colores.Tinta.copy(alpha = 0.05f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .rotate(-18f)
                    .clearAndSetSemantics { },
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Colores.MantequillaSuave, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Icon(Iconos.Informacion, contentDescription = null, tint = Colores.MantequillaTexto, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (enTaller) "Constancia simulada" else "Transacción simulada",
                        style = MaterialTheme.typography.labelMedium,
                        color = Colores.MantequillaTexto,
                    )
                }
                DatoTiquete("Referencia", comprobante.referencia)
                DatoTiquete("Fecha", "${Formato.fechaLarga(comprobante.fecha.toLocalDate())}, ${Formato.hora(comprobante.fecha)}")
                DatoTiquete("Método", comprobante.metodo.enComprobante)
                DatoTiquete("Orden", comprobante.codigoOrden)
                DatoTiquete("Estado", if (enTaller) "Pendiente, en el taller" else "Aprobado")
            }
        }

        CortePerforado()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        ) {
            Text(
                text = if (enTaller) "Por pagar" else "Total pagado",
                style = MaterialTheme.typography.titleSmall,
                color = Colores.Tinta,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Formato.pesos(comprobante.monto),
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                color = Colores.Tinta,
            )
        }
    }
}

@Composable
private fun DatoTiquete(nombre: String, valor: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Text(text = nombre, style = MaterialTheme.typography.bodySmall, color = Colores.Tinta3)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = valor,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = Colores.Tinta,
            textAlign = TextAlign.End,
        )
    }
}

/** Dos muescas color crema en los bordes y una línea punteada entre ellas. */
@Composable
private fun CortePerforado() {
    val linea = Colores.Linea2
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .drawBehind {
                val margen = 20.dp.toPx()
                drawLine(
                    color = linea,
                    start = Offset(margen, size.height / 2),
                    end = Offset(size.width - margen, size.height / 2),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                )
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-12).dp)
                .size(24.dp)
                .background(Colores.Crema, CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 12.dp)
                .size(24.dp)
                .background(Colores.Crema, CircleShape),
        )
    }
}

private fun textoParaCompartir(comprobante: Comprobante): String = buildString {
    appendLine("Comprobante RinFix (demostración, sin cobro real)")
    appendLine("Referencia: ${comprobante.referencia}")
    appendLine("Orden: ${comprobante.codigoOrden}")
    appendLine("Método: ${comprobante.metodo.enComprobante}")
    appendLine("Fecha: ${Formato.fechaLarga(comprobante.fecha.toLocalDate())}, ${Formato.hora(comprobante.fecha)}")
    append(
        if (comprobante.estado == EstadoPago.EnTaller) "Por pagar en el taller: " else "Total: ",
    )
    append(Formato.pesos(comprobante.monto))
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 900)
@Composable
private fun VistaPreviaComprobante() {
    TemaRinFix {
        ContenidoComprobante(
            comprobante = Comprobante(
                id = "comprobante-1",
                referencia = "DEMO-7K2Q9M",
                usuarioId = "cliente-1",
                idOrden = "orden-1042",
                codigoOrden = "RF-1042",
                metodo = MetodoPago.Tarjeta,
                monto = 210_000,
                fecha = LocalDateTime.now(),
                estado = EstadoPago.Aprobado,
            ),
            alVolver = {},
            alVerSeguimiento = {},
            alCompartir = {},
        )
    }
}
