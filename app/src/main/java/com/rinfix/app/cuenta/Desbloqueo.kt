package com.rinfix.app.cuenta

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.componentes.Avatar
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.CirculoHuella
import com.rinfix.app.componentes.Enlace
import com.rinfix.app.componentes.LineasTecnicas
import com.rinfix.app.componentes.Logo
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.datos.Rol
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada

/**
 * Desbloqueo al volver a abrir la app con la sesión abierta y la huella activada.
 *
 * - La ventana de huella se abre sola al llegar (eso lo decide quien usa la pantalla);
 *   el círculo grande la vuelve a abrir.
 * - Siempre hay salida: «Usar contraseña» o «No soy …» para cambiar de cuenta.
 * - Con `huellaInvalida` (se registró una huella nueva o se quitaron todas) ya no se
 *   ofrece la huella: solo entrar con contraseña.
 */
@Composable
fun PantallaDesbloqueo(
    nombre: String,
    rol: Rol,
    alPedirHuella: () -> Unit,
    alUsarContrasena: () -> Unit,
    alCambiarCuenta: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    huellaInvalida: Boolean = false,
) {
    val visible = recordarEntrada()
    val primerNombre = nombre.trim().substringBefore(' ')
    val textoCambiarCuenta = if (primerNombre.isEmpty()) "Cambiar de cuenta" else "No soy $primerNombre"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Colores.Crema)
                drawRect(
                    Brush.radialGradient(
                        0f to LuzCentral,
                        0.7f to Colores.Crema,
                        center = Offset(size.width / 2, size.height * 0.35f),
                        radius = maxOf(size.width * 0.9f, size.height * 0.55f),
                    ),
                )
            },
    ) {
        LineasTecnicas(
            tamano = 420.dp,
            color = Colores.Tinta.copy(alpha = 0.07f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 140.dp),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .safeDrawingPadding()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 34.dp),
        ) {
            Logo(tamano = 42.dp)

            Box(
                modifier = Modifier
                    .padding(top = 64.dp)
                    .aparecer(visible, orden = 0),
            ) {
                Avatar(nombre = nombre, tamano = 88.dp)
                // Punto menta: la sesión sigue abierta en este teléfono.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-4).dp)
                        .size(20.dp)
                        .border(3.dp, Colores.Crema, CircleShape)
                        .padding(3.dp)
                        .background(Colores.Menta, CircleShape),
                )
            }

            Text(
                text = if (primerNombre.isEmpty()) "Hola de nuevo" else "Hola de nuevo, $primerNombre",
                style = MaterialTheme.typography.headlineMedium,
                color = Colores.Tinta,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .aparecer(visible, orden = 1),
            )
            Text(
                text = when (rol) {
                    Rol.Cliente -> "Desbloquea para ver el avance de tu reparación."
                    Rol.Taller -> "Desbloquea para ver los trabajos del taller."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Colores.Tinta2,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(top = 8.dp)
                    .aparecer(visible, orden = 2),
            )

            // Empuja la huella hacia abajo, cerca del pulgar; en pantallas bajas deja al menos 40 dp.
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(40.dp))

            if (huellaInvalida) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.aparecer(visible, orden = 3),
                ) {
                    Aviso(
                        titulo = "Entra con tu contraseña.",
                        mensaje = "Cambiaron las huellas de este teléfono y, por seguridad, el desbloqueo " +
                            "se desactivó. Puedes activarlo de nuevo después de entrar.",
                        tipo = TipoAviso.Informacion,
                    )
                    Boton(
                        texto = "Usar contraseña",
                        alPulsar = alUsarContrasena,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Enlace(texto = textoCambiarCuenta, alPulsar = alCambiarCuenta, color = Colores.Tinta2)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.aparecer(visible, orden = 3),
                ) {
                    CirculoHuella(
                        tamano = 96.dp,
                        duracionPulso = 2200,
                        alPulsar = alPedirHuella,
                        descripcion = "Desbloquear con huella",
                    )
                    AnimatedContent(
                        targetState = error,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                        label = "mensaje de desbloqueo",
                        modifier = Modifier.padding(top = 16.dp),
                    ) { mensaje ->
                        Text(
                            text = mensaje ?: "Toca para usar tu huella",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (mensaje != null) Colores.FrenoTexto else Colores.Tinta2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 280.dp),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(top = 14.dp),
                    ) {
                        Enlace(texto = "Usar contraseña", alPulsar = alUsarContrasena, color = Colores.Tinta2)
                        Enlace(texto = textoCambiarCuenta, alPulsar = alCambiarCuenta, color = Colores.Tinta2)
                    }
                }
            }
        }
    }
}

/** Centro un poco más claro que la crema, como una luz sobre el banco de trabajo. */
private val LuzCentral = Color(0xFFFFFAF4)

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 800)
@Composable
private fun VistaPreviaDesbloqueo() {
    TemaRinFix {
        PantallaDesbloqueo(
            nombre = "Laura Méndez",
            rol = Rol.Cliente,
            alPedirHuella = {},
            alUsarContrasena = {},
            alCambiarCuenta = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 800)
@Composable
private fun VistaPreviaDesbloqueoConError() {
    TemaRinFix {
        PantallaDesbloqueo(
            nombre = "Carlos Rueda",
            rol = Rol.Taller,
            alPedirHuella = {},
            alUsarContrasena = {},
            alCambiarCuenta = {},
            error = "Demasiados intentos. Espera un momento e inténtalo de nuevo.",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 800)
@Composable
private fun VistaPreviaDesbloqueoHuellaInvalida() {
    TemaRinFix {
        PantallaDesbloqueo(
            nombre = "Laura Méndez",
            rol = Rol.Cliente,
            alPedirHuella = {},
            alUsarContrasena = {},
            alCambiarCuenta = {},
            huellaInvalida = true,
        )
    }
}
