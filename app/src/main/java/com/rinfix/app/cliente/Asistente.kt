package com.rinfix.app.cliente

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rinfix.app.componentes.PantallaProvisional

@Composable
fun PantallaAsistente(modifier: Modifier = Modifier) {
    PantallaProvisional(
        titulo = "Asistente",
        descripcion = "Pregúntale al asistente sobre tu diagnóstico y tus servicios.",
        modifier = modifier,
    )
}
