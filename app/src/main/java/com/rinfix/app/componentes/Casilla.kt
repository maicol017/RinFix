package com.rinfix.app.componentes

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.TemaRinFix

private val FormaCaja = RoundedCornerShape(8.dp)

/**
 * Casilla para aceptar o marcar algo, con su texto al lado.
 * Toda la fila responde al toque. Al marcarla, la caja se llena de tinta y el check
 * aparece con un pequeño rebote.
 */
@Composable
fun Casilla(
    marcada: Boolean,
    alCambiar: (Boolean) -> Unit,
    texto: String,
    modifier: Modifier = Modifier,
    habilitada: Boolean = true,
) {
    val vista = LocalView.current
    val fondo by animateColorAsState(
        targetValue = if (marcada) Colores.Tinta else Colores.Papel,
        animationSpec = tween(durationMillis = 200),
        label = "fondo de la casilla",
    )
    val borde by animateColorAsState(
        targetValue = if (marcada) Colores.Tinta else Colores.Linea2,
        animationSpec = tween(durationMillis = 200),
        label = "borde de la casilla",
    )
    val escalaCheck by animateFloatAsState(
        targetValue = if (marcada) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "check de la casilla",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .clip(Formas.Pequena)
            .toggleable(
                value = marcada,
                enabled = habilitada,
                role = Role.Checkbox,
                onValueChange = {
                    vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    alCambiar(it)
                },
            )
            .padding(vertical = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .background(fondo, FormaCaja)
                .border(2.dp, borde, FormaCaja),
        ) {
            Icon(
                imageVector = Iconos.Hecho,
                contentDescription = null,
                tint = Colores.Crema,
                modifier = Modifier
                    .size(15.dp)
                    .graphicsLayer {
                        scaleX = escalaCheck
                        scaleY = escalaCheck
                        alpha = escalaCheck.coerceIn(0f, 1f)
                    },
            )
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = if (habilitada) Colores.Tinta2 else Colores.Tinta3,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, widthDp = 380)
@Composable
private fun VistaPreviaCasilla() {
    var marcada by remember { mutableStateOf(true) }
    TemaRinFix {
        Column(modifier = Modifier.padding(20.dp)) {
            Casilla(
                marcada = marcada,
                alCambiar = { marcada = it },
                texto = "Acepto los términos y la política de tratamiento de datos personales.",
            )
            Casilla(marcada = false, alCambiar = {}, texto = "Recibir avisos cuando cambie mi reparación")
        }
    }
}
