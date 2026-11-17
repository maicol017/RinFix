package com.rinfix.app.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rinfix.app.datos.Avance
import com.rinfix.app.datos.EtapaOrden
import com.rinfix.app.datos.FotoAvance
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.MomentoFoto
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.OrdenesDeEjemplo
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix
import java.time.LocalDate
import java.time.LocalDateTime

/** Cómo va cada etapa en la línea de tiempo. */
private enum class EstadoEtapa { Hecha, Actual, Pendiente }

/**
 * Línea de tiempo de una orden, como el seguimiento de un pedido: las 7 etapas en orden,
 * con fecha, nota, fotos y quién lo registró. La etapa actual llega abierta; las demás
 * se abren al tocarlas. `alVerFoto` abre una foto en grande.
 */
@Composable
fun LineaTiempo(
    orden: Orden,
    alVerFoto: (FotoAvance, Avance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val etapas = EtapaOrden.entries
    Column(modifier = modifier.fillMaxWidth()) {
        etapas.forEachIndexed { indice, etapa ->
            val estado = when {
                etapa < orden.etapa -> EstadoEtapa.Hecha
                etapa == orden.etapa && etapa == EtapaOrden.Entregada -> EstadoEtapa.Hecha
                etapa == orden.etapa -> EstadoEtapa.Actual
                else -> EstadoEtapa.Pendiente
            }
            val siguienteAlcanzada = indice < etapas.lastIndex && etapas[indice + 1] <= orden.etapa
            PasoLinea(
                etapa = etapa,
                avance = orden.avances.lastOrNull { it.etapa == etapa },
                estado = estado,
                conLineaArriba = indice > 0,
                conLineaAbajo = indice < etapas.lastIndex,
                lineaAbajoAlcanzada = siguienteAlcanzada,
                alVerFoto = alVerFoto,
            )
        }
    }
}

private val AnchoGuia = 28.dp
private val CentroPunto = 23.dp

@Composable
private fun PasoLinea(
    etapa: EtapaOrden,
    avance: Avance?,
    estado: EstadoEtapa,
    conLineaArriba: Boolean,
    conLineaAbajo: Boolean,
    lineaAbajoAlcanzada: Boolean,
    alVerFoto: (FotoAvance, Avance) -> Unit,
) {
    val tieneDetalle = avance != null && (avance.nota != null || avance.fotos.isNotEmpty() || avance.registradoPor != null)
    var abierto by rememberSaveable(etapa) { mutableStateOf(estado == EstadoEtapa.Actual) }
    val giro by animateFloatAsState(if (abierto) 180f else 0f, label = "flecha ${etapa.name}")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Línea vertical que une los puntos: menta hasta la etapa actual, gris después.
                val x = AnchoGuia.toPx() / 2
                val centro = CentroPunto.toPx()
                val trazo = 2.dp.toPx()
                val alcanzadaArriba = estado != EstadoEtapa.Pendiente
                if (conLineaArriba) {
                    drawLine(
                        color = if (alcanzadaArriba) Colores.Menta else Colores.Linea2,
                        start = Offset(x, 0f),
                        end = Offset(x, centro),
                        strokeWidth = trazo,
                    )
                }
                if (conLineaAbajo) {
                    drawLine(
                        color = if (lineaAbajoAlcanzada) Colores.Menta else Colores.Linea2,
                        start = Offset(x, centro),
                        end = Offset(x, size.height),
                        strokeWidth = trazo,
                    )
                }
            },
    ) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .width(AnchoGuia)
                .padding(top = CentroPunto - 11.dp),
        ) {
            PuntoEtapa(estado = estado)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, bottom = 6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (tieneDetalle) {
                            Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = if (abierto) "Cerrar detalle" else "Ver detalle",
                            ) { abierto = !abierto }
                        } else {
                            Modifier
                        },
                    )
                    .semantics {
                        stateDescription = when (estado) {
                            EstadoEtapa.Hecha -> "Terminada"
                            EstadoEtapa.Actual -> "Etapa actual"
                            EstadoEtapa.Pendiente -> "Pendiente"
                        }
                    },
            ) {
                Text(
                    text = etapa.titulo,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (estado == EstadoEtapa.Actual) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = if (estado == EstadoEtapa.Pendiente) Colores.Tinta3 else Colores.Tinta,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = when {
                        avance != null -> fechaAvance(avance.fecha)
                        estado == EstadoEtapa.Actual -> "Ahora"
                        else -> "Pendiente"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.Tinta3,
                )
                if (tieneDetalle) {
                    Icon(
                        imageVector = Iconos.Abajo,
                        contentDescription = null,
                        tint = Colores.Tinta3,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(16.dp)
                            .graphicsLayer { rotationZ = giro },
                    )
                }
            }

            if (avance != null && tieneDetalle) {
                AnimatedVisibility(
                    visible = abierto,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    DetalleAvance(avance = avance, alVerFoto = alVerFoto)
                }
            }
        }
    }
}

@Composable
private fun DetalleAvance(avance: Avance, alVerFoto: (FotoAvance, Avance) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 12.dp),
    ) {
        avance.nota?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = Colores.Tinta2)
        }
        if (avance.fotos.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                avance.fotos.forEachIndexed { indice, foto ->
                    MiniaturaAvance(
                        foto = foto,
                        indice = indice,
                        alPulsar = { alVerFoto(foto, avance) },
                    )
                }
            }
        }
        avance.registradoPor?.let { quien ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Avatar(nombre = quien.substringBefore(','), tamano = 22.dp, fondo = Colores.Menta, colorTexto = Colores.MentaTexto)
                Text(text = "Registrado por $quien", style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
            }
        }
    }
}

/** Punto de la etapa: menta con chulito si terminó, tinta que late si es la actual, hueco si falta. */
@Composable
private fun PuntoEtapa(estado: EstadoEtapa) {
    when (estado) {
        EstadoEtapa.Hecha -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .background(Colores.Menta, CircleShape),
        ) {
            Icon(Iconos.Hecho, contentDescription = null, tint = Colores.MentaTexto, modifier = Modifier.size(13.dp))
        }

        EstadoEtapa.Actual -> {
            val transicion = rememberInfiniteTransition(label = "etapa actual")
            val pulso by transicion.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    keyframes {
                        durationMillis = 1800
                        0f at 0
                        1f at 1440
                    },
                ),
                label = "pulso",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .drawBehind {
                        drawCircle(
                            color = Colores.Menta.copy(alpha = 0.6f * (1f - pulso)),
                            radius = size.minDimension / 2 + 8.dp.toPx() * pulso,
                        )
                    }
                    .background(Colores.Tinta, CircleShape),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Colores.Crema, CircleShape),
                )
            }
        }

        EstadoEtapa.Pendiente -> Box(
            modifier = Modifier
                .size(22.dp)
                .background(Colores.Papel, CircleShape)
                .border(2.dp, Colores.Linea2, CircleShape),
        )
    }
}

/** Foto pequeña de un avance. Sin archivo (datos de ejemplo), muestra un rin dibujado. */
@Composable
fun MiniaturaAvance(
    foto: FotoAvance,
    indice: Int,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val descripcion = "Foto ${indice + 1}, ${foto.momento.nombre.lowercase()}"
    Box(
        modifier = modifier
            .size(width = 84.dp, height = 66.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClickLabel = "Ver en grande", onClick = alPulsar),
    ) {
        val direccion = foto.direccion
        if (direccion != null) {
            FotoGuardada(ruta = direccion, descripcion = descripcion, modifier = Modifier.matchParentSize())
        } else {
            FotoRin(
                ancho = 84.dp,
                alto = 66.dp,
                tamanoRin = 58.dp,
                tono = tonoDe(foto.momento),
                conDano = foto.momento == MomentoFoto.Antes,
                inclinacion = 10f + indice * 25f,
                radio = 14.dp,
                descripcion = descripcion,
            )
        }
    }
}

/**
 * Foto de un avance en grande, sobre fondo oscuro, con la etapa y el momento.
 * Se cierra tocando fuera o con el botón.
 */
@Composable
fun VisorFotoAvance(foto: FotoAvance, avance: Avance, alCerrar: () -> Unit) {
    val ancho = LocalConfiguration.current.screenWidthDp.dp - 32.dp
    Dialog(
        onDismissRequest = alCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Colores.Tinta.copy(alpha = 0.94f))
                .clickable(role = Role.Button, onClickLabel = "Cerrar foto", onClick = alCerrar),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.align(Alignment.Center),
            ) {
                val direccion = foto.direccion
                if (direccion != null) {
                    FotoGuardada(
                        ruta = direccion,
                        descripcion = "${avance.etapa.titulo}, ${foto.momento.nombre.lowercase()}",
                        modifier = Modifier
                            .size(width = ancho, height = ancho * 0.8f)
                            .clip(RoundedCornerShape(22.dp)),
                    )
                } else {
                    FotoRin(
                        ancho = ancho,
                        alto = ancho * 0.8f,
                        tamanoRin = ancho * 0.66f,
                        tono = tonoDe(foto.momento),
                        conDano = foto.momento == MomentoFoto.Antes,
                        inclinacion = 30f,
                        radio = 22.dp,
                        descripcion = "${avance.etapa.titulo}, ${foto.momento.nombre.lowercase()}",
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Etiqueta(texto = foto.momento.nombre, tipo = TipoEtiqueta.Vidrio, pequena = true)
                    Text(
                        text = "${avance.etapa.titulo}, ${fechaAvance(avance.fecha)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Colores.Crema,
                    )
                }
            }
            BotonIcono(
                icono = Iconos.Cerrar,
                descripcion = "Cerrar foto",
                alPulsar = alCerrar,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
            )
        }
    }
}

private fun tonoDe(momento: MomentoFoto): TonoFoto = when (momento) {
    MomentoFoto.Antes -> TonoFoto.Banco
    MomentoFoto.Durante -> TonoFoto.Clara
    MomentoFoto.Despues -> TonoFoto.Estudio
}

/** «Hoy, 10:42 a. m.» o «14 nov, 9:12 a. m.» */
internal fun fechaAvance(fecha: LocalDateTime, hoy: LocalDate = LocalDate.now()): String =
    if (fecha.toLocalDate() == hoy) "Hoy, ${Formato.hora(fecha)}" else "${Formato.fechaCorta(fecha)}, ${Formato.hora(fecha)}"

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, heightDp = 760)
@Composable
private fun VistaPreviaLineaTiempo() {
    TemaRinFix {
        LineaTiempo(
            orden = OrdenesDeEjemplo.ordenesIniciales(LocalDateTime.now()).first(),
            alVerFoto = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}

