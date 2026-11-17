package com.rinfix.app.cuenta

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rinfix.app.componentes.PantallaProvisional

@Composable
fun PantallaPerfil(modifier: Modifier = Modifier) {
    PantallaProvisional(
        titulo = "Perfil",
        descripcion = "Tus datos, vehículos, desbloqueo con huella y cierre de sesión.",
        modifier = modifier,
    )
}
