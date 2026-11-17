package com.rinfix.app.cuenta

import android.os.Build
import android.util.Patterns
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.Campo
import com.rinfix.app.componentes.Casilla
import com.rinfix.app.componentes.Enlace
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada

/**
 * Registro de un cliente nuevo con correo y contraseña.
 *
 * - El correo muestra un check verde cuando está bien escrito.
 * - La contraseña tiene un medidor de cuatro barras que se llena mientras se escribe.
 * - «Crear cuenta» se habilita cuando la contraseña es al menos aceptable y se acepta la
 *   política de datos (Ley 1581 de 2012). Nombre y correo se revisan al pulsarlo.
 *
 * Las cuentas de taller no se crean aquí: llegan con un código de invitación.
 */
@Composable
fun PantallaRegistro(
    alVolver: () -> Unit,
    alCrear: (nombre: String, correo: String, contrasena: String) -> Unit,
    alVerPolitica: () -> Unit,
    alIniciarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    error: String? = null,
    intentosFallidos: Int = 0,
    sinConexion: Boolean = false,
) {
    val visible = recordarEntrada()
    val foco = LocalFocusManager.current
    val vista = LocalView.current

    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var acepta by rememberSaveable { mutableStateOf(false) }
    var errorNombre by rememberSaveable { mutableStateOf<String?>(null) }
    var errorCorreo by rememberSaveable { mutableStateOf<String?>(null) }
    var sacudidas by rememberSaveable { mutableIntStateOf(0) }

    val correoValido = Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()
    val nivel = nivelContrasena(contrasena)
    val listo = nivel >= 2 && acepta && !sinConexion

    LaunchedEffect(intentosFallidos) {
        if (intentosFallidos > 0) {
            sacudidas++
            vista.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.REJECT
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                },
            )
        }
    }

    fun enviar() {
        errorNombre = if (nombre.isBlank()) "Escribe tu nombre." else null
        errorCorreo = when {
            correo.isBlank() -> "Escribe tu correo."
            !correoValido -> "Escribe un correo válido, por ejemplo nombre@correo.com."
            else -> null
        }
        if (errorNombre != null || errorCorreo != null) {
            sacudidas++
            return
        }
        foco.clearFocus()
        alCrear(nombre.trim(), correo.trim(), contrasena)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding(),
    ) {
        BarraSuperior(alVolver = alVolver)

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        ) {
            Column(modifier = Modifier.aparecer(visible, orden = 0)) {
                Text(
                    text = "Crea tu cuenta",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Colores.Tinta,
                )
                Text(
                    text = "Guarda tus diagnósticos y sigue tus reparaciones.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Colores.Tinta2,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            AnimatedVisibility(visible = sinConexion) {
                Aviso(
                    titulo = "Sin conexión.",
                    mensaje = "Conéctate a internet para crear tu cuenta.",
                    tipo = TipoAviso.SinConexion,
                )
            }
            AnimatedVisibility(visible = error != null) {
                Aviso(titulo = "No pudimos crear la cuenta.", mensaje = error.orEmpty(), tipo = TipoAviso.Error)
            }

            Campo(
                valor = nombre,
                alCambiar = {
                    nombre = it
                    errorNombre = null
                },
                etiqueta = "Nombre completo",
                icono = Iconos.Usuario,
                error = errorNombre,
                sacudir = sacudidas,
                opcionesTeclado = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                accionesTeclado = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
                tipoContenido = ContentType.PersonFullName,
                habilitado = !cargando,
                modifier = Modifier.aparecer(visible, orden = 1),
            )
            Campo(
                valor = correo,
                alCambiar = {
                    correo = it
                    errorCorreo = null
                },
                etiqueta = "Correo",
                icono = Iconos.Correo,
                error = errorCorreo,
                valido = correoValido,
                sacudir = sacudidas,
                tipoTeclado = KeyboardType.Email,
                opcionesTeclado = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
                accionesTeclado = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
                tipoContenido = ContentType.EmailAddress,
                habilitado = !cargando,
                modifier = Modifier.aparecer(visible, orden = 2),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.aparecer(visible, orden = 3),
            ) {
                Campo(
                    valor = contrasena,
                    alCambiar = { contrasena = it },
                    etiqueta = "Contraseña",
                    icono = Iconos.Candado,
                    marcador = "Mínimo 8 caracteres",
                    esContrasena = true,
                    opcionesTeclado = KeyboardOptions(imeAction = ImeAction.Done),
                    accionesTeclado = KeyboardActions(onDone = { foco.clearFocus() }),
                    tipoContenido = ContentType.NewPassword,
                    habilitado = !cargando,
                )
                MedidorContrasena(nivel = nivel, vacia = contrasena.isEmpty())
            }

            Column(modifier = Modifier.aparecer(visible, orden = 4)) {
                Casilla(
                    marcada = acepta,
                    alCambiar = { acepta = it },
                    texto = "Acepto los términos y la política de tratamiento de datos personales (Ley 1581 de 2012).",
                    habilitada = !cargando,
                )
                Enlace(
                    texto = "Leer términos y política de datos",
                    alPulsar = alVerPolitica,
                    estilo = MaterialTheme.typography.labelMedium,
                    color = Colores.Tinta2,
                    modifier = Modifier.padding(start = 30.dp),
                )
            }

            Boton(
                texto = "Crear cuenta",
                alPulsar = ::enviar,
                ocupado = cargando,
                habilitado = listo,
                modifier = Modifier
                    .fillMaxWidth()
                    .aparecer(visible, orden = 5),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "¿Ya tienes cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Colores.Tinta2,
                )
                Enlace(texto = "Inicia sesión", alPulsar = alIniciarSesion)
            }

            Text(
                text = "Las cuentas de taller se crean con un código de invitación del administrador.",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.Tinta3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Nivel de la contraseña de 0 a 4: un punto por tener 8 o más caracteres, mayúsculas,
 * números y símbolos. Si tiene menos de 8 caracteres, no pasa de 1.
 */
internal fun nivelContrasena(contrasena: String): Int {
    if (contrasena.isEmpty()) return 0
    var nivel = 0
    if (contrasena.length >= 8) nivel++
    if (contrasena.any { it.isUpperCase() }) nivel++
    if (contrasena.any { it.isDigit() }) nivel++
    if (contrasena.any { !it.isLetterOrDigit() }) nivel++
    return if (contrasena.length < 8) minOf(nivel, 1) else nivel
}

private val TextosNivel = listOf("Muy débil", "Muy débil", "Aceptable", "Buena", "Fuerte")
private val ColorAmbar = Color(0xFFE7B93A)

/** Cuatro barras que se llenan según el nivel, con el nombre del nivel debajo. */
@Composable
private fun MedidorContrasena(nivel: Int, vacia: Boolean) {
    val colorNivel = when (nivel) {
        1 -> Colores.Freno
        2 -> ColorAmbar
        3 -> Colores.Menta
        else -> Colores.MentaTexto
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until 4) {
                val color by animateColorAsState(
                    targetValue = if (i < nivel) colorNivel else Colores.Tinta.copy(alpha = 0.1f),
                    animationSpec = tween(durationMillis = 300),
                    label = "barra $i",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(color, Formas.Pildora),
                )
            }
        }
        Text(
            text = if (vacia) "Usa 8 o más caracteres, con números y símbolos." else TextosNivel[nivel],
            style = MaterialTheme.typography.bodySmall,
            color = Colores.Tinta3,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, widthDp = 380, heightDp = 900)
@Composable
private fun VistaPreviaRegistro() {
    TemaRinFix {
        PantallaRegistro(
            alVolver = {},
            alCrear = { _, _, _ -> },
            alVerPolitica = {},
            alIniciarSesion = {},
        )
    }
}
