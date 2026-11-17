package com.rinfix.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.navegacion.NavegacionRinFix
import com.rinfix.app.navegacion.Rol

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
                    // Temporal: el rol se elige desde Perfil hasta que exista el inicio de sesión.
                    var rol by rememberSaveable { mutableStateOf(Rol.Cliente) }
                    key(rol) {
                        NavegacionRinFix(
                            rol = rol,
                            alCambiarRol = {
                                rol = if (rol == Rol.Cliente) Rol.Taller else Rol.Cliente
                            },
                        )
                    }
                }
            }
        }
    }
}
