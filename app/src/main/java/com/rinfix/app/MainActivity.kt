package com.rinfix.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import com.rinfix.app.apertura.Apertura
import com.rinfix.app.datos.Servicios
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.navegacion.NavegacionCuenta
import com.rinfix.app.navegacion.NavegacionRinFix
import kotlinx.coroutines.launch

/**
 * Punto de entrada de la app. Android exige este archivo con este nombre.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // La app dibuja detrás de las barras del sistema. Los íconos de la barra de estado
        // van siempre oscuros porque el fondo de RinFix es claro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        setContent {
            TemaRinFix {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val usuario by Servicios.cuentas.usuarioActual.collectAsState()
                    val alcance = rememberCoroutineScope()

                    // La apertura se ve solo al abrir la app desde cero, no al girar el teléfono
                    // ni al volver a ella. Mientras corre, la app ya se está preparando debajo.
                    var aperturaTerminada by rememberSaveable { mutableStateOf(false) }

                    Box {
                        // Sin sesión: bienvenida y acceso. Con sesión: la app del rol del usuario.
                        Crossfade(
                            targetState = usuario,
                            animationSpec = tween(durationMillis = 350),
                            label = "sesión",
                        ) { actual ->
                            if (actual == null) {
                                NavegacionCuenta()
                            } else {
                                NavegacionRinFix(
                                    rol = actual.rol,
                                    alCerrarSesion = {
                                        alcance.launch { Servicios.cuentas.salir() }
                                    },
                                )
                            }
                        }
                        if (!aperturaTerminada) {
                            Apertura(alTerminar = { aperturaTerminada = true })
                        }
                    }
                }
            }
        }
    }
}
