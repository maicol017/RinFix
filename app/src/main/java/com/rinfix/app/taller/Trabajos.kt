package com.rinfix.app.taller

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rinfix.app.componentes.PantallaProvisional

@Composable
fun PantallaTrabajos(modifier: Modifier = Modifier) {
    PantallaProvisional(
        titulo = "Trabajos",
        descripcion = "Trabajos asignados, avance por etapas y fotos del proceso.",
        modifier = modifier,
    )
}
