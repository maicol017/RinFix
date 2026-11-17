package com.rinfix.app.taller

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rinfix.app.componentes.PantallaProvisional

@Composable
fun PantallaAgenda(modifier: Modifier = Modifier) {
    PantallaProvisional(
        titulo = "Agenda",
        descripcion = "Semana de trabajo, capacidad del día y entregas.",
        modifier = modifier,
    )
}
