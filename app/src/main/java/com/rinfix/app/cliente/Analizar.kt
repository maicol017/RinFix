package com.rinfix.app.cliente

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.BotonIcono
import com.rinfix.app.componentes.Enlace
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.MiniaturaFoto
import com.rinfix.app.componentes.SelectorFichas
import com.rinfix.app.componentes.SelectorSegmentos
import com.rinfix.app.componentes.VisorCamara
import com.rinfix.app.componentes.recordarControlCamara
import com.rinfix.app.datos.GarajeDeEjemplo
import com.rinfix.app.datos.MaterialRin
import com.rinfix.app.datos.TamanosRin
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.IconosDeArribaClaros
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.ia.Luz
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cámara para fotografiar el rin (de 1 a 3 fotos) y enviarlas al análisis.
 *
 * - Pide el permiso de cámara al abrir. Si la persona lo niega, explica para qué es y ofrece
 *   la galería; si lo negó para siempre, lleva a los ajustes del teléfono.
 * - Revisa la luz en vivo y la guía se pone menta cuando la luz es buena.
 * - Con `idVehiculo` (desde «Analizar este rin» en Mi garaje) llega con ese vehículo elegido.
 */
@Composable
fun PantallaAnalizar(
    alCerrar: () -> Unit,
    alAnalizar: (idAnalisis: String) -> Unit,
    modifier: Modifier = Modifier,
    idVehiculo: String? = null,
    logica: AnalizarLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    val contexto = LocalContext.current
    val vista = LocalView.current
    val densidad = LocalDensity.current
    val alcance = rememberCoroutineScope()
    val control = recordarControlCamara()

    LaunchedEffect(Unit) { logica.preparar(idVehiculo) }
    IconosDeArribaClaros()

    // ---- Permiso de cámara ----
    val actividad = remember(contexto) { contexto.buscarActividad() }
    var conPermiso by remember { mutableStateOf(tienePermisoCamara(contexto)) }
    var yaPregunto by rememberSaveable { mutableStateOf(false) }
    var negadoSiempre by rememberSaveable { mutableStateOf(false) }
    var camaraFallo by rememberSaveable { mutableStateOf(false) }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        conPermiso = concedido
        yaPregunto = true
        negadoSiempre = !concedido && actividad != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(actividad, Manifest.permission.CAMERA)
    }
    // Al volver de los ajustes del teléfono, el permiso pudo cambiar.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { conPermiso = tienePermisoCamara(contexto) }
    LaunchedEffect(Unit) {
        if (!conPermiso && !yaPregunto) pedirPermiso.launch(Manifest.permission.CAMERA)
    }

    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) logica.agregarDeGaleria(uri)
    }
    fun abrirGaleria() {
        if (estado.puedeTomarMas && !estado.tomando) {
            elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
    fun abrirAjustes() {
        contexto.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null)),
        )
    }

    // ---- Tomar la foto ----
    val camaraActiva = conPermiso && !camaraFallo
    val destello = remember { Animatable(0f) }
    fun tomarFoto() {
        if (!estado.puedeTomarMas || estado.tomando || !control.lista) return
        vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        alcance.launch {
            destello.snapTo(0.8f)
            destello.animateTo(0f, tween(durationMillis = 320))
        }
        logica.empezarFoto()
        control.tomarFoto(contexto, logica.archivoNuevo(), logica::fotoTomada)
    }

    // Los errores se van solos después de unos segundos.
    LaunchedEffect(estado.error) {
        if (estado.error != null) {
            delay(4_000)
            logica.limpiarError()
        }
    }

    var altoPanel by remember { mutableStateOf(0.dp) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Colores.Tinta),
    ) {
        if (camaraActiva) {
            VisorCamara(
                control = control,
                linterna = estado.linterna,
                alRevisarEncuadre = logica::revisarEncuadre,
                alFallar = { camaraFallo = true },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Destello blanco al tomar la foto.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = destello.value }
                .background(Color.White),
        )

        // Zona visible de la cámara, encima del panel.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = (altoPanel - SolapePanel).coerceAtLeast(0.dp)),
        ) {
            if (camaraActiva) {
                Guia(listo = estado.encuadre.luz == Luz.Buena, modifier = Modifier.align(Alignment.Center))
            } else {
                SinCamara(
                    fallo = camaraFallo,
                    negadoSiempre = negadoSiempre,
                    alPedirPermiso = { pedirPermiso.launch(Manifest.permission.CAMERA) },
                    alAbrirAjustes = ::abrirAjustes,
                    alReintentar = { camaraFallo = false },
                    alAbrirGaleria = ::abrirGaleria,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BotonSobreCamara(icono = Iconos.Cerrar, descripcion = "Cerrar cámara", alPulsar = alCerrar)
                    Text(
                        text = "Foto del rin",
                        style = MaterialTheme.typography.titleMedium,
                        color = Colores.Crema,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    if (camaraActiva && control.tieneLinterna) {
                        BotonSobreCamara(
                            icono = Iconos.Flash,
                            descripcion = if (estado.linterna) "Apagar linterna" else "Prender linterna",
                            alPulsar = logica::cambiarLinterna,
                            activo = estado.linterna,
                        )
                    } else {
                        Spacer(modifier = Modifier.size(44.dp))
                    }
                }
                if (camaraActiva) ChipLuz(luz = estado.encuadre.luz)
                AnimatedVisibility(visible = estado.error != null) {
                    ChipError(texto = estado.error.orEmpty())
                }
            }

            if (estado.fotos.isNotEmpty()) {
                TiraFotos(
                    fotos = estado.fotos,
                    alQuitar = logica::quitarFoto,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 14.dp),
                )
            }
        }

        PanelCaptura(
            estado = estado,
            puedeDisparar = camaraActiva && control.lista && estado.puedeTomarMas && !estado.tomando,
            alElegirVehiculo = logica::elegirVehiculo,
            alElegirMaterial = logica::elegirMaterial,
            alElegirTamano = logica::elegirTamano,
            alAbrirGaleria = ::abrirGaleria,
            alTomarFoto = ::tomarFoto,
            alAnalizar = { logica.analizar(alAnalizar) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { altoPanel = with(densidad) { it.height.toDp() } },
        )
    }
}

/** Cuánto se monta el panel blanco sobre la imagen de la cámara. */
private val SolapePanel = 30.dp

// ---------- Sobre la cámara ----------

/** Círculo guía con esquinas de encuadre. Respira despacio y se pone menta cuando la luz es buena. */
@Composable
private fun Guia(listo: Boolean, modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "guía")
    val respiro by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "respiro de la guía",
    )
    val color by animateColorAsState(
        targetValue = if (listo) Colores.Menta else Colores.Crema.copy(alpha = 0.85f),
        animationSpec = tween(300),
        label = "color de la guía",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Canvas(
            modifier = Modifier
                .size(272.dp)
                .clearAndSetSemantics { },
        ) {
            val trazo = 2.dp.toPx()
            drawCircle(
                color = color,
                radius = 118.dp.toPx() * respiro,
                style = Stroke(
                    width = trazo,
                    pathEffect = if (listo) null else PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx())),
                ),
            )
            // Esquinas de encuadre
            val largo = 30.dp.toPx()
            val lado = size.minDimension
            val esquinas = listOf(
                Triple(Offset(0f, 0f), 1f, 1f),
                Triple(Offset(lado, 0f), -1f, 1f),
                Triple(Offset(0f, lado), 1f, -1f),
                Triple(Offset(lado, lado), -1f, -1f),
            )
            esquinas.forEach { (punto, sx, sy) ->
                val camino = Path().apply {
                    moveTo(punto.x, punto.y + largo * sy)
                    lineTo(punto.x, punto.y)
                    lineTo(punto.x + largo * sx, punto.y)
                }
                drawPath(
                    path = camino,
                    color = Colores.Crema.copy(alpha = 0.9f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
        Text(
            text = "Centra el rin en el círculo, a unos 50 cm",
            style = MaterialTheme.typography.bodySmall,
            color = Colores.Crema,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun BotonSobreCamara(
    icono: ImageVector,
    descripcion: String,
    alPulsar: () -> Unit,
    activo: Boolean = false,
) {
    val fondo by animateColorAsState(
        targetValue = if (activo) Colores.Menta else Color.White.copy(alpha = 0.12f),
        label = "fondo del botón",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(fondo)
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable(role = Role.Button, onClickLabel = descripcion, onClick = alPulsar),
    ) {
        Icon(
            imageVector = icono,
            contentDescription = descripcion,
            tint = if (activo) Colores.Tinta else Colores.Crema,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Estado de la luz en vivo. El lector de pantalla lo anuncia cuando cambia. */
@Composable
private fun ChipLuz(luz: Luz) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(Formas.Pildora)
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), Formas.Pildora)
            .height(28.dp)
            .padding(horizontal = 11.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        when (luz) {
            Luz.Revisando -> CircularProgressIndicator(
                color = Colores.Crema,
                strokeWidth = 2.dp,
                modifier = Modifier.size(12.dp),
            )
            Luz.Buena -> Icon(Iconos.Hecho, contentDescription = null, tint = Colores.Menta, modifier = Modifier.size(14.dp))
            Luz.Poca, Luz.Reflejo -> Icon(
                Iconos.Alerta,
                contentDescription = null,
                tint = Colores.Mantequilla,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(text = luz.mensaje, style = MaterialTheme.typography.labelMedium, color = Colores.Crema)
    }
}

@Composable
private fun ChipError(texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(Formas.Campo)
            .background(Colores.Freno)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        Icon(Iconos.Alerta, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(text = texto, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

/** Fotos ya tomadas, abajo a la izquierda, cada una con su botón para quitarla. */
@Composable
private fun TiraFotos(fotos: List<String>, alQuitar: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        fotos.forEachIndexed { indice, ruta ->
            key(ruta) {
                Box {
                    MiniaturaFoto(
                        ruta = ruta,
                        tamano = 56.dp,
                        descripcion = "Foto ${indice + 1}",
                        modifier = Modifier.border(2.dp, Colores.Crema, RoundedCornerShape(14.dp)),
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-10).dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "Quitar foto ${indice + 1}") { alQuitar(ruta) },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .background(Colores.Tinta, CircleShape)
                                .border(1.5.dp, Colores.Crema, CircleShape),
                        ) {
                            Icon(
                                imageVector = Iconos.Cerrar,
                                contentDescription = "Quitar foto ${indice + 1}",
                                tint = Colores.Crema,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Sin permiso o sin cámara: explica qué pasa y ofrece cómo seguir. */
@Composable
private fun SinCamara(
    fallo: Boolean,
    negadoSiempre: Boolean,
    alPedirPermiso: () -> Unit,
    alAbrirAjustes: () -> Unit,
    alReintentar: () -> Unit,
    alAbrirGaleria: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .background(Colores.Crema.copy(alpha = 0.1f), CircleShape),
        ) {
            Icon(Iconos.Camara, contentDescription = null, tint = Colores.Crema, modifier = Modifier.size(36.dp))
        }
        Text(
            text = if (fallo) "No pudimos abrir la cámara" else "Activa la cámara",
            style = MaterialTheme.typography.headlineSmall,
            color = Colores.Crema,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            text = if (fallo) {
                "Puede que otra app la esté usando. Ciérrala e inténtalo de nuevo, o elige una foto de tu galería."
            } else {
                "RinFix la usa solo para fotografiar el rin. También puedes elegir una foto de tu galería."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.Crema.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Boton(
            texto = when {
                fallo -> "Intentar de nuevo"
                negadoSiempre -> "Abrir ajustes"
                else -> "Permitir cámara"
            },
            alPulsar = when {
                fallo -> alReintentar
                negadoSiempre -> alAbrirAjustes
                else -> alPedirPermiso
            },
            estilo = EstiloBoton.Menta,
            modifier = Modifier.padding(top = 20.dp),
        )
        Enlace(texto = "Elegir de la galería", alPulsar = alAbrirGaleria, color = Colores.Crema)
    }
}

// ---------- Panel de abajo ----------

@Composable
private fun PanelCaptura(
    estado: AnalizarLogica.Estado,
    puedeDisparar: Boolean,
    alElegirVehiculo: (String?) -> Unit,
    alElegirMaterial: (MaterialRin) -> Unit,
    alElegirTamano: (Int?) -> Unit,
    alAbrirGaleria: () -> Unit,
    alTomarFoto: () -> Unit,
    alAnalizar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Colores.Papel)
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 12.dp),
    ) {
        if (estado.vehiculos.isNotEmpty()) {
            val opciones: List<String?> = estado.vehiculos.map { it.id } + listOf(null)
            SelectorFichas(
                opciones = opciones,
                seleccionada = estado.idVehiculo,
                alElegir = alElegirVehiculo,
                texto = { id -> estado.vehiculos.firstOrNull { it.id == id }?.nombre ?: "Otro carro" },
                descripcion = { id -> estado.vehiculos.firstOrNull { it.id == id }?.titulo ?: "Otro carro" },
                enUnaLinea = true,
            )
        }
        FilaDato(etiqueta = "Material") {
            SelectorSegmentos(
                opciones = MaterialRin.entries,
                seleccionada = estado.material,
                alElegir = alElegirMaterial,
                texto = { it.nombre },
                modifier = Modifier.weight(1f),
            )
        }
        FilaDato(etiqueta = "Tamaño") {
            SelectorFichas(
                opciones = TamanosRin + listOf(null),
                seleccionada = estado.pulgadas,
                alElegir = alElegirTamano,
                texto = { tamano -> tamano?.let { "$it\"" } ?: "No sé" },
                descripcion = { tamano -> tamano?.let { "$it pulgadas" } ?: "No sé el tamaño" },
                enUnaLinea = true,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 6.dp, end = 6.dp),
        ) {
            BotonIcono(
                icono = Iconos.Galeria,
                descripcion = "Elegir de la galería",
                alPulsar = alAbrirGaleria,
                habilitado = estado.puedeTomarMas && !estado.tomando,
            )
            Disparador(habilitado = puedeDisparar, alPulsar = alTomarFoto)
            BotonAnalizar(cantidad = estado.fotos.size, enviando = estado.enviando, alPulsar = alAnalizar)
        }
        Text(
            text = "Toma de 1 a 3 fotos: el rin completo, el borde y de cerca al daño.",
            style = MaterialTheme.typography.labelSmall,
            color = Colores.Tinta3,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FilaDato(etiqueta: String, contenido: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.titleSmall,
            color = Colores.Tinta,
            modifier = Modifier.width(64.dp),
        )
        contenido()
    }
}

/** Botón redondo para tomar la foto: anillo de tinta y centro que se hunde al presionar. */
@Composable
private fun Disparador(habilitado: Boolean, alPulsar: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "escala del disparador",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer { alpha = if (habilitado) 1f else 0.4f }
            .border(4.dp, Colores.Tinta, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = habilitado,
                role = Role.Button,
                onClick = alPulsar,
            )
            .semantics { contentDescription = "Tomar foto" },
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .graphicsLayer {
                    scaleX = escala
                    scaleY = escala
                }
                .background(Colores.Tinta, CircleShape),
        )
    }
}

/** A la derecha del disparador: cuántas fotos van, y con al menos una, el botón para analizar. */
@Composable
private fun BotonAnalizar(cantidad: Int, enviando: Boolean, alPulsar: () -> Unit) {
    AnimatedContent(
        targetState = cantidad == 0,
        transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.7f)) togetherWith fadeOut(tween(120)) },
        label = "botón de analizar",
    ) { sinFotos ->
        if (sinFotos) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                Text(
                    text = "0 de ${AnalizarLogica.MaximoFotos}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Colores.Tinta3,
                )
            }
        } else {
            val descripcion = if (cantidad == 1) "Analizar 1 foto" else "Analizar $cantidad fotos"
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Colores.Menta)
                        .clickable(enabled = !enviando, role = Role.Button, onClick = alPulsar)
                        .semantics { contentDescription = descripcion },
                ) {
                    if (enviando) {
                        CircularProgressIndicator(
                            color = Colores.Tinta,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Icon(Iconos.Flecha, contentDescription = null, tint = Colores.Tinta, modifier = Modifier.size(24.dp))
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(22.dp)
                        .background(Colores.Tinta, CircleShape)
                        .border(2.dp, Colores.Papel, CircleShape)
                        .clearAndSetSemantics { },
                ) {
                    Text(
                        text = cantidad.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Colores.Crema,
                    )
                }
            }
        }
    }
}

// ---------- Ayudas ----------

private fun tienePermisoCamara(contexto: Context): Boolean =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.buscarActividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActividad()
    else -> null
}

@Preview(showBackground = true, backgroundColor = 0xFF030302, heightDp = 360)
@Composable
private fun VistaPreviaPanelCaptura() {
    TemaRinFix {
        Box(modifier = Modifier.fillMaxSize()) {
            PanelCaptura(
                estado = AnalizarLogica.Estado(
                    vehiculos = GarajeDeEjemplo.vehiculosIniciales().getValue("cliente-1"),
                    idVehiculo = "vehiculo-1",
                    material = MaterialRin.Aleacion,
                    pulgadas = 17,
                ),
                puedeDisparar = true,
                alElegirVehiculo = {},
                alElegirMaterial = {},
                alElegirTamano = {},
                alAbrirGaleria = {},
                alTomarFoto = {},
                alAnalizar = {},
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
