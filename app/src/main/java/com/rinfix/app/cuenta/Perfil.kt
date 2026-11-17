package com.rinfix.app.cuenta

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.PantallaProvisional

/**
 * Perfil del usuario, compartido por cliente y taller.
 * Por ahora es provisional; ya permite cerrar la sesión.
 */
@Composable
fun PantallaPerfil(
    modifier: Modifier = Modifier,
    alCerrarSesion: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        PantallaProvisional(
            titulo = "Perfil",
            descripcion = "Tus datos, vehículos, desbloqueo con huella y cierre de sesión.",
            modifier = Modifier.weight(1f),
        )
        if (alCerrarSesion != null) {
            Boton(
                texto = "Cerrar sesión",
                alPulsar = alCerrarSesion,
                estilo = EstiloBoton.Blanco,
                icono = Iconos.Salir,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}
