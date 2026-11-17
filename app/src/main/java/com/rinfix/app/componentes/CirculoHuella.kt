package com.rinfix.app.componentes

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Movimiento
import com.rinfix.app.diseno.TemaRinFix

/**
 * Círculo oscuro con la huella y un anillo menta que late hacia afuera.
 * Lo usan «Activar huella» (solo como dibujo) y «Desbloqueo» (como botón).
 *
 * - Si `alPulsar` no es nulo, se comporta como botón: se hunde al tocarlo y vibra suave.
 *   En ese caso `descripcion` es lo que anuncia el lector de pantalla.
 * - El anillo se dibuja por fuera del círculo; deja unos 18 dp libres alrededor.
 */
@Composable
fun CirculoHuella(
    modifier: Modifier = Modifier,
    tamano: Dp = 96.dp,
    duracionPulso: Int = 2000,
    alPulsar: (() -> Unit)? = null,
    descripcion: String? = null,
) {
    val transicion = rememberInfiniteTransition(label = "pulso de la huella")
    val pulso by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = duracionPulso
                0f at 0 using Movimiento.Suave
                1f at (duracionPulso * 0.8f).toInt()
            },
        ),
        label = "pulso",
    )

    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "escala de la huella",
    )
    val vista = LocalView.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamano)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .drawBehind {
                // Se lee aquí, al dibujar, para que el latido no recomponga la pantalla.
                drawCircle(
                    color = Colores.Menta.copy(alpha = 0.65f * (1f - pulso)),
                    radius = size.minDimension / 2 + AnilloPulso.toPx() * pulso,
                )
            }
            .background(Colores.Tinta, CircleShape)
            .then(
                if (alPulsar != null) {
                    Modifier.clickable(
                        interactionSource = interaccion,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            alPulsar()
                        },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Icon(
            imageVector = Iconos.Huella,
            contentDescription = if (alPulsar != null) descripcion else null,
            tint = Colores.Crema,
            modifier = Modifier.size(tamano * 0.47f),
        )
    }
}

private val AnilloPulso = 18.dp

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaCirculoHuella() {
    TemaRinFix {
        CirculoHuella(modifier = Modifier.padding(24.dp))
    }
}
