package com.rinfix.app.navegacion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rinfix.app.cliente.PantallaAnalizando
import com.rinfix.app.cliente.PantallaAnalizar
import com.rinfix.app.cliente.PantallaResultado
import com.rinfix.app.cliente.PantallaAsistente
import com.rinfix.app.cliente.PantallaGaraje
import com.rinfix.app.cliente.PantallaInicio
import com.rinfix.app.cliente.PantallaOrdenes
import com.rinfix.app.componentes.BarraInferior
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.BotonCentral
import com.rinfix.app.componentes.EspacioBarraInferior
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.PantallaProvisional
import com.rinfix.app.cuenta.PantallaPerfil
import com.rinfix.app.datos.Rol
import com.rinfix.app.diseno.Colores
import com.rinfix.app.taller.PantallaAgenda
import com.rinfix.app.taller.PantallaPanel
import com.rinfix.app.taller.PantallaTrabajos

/** La misma curva del prototipo: arranca rápido y frena suave al llegar. */
private val CurvaSuave = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * Navegación de RinFix para un rol.
 *
 * - Entre pestañas: solo fundido, porque son pantallas hermanas.
 * - Hacia una pantalla de detalle: entra desde la derecha; al volver, sale hacia la derecha.
 * - Analizar sube desde abajo, como una cámara que se abre, y oculta la barra inferior.
 *
 * `alCerrarSesion` lo usa Perfil para salir de la cuenta.
 */
@Composable
fun NavegacionRinFix(
    rol: Rol,
    modifier: Modifier = Modifier,
    alCerrarSesion: (() -> Unit)? = null,
) {
    val controlador = rememberNavController()
    val entradaActual by controlador.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    val esCliente = rol == Rol.Cliente
    val pestanas = if (esCliente) PestanasCliente else PestanasTaller
    val rutaInicial = if (esCliente) Rutas.Inicio else Rutas.Panel
    val mostrarBarra = rutaActual == null || pestanas.any { it.ruta == rutaActual }

    // Las pestañas dejan espacio abajo para que la barra flotante no tape su contenido.
    val espacioPestana = Modifier
        .safeDrawingPadding()
        .padding(bottom = EspacioBarraInferior)

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = controlador,
            startDestination = rutaInicial,
            enterTransition = { entrada() },
            exitTransition = { salida() },
            popEnterTransition = { fadeIn(tween(durationMillis = 220)) },
            popExitTransition = { salidaAlVolver() },
        ) {
            // Cliente
            composable(Rutas.Inicio) {
                PantallaInicio(
                    alAnalizar = { controlador.navigate(Rutas.analizar()) { launchSingleTop = true } },
                    alVerOrden = { id -> controlador.navigate(Rutas.seguimiento(id)) },
                    alVerOrdenes = { controlador.irAPestana(Rutas.Ordenes) },
                    alAgendar = { controlador.navigate(Rutas.Agendar) },
                    alPagar = { id -> controlador.navigate(Rutas.pago(id)) },
                    alAbrirAsistente = { controlador.irAPestana(Rutas.Asistente) },
                    alVerGaraje = { controlador.navigate(Rutas.garaje()) },
                    alAgregarVehiculo = { controlador.navigate(Rutas.garaje(agregar = true)) },
                    alVerDiagnostico = { id -> controlador.navigate(Rutas.resultado(id)) },
                    modifier = espacioPestana,
                )
            }
            composable(Rutas.Ordenes) { PantallaOrdenes(modifier = espacioPestana) }
            composable(Rutas.Asistente) { PantallaAsistente(modifier = espacioPestana) }
            composable(
                route = Rutas.Analizar,
                arguments = listOf(
                    navArgument("vehiculo") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
                enterTransition = {
                    slideIntoContainer(
                        towards = SlideDirection.Up,
                        animationSpec = tween(durationMillis = 380, easing = CurvaSuave),
                        initialOffset = { it / 3 },
                    ) + fadeIn(tween(durationMillis = 220))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = SlideDirection.Down,
                        animationSpec = tween(durationMillis = 300, easing = CurvaSuave),
                        targetOffset = { it / 3 },
                    ) + fadeOut(tween(durationMillis = 200))
                },
            ) { entrada ->
                PantallaAnalizar(
                    alCerrar = { controlador.popBackStack() },
                    alAnalizar = { id -> controlador.navigate(Rutas.analizando(id)) },
                    idVehiculo = entrada.arguments?.getString("vehiculo"),
                )
            }
            composable(Rutas.Analizando) {
                PantallaAnalizando(
                    alVolver = { controlador.popBackStack() },
                    alVerResultado = { id ->
                        // Al ver el resultado, atrás ya no vuelve a la cámara ni al análisis.
                        controlador.navigate(Rutas.resultado(id)) {
                            popUpTo(Rutas.Analizar) { inclusive = true }
                        }
                    },
                    modifier = Modifier.safeDrawingPadding(),
                )
            }

            // Cliente: detalles que se construyen en los próximos pasos
            composable(
                route = Rutas.Garaje,
                arguments = listOf(
                    navArgument("agregar") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) { entrada ->
                PantallaGaraje(
                    alVolver = { controlador.popBackStack() },
                    alAnalizar = { id -> controlador.navigate(Rutas.analizar(id)) { launchSingleTop = true } },
                    abrirFormulario = entrada.arguments?.getBoolean("agregar") ?: false,
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
            composable(Rutas.Agendar) {
                PorConstruir(
                    titulo = "Agendar una revisión",
                    descripcion = "Elegir fecha y hora para que el taller revise un rin.",
                    alVolver = { controlador.popBackStack() },
                )
            }
            composable(Rutas.Resultado) {
                PantallaResultado(
                    alVolver = { controlador.popBackStack() },
                    alSolicitar = { controlador.navigate(Rutas.Agendar) },
                    alPreguntar = { controlador.irAPestana(Rutas.Asistente) },
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
            composable(Rutas.Seguimiento) {
                PorConstruir(
                    titulo = "Seguimiento",
                    descripcion = "Línea de tiempo de la reparación con las fotos de cada etapa.",
                    alVolver = { controlador.popBackStack() },
                )
            }
            composable(Rutas.Pago) {
                PorConstruir(
                    titulo = "Pagar servicio",
                    descripcion = "Pago simulado con resumen de costos y comprobante.",
                    alVolver = { controlador.popBackStack() },
                )
            }

            // Taller
            composable(Rutas.Panel) { PantallaPanel(modifier = espacioPestana) }
            composable(Rutas.Agenda) { PantallaAgenda(modifier = espacioPestana) }
            composable(Rutas.Trabajos) { PantallaTrabajos(modifier = espacioPestana) }

            // Ambos
            composable(Rutas.Perfil) {
                PantallaPerfil(
                    modifier = espacioPestana,
                    alCerrarSesion = alCerrarSesion,
                )
            }
        }

        AnimatedVisibility(
            visible = mostrarBarra,
            enter = slideInVertically(tween(durationMillis = 320, easing = CurvaSuave)) { it } +
                fadeIn(tween(durationMillis = 200)),
            exit = slideOutVertically(tween(durationMillis = 240)) { it } +
                fadeOut(tween(durationMillis = 160)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BarraInferior(
                pestanas = pestanas,
                seleccionada = rutaActual ?: rutaInicial,
                alSeleccionar = { ruta -> controlador.irAPestana(ruta) },
                colorActivo = if (esCliente) Colores.Menta else Colores.Mantequilla,
                botonCentral = if (esCliente) {
                    BotonCentral(
                        icono = Iconos.Rin,
                        descripcion = "Analizar un rin",
                        alPulsar = { controlador.navigate(Rutas.analizar()) { launchSingleTop = true } },
                    )
                } else {
                    null
                },
            )
        }
    }
}

/** Pantalla de detalle que todavía no existe: título, qué tendrá y botón para volver. */
@Composable
private fun PorConstruir(titulo: String, descripcion: String, alVolver: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        BarraSuperior(titulo = titulo, alVolver = alVolver)
        PantallaProvisional(titulo = titulo, descripcion = descripcion)
    }
}

/**
 * Cambia de pestaña sin apilar pantallas: si ya visitaste la pestaña, vuelve
 * a como la dejaste, y el botón atrás del teléfono siempre regresa a la pestaña inicial.
 */
private fun NavHostController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.entrePestanas(): Boolean =
    initialState.destination.route in RutasPestanas && targetState.destination.route in RutasPestanas

private fun AnimatedContentTransitionScope<NavBackStackEntry>.entrada(): EnterTransition =
    if (entrePestanas()) {
        fadeIn(tween(durationMillis = 220))
    } else {
        slideIntoContainer(
            towards = SlideDirection.Start,
            animationSpec = tween(durationMillis = 320, easing = CurvaSuave),
            initialOffset = { it / 5 },
        ) + fadeIn(tween(durationMillis = 220))
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.salida(): ExitTransition =
    fadeOut(tween(durationMillis = if (entrePestanas()) 160 else 200))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.salidaAlVolver(): ExitTransition =
    if (entrePestanas()) {
        fadeOut(tween(durationMillis = 160))
    } else {
        slideOutOfContainer(
            towards = SlideDirection.End,
            animationSpec = tween(durationMillis = 280, easing = CurvaSuave),
            targetOffset = { it / 5 },
        ) + fadeOut(tween(durationMillis = 200))
    }
