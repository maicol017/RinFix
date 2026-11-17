package com.rinfix.app.componentes

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.sombra

/** Variantes del botón. */
enum class EstiloBoton {

    /** Acción principal de la pantalla: fondo tinta. Idealmente uno por pantalla. */
    Principal,

    /** Acción secundaria: fondo blanco con borde. */
    Blanco,

    /** Confirmación positiva, por ejemplo «Publicar avance» o «Listo». */
    Menta,

    /** Acción menor sin fondo, por ejemplo «Ahora no». */
    Transparente,
}

/** Tamaños del botón: alto y espacio a los lados. */
enum class TamanoBoton(val alto: Dp, val relleno: Dp) {
    Normal(alto = 56.dp, relleno = 22.dp),
    Pequeno(alto = 44.dp, relleno = 18.dp),
}

/** Sombra propia del botón principal: corta y oscura, pegada a la parte de abajo. */
private val SombraPrincipal = Shadow(
    radius = 20.dp,
    spread = (-10).dp,
    color = Colores.Tinta.copy(alpha = 0.6f),
    offset = DpOffset(x = 0.dp, y = 10.dp),
)

/**
 * Botón tipo píldora de RinFix.
 *
 * - Al presionarlo se hunde al 96 %, rebota y vibra suave.
 * - Con `ocupado = true` muestra un indicador de carga, conserva su ancho y no acepta otro toque.
 * - Con `habilitado = false` se ve atenuado y no responde.
 */
@Composable
fun Boton(
    texto: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    estilo: EstiloBoton = EstiloBoton.Principal,
    tamano: TamanoBoton = TamanoBoton.Normal,
    icono: ImageVector? = null,
    ocupado: Boolean = false,
    habilitado: Boolean = true,
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "escala del botón",
    )
    val opacidadTexto by animateFloatAsState(
        targetValue = if (ocupado) 0f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "texto del botón",
    )
    val vista = LocalView.current

    val fondo: Color
    val colorContenido: Color
    when (estilo) {
        EstiloBoton.Principal -> {
            fondo = Colores.Tinta
            colorContenido = Colores.Crema
        }
        EstiloBoton.Blanco -> {
            fondo = Colores.Papel
            colorContenido = Colores.Tinta
        }
        EstiloBoton.Menta -> {
            fondo = Colores.Menta
            colorContenido = Colores.Tinta
        }
        EstiloBoton.Transparente -> {
            fondo = Color.Transparent
            colorContenido = Colores.Tinta
        }
    }
    val alto = if (estilo == EstiloBoton.Transparente) TamanoBoton.Pequeno.alto else tamano.alto
    val estiloTexto = if (tamano == TamanoBoton.Normal) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
                alpha = if (habilitado) 1f else 0.45f
            }
            .then(
                when (estilo) {
                    EstiloBoton.Principal -> Modifier.dropShadow(Formas.Pildora, SombraPrincipal)
                    EstiloBoton.Blanco -> Modifier.sombra(Sombra.Baja, Formas.Pildora)
                    else -> Modifier
                },
            )
            .clip(Formas.Pildora)
            .background(fondo)
            .then(
                if (estilo == EstiloBoton.Blanco) {
                    Modifier.border(1.dp, Colores.Linea2, Formas.Pildora)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = habilitado && !ocupado,
                role = Role.Button,
                onClick = {
                    vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    alPulsar()
                },
            )
            .semantics { if (ocupado) stateDescription = "Cargando" }
            .heightIn(min = alto)
            .padding(horizontal = tamano.relleno),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.graphicsLayer { alpha = opacidadTexto },
        ) {
            if (icono != null) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorContenido,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = texto,
                color = colorContenido,
                style = estiloTexto,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AnimatedVisibility(
            visible = ocupado,
            enter = fadeIn(tween(durationMillis = 150)),
            exit = fadeOut(tween(durationMillis = 150)),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colorContenido,
                trackColor = colorContenido.copy(alpha = 0.25f),
                strokeWidth = 2.5.dp,
            )
        }
    }
}

/**
 * Botón redondo solo con ícono, por ejemplo «atrás» o «notificaciones».
 * `descripcion` es lo que lee el lector de pantalla.
 */
@Composable
fun BotonIcono(
    icono: ImageVector,
    descripcion: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "escala del botón de ícono",
    )
    val vista = LocalView.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
                alpha = if (habilitado) 1f else 0.45f
            }
            .size(44.dp)
            .clip(CircleShape)
            .background(Colores.Papel)
            .border(1.dp, Colores.Linea, CircleShape)
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = habilitado,
                role = Role.Button,
                onClickLabel = descripcion,
                onClick = {
                    vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    alPulsar()
                },
            ),
    ) {
        Icon(
            imageVector = icono,
            contentDescription = descripcion,
            tint = Colores.Tinta,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaBotones() {
    TemaRinFix {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Boton(texto = "Analizar mi rin", alPulsar = {}, modifier = Modifier.fillMaxWidth())
            Boton(
                texto = "Ver cotización",
                alPulsar = {},
                estilo = EstiloBoton.Blanco,
                modifier = Modifier.fillMaxWidth(),
            )
            Boton(texto = "Publicar avance", alPulsar = {}, estilo = EstiloBoton.Menta)
            Boton(texto = "Ahora no", alPulsar = {}, estilo = EstiloBoton.Transparente)
            Boton(texto = "Pagar", alPulsar = {}, ocupado = true, modifier = Modifier.fillMaxWidth())
            Boton(texto = "Continuar", alPulsar = {}, tamano = TamanoBoton.Pequeno, habilitado = false)
        }
    }
}
