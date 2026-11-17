package com.rinfix.app.componentes

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.Movimiento
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.sombra

/**
 * Opciones de una sola elección en una píldora, con una pastilla blanca que se desliza
 * hasta la elegida. Para pocas opciones cortas, como «Aleación · Acero · No sé».
 */
@Composable
fun <T> SelectorSegmentos(
    opciones: List<T>,
    seleccionada: T,
    alElegir: (T) -> Unit,
    texto: (T) -> String,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    val indice = opciones.indexOf(seleccionada).coerceAtLeast(0)
    val vista = LocalView.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(Colores.Crema2, Formas.Pildora)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        val anchoOpcion = maxWidth / opciones.size
        val desplazamiento by animateDpAsState(
            targetValue = anchoOpcion * indice,
            animationSpec = tween(durationMillis = 350, easing = Movimiento.Suave),
            label = "pastilla del selector",
        )
        Box(
            modifier = Modifier
                .offset(x = desplazamiento)
                .width(anchoOpcion)
                .height(AltoOpcion)
                .sombra(Sombra.Baja, Formas.Pildora)
                .background(Colores.Papel, Formas.Pildora),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            opciones.forEach { opcion ->
                val elegida = opcion == seleccionada
                val color by animateColorAsState(
                    targetValue = if (elegida) Colores.Tinta else Colores.Tinta2,
                    label = "texto de la opción",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = AltoOpcion)
                        .clip(Formas.Pildora)
                        .selectable(
                            selected = elegida,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = habilitado,
                            role = Role.RadioButton,
                            onClick = {
                                if (!elegida) {
                                    vista.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    alElegir(opcion)
                                }
                            },
                        ),
                ) {
                    Text(
                        text = texto(opcion),
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val AltoOpcion = 38.dp

/**
 * Fichas redondas de una sola elección que bajan de línea si no caben,
 * por ejemplo los tamaños de rin. La elegida se pone oscura.
 *
 * `descripcion` es lo que lee el lector de pantalla: «17 pulgadas» en lugar de «17"».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SelectorFichas(
    opciones: List<T>,
    seleccionada: T?,
    alElegir: (T) -> Unit,
    texto: (T) -> String,
    modifier: Modifier = Modifier,
    descripcion: (T) -> String = texto,
    habilitado: Boolean = true,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.selectableGroup(),
    ) {
        opciones.forEach { opcion ->
            Ficha(
                texto = texto(opcion),
                descripcion = descripcion(opcion),
                elegida = opcion == seleccionada,
                habilitada = habilitado,
                alPulsar = { alElegir(opcion) },
            )
        }
    }
}

@Composable
private fun Ficha(
    texto: String,
    descripcion: String,
    elegida: Boolean,
    habilitada: Boolean,
    alPulsar: () -> Unit,
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionada by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionada) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "escala de la ficha",
    )
    val fondo by animateColorAsState(if (elegida) Colores.Tinta else Colores.Papel, label = "fondo de la ficha")
    val colorTexto by animateColorAsState(if (elegida) Colores.Crema else Colores.Tinta, label = "texto de la ficha")
    val vista = LocalView.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .defaultMinSize(minWidth = 52.dp, minHeight = 44.dp)
            .clip(Formas.Pildora)
            .background(fondo)
            .border(1.dp, if (elegida) Colores.Tinta else Colores.Linea2, Formas.Pildora)
            .selectable(
                selected = elegida,
                interactionSource = interaccion,
                indication = null,
                enabled = habilitada,
                role = Role.RadioButton,
                onClick = {
                    vista.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    alPulsar()
                },
            )
            .semantics { contentDescription = descripcion }
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = colorTexto,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaSelectores() {
    TemaRinFix {
        var material by remember { mutableStateOf("Aleación") }
        var tamano by remember { mutableStateOf<Int?>(17) }
        Column(modifier = Modifier.padding(16.dp)) {
            SelectorSegmentos(
                opciones = listOf("Aleación", "Acero", "No sé"),
                seleccionada = material,
                alElegir = { material = it },
                texto = { it },
            )
            SelectorFichas(
                opciones = listOf(13, 14, 15, 16, 17, 18, 19, 20, null),
                seleccionada = tamano,
                alElegir = { tamano = it },
                texto = { it?.let { pulgadas -> "$pulgadas\"" } ?: "No sé" },
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
