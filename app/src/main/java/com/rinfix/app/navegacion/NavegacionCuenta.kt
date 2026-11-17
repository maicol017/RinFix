package com.rinfix.app.navegacion

import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rinfix.app.cuenta.CuentaLogica
import com.rinfix.app.cuenta.PantallaAcceso
import com.rinfix.app.cuenta.PantallaBienvenida
import com.rinfix.app.cuenta.PantallaCodigoTaller
import com.rinfix.app.cuenta.PantallaRegistro

/** Pantallas para quien todavía no ha iniciado sesión. */
object RutasCuenta {
    const val Bienvenida = "bienvenida"
    const val Acceso = "acceso"
    const val Registro = "registro"
    const val CodigoTaller = "codigo-taller"
}

private val CurvaSuave = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * Navegación de cuenta: Bienvenida → Iniciar sesión, Registro o Código de taller.
 * Cuando alguien entra con éxito, la app cambia sola a la navegación de su rol.
 *
 * Con `correoSugerido` (viene de «Usar contraseña» en el desbloqueo) abre directo
 * «Iniciar sesión» con ese correo escrito. Atrás sigue llevando a la Bienvenida.
 */
@Composable
fun NavegacionCuenta(modifier: Modifier = Modifier, correoSugerido: String? = null) {
    val controlador = rememberNavController()
    val contexto = LocalContext.current

    var accesoAbierto by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (correoSugerido != null && !accesoAbierto) {
            accesoAbierto = true
            controlador.navigate(RutasCuenta.Acceso)
        }
    }

    // Funciones que llegan con la conexión al backend. Mientras tanto se avisa con claridad.
    fun proximamente(que: String) {
        Toast.makeText(contexto, "$que estará disponible al conectar la cuenta real.", Toast.LENGTH_SHORT).show()
    }

    NavHost(
        navController = controlador,
        startDestination = RutasCuenta.Bienvenida,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(SlideDirection.Start, tween(320, easing = CurvaSuave)) { it / 5 } +
                fadeIn(tween(220))
        },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = {
            slideOutOfContainer(SlideDirection.End, tween(280, easing = CurvaSuave)) { it / 5 } +
                fadeOut(tween(200))
        },
    ) {
        composable(RutasCuenta.Bienvenida) {
            val logica: CuentaLogica = viewModel()
            PantallaBienvenida(
                alContinuarConGoogle = logica::entrarConGoogle,
                alCrearCuenta = { controlador.navigate(RutasCuenta.Registro) },
                alIniciarSesion = { controlador.navigate(RutasCuenta.Acceso) },
                alEntrarConCodigo = { controlador.navigate(RutasCuenta.CodigoTaller) },
            )
        }

        composable(RutasCuenta.Acceso) {
            val logica: CuentaLogica = viewModel()
            val estado by logica.estado.collectAsState()
            PantallaAcceso(
                alVolver = { controlador.popBackStack() },
                alEntrar = logica::entrar,
                alContinuarConGoogle = logica::entrarConGoogle,
                alOlvidarContrasena = { proximamente("Recuperar la contraseña") },
                alCrearCuenta = {
                    controlador.navigate(RutasCuenta.Registro) {
                        popUpTo(RutasCuenta.Bienvenida)
                    }
                },
                correoInicial = correoSugerido.orEmpty(),
                cargando = estado.cargando,
                error = estado.error,
                intentosFallidos = estado.intentosFallidos,
            )
        }

        composable(RutasCuenta.Registro) {
            val logica: CuentaLogica = viewModel()
            val estado by logica.estado.collectAsState()
            PantallaRegistro(
                alVolver = { controlador.popBackStack() },
                alCrear = logica::crearCuenta,
                alVerPolitica = { proximamente("La política de datos") },
                alIniciarSesion = {
                    controlador.navigate(RutasCuenta.Acceso) {
                        popUpTo(RutasCuenta.Bienvenida)
                    }
                },
                cargando = estado.cargando,
                error = estado.error,
                intentosFallidos = estado.intentosFallidos,
            )
        }

        composable(RutasCuenta.CodigoTaller) {
            val logica: CuentaLogica = viewModel()
            val estado by logica.estado.collectAsState()
            PantallaCodigoTaller(
                alVolver = { controlador.popBackStack() },
                alUnirse = logica::unirseATaller,
                cargando = estado.cargando,
                error = estado.error,
                intentosFallidos = estado.intentosFallidos,
            )
        }
    }
}
