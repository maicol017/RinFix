package com.rinfix.app.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix
import kotlinx.coroutines.delay

/** Bloque que brilla de izquierda a derecha mientras algo carga. */
fun Modifier.brilloDeCarga(forma: Shape = RoundedCornerShape(12.dp)): Modifier = composed {
    val transicion = rememberInfiniteTransition(label = "brillo de carga")
    val avance by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_200, easing = LinearEasing)),
        label = "avance del brillo",
    )
    clip(forma).drawBehind {
        drawRect(Colores.Crema2)
        val ancho = size.width
        val inicio = -ancho + avance * ancho * 2
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Colores.Crema2, Colores.Papel.copy(alpha = 0.75f), Colores.Crema2),
                start = Offset(inicio, 0f),
                end = Offset(inicio + ancho * 0.6f, size.height),
            ),
        )
    }
}

/**
 * Silueta de una pantalla de detalle mientras llegan sus datos. Aparece solo si la carga
 * tarda más de un instante, para no parpadear cuando los datos ya están en el teléfono.
 */
@Composable
fun EsqueletoDetalle(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(250)
        visible = true
    }
    AnimatedVisibility(visible = visible, enter = fadeIn(), modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clearAndSetSemantics { contentDescription = "Cargando" },
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(170.dp).brilloDeCarga(RoundedCornerShape(22.dp)))
            Box(modifier = Modifier.fillMaxWidth(0.55f).height(22.dp).brilloDeCarga())
            Box(modifier = Modifier.fillMaxWidth().height(14.dp).brilloDeCarga())
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).brilloDeCarga())
            Box(modifier = Modifier.fillMaxWidth().height(120.dp).brilloDeCarga(RoundedCornerShape(22.dp)))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaEsqueleto() {
    TemaRinFix { EsqueletoDetalle() }
}
