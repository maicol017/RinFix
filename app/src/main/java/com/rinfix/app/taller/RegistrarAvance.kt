package com.rinfix.app.taller

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.Campo
import com.rinfix.app.componentes.ControlCamara
import com.rinfix.app.componentes.EstadoVacio
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.MiniaturaFoto
import com.rinfix.app.componentes.SelectorSegmentos
import com.rinfix.app.componentes.TamanoBoton
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.componentes.VisorCamara
import com.rinfix.app.componentes.recordarControlCamara
import com.rinfix.app.componentes.tipoEtiqueta
import com.rinfix.app.datos.MomentoFoto
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.IconosDeArribaClaros
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import kotlinx.coroutines.launch

/**
 * El taller registra una novedad sin cambiar de etapa: fotos (cámara o galería), el momento
 * (antes, durante, después), lo que se está haciendo y una nota. El cliente la ve al instante.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaRegistrarAvance(
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
    logica: RegistrarAvanceLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    val formulario = estado.formulario
    val contexto = LocalContext.current
    val vista = LocalView.current
    val alcance = rememberCoroutineScope()
    val control = recordarControlCamara()
    val visible = recordarEntrada()

    // ---- Cámara y permiso ----
    var camaraAbierta by rememberSaveable { mutableStateOf(false) }
    var conPermiso by remember { mutableStateOf(tienePermisoCamara(contexto)) }
    var negadoSiempre by rememberSaveable { mutableStateOf(false) }
    val actividad = remember(contexto) { contexto.buscarActividad() }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        conPermiso = concedido
        if (concedido) {
            camaraAbierta = true
        } else {
            negadoSiempre = actividad != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(actividad, Manifest.permission.CAMERA)
            logica.mostrarError(
                if (negadoSiempre) {
                    "La cámara está bloqueada para RinFix. Actívala en los ajustes del teléfono o elige fotos de la galería."
                } else {
                    "Sin permiso de cámara puedes elegir fotos de la galería."
                },
            )
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { conPermiso = tienePermisoCamara(contexto) }

    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) logica.agregarDeGaleria(uri)
    }

    fun abrirAjustes() {
        contexto.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null)),
        )
    }

    fun abrirCamara() {
        if (!formulario.puedeTomarMas) return
        when {
            conPermiso -> camaraAbierta = true
            negadoSiempre -> abrirAjustes()
            else -> pedirPermiso.launch(Manifest.permission.CAMERA)
        }
    }

    val destello = remember { Animatable(0f) }
    fun tomarFoto() {
        if (!formulario.puedeTomarMas || formulario.tomando || !control.lista) return
        vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        alcance.launch {
            destello.snapTo(0.8f)
            destello.animateTo(0f, tween(durationMillis = 320))
        }
        logica.empezarFoto()
        control.tomarFoto(contexto, logica.archivoNuevo(), logica::fotoTomada)
    }

    // Con el máximo de fotos, la cámara se cierra sola.
    LaunchedEffect(formulario.fotos.size) {
        if (!formulario.puedeTomarMas) camaraAbierta = false
    }
    BackHandler(enabled = camaraAbierta) { camaraAbierta = false }

    val orden = estado.orden
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            BarraSuperior(titulo = "Registrar avance", alVolver = alVolver)
            when {
                estado.cargando -> Unit
                orden == null -> EstadoVacio(
                    icono = Iconos.Herramienta,
                    titulo = "No encontramos este trabajo",
                    mensaje = "Vuelve a la lista de trabajos e inténtalo de nuevo.",
                    textoAccion = "Volver",
                    alPulsarAccion = alVolver,
                )
                else -> Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.aparecer(visible, orden = 0),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Orden ${orden.codigo}", style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
                            Text(
                                text = "${orden.vehiculo}, ${orden.rin}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Colores.Tinta,
                            )
                        }
                        Etiqueta(texto = orden.etapa.estado, tipo = tipoEtiqueta(orden.etapa), conPunto = true, pequena = true)
                    }

                    Column(modifier = Modifier.aparecer(visible, orden = 1)) {
                        Text(text = "Fotos", style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
                        Text(
                            text = "Hasta ${RegistrarAvanceLogica.MaximoFotos}. El cliente las ve en su seguimiento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Colores.Tinta3,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 10.dp),
                        ) {
                            formulario.fotos.forEachIndexed { indice, ruta ->
                                FotoElegida(ruta = ruta, numero = indice + 1, alQuitar = { logica.quitarFoto(ruta) })
                            }
                            if (formulario.tomando && !camaraAbierta) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(LadoFoto)
                                        .background(Colores.Crema2, FormaFoto),
                                ) {
                                    CircularProgressIndicator(color = Colores.Tinta, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                }
                            }
                            if (formulario.puedeTomarMas) {
                                CuadroAgregar(icono = Iconos.Camara, texto = "Tomar foto", alPulsar = ::abrirCamara)
                                CuadroAgregar(
                                    icono = Iconos.Galeria,
                                    texto = "Galería",
                                    alPulsar = {
                                        if (!formulario.tomando) {
                                            elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        }
                                    },
                                )
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.aparecer(visible, orden = 2),
                    ) {
                        Text(text = "¿En qué momento?", style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
                        SelectorSegmentos(
                            opciones = MomentoFoto.entries,
                            seleccionada = formulario.momento,
                            alElegir = logica::elegirMomento,
                            texto = { it.nombre },
                            habilitado = !formulario.guardando,
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.aparecer(visible, orden = 3),
                    ) {
                        Campo(
                            valor = formulario.detalle,
                            alCambiar = logica::cambiarDetalle,
                            etiqueta = "Qué se está haciendo",
                            icono = Iconos.Herramienta,
                            ayuda = "El cliente lo ve como el estado actual de su rin.",
                            marcador = "Ej.: Pintura y curado",
                            opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            habilitado = !formulario.guardando,
                        )
                        Campo(
                            valor = formulario.nota,
                            alCambiar = logica::cambiarNota,
                            etiqueta = "Nota para el cliente",
                            ayuda = "Opcional",
                            marcador = "Ej.: Terminamos el enderezado; mañana pintamos.",
                            lineas = 3,
                            opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            habilitado = !formulario.guardando,
                        )
                    }

                    AnimatedVisibility(visible = formulario.error != null) {
                        Aviso(
                            titulo = "Atención.",
                            mensaje = formulario.error.orEmpty(),
                            tipo = TipoAviso.Error,
                            textoAccion = if (negadoSiempre) "Abrir ajustes" else "Entendido",
                            alPulsarAccion = {
                                logica.limpiarError()
                                if (negadoSiempre) abrirAjustes()
                            },
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.aparecer(visible, orden = 4),
                    ) {
                        Boton(
                            texto = "Publicar avance",
                            alPulsar = {
                                logica.publicar {
                                    Toast.makeText(contexto, "Avance publicado", Toast.LENGTH_SHORT).show()
                                    alVolver()
                                }
                            },
                            estilo = EstiloBoton.Menta,
                            icono = Iconos.Enviar,
                            ocupado = formulario.guardando,
                            habilitado = estado.hayAlgo && !formulario.tomando,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = if (estado.hayAlgo) {
                                "El cliente recibe un aviso al instante."
                            } else {
                                "Agrega una foto, una nota o lo que se está haciendo."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Colores.Tinta3,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = camaraAbierta, enter = fadeIn(), exit = fadeOut()) {
            CamaraAvance(
                control = control,
                cantidad = formulario.fotos.size,
                tomando = formulario.tomando,
                destello = destello.value,
                alDisparar = ::tomarFoto,
                alCerrar = { camaraAbierta = false },
                alFallar = {
                    camaraAbierta = false
                    logica.mostrarError("No pudimos abrir la cámara. Puede que otra app la esté usando; elige fotos de la galería.")
                },
            )
        }
    }
}

private val LadoFoto = 96.dp
private val FormaFoto = RoundedCornerShape(18.dp)

@Composable
private fun FotoElegida(ruta: String, numero: Int, alQuitar: () -> Unit) {
    Box(modifier = Modifier.size(LadoFoto)) {
        MiniaturaFoto(ruta = ruta, tamano = LadoFoto, radio = 18.dp, descripcion = "Foto $numero")
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Colores.Tinta.copy(alpha = 0.6f))
                .clickable(role = Role.Button, onClickLabel = "Quitar la foto $numero", onClick = alQuitar)
                .semantics { contentDescription = "Quitar la foto $numero" },
        ) {
            Icon(Iconos.Cerrar, contentDescription = null, tint = Colores.Crema, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CuadroAgregar(icono: ImageVector, texto: String, alPulsar: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(LadoFoto)
            .clip(FormaFoto)
            .background(Colores.Crema2)
            .border(1.5.dp, Colores.Linea2, FormaFoto)
            .clickable(role = Role.Button, onClickLabel = texto, onClick = alPulsar),
    ) {
        Icon(icono, contentDescription = null, tint = Colores.Tinta2, modifier = Modifier.size(24.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = Colores.Tinta2,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Cámara a pantalla completa para fotos de avance: disparador, contador y «Listo». */
@Composable
private fun CamaraAvance(
    control: ControlCamara,
    cantidad: Int,
    tomando: Boolean,
    destello: Float,
    alDisparar: () -> Unit,
    alCerrar: () -> Unit,
    alFallar: () -> Unit,
) {
    IconosDeArribaClaros()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Colores.Tinta)
            // Bloquea los toques hacia el formulario de abajo.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
    ) {
        VisorCamara(control = control, alFallar = alFallar, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = destello }
                .background(Color.White),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(FondoSobreCamara)
                    .clickable(role = Role.Button, onClickLabel = "Cerrar la cámara", onClick = alCerrar)
                    .semantics { contentDescription = "Cerrar la cámara" },
            ) {
                Icon(Iconos.Cerrar, contentDescription = null, tint = Colores.Crema, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$cantidad de ${RegistrarAvanceLogica.MaximoFotos} fotos",
                style = MaterialTheme.typography.labelLarge,
                color = Colores.Crema,
                modifier = Modifier
                    .background(FondoSobreCamara, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(bottom = 28.dp),
        ) {
            Disparador(
                habilitado = !tomando && control.lista && cantidad < RegistrarAvanceLogica.MaximoFotos,
                alPulsar = alDisparar,
                modifier = Modifier.align(Alignment.Center),
            )
            if (cantidad > 0) {
                Boton(
                    texto = "Listo",
                    alPulsar = alCerrar,
                    estilo = EstiloBoton.Menta,
                    tamano = TamanoBoton.Pequeno,
                    icono = Iconos.Hecho,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 20.dp),
                )
            }
        }
    }
}

private val FondoSobreCamara = Colores.Tinta.copy(alpha = 0.5f)

/** Disparador claro, para encima de la imagen de la cámara. */
@Composable
private fun Disparador(habilitado: Boolean, alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "escala del disparador",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(78.dp)
            .graphicsLayer { alpha = if (habilitado) 1f else 0.4f }
            .border(4.dp, Colores.Crema, CircleShape)
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
                .size(60.dp)
                .graphicsLayer {
                    scaleX = escala
                    scaleY = escala
                }
                .background(Colores.Crema, CircleShape),
        )
    }
}

private fun tienePermisoCamara(contexto: Context): Boolean =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.buscarActividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActividad()
    else -> null
}
