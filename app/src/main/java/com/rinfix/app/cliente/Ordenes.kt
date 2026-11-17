package com.rinfix.app.cliente

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rinfix.app.componentes.PantallaProvisional

@Composable
fun PantallaOrdenes(modifier: Modifier = Modifier) {
    PantallaProvisional(
        titulo = "Órdenes",
        descripcion = "Tus reparaciones activas y el historial de servicios.",
        modifier = modifier,
    )
}
