package com.rinfix.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.rinfix.app.apertura.Apertura
import com.rinfix.app.datos.Servicios
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.navegacion.Puerta
import com.rinfix.app.navegacion.PuertaLogica

/**
 * Punto de entrada de la app. Android exige este archivo con este nombre.
 *
 * Es una FragmentActivity porque la ventana de huella del sistema la necesita.
 */
class MainActivity : FragmentActivity() {

    private val puerta: PuertaLogica by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Servicios.iniciar(this)

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
                    // La apertura se ve solo al abrir la app desde cero, no al girar el teléfono
                    // ni al volver a ella. Mientras corre, la app ya se está preparando debajo.
                    var aperturaTerminada by rememberSaveable { mutableStateOf(false) }

                    Box {
                        Puerta(logica = puerta, listo = aperturaTerminada)
                        if (!aperturaTerminada) {
                            Apertura(alTerminar = { aperturaTerminada = true })
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        puerta.alVolverALaApp()
    }

    override fun onStop() {
        super.onStop()
        puerta.alSalirDeLaApp()
    }
}
