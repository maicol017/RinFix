package com.rinfix.app.cliente

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.abrirEnMapas
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.FotoRin
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.MiniaturaFoto
import com.rinfix.app.componentes.TamanoBoton
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TarjetaOrden
import com.rinfix.app.componentes.TipoEtiqueta
import com.rinfix.app.componentes.TonoFoto
import com.rinfix.app.componentes.lineaPunteadaArriba
import com.rinfix.app.datos.AccionRespuesta
import com.rinfix.app.datos.Autor
import com.rinfix.app.datos.Catalogo
import com.rinfix.app.datos.Destino
import com.rinfix.app.datos.Diagnostico
import com.rinfix.app.datos.FilaTabla
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.Mensaje
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.OrdenesDeEjemplo
import com.rinfix.app.datos.Pieza
import com.rinfix.app.datos.Servidor
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import com.rinfix.app.diseno.sombra
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.sin

/**
 * Asistente del cliente: conversación con preguntas sugeridas y respuestas con tarjetas
 * (órdenes, diagnósticos, tarifas) y botones que llevan a la pantalla indicada.
 * Hoy responde con reglas de ejemplo y lo dice en pantalla.
 */
@Composable
fun PantallaAsistente(
    alAnalizar: () -> Unit,
    alVerOrden: (idOrden: String) -> Unit,
    alVerDiagnostico: (idDiagnostico: String) -> Unit,
    alAgendar: (idDiagnostico: String?) -> Unit,
    alPagar: (idOrden: String) -> Unit,
    modifier: Modifier = Modifier,
    logica: AsistenteLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    val contexto = LocalContext.current
    ContenidoAsistente(
        estado = estado,
        alEnviar = logica::enviar,
        alReiniciar = logica::reiniciar,
        alAnalizar = alAnalizar,
        alIr = { destino ->
            when (destino) {
                Destino.Analizar -> alAnalizar()
                is Destino.Agendar -> alAgendar(destino.idDiagnostico)
                is Destino.Seguimiento -> alVerOrden(destino.idOrden)
                is Destino.VerDiagnostico -> alVerDiagnostico(destino.idDiagnostico)
                is Destino.Pagar -> alPagar(destino.idOrden)
                Destino.ComoLlegar -> abrirEnMapas(contexto, Catalogo.DireccionTaller)
            }
        },
        modifier = modifier,
    )
}

private val FormaPersona = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 6.dp, bottomStart = 22.dp)
private val FormaAsistente = RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp)
private val FormaCampo = RoundedCornerShape(24.dp)
private val AnchoMaximo = 340.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContenidoAsistente(
    estado: AsistenteLogica.Estado,
    alEnviar: (String) -> Boolean,
    alReiniciar: () -> Unit,
    alAnalizar: () -> Unit,
    alIr: (Destino) -> Unit,
    modifier: Modifier = Modifier,
) {
    var texto by rememberSaveable { mutableStateOf("") }
    val lista = rememberLazyListState()
    val mensajes = estado.mensajes
    val ultimo = mensajes.lastOrNull()
    val sugerenciasFinales =
        if (!estado.escribiendo && ultimo != null && ultimo.autor == Autor.Asistente) ultimo.sugerencias else emptyList()

    // Los mensajes que ya estaban al abrir la pestaña no se animan; los nuevos aparecen por partes, una vez.
    var yaEstaban by remember { mutableStateOf<Set<String>?>(null) }
    val animados = remember { mutableSetOf<String>() }
    LaunchedEffect(estado.cargando) {
        if (!estado.cargando && yaEstaban == null) yaEstaban = mensajes.map { it.id }.toSet()
    }

    // Índices de la lista: 0 es el encabezado, luego un elemento por mensaje y al final
    // «escribiendo» o las sugerencias.
    val indiceUltimoMensaje = mensajes.size
    val indiceFinal = mensajes.size + if (estado.escribiendo || sugerenciasFinales.isNotEmpty()) 1 else 0
    LaunchedEffect(mensajes.size, estado.escribiendo) {
        if (mensajes.isNotEmpty()) {
            // Una respuesta nueva se muestra desde su inicio; lo demás, hasta el final.
            val destino = if (ultimo?.autor == Autor.Asistente) indiceUltimoMensaje else indiceFinal
            lista.animateScrollToItem(destino)
        }
    }
    val tecladoAbierto = WindowInsets.isImeVisible
    LaunchedEffect(tecladoAbierto) {
        if (tecladoAbierto && mensajes.isNotEmpty()) lista.animateScrollToItem(indiceFinal)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Encabezado(
            escribiendo = estado.escribiendo,
            hayMensajes = mensajes.isNotEmpty(),
            alReiniciar = alReiniciar,
        )
        LazyColumn(
            state = lista,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (mensajes.isEmpty()) {
                if (!estado.cargando) {
                    item(key = "bienvenida") {
                        Bienvenida(
                            nombre = estado.nombre,
                            sugerencias = estado.sugerencias,
                            alElegir = { alEnviar(it) },
                        )
                    }
                }
            } else {
                item(key = "inicio") { InicioConversacion(fecha = mensajes.first().fecha) }
                items(mensajes, key = { it.id }) { mensaje ->
                    val animar = remember(mensaje.id) {
                        yaEstaban?.let { mensaje.id !in it } == true && animados.add(mensaje.id)
                    }
                    if (mensaje.autor == Autor.Persona) {
                        BurbujaPersona(mensaje = mensaje, modifier = Modifier.animateItem())
                    } else {
                        RespuestaAsistente(
                            mensaje = mensaje,
                            ordenes = estado.ordenes,
                            diagnosticos = estado.diagnosticos,
                            animar = animar,
                            alIr = alIr,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                if (estado.escribiendo) {
                    item(key = "escribiendo") { Escribiendo(modifier = Modifier.animateItem()) }
                } else if (sugerenciasFinales.isNotEmpty()) {
                    item(key = "sugerencias-${ultimo?.id}") {
                        FilaSugerencias(
                            sugerencias = sugerenciasFinales,
                            alElegir = { alEnviar(it) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
        BarraEscribir(
            texto = texto,
            alCambiar = { texto = it.take(AsistenteLogica.MaximoTexto) },
            alEnviar = { if (alEnviar(texto)) texto = "" },
            alAnalizar = alAnalizar,
            ocupado = estado.escribiendo,
        )
    }
}

@Composable
private fun Encabezado(escribiendo: Boolean, hayMensajes: Boolean, alReiniciar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
    ) {
        AvatarAsistente(tamano = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Asistente",
                style = MaterialTheme.typography.titleLarge,
                color = Colores.Tinta,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = when {
                    escribiendo -> "Escribiendo…"
                    Servidor.configurado -> "Con Claude"
                    else -> "Responde con datos de ejemplo"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (escribiendo) Colores.LavandaTexto else Colores.Tinta3,
            )
        }
        if (hayMensajes) {
            BotonRedondo(
                icono = Iconos.Reintentar,
                descripcion = "Empezar una conversación nueva",
                alPulsar = alReiniciar,
            )
        }
    }
}

/** Círculo lavanda con el ícono de la IA. */
@Composable
private fun AvatarAsistente(tamano: Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamano)
            .background(Colores.Lavanda, CircleShape),
    ) {
        Icon(Iconos.Ia, contentDescription = null, tint = Colores.LavandaTexto, modifier = Modifier.size(tamano * 0.52f))
    }
}

@Composable
private fun BotonRedondo(
    icono: ImageVector,
    descripcion: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    fondo: Color = Colores.Papel,
    color: Color = Colores.Tinta,
    conBorde: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(fondo)
            .then(if (conBorde) Modifier.border(1.dp, Colores.Linea2, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClickLabel = descripcion, onClick = alPulsar)
            .semantics { contentDescription = descripcion },
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
    }
}

// ---------- Sin mensajes ----------

@Composable
private fun Bienvenida(nombre: String, sugerencias: List<String>, alElegir: (String) -> Unit) {
    val visible = recordarEntrada()
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .aparecer(visible, orden = 0)
                .fillMaxWidth()
                .background(Colores.LavandaSuave, RoundedCornerShape(28.dp))
                .padding(22.dp),
        ) {
            AvatarAsistente(tamano = 52.dp)
            Text(
                text = if (nombre.isBlank()) "Hola" else "Hola, $nombre",
                style = MaterialTheme.typography.headlineSmall,
                color = Colores.Tinta,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .semantics { heading() },
            )
            Text(
                text = "Pregúntame cómo va tu reparación, qué significa un diagnóstico o cuánto cuesta arreglar un rin.",
                style = MaterialTheme.typography.bodyMedium,
                color = Colores.Tinta2,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Text(
            text = "Para empezar",
            style = MaterialTheme.typography.titleSmall,
            color = Colores.Tinta,
            modifier = Modifier
                .aparecer(visible, orden = 1)
                .padding(start = 4.dp, top = 8.dp),
        )
        sugerencias.forEachIndexed { indice, sugerencia ->
            SugerenciaGrande(
                texto = sugerencia,
                alPulsar = { alElegir(sugerencia) },
                modifier = Modifier.aparecer(visible, orden = indice + 2),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .aparecer(visible, orden = sugerencias.size + 2)
                .padding(start = 4.dp, end = 4.dp, top = 6.dp),
        ) {
            Icon(Iconos.Informacion, contentDescription = null, tint = Colores.Tinta3, modifier = Modifier.size(16.dp))
            Text(
                text = "El asistente orienta, pero no reemplaza la revisión del taller. " +
                    if (Servidor.configurado) "Responde con Claude." else "En esta versión responde con datos de ejemplo.",
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Tinta3,
            )
        }
    }
}

@Composable
private fun SugerenciaGrande(texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    Tarjeta(
        alPulsar = alPulsar,
        descripcionAccion = "Preguntar: $texto",
        relleno = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        espacio = 0,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(Colores.LavandaSuave, RoundedCornerShape(12.dp)),
            ) {
                Icon(Iconos.Chat, contentDescription = null, tint = Colores.LavandaTexto, modifier = Modifier.size(18.dp))
            }
            Text(
                text = texto,
                style = MaterialTheme.typography.titleSmall,
                color = Colores.Tinta,
                modifier = Modifier.weight(1f),
            )
            Icon(Iconos.Siguiente, contentDescription = null, tint = Colores.Tinta3, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------- Conversación ----------

@Composable
private fun InicioConversacion(fecha: LocalDateTime) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Text(text = Formato.diaYHora(fecha), style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(Colores.LavandaSuave, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(Iconos.Informacion, contentDescription = null, tint = Colores.LavandaTexto, modifier = Modifier.size(14.dp))
            Text(
                text = if (Servidor.configurado) {
                    "Responde Claude. El asistente orienta, pero no reemplaza la revisión del taller."
                } else {
                    "Respuestas de ejemplo. El asistente orienta, pero no reemplaza la revisión del taller."
                },
                style = MaterialTheme.typography.labelSmall,
                color = Colores.LavandaTexto,
            )
        }
    }
}

@Composable
private fun BurbujaPersona(mensaje: Mensaje, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 48.dp),
    ) {
        Text(
            text = mensaje.texto,
            style = MaterialTheme.typography.bodyLarge,
            color = Colores.Crema,
            modifier = Modifier
                .background(Colores.Tinta, FormaPersona)
                .padding(horizontal = 16.dp, vertical = 11.dp)
                .clearAndSetSemantics { contentDescription = "Tú: ${mensaje.texto}" },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RespuestaAsistente(
    mensaje: Mensaje,
    ordenes: Map<String, Orden>,
    diagnosticos: Map<String, Diagnostico>,
    animar: Boolean,
    alIr: (Destino) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = if (animar) recordarEntrada() else true
    val dentro = mensaje.piezas.filter { it is Pieza.Tabla || it is Pieza.Puntos || it is Pieza.Cuidado }
    val fuera = mensaje.piezas.filter { it is Pieza.OrdenRelacionada || it is Pieza.DiagnosticoRelacionado }
    var turno = 1

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 12.dp),
    ) {
        AvatarAsistente(tamano = 30.dp, modifier = Modifier.aparecer(visible, orden = 0))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .aparecer(visible, orden = turno++)
                    .widthIn(max = AnchoMaximo)
                    .sombra(Sombra.Baja, FormaAsistente)
                    .background(Colores.Papel, FormaAsistente)
                    .padding(16.dp),
            ) {
                Text(text = mensaje.texto, style = MaterialTheme.typography.bodyLarge, color = Colores.Tinta)
                dentro.forEach { pieza ->
                    val aparicion = Modifier.aparecer(visible, orden = turno++)
                    when (pieza) {
                        is Pieza.Tabla -> PiezaTabla(tabla = pieza, modifier = aparicion)
                        is Pieza.Puntos -> PiezaPuntos(puntos = pieza.puntos, modifier = aparicion)
                        is Pieza.Cuidado -> PiezaCuidado(texto = pieza.texto, modifier = aparicion)
                        else -> Unit
                    }
                }
            }
            fuera.forEach { pieza ->
                when (pieza) {
                    is Pieza.OrdenRelacionada -> ordenes[pieza.idOrden]?.let { orden ->
                        TarjetaOrden(
                            orden = orden,
                            alPulsar = { alIr(Destino.Seguimiento(orden.id)) },
                            modifier = Modifier
                                .aparecer(visible, orden = turno++)
                                .widthIn(max = AnchoMaximo),
                        )
                    }
                    is Pieza.DiagnosticoRelacionado -> diagnosticos[pieza.idDiagnostico]?.let { diagnostico ->
                        TarjetaDiagnostico(
                            diagnostico = diagnostico,
                            alPulsar = { alIr(Destino.VerDiagnostico(diagnostico.id)) },
                            modifier = Modifier
                                .aparecer(visible, orden = turno++)
                                .widthIn(max = AnchoMaximo),
                        )
                    }
                    else -> Unit
                }
            }
            if (mensaje.acciones.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .aparecer(visible, orden = turno++)
                        .padding(top = 2.dp),
                ) {
                    mensaje.acciones.forEachIndexed { indice, accion ->
                        Boton(
                            texto = accion.texto,
                            alPulsar = { alIr(accion.destino) },
                            estilo = if (indice == 0) EstiloBoton.Lavanda else EstiloBoton.Blanco,
                            tamano = TamanoBoton.Pequeno,
                            icono = iconoDe(accion.destino),
                        )
                    }
                }
            }
            Text(
                text = Formato.hora(mensaje.fecha) + if (mensaje.deEjemplo) ", respuesta de ejemplo" else "",
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Tinta3,
                modifier = Modifier
                    .aparecer(visible, orden = turno)
                    .padding(start = 4.dp),
            )
        }
    }
}

private fun iconoDe(destino: Destino): ImageVector = when (destino) {
    Destino.Analizar -> Iconos.Camara
    is Destino.Agendar -> Iconos.Calendario
    is Destino.Seguimiento -> Iconos.Lista
    is Destino.VerDiagnostico -> Iconos.Ia
    is Destino.Pagar -> Iconos.TarjetaPago
    Destino.ComoLlegar -> Iconos.Ubicacion
}

@Composable
private fun PiezaTabla(tabla: Pieza.Tabla, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (tabla.dePrecios) Colores.MantequillaSuave else Colores.Crema, forma)
            .padding(14.dp),
    ) {
        Text(
            text = tabla.titulo,
            style = MaterialTheme.typography.titleSmall,
            color = Colores.Tinta,
            modifier = Modifier.semantics { heading() },
        )
        tabla.filas.forEachIndexed { indice, fila ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .padding(top = if (indice == 0) 10.dp else 8.dp)
                    .fillMaxWidth()
                    .then(if (indice > 0) Modifier.lineaPunteadaArriba().padding(top = 8.dp) else Modifier)
                    .semantics(mergeDescendants = true) {},
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = fila.nombre, style = MaterialTheme.typography.bodyMedium, color = Colores.Tinta)
                    fila.detalle?.let {
                        Text(text = it, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
                    }
                }
                Text(
                    text = fila.valor,
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = Colores.Tinta,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = 150.dp),
                )
            }
        }
        tabla.nota?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = if (tabla.dePrecios) Colores.MantequillaTexto else Colores.Tinta3,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun PiezaPuntos(puntos: List<String>, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        puntos.forEach { punto ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(6.dp)
                        .background(Colores.LavandaTexto, CircleShape),
                )
                Text(text = punto, style = MaterialTheme.typography.bodyMedium, color = Colores.Tinta2)
            }
        }
    }
}

@Composable
private fun PiezaCuidado(texto: String, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(Colores.FrenoSuave, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Icon(Iconos.Alerta, contentDescription = "Atención", tint = Colores.FrenoTexto, modifier = Modifier.size(18.dp))
        Text(text = texto, style = MaterialTheme.typography.bodySmall, color = Colores.FrenoTexto)
    }
}

@Composable
private fun TarjetaDiagnostico(diagnostico: Diagnostico, alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    val fecha = Formato.fechaCorta(diagnostico.fecha)
    Tarjeta(
        fondo = Colores.LavandaSuave,
        alPulsar = alPulsar,
        descripcionAccion = "Ver el diagnóstico del $fecha",
        relleno = PaddingValues(14.dp),
        espacio = 0,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val foto = diagnostico.fotos.firstOrNull()
            if (foto != null) {
                MiniaturaFoto(ruta = foto, tamano = 52.dp)
            } else {
                FotoRin(ancho = 52.dp, alto = 52.dp, tamanoRin = 46.dp, tono = TonoFoto.Clara, conDano = true, radio = 14.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Diagnóstico del $fecha", style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
                Text(
                    text = diagnostico.vehiculo ?: diagnostico.resumen.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.Tinta3,
                    maxLines = 1,
                )
                Text(
                    text = Formato.rangoPesos(diagnostico.precioMinimo, diagnostico.precioMaximo),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = Colores.Tinta,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Etiqueta(texto = diagnostico.gravedad.nombre, tipo = TipoEtiqueta.Ia, pequena = true)
        }
    }
}

/** Tres puntos que saltan mientras el asistente prepara la respuesta. */
@Composable
private fun Escribiendo(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "escribiendo")
    val progreso by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1100, easing = LinearEasing)),
        label = "puntos",
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth(),
    ) {
        AvatarAsistente(tamano = 30.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .sombra(Sombra.Baja, FormaAsistente)
                .background(Colores.Papel, FormaAsistente)
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .clearAndSetSemantics { contentDescription = "El asistente está escribiendo" },
        ) {
            repeat(3) { indice ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer {
                            val fase = ((progreso - indice * 0.16f) % 1f + 1f) % 1f
                            val salto = if (fase < 0.34f) sin(fase / 0.34f * PI.toFloat()) else 0f
                            translationY = -salto * 5.dp.toPx()
                            alpha = 0.4f + 0.6f * salto
                        }
                        .background(Colores.LavandaTexto, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun FilaSugerencias(sugerencias: List<String>, alElegir: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 38.dp),
    ) {
        sugerencias.forEach { sugerencia ->
            Text(
                text = sugerencia,
                style = MaterialTheme.typography.labelLarge,
                color = Colores.Tinta,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Colores.Papel)
                    .border(1.dp, Colores.Linea2, CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Preguntar") { alElegir(sugerencia) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

// ---------- Campo para escribir ----------

@Composable
private fun BarraEscribir(
    texto: String,
    alCambiar: (String) -> Unit,
    alEnviar: () -> Unit,
    alAnalizar: () -> Unit,
    ocupado: Boolean,
) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    val borde by animateColorAsState(if (enfocado) Colores.Tinta else Colores.Linea2, label = "borde del campo")
    val puedeEnviar = texto.isNotBlank() && !ocupado

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
    ) {
        BotonRedondo(
            icono = Iconos.Camara,
            descripcion = "Analizar un rin con la cámara",
            alPulsar = alAnalizar,
            fondo = Colores.Lavanda,
            color = Colores.LavandaTexto,
            conBorde = false,
        )
        BasicTextField(
            value = texto,
            onValueChange = alCambiar,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Colores.Tinta),
            cursorBrush = SolidColor(Colores.Tinta),
            maxLines = 5,
            interactionSource = interaccion,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { if (puedeEnviar) alEnviar() }),
            modifier = Modifier
                .weight(1f)
                .sombra(Sombra.Baja, FormaCampo),
            decorationBox = { campo ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .background(Colores.Papel, FormaCampo)
                        .border(1.5.dp, borde, FormaCampo)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    if (texto.isEmpty()) {
                        Text(text = "Escribe tu pregunta", style = MaterialTheme.typography.bodyLarge, color = Colores.Tinta3)
                    }
                    campo()
                }
            },
        )
        BotonEnviar(habilitado = puedeEnviar, alPulsar = alEnviar)
    }
}

@Composable
private fun BotonEnviar(habilitado: Boolean, alPulsar: () -> Unit) {
    val fondo by animateColorAsState(
        if (habilitado) Colores.Tinta else Colores.Tinta.copy(alpha = 0.12f),
        label = "fondo de enviar",
    )
    val colorIcono by animateColorAsState(if (habilitado) Colores.Crema else Colores.Tinta3, label = "ícono de enviar")
    val escala by animateFloatAsState(
        targetValue = if (habilitado) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "escala de enviar",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(CircleShape)
            .background(fondo)
            .clickable(enabled = habilitado, role = Role.Button, onClickLabel = "Enviar pregunta", onClick = alPulsar)
            .semantics { contentDescription = "Enviar pregunta" },
    ) {
        Icon(Iconos.Enviar, contentDescription = null, tint = colorIcono, modifier = Modifier.size(22.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 900)
@Composable
private fun VistaPreviaAsistente() {
    val ahora = LocalDateTime.now()
    val orden = OrdenesDeEjemplo.ordenesIniciales(ahora).first()
    TemaRinFix {
        ContenidoAsistente(
            estado = AsistenteLogica.Estado(
                cargando = false,
                nombre = "Laura",
                mensajes = listOf(
                    Mensaje(id = "1", autor = Autor.Persona, texto = "¿Cuánto cuesta enderezar un rin?", fecha = ahora),
                    Mensaje(
                        id = "2",
                        autor = Autor.Asistente,
                        texto = "Esta es la tarifa para tu Mazda 3 (rin 17\", aleación). Suele tomar 1 día hábil y el balanceo va incluido.",
                        fecha = ahora,
                        piezas = listOf(
                            Pieza.Tabla(
                                titulo = "Tarifa del taller",
                                filas = listOf(FilaTabla("Enderezado del borde", "$60.000 – $90.000", "Rin 17\", aleación")),
                                nota = "El valor final lo confirma el taller cuando revisa el rin.",
                                dePrecios = true,
                            ),
                            Pieza.OrdenRelacionada(orden.id),
                        ),
                        acciones = listOf(
                            AccionRespuesta("Analizar mi rin", Destino.Analizar),
                            AccionRespuesta("Agendar reparación", Destino.Agendar()),
                        ),
                        sugerencias = listOf("¿Qué precios tiene el taller?", "¿Dónde queda el taller?"),
                        deEjemplo = true,
                    ),
                ),
                ordenes = mapOf(orden.id to orden),
            ),
            alEnviar = { true },
            alReiniciar = {},
            alAnalizar = {},
            alIr = {},
        )
    }
}
