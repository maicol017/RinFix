package com.rinfix.app.navegacion

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.fragment.app.FragmentActivity
import com.rinfix.app.cuenta.PantallaActivarHuella
import com.rinfix.app.cuenta.PantallaDesbloqueo
import com.rinfix.app.datos.Rol
import com.rinfix.app.navegacion.PuertaLogica.Etapa
import com.rinfix.app.seguridad.Huella
import kotlinx.coroutines.delay

/**
 * Primera pieza que se ve debajo de la apertura: muestra la etapa que decide [PuertaLogica]
 * y abre la ventana de huella del sistema cuando hace falta.
 *
 * `listo` es `false` mientras corre la apertura, para no abrir la huella encima de ella.
 */
@Composable
fun Puerta(logica: PuertaLogica, listo: Boolean, modifier: Modifier = Modifier) {
    val estado by logica.estado.collectAsState()
    val contexto = LocalContext.current
    val actividad = remember(contexto) { contexto.buscarActividad() }
    val vista = LocalView.current

    fun pedirDesbloqueo() {
        val pantalla = actividad ?: return
        val cifrador = logica.empezarDesbloqueo() ?: return
        Huella.pedir(
            actividad = pantalla,
            titulo = "Desbloquear RinFix",
            subtitulo = "Usa tu huella para entrar",
            textoAlterno = "Usar contraseña",
            cifrador = cifrador,
            alExito = logica::desbloqueado,
            alError = logica::errorAlDesbloquear,
            alAlterno = logica::usarContrasena,
            alCancelar = {},
        )
    }

    fun pedirActivacion() {
        val pantalla = actividad ?: return
        val cifrador = logica.empezarActivacion() ?: return
        Huella.pedir(
            actividad = pantalla,
            titulo = "Activar desbloqueo",
            subtitulo = "Confirma con tu huella",
            textoAlterno = "Cancelar",
            cifrador = cifrador,
            alExito = { desbloqueado ->
                if (logica.huellaActivada(desbloqueado)) {
                    vista.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            HapticFeedbackConstants.CONFIRM
                        } else {
                            HapticFeedbackConstants.CONTEXT_CLICK
                        },
                    )
                    Toast.makeText(contexto, "Desbloqueo con huella activado", Toast.LENGTH_SHORT).show()
                }
            },
            alError = logica::errorAlActivar,
            alAlterno = logica::activacionCancelada,
            alCancelar = logica::activacionCancelada,
        )
    }

    // Al llegar al desbloqueo, la ventana de huella se abre sola una vez.
    val etapa = estado.etapa
    LaunchedEffect(etapa, listo) {
        if (listo && etapa == Etapa.Desbloqueo && !estado.huellaInvalida && logica.debePedirSola()) {
            delay(650)
            pedirDesbloqueo()
        }
    }

    Crossfade(
        targetState = etapa,
        animationSpec = tween(durationMillis = 350),
        label = "puerta",
        modifier = modifier,
    ) { mostrada ->
        when (mostrada) {
            Etapa.Cuenta -> {
                val correo = remember { estado.correoSugerido }
                NavegacionCuenta(correoSugerido = correo)
            }

            Etapa.Desbloqueo -> {
                // Se fija al entrar para que no cambie mientras la pantalla se desvanece.
                val usuario = remember { estado.usuario }
                PantallaDesbloqueo(
                    nombre = usuario?.nombre.orEmpty(),
                    rol = usuario?.rol ?: Rol.Cliente,
                    alPedirHuella = ::pedirDesbloqueo,
                    alUsarContrasena = logica::usarContrasena,
                    alCambiarCuenta = logica::cambiarCuenta,
                    error = estado.errorDesbloqueo,
                    huellaInvalida = estado.huellaInvalida,
                )
            }

            Etapa.OfrecerHuella -> {
                BackHandler(onBack = logica::omitirHuella)
                PantallaActivarHuella(
                    alActivar = ::pedirActivacion,
                    alOmitir = logica::omitirHuella,
                    activando = estado.activando,
                    error = estado.errorActivacion,
                )
            }

            Etapa.App -> {
                val rol = remember { estado.usuario?.rol ?: Rol.Cliente }
                NavegacionRinFix(rol = rol, alCerrarSesion = logica::cerrarSesion)
            }
        }
    }
}

/** La ventana de huella necesita la actividad; en Compose solo se tiene el contexto. */
private tailrec fun Context.buscarActividad(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.buscarActividad()
    else -> null
}
