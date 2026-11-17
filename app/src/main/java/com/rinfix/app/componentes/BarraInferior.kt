package com.rinfix.app.componentes

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.TemaRinFix

/** Una pestaña de la barra inferior. `ruta` identifica la pantalla a la que lleva. */
data class Pestana(
    val ruta: String,
    val etiqueta: String,
    val icono: ImageVector,
)

/** Botón redondo que sobresale en el centro de la barra (el rin para analizar). */
data class BotonCentral(
    val icono: ImageVector,
    val descripcion: String,
    val alPulsar: () -> Unit,
)

/**
 * Espacio que las pantallas deben dejar abajo para que la barra no tape su contenido
 * (alto de la barra más su margen inferior). Se suma al espacio de la barra del sistema.
 */
val EspacioBarraInferior = 96.dp

/** Sombra de la barra: larga y difusa, para que se vea flotando. */
private val SombraBarra = Shadow(
    radius = 36.dp,
    spread = (-12).dp,
    color = Colores.Tinta.copy(alpha = 0.55f),
    offset = DpOffset(x = 0.dp, y = 18.dp),
)

/** Sombra del botón central. */
private val SombraBotonCentral = Shadow(
    radius = 22.dp,
    spread = (-6).dp,
    color = Colores.Tinta.copy(alpha = 0.45f),
    offset = DpOffset(x = 0.dp, y = 10.dp),
)

/**
 * Barra inferior flotante de RinFix.
 *
 * - La pestaña activa toma el color del rol (menta para el cliente, mantequilla para el taller)
 *   y muestra su nombre; las demás solo muestran el ícono.
 * - Si hay `botonCentral`, se ubica en la mitad de las pestañas y sobresale por encima.
 */
@Composable
fun BarraInferior(
    pestanas: List<Pestana>,
    seleccionada: String,
    alSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier,
    colorActivo: Color = Colores.Menta,
    botonCentral: BotonCentral? = null,
) {
    val mitad = pestanas.size / 2

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            // Arriba se deja espacio para la parte del botón central que sobresale,
            // así también responde si se toca por el borde superior.
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 20.dp,
                top = if (botonCentral != null) 18.dp else 0.dp,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .dropShadow(Formas.Pildora, SombraBarra)
                .background(Colores.Tinta, Formas.Pildora)
                .padding(horizontal = 10.dp)
                .selectableGroup(),
        ) {
            pestanas.forEachIndexed { indice, pestana ->
                if (botonCentral != null && indice == mitad) {
                    BotonCentralBarra(botonCentral)
                }
                ElementoBarra(
                    pestana = pestana,
                    activa = pestana.ruta == seleccionada,
                    colorActivo = colorActivo,
                    alPulsar = { alSeleccionar(pestana.ruta) },
                )
            }
        }
    }
}

@Composable
private fun ElementoBarra(
    pestana: Pestana,
    activa: Boolean,
    colorActivo: Color,
    alPulsar: () -> Unit,
) {
    val fondo by animateColorAsState(
        targetValue = if (activa) colorActivo else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "fondo de la pestaña",
    )
    val colorContenido by animateColorAsState(
        targetValue = if (activa) Colores.Tinta else Colores.Crema.copy(alpha = 0.72f),
        animationSpec = tween(durationMillis = 250),
        label = "color de la pestaña",
    )
    val interaccion = remember { MutableInteractionSource() }
    val presionada by interaccion.collectIsPressedAsState()
    val escalaIcono by animateFloatAsState(
        targetValue = if (presionada) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "escala del ícono",
    )
    val vista = LocalView.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(44.dp)
            .clip(Formas.Pildora)
            .background(fondo)
            .selectable(
                selected = activa,
                interactionSource = interaccion,
                indication = null,
                role = Role.Tab,
                onClick = {
                    if (!activa) vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    alPulsar()
                },
            )
            .semantics { contentDescription = pestana.etiqueta }
            .padding(horizontal = 11.dp),
    ) {
        Icon(
            imageVector = pestana.icono,
            contentDescription = null,
            tint = colorContenido,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    scaleX = escalaIcono
                    scaleY = escalaIcono
                },
        )
        AnimatedVisibility(
            visible = activa,
            enter = expandHorizontally(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            ) + fadeIn(tween(durationMillis = 200, delayMillis = 60)),
            exit = shrinkHorizontally(tween(durationMillis = 200)) + fadeOut(tween(durationMillis = 120)),
        ) {
            Text(
                text = pestana.etiqueta,
                color = colorContenido,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}

@Composable
private fun BotonCentralBarra(boton: BotonCentral) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "escala del botón central",
    )
    val vista = LocalView.current

    // Círculo crema de 70 (58 del botón + 6 de anillo a cada lado) que sube por encima de la barra.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(y = (-17).dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .size(70.dp)
            .dropShadow(CircleShape, SombraBotonCentral)
            .clip(CircleShape)
            .background(Colores.Crema)
            .clickable(
                interactionSource = interaccion,
                indication = null,
                role = Role.Button,
                onClickLabel = boton.descripcion,
                onClick = {
                    vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    boton.alPulsar()
                },
            ),
    ) {
        Icon(
            imageVector = boton.icono,
            contentDescription = boton.descripcion,
            tint = Colores.Tinta,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, widthDp = 380)
@Composable
private fun VistaPreviaBarras() {
    var cliente by remember { mutableStateOf("inicio") }
    var taller by remember { mutableStateOf("panel") }
    TemaRinFix {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(top = 30.dp)) {
            BarraInferior(
                pestanas = listOf(
                    Pestana("inicio", "Inicio", Iconos.Inicio),
                    Pestana("ordenes", "Órdenes", Iconos.Lista),
                    Pestana("asistente", "Asistente", Iconos.Ia),
                    Pestana("perfil", "Perfil", Iconos.Usuario),
                ),
                seleccionada = cliente,
                alSeleccionar = { cliente = it },
                botonCentral = BotonCentral(Iconos.Rin, "Analizar un rin", alPulsar = {}),
            )
            BarraInferior(
                pestanas = listOf(
                    Pestana("panel", "Panel", Iconos.Panel),
                    Pestana("agenda", "Agenda", Iconos.Calendario),
                    Pestana("trabajos", "Trabajos", Iconos.Lista),
                    Pestana("perfil", "Perfil", Iconos.Usuario),
                ),
                seleccionada = taller,
                alSeleccionar = { taller = it },
                colorActivo = Colores.Mantequilla,
            )
        }
    }
}
