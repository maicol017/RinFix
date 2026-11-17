package com.rinfix.app.componentes

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.sombra

/** Variantes de tarjeta según su importancia en la pantalla. */
enum class EstiloTarjeta(
    internal val forma: Shape,
    internal val sombra: Sombra,
    internal val conBorde: Boolean,
    internal val fondo: Color,
) {
    /** Tarjeta normal: blanca, sombra baja y borde fino. */
    Normal(Formas.Tarjeta, Sombra.Baja, conBorde = true, fondo = Colores.Papel),

    /** Tarjeta destacada, como el servicio activo en Inicio: más redondeada y más alta. */
    Principal(Formas.Principal, Sombra.Media, conBorde = false, fondo = Colores.Papel),

    /** Bloque dentro de otra tarjeta o sobre el fondo, sin sombra. */
    Plana(Formas.Campo, Sombra.Ninguna, conBorde = false, fondo = Colores.Crema2),
}

/**
 * Tarjeta de RinFix. El contenido se apila en columna.
 *
 * - `fondo` permite tarjetas de color, por ejemplo lavanda suave para la IA.
 * - Si `alPulsar` no es nulo, la tarjeta se hunde un poco al tocarla y vibra suave.
 *   En ese caso `descripcionAccion` es lo que anuncia el lector de pantalla.
 */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    estilo: EstiloTarjeta = EstiloTarjeta.Normal,
    fondo: Color = estilo.fondo,
    relleno: PaddingValues = PaddingValues(18.dp),
    espacio: Int = 10,
    alPulsar: (() -> Unit)? = null,
    descripcionAccion: String? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionada by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionada) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "escala de la tarjeta",
    )
    val vista = LocalView.current

    Column(
        verticalArrangement = Arrangement.spacedBy(espacio.dp),
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .sombra(estilo.sombra, estilo.forma)
            .clip(estilo.forma)
            .background(fondo)
            .then(
                if (estilo.conBorde) Modifier.border(1.dp, Colores.Linea, estilo.forma) else Modifier,
            )
            .then(
                if (alPulsar != null) {
                    Modifier.clickable(
                        interactionSource = interaccion,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = descripcionAccion,
                        onClick = {
                            vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            alPulsar()
                        },
                    )
                } else {
                    Modifier
                },
            )
            .padding(relleno),
        content = contenido,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaTarjetas() {
    TemaRinFix {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Tarjeta(
                estilo = EstiloTarjeta.Principal,
                alPulsar = {},
                descripcionAccion = "Ver seguimiento",
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Servicio activo", style = MaterialTheme.typography.labelMedium, color = Colores.Tinta3)
                Text("Reparación de rayones", style = MaterialTheme.typography.headlineSmall)
                Text("En pintura · entrega el viernes", style = MaterialTheme.typography.bodyMedium)
            }
            Tarjeta(modifier = Modifier.fillMaxWidth()) {
                Text("Mi garaje", style = MaterialTheme.typography.titleLarge)
                Text("Mazda 3 · rin 17\"", style = MaterialTheme.typography.bodyMedium, color = Colores.Tinta2)
            }
            Tarjeta(fondo = Colores.LavandaSuave, modifier = Modifier.fillMaxWidth()) {
                Text("Asistente", style = MaterialTheme.typography.titleMedium, color = Colores.LavandaTexto)
                Text(
                    "Pregúntame por tu diagnóstico.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Colores.LavandaTexto,
                )
            }
            Tarjeta(estilo = EstiloTarjeta.Plana, modifier = Modifier.fillMaxWidth()) {
                Text("Bloque plano dentro de otra tarjeta", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
