package com.rinfix.app.cuenta

import android.os.Build
import android.util.Patterns
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.Campo
import com.rinfix.app.componentes.Enlace
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada

/**
 * Inicio de sesión con correo y contraseña.
 *
 * La pantalla revisa el formato antes de enviar (correo válido y contraseña escrita).
 * Quien la usa decide si las credenciales son correctas y devuelve el resultado con
 * `cargando` y `error`; cada vez que `intentosFallidos` aumenta, los campos se sacuden
 * y el teléfono vibra.
 *
 * `correoInicial` llega escrito cuando la persona viene de «Usar contraseña» en el desbloqueo.
 */
@Composable
fun PantallaAcceso(
    alVolver: () -> Unit,
    alEntrar: (correo: String, contrasena: String) -> Unit,
    alContinuarConGoogle: () -> Unit,
    alOlvidarContrasena: () -> Unit,
    alCrearCuenta: () -> Unit,
    modifier: Modifier = Modifier,
    correoInicial: String = "",
    cargando: Boolean = false,
    error: String? = null,
    intentosFallidos: Int = 0,
    sinConexion: Boolean = false,
) {
    val visible = recordarEntrada()
    val foco = LocalFocusManager.current
    val vista = LocalView.current

    var correo by rememberSaveable { mutableStateOf(correoInicial) }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var errorCorreo by rememberSaveable { mutableStateOf<String?>(null) }
    var errorContrasena by rememberSaveable { mutableStateOf<String?>(null) }
    var sacudidas by rememberSaveable { mutableIntStateOf(0) }

    // Un intento rechazado sacude los campos y vibra
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
        val correoLimpio = correo.trim()
        errorCorreo = when {
            correoLimpio.isEmpty() -> "Escribe tu correo."
            !Patterns.EMAIL_ADDRESS.matcher(correoLimpio).matches() ->
                "Escribe un correo válido, por ejemplo nombre@correo.com."
            else -> null
        }
        errorContrasena = if (contrasena.isEmpty()) "Escribe tu contraseña." else null
        if (errorCorreo != null || errorContrasena != null) {
            sacudidas++
            return
        }
        foco.clearFocus()
        alEntrar(correoLimpio, contrasena)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding(),
    ) {
        BarraSuperior(alVolver = alVolver)

        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        ) {
            Column(modifier = Modifier.aparecer(visible, orden = 0)) {
                Text(
                    text = "Hola de nuevo",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Colores.Tinta,
                )
                Text(
                    text = "Entra con tu correo o con tu cuenta de Google.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Colores.Tinta2,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            AnimatedVisibility(visible = sinConexion) {
                Aviso(
                    titulo = "Sin conexión.",
                    mensaje = "Conéctate a internet para iniciar sesión.",
                    tipo = TipoAviso.SinConexion,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.aparecer(visible, orden = 1),
            ) {
                Campo(
                    valor = correo,
                    alCambiar = {
                        correo = it
                        errorCorreo = null
                    },
                    etiqueta = "Correo",
                    icono = Iconos.Correo,
                    error = errorCorreo,
                    sacudir = sacudidas,
                    tipoTeclado = KeyboardType.Email,
                    opcionesTeclado = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
                    accionesTeclado = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
                    tipoContenido = ContentType.EmailAddress,
                    habilitado = !cargando,
                )
                Campo(
                    valor = contrasena,
                    alCambiar = {
                        contrasena = it
                        errorContrasena = null
                    },
                    etiqueta = "Contraseña",
                    icono = Iconos.Candado,
                    error = errorContrasena ?: error,
                    sacudir = sacudidas,
                    esContrasena = true,
                    opcionesTeclado = KeyboardOptions(imeAction = ImeAction.Done),
                    accionesTeclado = KeyboardActions(onDone = { enviar() }),
                    tipoContenido = ContentType.Password,
                    habilitado = !cargando,
                )
                Enlace(
                    texto = "¿Olvidaste tu contraseña?",
                    alPulsar = alOlvidarContrasena,
                    estilo = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.End),
                )
            }

            Boton(
                texto = "Iniciar sesión",
                alPulsar = ::enviar,
                ocupado = cargando,
                habilitado = !sinConexion,
                modifier = Modifier
                    .fillMaxWidth()
                    .aparecer(visible, orden = 2),
            )

            Separador(modifier = Modifier.aparecer(visible, orden = 3))

            // En la fase 1 este botón se reemplaza por el botón oficial de Google.
            Boton(
                texto = "Continuar con Google",
                alPulsar = alContinuarConGoogle,
                estilo = EstiloBoton.Blanco,
                icono = Iconos.UsuarioCirculo,
                habilitado = !cargando && !sinConexion,
                modifier = Modifier
                    .fillMaxWidth()
                    .aparecer(visible, orden = 3),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aparecer(visible, orden = 4),
            ) {
                Text(
                    text = "¿No tienes cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Colores.Tinta2,
                )
                Enlace(texto = "Crea una", alPulsar = alCrearCuenta)
            }
        }
    }
}

/** Línea con una «o» en el medio, entre dos formas de entrar. */
@Composable
internal fun Separador(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Colores.Linea2),
        )
        Text(text = "o", style = MaterialTheme.typography.bodySmall, color = Colores.Tinta3)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Colores.Linea2),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, widthDp = 380, heightDp = 820)
@Composable
private fun VistaPreviaAcceso() {
    TemaRinFix {
        PantallaAcceso(
            alVolver = {},
            alEntrar = { _, _ -> },
            alContinuarConGoogle = {},
            alOlvidarContrasena = {},
            alCrearCuenta = {},
            error = "El correo o la contraseña no coinciden.",
        )
    }
}
