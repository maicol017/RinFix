package com.rinfix.app.cliente

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rinfix.app.componentes.PantallaProvisional

@Composable
fun PantallaAnalizar(modifier: Modifier = Modifier) {
    PantallaProvisional(
        titulo = "Analizar",
        descripcion = "Toma la foto del rin y sigue el análisis paso a paso.",
        modifier = modifier,
    )
}
