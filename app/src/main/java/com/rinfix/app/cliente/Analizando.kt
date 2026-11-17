package com.rinfix.app.cliente

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.FotoGuardada
import com.rinfix.app.componentes.FotoRin
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.componentes.TipoEtiqueta
import com.rinfix.app.componentes.TonoFoto
import com.rinfix.app.componentes.zonaEnPantalla
import com.rinfix.app.datos.AvanceAnalisis
import com.rinfix.app.datos.Caja
import com.rinfix.app.datos.EstadoPaso
import com.rinfix.app.datos.PasoAnalisis
import com.rinfix.app.datos.PasoEnCurso
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.sombra
import kotlinx.coroutines.delay

/**
 * Análisis paso a paso: revisión de la foto, detección, interpretación con IA y cotización.
 * Cada paso se muestra apenas termina; al final abre el resultado sola.
 */
@Composable
fun PantallaAnalizando(
    alVolver: () -> Unit,
    alVerResultado: (idDiagnostico: String) -> Unit,
    modifier: Modifier = Modifier,
    logica: AnalizandoLogica = viewModel(),
) {
    val avance by logica.avance.collectAsState()
    val vista = LocalView.current

    // Al terminar, vibra de confirmación y abre el resultado tras un momento.
    LaunchedEffect(avance.idDiagnostico) {
        val id = avance.idDiagnostico ?: return@LaunchedEffect
        vista.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK,
        )
        delay(900)
        alVerResultado(id)
    }

    ContenidoAnalizando(
        avance = avance,
        alVolver = alVolver,
        alVerResultado = alVerResultado,
        modifier = modifier,
    )
}

@Composable
private fun ContenidoAnalizando(
    avance: AvanceAnalisis,
    alVolver: () -> Unit,
    alVerResultado: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        BarraSuperior(titulo = "Análisis del rin", alVolver = alVolver)
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        ) {
            if (avance.deEjemplo) {
                Aviso(
                    titulo = "Análisis de ejemplo.",
                    mensaje = "Mientras conectamos el análisis real, los daños y precios son de ejemplo: no salen de tu foto.",
                    tipo = TipoAviso.Informacion,
                )
            }

            FotoEnAnalisis(avance = avance)
            TarjetaPasos(pasos = avance.pasos)

            val error = avance.error
            if (error != null) {
                Aviso(titulo = "No pudimos analizar.", mensaje = error, tipo = TipoAviso.Error)
                Boton(texto = "Volver a la cámara", alPulsar = alVolver, modifier = Modifier.fillMaxWidth())
            }

            AnimatedVisibility(
                visible = avance.idDiagnostico != null,
                enter = fadeIn() + slideInVertically { it / 3 },
            ) {
                Boton(
                    texto = "Ver resultados",
                    alPulsar = { avance.idDiagnostico?.let(alVerResultado) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = "Cada paso aparece apenas termina.",
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Tinta3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------- Foto con lo que va encontrando el análisis ----------

private val FormaFoto = RoundedCornerShape(26.dp)

@Composable
private fun FotoEnAnalisis(avance: AvanceAnalisis, modifier: Modifier = Modifier) {
    var tamanoImagen by remember { mutableStateOf<IntSize?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .sombra(Sombra.Media, FormaFoto)
            .clip(FormaFoto),
    ) {
        val foto = avance.foto
        if (foto != null) {
            FotoGuardada(
                ruta = foto,
                descripcion = "Foto del rin que enviaste",
                alCargar = { tamanoImagen = it },
                modifier = Modifier.matchParentSize(),
            )
        } else {
            FotoRin(
                ancho = maxWidth,
                alto = maxHeight,
                tamanoRin = 240.dp,
                tono = TonoFoto.Taller,
                conDano = true,
                inclinacion = 20f,
                radio = 0.dp,
            )
        }

        RecorteZona(visible = avance.interpretando, modifier = Modifier.matchParentSize())
        BandaEscaneo(visible = avance.interpretando, modifier = Modifier.matchParentSize())

        val densidad = LocalDensity.current
        val contenedor = with(densidad) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        avance.zonas.forEachIndexed { indice, (etiqueta, caja) ->
            MarcaZona(
                etiqueta = etiqueta,
                caja = caja,
                tamanoImagen = if (foto != null) tamanoImagen else null,
                contenedor = contenedor,
                orden = indice,
            )
        }

        AnimatedVisibility(
            visible = avance.fotoRevisada,
            enter = fadeIn() + slideInVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        ) {
            Etiqueta(texto = "Foto nítida", tipo = TipoEtiqueta.Blanca, icono = Iconos.Hecho, pequena = true)
        }
    }
}

/** Recuadro de una zona detectada: entra con un pequeño rebote; punteado si está por confirmar. */
@Composable
private fun MarcaZona(
    etiqueta: String,
    caja: Caja,
    tamanoImagen: IntSize?,
    contenedor: Size,
    orden: Int,
) {
    val densidad = LocalDensity.current
    val zona = zonaEnPantalla(caja, tamanoImagen, contenedor)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(orden * 160L)
        visible = true
    }
    val progreso by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "zona $etiqueta",
    )
    val dudosa = etiqueta.startsWith("¿")

    with(densidad) {
        Box(
            modifier = Modifier
                .offset(x = zona.left.toDp(), y = zona.top.toDp())
                .size(width = zona.width.toDp(), height = zona.height.toDp())
                .graphicsLayer {
                    alpha = progreso.coerceIn(0f, 1f)
                    val escala = 1.25f - 0.25f * progreso
                    scaleX = escala
                    scaleY = escala
                }
                .drawBehind {
                    val trazo = 2.5.dp.toPx()
                    drawRoundRect(
                        color = Colores.Tinta.copy(alpha = 0.22f),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(width = trazo + 6.dp.toPx()),
                    )
                    drawRoundRect(
                        color = Colores.Lavanda,
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(
                            width = trazo,
                            pathEffect = if (dudosa) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else null,
                        ),
                    )
                },
        ) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Colores.LavandaTexto,
                maxLines = 1,
                modifier = Modifier
                    .offset(y = (-26).dp)
                    .background(Colores.Lavanda, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

/** Mientras se interpreta: oscurece fuera de la zona del rin, que es lo único que se envía. */
@Composable
private fun RecorteZona(visible: Boolean, modifier: Modifier = Modifier) {
    val opacidad by animateFloatAsState(if (visible) 1f else 0f, tween(300), label = "recorte")
    Canvas(modifier = modifier.graphicsLayer { alpha = opacidad }) {
        val margenX = 52.dp.toPx()
        val margenY = 28.dp.toPx()
        val sombra = Colores.Tinta.copy(alpha = 0.28f)
        drawRect(sombra, topLeft = Offset.Zero, size = Size(size.width, margenY))
        drawRect(sombra, topLeft = Offset(0f, size.height - margenY), size = Size(size.width, margenY))
        drawRect(sombra, topLeft = Offset(0f, margenY), size = Size(margenX, size.height - margenY * 2))
        drawRect(
            sombra,
            topLeft = Offset(size.width - margenX, margenY),
            size = Size(margenX, size.height - margenY * 2),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(margenX, margenY),
            size = Size(size.width - margenX * 2, size.height - margenY * 2),
            cornerRadius = CornerRadius(20.dp.toPx()),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

/** Banda lavanda que recorre la foto de arriba abajo mientras la IA interpreta. */
@Composable
private fun BandaEscaneo(visible: Boolean, modifier: Modifier = Modifier) {
    val opacidad by animateFloatAsState(if (visible) 1f else 0f, tween(300), label = "banda")
    if (opacidad == 0f) return
    val transicion = rememberInfiniteTransition(label = "escaneo")
    val recorrido by transicion.animateFloat(
        initialValue = -0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "recorrido",
    )
    Canvas(modifier = modifier.graphicsLayer { alpha = opacidad }) {
        val alto = size.height * 0.34f
        val arriba = size.height * recorrido
        drawRect(
            brush = Brush.verticalGradient(
                0f to Colores.Lavanda.copy(alpha = 0f),
                0.8f to Colores.Lavanda.copy(alpha = 0.5f),
                1f to Colores.LavandaSuave.copy(alpha = 0.95f),
                startY = arriba,
                endY = arriba + alto,
            ),
            topLeft = Offset(0f, arriba),
            size = Size(size.width, alto),
        )
    }
}

// ---------- Pasos ----------

@Composable
private fun TarjetaPasos(pasos: List<PasoEnCurso>, modifier: Modifier = Modifier) {
    Tarjeta(
        relleno = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        espacio = 0,
        modifier = modifier.fillMaxWidth(),
    ) {
        pasos.forEachIndexed { indice, paso ->
            if (indice > 0) HorizontalDivider(thickness = 1.dp, color = Colores.Linea)
            FilaPaso(paso = paso)
        }
    }
}

@Composable
private fun FilaPaso(paso: PasoEnCurso) {
    val opacidad by animateFloatAsState(
        targetValue = if (paso.estado == EstadoPaso.Pendiente) 0.55f else 1f,
        animationSpec = tween(300),
        label = "paso ${paso.paso.name}",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = opacidad }
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        IndicadorPaso(estado = paso.estado)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = paso.paso.titulo, style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
            AnimatedContent(
                targetState = paso.detalle to paso.estado,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "detalle del paso",
            ) { (detalle, estado) ->
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (estado == EstadoPaso.Listo) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = when (estado) {
                        EstadoPaso.Listo -> Colores.MentaTexto
                        EstadoPaso.Fallo -> Colores.FrenoTexto
                        else -> Colores.Tinta3
                    },
                )
            }
        }
        Icon(
            imageVector = when (paso.paso) {
                PasoAnalisis.Foto, PasoAnalisis.Deteccion -> Iconos.Celular
                PasoAnalisis.Interpretacion -> Iconos.Nube
                PasoAnalisis.Cotizacion -> Iconos.Recibo
            },
            contentDescription = null,
            tint = Colores.Tinta3,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun IndicadorPaso(estado: EstadoPaso) {
    val escala by animateFloatAsState(
        targetValue = if (estado == EstadoPaso.Listo) 1f else 0.6f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "indicador",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp)) {
        when (estado) {
            EstadoPaso.Pendiente -> Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, Colores.Linea2, CircleShape),
            )
            EstadoPaso.EnCurso -> CircularProgressIndicator(
                color = Colores.LavandaTexto,
                trackColor = Colores.Lavanda,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp),
            )
            EstadoPaso.Listo -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = escala
                        scaleY = escala
                    }
                    .background(Colores.Menta, CircleShape),
            ) {
                Icon(Iconos.Hecho, contentDescription = null, tint = Colores.MentaTexto, modifier = Modifier.size(14.dp))
            }
            EstadoPaso.Fallo -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .background(Colores.FrenoSuave, CircleShape),
            ) {
                Icon(Iconos.Cerrar, contentDescription = null, tint = Colores.FrenoTexto, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 900)
@Composable
private fun VistaPreviaAnalizando() {
    TemaRinFix {
        ContenidoAnalizando(
            avance = AvanceAnalisis(
                pasos = listOf(
                    PasoEnCurso(PasoAnalisis.Foto, EstadoPaso.Listo, "Nitidez y luz correctas"),
                    PasoEnCurso(PasoAnalisis.Deteccion, EstadoPaso.Listo, "2 zonas posibles"),
                    PasoEnCurso(PasoAnalisis.Interpretacion, EstadoPaso.EnCurso, "Interpretando la zona con Claude"),
                    PasoEnCurso(PasoAnalisis.Cotizacion),
                ),
                zonas = listOf(
                    "Rayón" to Caja(0.58f, 0.18f, 0.24f, 0.24f),
                    "¿Deformación?" to Caja(0.2f, 0.62f, 0.2f, 0.2f),
                ),
                fotoRevisada = true,
                interpretando = true,
                deEjemplo = true,
            ),
            alVolver = {},
            alVerResultado = {},
        )
    }
}
