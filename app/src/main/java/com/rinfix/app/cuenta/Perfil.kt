package com.rinfix.app.cuenta

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.PantallaProvisional

/**
 * Perfil del usuario, compartido por cliente y taller.
 *
 * `alCambiarRol` es solo para pruebas: deja ver el lado del taller mientras
 * no existe el inicio de sesión. Se quita en la fase 1.
 */
@Composable
fun PantallaPerfil(
    modifier: Modifier = Modifier,
    textoCambioRol: String? = null,
    alCambiarRol: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        PantallaProvisional(
            titulo = "Perfil",
            descripcion = "Tus datos, vehículos, desbloqueo con huella y cierre de sesión.",
            modifier = Modifier.weight(1f),
        )
        if (textoCambioRol != null && alCambiarRol != null) {
            Boton(
                texto = textoCambioRol,
                alPulsar = alCambiarRol,
                estilo = EstiloBoton.Blanco,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}
