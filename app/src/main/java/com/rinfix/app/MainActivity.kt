package com.rinfix.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rinfix.app.cliente.PantallaInicio
import com.rinfix.app.diseno.TemaRinFix

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
                    // Por ahora se muestra Inicio para revisar el tema.
                    // En el paso de navegación se reemplaza por la navegación completa.
                    PantallaInicio(modifier = Modifier.safeDrawingPadding())
                }
            }
        }
    }
}
