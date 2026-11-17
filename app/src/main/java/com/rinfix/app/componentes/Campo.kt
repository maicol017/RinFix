package com.rinfix.app.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.TemaRinFix

/**
 * Campo de texto de RinFix.
 *
 * - Etiqueta arriba y caja blanca de 56 dp con ícono a la izquierda.
 * - Al enfocarlo, el borde se oscurece y aparece un halo lavanda alrededor.
 * - Con `error`, el borde y el halo se vuelven rojos y el mensaje aparece debajo.
 * - Con `valido`, el borde se pone verde y aparece un check (por ejemplo, correo bien escrito).
 * - `marcador` es el texto gris de ejemplo que se ve mientras el campo está vacío.
 * - Cada vez que `sacudir` cambia, el campo se sacude un instante (intento fallido).
 * - `esContrasena` oculta el texto y agrega el ojo para mostrarlo.
 * - `tipoContenido` ayuda a los gestores de contraseñas a llenar el campo.
 */
/**
 * Fondo de los campos de texto. Blanco sobre la crema de las pantallas;
 * las hojas, que son blancas, lo cambian a crema para que el campo se distinga.
 */
val LocalFondoCampo = staticCompositionLocalOf { Colores.Papel }

@Composable
fun Campo(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    error: String? = null,
    ayuda: String? = null,
    marcador: String? = null,
    valido: Boolean = false,
    sacudir: Int = 0,
    esContrasena: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    opcionesTeclado: KeyboardOptions = KeyboardOptions.Default,
    accionesTeclado: KeyboardActions = KeyboardActions.Default,
    tipoContenido: ContentType? = null,
    habilitado: Boolean = true,
) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    val fondoCampo = LocalFondoCampo.current
    var mostrarTexto by rememberSaveable { mutableStateOf(false) }
    val conError = error != null

    val colorBorde by animateColorAsState(
        targetValue = when {
            conError -> Colores.Freno
            valido -> Colores.MentaTexto
            enfocado -> Colores.Tinta
            else -> Colores.Linea2
        },
        animationSpec = tween(durationMillis = 200),
        label = "borde del campo",
    )
    val colorHalo by animateColorAsState(
        targetValue = when {
            conError -> Colores.Freno.copy(alpha = 0.12f)
            enfocado -> Colores.Lavanda.copy(alpha = 0.45f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "halo del campo",
    )
    val colorIcono by animateColorAsState(
        targetValue = if (enfocado) Colores.Tinta else Colores.Tinta3,
        animationSpec = tween(durationMillis = 200),
        label = "ícono del campo",
    )

    // Sacudida corta cuando falla un intento
    val desplazamiento = remember { Animatable(0f) }
    LaunchedEffect(sacudir) {
        if (sacudir > 0) {
            desplazamiento.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 360
                    -8f at 45
                    8f at 105
                    -6f at 165
                    6f at 225
                    -3f at 285
                    3f at 330
                },
            )
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier.graphicsLayer {
            translationX = desplazamiento.value * density
            alpha = if (habilitado) 1f else 0.5f
        },
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.titleSmall,
            color = Colores.Tinta,
        )

        BasicTextField(
            value = valor,
            onValueChange = alCambiar,
            enabled = habilitado,
            singleLine = true,
            interactionSource = interaccion,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Colores.Tinta),
            cursorBrush = SolidColor(Colores.Tinta),
            visualTransformation = if (esContrasena && !mostrarTexto) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = opcionesTeclado.copy(keyboardType = if (esContrasena) KeyboardType.Password else tipoTeclado),
            keyboardActions = accionesTeclado,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = etiqueta
                    if (tipoContenido != null) contentType = tipoContenido
                    // Anuncia el error al lector de pantalla.
                    if (error != null) this.error(error)
                },
            decorationBox = { campo ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .drawBehind {
                            // Halo de 4 dp por fuera del campo
                            val halo = 4.dp.toPx()
                            drawRoundRect(
                                color = colorHalo,
                                topLeft = Offset(-halo, -halo),
                                size = Size(size.width + halo * 2, size.height + halo * 2),
                                cornerRadius = CornerRadius(20.dp.toPx()),
                            )
                        }
                        .background(fondoCampo, Formas.Campo)
                        .border(1.5.dp, colorBorde, Formas.Campo)
                        .padding(horizontal = 14.dp),
                ) {
                    if (icono != null) {
                        Icon(
                            imageVector = icono,
                            contentDescription = null,
                            tint = colorIcono,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (valor.isEmpty() && marcador != null) {
                            Text(
                                text = marcador,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Colores.Tinta3,
                                maxLines = 1,
                            )
                        }
                        campo()
                    }
                    if (valido && !conError && !esContrasena) {
                        Icon(
                            imageVector = Iconos.Hecho,
                            contentDescription = "Válido",
                            tint = Colores.MentaTexto,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (esContrasena) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = if (mostrarTexto) "Ocultar contraseña" else "Mostrar contraseña",
                                    onClick = { mostrarTexto = !mostrarTexto },
                                ),
                        ) {
                            Icon(
                                imageVector = if (mostrarTexto) Iconos.OjoTachado else Iconos.Ojo,
                                contentDescription = if (mostrarTexto) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = Colores.Tinta3,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            },
        )

        AnimatedVisibility(
            visible = conError || ayuda != null,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(160)) + fadeOut(tween(120)),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (conError) {
                    Icon(
                        imageVector = Iconos.Alerta,
                        contentDescription = null,
                        tint = Colores.FrenoTexto,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(15.dp),
                    )
                }
                Text(
                    text = error ?: ayuda.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (conError) Colores.FrenoTexto else Colores.Tinta3,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, widthDp = 380)
@Composable
private fun VistaPreviaCampos() {
    TemaRinFix {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Campo(valor = "laura@correo.com", alCambiar = {}, etiqueta = "Correo", icono = Iconos.Correo)
            Campo(valor = "rines2025", alCambiar = {}, etiqueta = "Contraseña", icono = Iconos.Candado, esContrasena = true)
            Campo(
                valor = "laura@",
                alCambiar = {},
                etiqueta = "Correo",
                icono = Iconos.Correo,
                error = "Escribe un correo válido, por ejemplo nombre@correo.com.",
            )
        }
    }
}
