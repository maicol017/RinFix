package com.rinfix.app.cliente

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.BotonIcono
import com.rinfix.app.componentes.EstadoVacio
import com.rinfix.app.componentes.EsqueletoDetalle
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.FotoGuardada
import com.rinfix.app.componentes.FotoRin
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.componentes.TipoEtiqueta
import com.rinfix.app.componentes.TonoFoto
import com.rinfix.app.componentes.zonaEnPantalla
import com.rinfix.app.datos.AnalisisDeEjemplo
import com.rinfix.app.datos.Catalogo
import com.rinfix.app.datos.Dano
import com.rinfix.app.datos.Diagnostico
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.LineaCotizacion
import com.rinfix.app.datos.MaterialRin
import com.rinfix.app.datos.SolicitudAnalisis
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Movimiento
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import com.rinfix.app.diseno.sombra
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * Diagnóstico preliminar: foto con los hallazgos numerados, gravedad, cada hallazgo explicado,
 * la cotización del catálogo del taller y la acción para solicitar la reparación.
 */
@Composable
fun PantallaResultado(
    alVolver: () -> Unit,
    alSolicitar: (idDiagnostico: String) -> Unit,
    alPreguntar: () -> Unit,
    modifier: Modifier = Modifier,
    logica: ResultadoLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    val diagnostico = estado.diagnostico
    when {
        estado.cargando -> Column(modifier = modifier.fillMaxSize()) {
            BarraSuperior(titulo = "Diagnóstico preliminar", alVolver = alVolver)
            EsqueletoDetalle()
        }
        diagnostico == null -> Column(modifier = modifier.fillMaxSize()) {
            BarraSuperior(titulo = "Diagnóstico preliminar", alVolver = alVolver)
            EstadoVacio(
                icono = Iconos.Ia,
                titulo = "No encontramos este diagnóstico",
                mensaje = "Puede que se haya borrado. Analiza el rin de nuevo para ver un resultado.",
                fondoIcono = Colores.Lavanda,
                colorIcono = Colores.LavandaTexto,
            )
        }
        else -> ContenidoResultado(
            diagnostico = diagnostico,
            alVolver = alVolver,
            alSolicitar = { alSolicitar(diagnostico.id) },
            alPreguntar = {
                logica.preguntarAlAsistente()
                alPreguntar()
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun ContenidoResultado(
    diagnostico: Diagnostico,
    alVolver: () -> Unit,
    alSolicitar: () -> Unit,
    alPreguntar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = recordarEntrada()
    val contexto = LocalContext.current
    var seleccionado by rememberSaveable { mutableIntStateOf(0) }

    fun compartir() {
        val intento = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, resumenParaCompartir(diagnostico))
        }
        contexto.startActivity(Intent.createChooser(intento, "Compartir diagnóstico"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        BarraSuperior(
            titulo = "Diagnóstico preliminar",
            alVolver = alVolver,
            accion = { BotonIcono(icono = Iconos.Compartir, descripcion = "Compartir diagnóstico", alPulsar = ::compartir) },
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        ) {
            if (diagnostico.deEjemplo) {
                Aviso(
                    titulo = "Resultado de ejemplo.",
                    mensaje = "Los daños y precios de esta pantalla son de ejemplo mientras conectamos el análisis real. " +
                        "No salen de tu foto.",
                    tipo = TipoAviso.Informacion,
                )
            }

            FotoHallazgos(
                diagnostico = diagnostico,
                seleccionado = seleccionado,
                alElegir = { seleccionado = it },
                modifier = Modifier.aparecer(visible, orden = 0),
            )
            Leyenda(conModelo = diagnostico.deEjemplo, modifier = Modifier.aparecer(visible, orden = 1))
            TarjetaGravedad(diagnostico = diagnostico, modifier = Modifier.aparecer(visible, orden = 2))

            diagnostico.danos.forEachIndexed { indice, dano ->
                TarjetaHallazgo(
                    numero = indice + 1,
                    dano = dano,
                    conModelo = diagnostico.deEjemplo,
                    seleccionado = indice == seleccionado,
                    alPulsar = { seleccionado = indice },
                    modifier = Modifier.aparecer(visible, orden = 3 + indice),
                )
            }

            if (diagnostico.danos.isEmpty()) {
                SinDanosVisibles(modifier = Modifier.aparecer(visible, orden = 3))
            } else {
                TarjetaCotizacion(
                    diagnostico = diagnostico,
                    modifier = Modifier.aparecer(visible, orden = 3 + diagnostico.danos.size),
                )
            }

            diagnostico.alerta?.let { alerta ->
                Aviso(titulo = "Recomendamos revisión presencial.", mensaje = alerta, tipo = TipoAviso.Error)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Boton(
                    texto = if (diagnostico.danos.isEmpty()) "Pedir una revisión" else "Solicitar reparación",
                    alPulsar = alSolicitar,
                    modifier = Modifier.fillMaxWidth(),
                )
                Boton(
                    texto = "Preguntar al asistente",
                    alPulsar = alPreguntar,
                    estilo = EstiloBoton.Blanco,
                    icono = Iconos.Ia,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = "Este diagnóstico es preliminar y no reemplaza la inspección de seguridad del taller.",
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Tinta3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------- Foto con hallazgos ----------

private val FormaFoto = RoundedCornerShape(26.dp)

@Composable
private fun FotoHallazgos(
    diagnostico: Diagnostico,
    seleccionado: Int,
    alElegir: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tamanoImagen by remember { mutableStateOf<IntSize?>(null) }
    val foto = diagnostico.fotos.firstOrNull()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(262.dp)
            .sombra(Sombra.Media, FormaFoto)
            .clip(FormaFoto),
    ) {
        if (foto != null) {
            FotoGuardada(
                ruta = foto,
                descripcion = "Foto del rin analizada",
                alCargar = { tamanoImagen = it },
                modifier = Modifier.matchParentSize(),
            )
        } else {
            FotoRin(
                ancho = maxWidth,
                alto = maxHeight,
                tamanoRin = 220.dp,
                tono = TonoFoto.Taller,
                conDano = true,
                inclinacion = 20f,
                radio = 0.dp,
            )
        }

        val densidad = LocalDensity.current
        val contenedor = with(densidad) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        diagnostico.danos.forEachIndexed { indice, dano ->
            val caja = dano.caja ?: return@forEachIndexed
            val zona = zonaEnPantalla(caja, if (foto != null) tamanoImagen else null, contenedor)
            val elegido = indice == seleccionado
            with(densidad) {
                MarcaHallazgo(
                    numero = indice + 1,
                    nombre = dano.tipo.nombre,
                    elegido = elegido,
                    alPulsar = { alElegir(indice) },
                    modifier = Modifier.offset(
                        x = zona.center.x.toDp() - 22.dp,
                        y = zona.center.y.toDp() - 22.dp,
                    ),
                )
            }
        }

        Etiqueta(
            texto = "Toca un número",
            tipo = TipoEtiqueta.Vidrio,
            icono = Iconos.Ampliar,
            pequena = true,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(Colores.Tinta.copy(alpha = 0.35f), RoundedCornerShape(50)),
        )
    }
}

/** Número sobre la foto. El elegido se oscurece, crece un poco y muestra el nombre del daño. */
@Composable
private fun MarcaHallazgo(
    numero: Int,
    nombre: String,
    elegido: Boolean,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val escala by animateFloatAsState(
        targetValue = if (elegido) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "marca $numero",
    )
    val fondo by animateColorAsState(if (elegido) Colores.Tinta else Colores.Papel, label = "fondo de la marca")
    val texto by animateColorAsState(if (elegido) Colores.Crema else Colores.Tinta, label = "texto de la marca")

    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Ver hallazgo $numero", onClick = alPulsar)
                .semantics { contentDescription = "Hallazgo $numero: $nombre" },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        scaleX = escala
                        scaleY = escala
                    }
                    .sombra(Sombra.Media, CircleShape)
                    .background(fondo, CircleShape)
                    .border(2.dp, Colores.Papel, CircleShape),
            ) {
                Text(
                    text = numero.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = texto,
                )
            }
        }
        AnimatedVisibility(
            visible = elegido,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.offset(x = (-8).dp, y = (-34).dp),
        ) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Colores.Crema,
                maxLines = 1,
                modifier = Modifier
                    .background(Colores.Tinta, RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
                    .clearAndSetSemantics { },
            )
        }
    }
}

// ---------- Leyenda y gravedad ----------

@OptIn(ExperimentalLayoutApi::class)
/**
 * Quién hizo cada parte. `conModelo` es para los diagnósticos de ejemplo, que cuentan cómo será
 * con el modelo del teléfono; en el análisis real de hoy, Claude detecta e interpreta.
 */
@Composable
private fun Leyenda(conModelo: Boolean, modifier: Modifier = Modifier) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (conModelo) {
            ItemLeyenda(Iconos.Capas, "Detección en el teléfono")
            ItemLeyenda(Iconos.Ia, "Interpretación de Claude")
        } else {
            ItemLeyenda(Iconos.Ia, "Detección e interpretación de Claude")
        }
        ItemLeyenda(Iconos.Recibo, "Precio del catálogo")
    }
}

@Composable
private fun ItemLeyenda(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icono, contentDescription = null, tint = Colores.Tinta2, modifier = Modifier.size(15.dp))
        Text(text = texto, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta2)
    }
}

@Composable
private fun TarjetaGravedad(diagnostico: Diagnostico, modifier: Modifier = Modifier) {
    Tarjeta(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                IndicadorGravedad(indice = diagnostico.indiceGravedad)
                Row(modifier = Modifier.width(140.dp)) {
                    Text("Leve", style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3, modifier = Modifier.weight(1f))
                    Text("Alta", style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Gravedad estimada", style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
                Text(
                    text = diagnostico.gravedad.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Colores.Tinta,
                )
                Text(
                    text = "Según lo visible en la foto. El taller la confirma.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.Tinta2,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Medio círculo de leve (menta) a alta (freno) con una aguja que llega a la gravedad estimada. */
@Composable
private fun IndicadorGravedad(indice: Float) {
    val enVistaPrevia = LocalInspectionMode.current
    val aguja = remember { Animatable(if (enVistaPrevia) indice else 0f) }
    LaunchedEffect(indice) {
        aguja.animateTo(indice, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow))
    }
    Canvas(
        modifier = Modifier
            .size(width = 140.dp, height = 78.dp)
            .semantics { contentDescription = "Gravedad ${(indice * 100).toInt()} de 100" },
    ) {
        val grosor = 12.dp.toPx()
        val radio = size.width / 2 - grosor / 2
        val centro = Offset(size.width / 2, size.height - 4.dp.toPx())
        val esquina = Offset(centro.x - radio, centro.y - radio)
        val area = Size(radio * 2, radio * 2)
        val tramos = listOf(Colores.Menta, Colores.Mantequilla, Colores.Freno.copy(alpha = 0.75f))
        tramos.forEachIndexed { i, color ->
            drawArc(
                color = color,
                startAngle = 180f + i * 60f + 2f,
                sweepAngle = 56f,
                useCenter = false,
                topLeft = esquina,
                size = area,
                style = Stroke(width = grosor, cap = StrokeCap.Round),
            )
        }
        val angulo = Math.toRadians((180.0 + 180.0 * aguja.value.coerceIn(0f, 1f)))
        val punta = Offset(
            x = centro.x + (radio * 0.78f) * cos(angulo).toFloat(),
            y = centro.y + (radio * 0.78f) * sin(angulo).toFloat(),
        )
        drawLine(Colores.Tinta, centro, punta, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Colores.Tinta, radius = 6.dp.toPx(), center = centro)
    }
}

// ---------- Hallazgos ----------

@Composable
private fun TarjetaHallazgo(
    numero: Int,
    dano: Dano,
    conModelo: Boolean,
    seleccionado: Boolean,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borde by animateColorAsState(
        targetValue = if (seleccionado) Colores.Tinta else Color.Transparent,
        label = "borde del hallazgo",
    )
    Tarjeta(
        alPulsar = alPulsar,
        descripcionAccion = "Ver en la foto",
        espacio = 10,
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, borde, RoundedCornerShape(22.dp)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .background(Colores.Tinta, CircleShape),
            ) {
                Text(
                    text = numero.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Colores.Crema,
                )
            }
            Text(
                text = dano.descripcion,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.5.sp),
                color = Colores.Tinta,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (dano.confirmado) {
                Etiqueta(texto = dano.gravedad.nombre, tipo = TipoEtiqueta.EnProceso, pequena = true)
            } else {
                Etiqueta(texto = "Por confirmar", tipo = TipoEtiqueta.Ia, pequena = true)
            }
        }

        // Quién lo vio y qué tan seguro está.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (conModelo) Coincidencia(icono = Iconos.Celular, texto = "Modelo", confirma = true)
            Coincidencia(icono = Iconos.Ia, texto = "Claude", confirma = dano.confirmado)
            Text(
                text = "Confianza ${(dano.confianza * 100).roundToInt()} %",
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Tinta3,
            )
        }

        if (dano.explicacion.isNotBlank()) {
            Text(text = dano.explicacion, style = MaterialTheme.typography.bodySmall, color = Colores.Tinta2)
        }

        Catalogo.servicios[dano.tipo]?.let { servicio ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(Colores.Crema2, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Icon(Iconos.Herramienta, contentDescription = null, tint = Colores.Tinta2, modifier = Modifier.size(16.dp))
                Text(
                    text = if (dano.confirmado) servicio.nombre else "${servicio.nombre}, solo si se confirma",
                    style = MaterialTheme.typography.labelMedium,
                    color = Colores.Tinta,
                )
            }
        }
    }
}

@Composable
private fun Coincidencia(icono: ImageVector, texto: String, confirma: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .background(if (confirma) Colores.MentaSuave else Colores.LavandaSuave, RoundedCornerShape(50))
            .heightIn(min = 26.dp)
            .padding(horizontal = 9.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = if (confirma) "$texto: lo detectó" else "$texto: no lo pudo confirmar"
            },
    ) {
        Icon(icono, contentDescription = null, tint = if (confirma) Colores.MentaTexto else Colores.LavandaTexto, modifier = Modifier.size(14.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (confirma) Colores.MentaTexto else Colores.LavandaTexto,
        )
        Icon(
            imageVector = if (confirma) Iconos.Hecho else Iconos.Informacion,
            contentDescription = null,
            tint = if (confirma) Colores.MentaTexto else Colores.LavandaTexto,
            modifier = Modifier.size(12.dp),
        )
    }
}

/** Cuando Claude no ve daños: lo dice, sin cotización, y recuerda lo que una foto no muestra. */
@Composable
private fun SinDanosVisibles(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(Colores.MentaSuave, RoundedCornerShape(26.dp))
            .padding(18.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(Colores.Menta, CircleShape),
        ) {
            Icon(Iconos.Hecho, contentDescription = null, tint = Colores.MentaTexto, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "No vemos daños en la foto",
                style = MaterialTheme.typography.titleMedium,
                color = Colores.Tinta,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "Si notas vibración, ruidos o pérdida de aire, pide una revisión: hay daños que una foto no muestra.",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.MentaTexto,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// ---------- Cotización ----------

private val FormaCotizacion = RoundedCornerShape(30.dp)

/** Tarjeta mantequilla con el rango que sube contando, los días y el desglose plegable. */
@Composable
private fun TarjetaCotizacion(diagnostico: Diagnostico, modifier: Modifier = Modifier) {
    var desglose by rememberSaveable { mutableStateOf(false) }
    val enVistaPrevia = LocalInspectionMode.current
    val conteo = remember { Animatable(if (enVistaPrevia) 1f else 0f) }
    LaunchedEffect(diagnostico.id) {
        conteo.animateTo(1f, tween(durationMillis = 1000, delayMillis = 400, easing = Movimiento.Suave))
    }
    val giro by animateFloatAsState(if (desglose) 180f else 0f, label = "flecha del desglose")
    fun contar(valor: Long): Long = ((valor * conteo.value) / 1_000).roundToLong() * 1_000

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .sombra(Sombra.Media, FormaCotizacion)
            .clip(FormaCotizacion)
            .background(
                Brush.linearGradient(
                    0f to Color(0xFFFEF0B8),
                    0.55f to Colores.Mantequilla,
                    1f to Color(0xFFF8DE7E),
                ),
            )
            .drawBehind {
                // Aro decorativo en la esquina, como una llanta vista de frente.
                drawCircle(
                    color = Color.White.copy(alpha = 0.22f),
                    radius = 77.dp.toPx(),
                    center = Offset(size.width + 30.dp.toPx(), -30.dp.toPx()),
                    style = Stroke(width = 26.dp.toPx()),
                )
            }
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Cotización estimada",
                style = MaterialTheme.typography.labelLarge,
                color = Colores.MantequillaTexto,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            Etiqueta(texto = "Orientativa", tipo = TipoEtiqueta.Blanca, pequena = true)
        }
        Text(
            text = "${Formato.pesos(contar(diagnostico.precioMinimo))} – ${Formato.pesos(contar(diagnostico.precioMaximo))}",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp, fontFeatureSettings = "tnum"),
            color = Colores.Tinta,
            modifier = Modifier.semantics {
                contentDescription = "Entre ${Formato.pesos(diagnostico.precioMinimo)} y ${Formato.pesos(diagnostico.precioMaximo)}"
            },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Iconos.Reloj, contentDescription = null, tint = Colores.MantequillaTexto, modifier = Modifier.size(16.dp))
            Text(
                text = textoDias(diagnostico.diasMinimos, diagnostico.diasMaximos),
                style = MaterialTheme.typography.bodySmall,
                color = Colores.MantequillaTexto,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(top = 4.dp)
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = { desglose = !desglose }),
        ) {
            Text(
                text = if (desglose) "Ocultar desglose" else "Ver desglose",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Colores.Tinta,
            )
            Icon(
                imageVector = Iconos.Abajo,
                contentDescription = null,
                tint = Colores.Tinta,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = giro },
            )
        }
        AnimatedVisibility(visible = desglose, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                diagnostico.cotizacion.forEach { linea -> FilaCotizacion(linea) }
                Text(
                    text = "Tarifas del ${diagnostico.taller}. Claude explica los hallazgos, pero no fija precios.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.MantequillaTexto,
                )
            }
        }
    }
}

@Composable
private fun FilaCotizacion(linea: LineaCotizacion) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = linea.servicio, style = MaterialTheme.typography.bodyMedium, color = Colores.Tinta)
            linea.detalle?.let {
                Text(text = it, style = MaterialTheme.typography.labelSmall, color = Colores.MantequillaTexto)
            }
        }
        Text(
            text = if (linea.incluido) "Incluido" else Formato.rangoPesos(linea.minimo, linea.maximo),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = Colores.Tinta,
        )
    }
}

private fun textoDias(minimo: Int, maximo: Int): String = when {
    minimo == maximo && minimo == 1 -> "1 día hábil"
    minimo == maximo -> "$minimo días hábiles"
    else -> "$minimo a $maximo días hábiles"
}

/** Texto corto para compartir por WhatsApp o correo. */
private fun resumenParaCompartir(diagnostico: Diagnostico): String = buildString {
    append("Diagnóstico preliminar RinFix (${Formato.fechaCorta(diagnostico.fecha)})")
    diagnostico.vehiculo?.let { append("\nVehículo: $it") }
    append("\nHallazgos:")
    diagnostico.danos.forEachIndexed { indice, dano ->
        val estado = if (dano.confirmado) dano.gravedad.nombre.lowercase() else "por confirmar"
        append("\n${indice + 1}. ${dano.descripcion} ($estado)")
    }
    if (diagnostico.danos.isEmpty()) {
        append("\nNo se ven daños en la foto.")
    } else {
        append("\nCotización estimada: ${Formato.rangoPesos(diagnostico.precioMinimo, diagnostico.precioMaximo)}")
        append(", ${textoDias(diagnostico.diasMinimos, diagnostico.diasMaximos)}.")
    }
    append("\nEs preliminar: el taller confirma en la inspección.")
    if (diagnostico.deEjemplo) append("\n(Datos de ejemplo)")
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 1700)
@Composable
private fun VistaPreviaResultado() {
    TemaRinFix {
        ContenidoResultado(
            diagnostico = AnalisisDeEjemplo.armarDiagnostico(
                id = "vista",
                solicitud = SolicitudAnalisis(
                    fotos = emptyList(),
                    material = MaterialRin.Aleacion,
                    pulgadas = 17,
                    vehiculo = "Mazda 3 2021",
                ),
                danos = AnalisisDeEjemplo.danosDeEjemplo(),
            ),
            alVolver = {},
            alSolicitar = {},
            alPreguntar = {},
        )
    }
}
