package com.rinfix.app.cuenta

import android.os.Build
import android.util.Patterns
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.Campo
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada

/**
 * Cuenta de trabajador con el código de invitación del taller.
 * Es la única forma de obtener el rol de taller: ni el registro normal ni Google lo dan.
 */
@Composable
fun PantallaCodigoTaller(
    alVolver: () -> Unit,
    alUnirse: (codigo: String, nombre: String, correo: String, contrasena: String) -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    error: String? = null,
    intentosFallidos: Int = 0,
) {
    val visible = recordarEntrada()
    val foco = LocalFocusManager.current
    val vista = LocalView.current

    var codigo by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var errorCodigo by rememberSaveable { mutableStateOf<String?>(null) }
    var errorNombre by rememberSaveable { mutableStateOf<String?>(null) }
    var errorCorreo by rememberSaveable { mutableStateOf<String?>(null) }
    var errorContrasena by rememberSaveable { mutableStateOf<String?>(null) }
    var sacudidas by rememberSaveable { mutableIntStateOf(0) }

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
        errorCodigo = if (codigo.isBlank()) "Escribe el código que te dio el administrador." else null
        errorNombre = if (nombre.isBlank()) "Escribe tu nombre." else null
        errorCorreo = if (!Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()) {
            "Escribe un correo válido, por ejemplo nombre@correo.com."
        } else {
            null
        }
        errorContrasena = if (nivelContrasena(contrasena) < 2) {
            "Usa 8 o más caracteres, con números o símbolos."
        } else {
            null
        }
        if (listOf(errorCodigo, errorNombre, errorCorreo, errorContrasena).any { it != null }) {
            sacudidas++
            return
        }
        foco.clearFocus()
        alUnirse(codigo.trim(), nombre.trim(), correo.trim(), contrasena)
    }

    val siguiente = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) })

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
                    text = "Únete a tu taller",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Colores.Tinta,
                )
                Text(
                    text = "Escribe el código que te dio el administrador. Con él, tu cuenta queda como trabajador del taller.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Colores.Tinta2,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            AnimatedVisibility(visible = error != null) {
                Aviso(titulo = "No pudimos unirte.", mensaje = error.orEmpty(), tipo = TipoAviso.Error)
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.aparecer(visible, orden = 1),
            ) {
                Campo(
                    valor = codigo,
                    alCambiar = {
                        codigo = it.uppercase()
                        errorCodigo = null
                    },
                    etiqueta = "Código de invitación",
                    icono = Iconos.Llave,
                    marcador = "Ej.: ABCD-1234",
                    error = errorCodigo,
                    sacudir = sacudidas,
                    opcionesTeclado = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next,
                    ),
                    accionesTeclado = siguiente,
                    habilitado = !cargando,
                )
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
                    accionesTeclado = siguiente,
                    tipoContenido = ContentType.PersonFullName,
                    habilitado = !cargando,
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
                    valido = Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches(),
                    sacudir = sacudidas,
                    tipoTeclado = KeyboardType.Email,
                    opcionesTeclado = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
                    accionesTeclado = siguiente,
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
                    marcador = "Mínimo 8 caracteres",
                    error = errorContrasena,
                    sacudir = sacudidas,
                    esContrasena = true,
                    opcionesTeclado = KeyboardOptions(imeAction = ImeAction.Done),
                    accionesTeclado = KeyboardActions(onDone = { enviar() }),
                    tipoContenido = ContentType.NewPassword,
                    habilitado = !cargando,
                )
            }

            Boton(
                texto = "Unirme al taller",
                alPulsar = ::enviar,
                ocupado = cargando,
                icono = Iconos.Taller,
                modifier = Modifier
                    .fillMaxWidth()
                    .aparecer(visible, orden = 2),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, widthDp = 380, heightDp = 860)
@Composable
private fun VistaPreviaCodigoTaller() {
    TemaRinFix {
        PantallaCodigoTaller(alVolver = {}, alUnirse = { _, _, _, _ -> })
    }
}
