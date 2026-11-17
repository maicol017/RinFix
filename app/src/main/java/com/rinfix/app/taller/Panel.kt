package com.rinfix.app.taller

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rinfix.app.componentes.PantallaProvisional

@Composable
fun PantallaPanel(modifier: Modifier = Modifier) {
    PantallaProvisional(
        titulo = "Panel",
        descripcion = "Resumen del día, solicitudes nuevas y trabajos en curso.",
        modifier = modifier,
    )
}
