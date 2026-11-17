package com.rinfix.app.cliente

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rinfix.app.componentes.PantallaProvisional

@Composable
fun PantallaInicio(modifier: Modifier = Modifier) {
    PantallaProvisional(
        titulo = "Inicio",
        descripcion = "Servicio activo con su etapa, acceso para analizar un rin, Mi garaje y actividad reciente.",
        modifier = modifier,
    )
}
