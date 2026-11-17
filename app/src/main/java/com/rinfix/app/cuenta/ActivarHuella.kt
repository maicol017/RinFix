package com.rinfix.app.cuenta

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.CirculoHuella
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.componentes.TipoEtiqueta
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada

/**
 * Ofrece el desbloqueo con huella justo después del primer acceso.
 *
 * - «Activar desbloqueo» abre la ventana de huella del sistema para confirmar.
 * - «Ahora no» sigue a la app sin insistir; la opción queda en Perfil.
 * - `error` aparece si la confirmación falla, para reintentar o dejarlo para después.
 */
@Composable
fun PantallaActivarHuella(
    alActivar: () -> Unit,
    alOmitir: () -> Unit,
    modifier: Modifier = Modifier,
    activando: Boolean = false,
    error: String? = null,
) {
    val visible = recordarEntrada()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 24.dp),
    ) {
        ArteHuella(visible = visible, modifier = Modifier.aparecer(visible, orden = 0))

        Text(
            text = "Entra más rápido la próxima vez",
            style = MaterialTheme.typography.headlineLarge,
            color = Colores.Tinta,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 14.dp)
                .aparecer(visible, orden = 1),
        )
        Text(
            text = "Desbloquea RinFix con tu huella, o con tu rostro si tu teléfono lo permite. " +
                "Android verifica que eres tú; RinFix nunca recibe ni guarda tus datos biométricos.",
            style = MaterialTheme.typography.bodyLarge,
            color = Colores.Tinta2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 10.dp)
                .aparecer(visible, orden = 2),
        )

        AnimatedVisibility(visible = error != null) {
            Aviso(
                titulo = "No se activó.",
                mensaje = error.orEmpty(),
                tipo = TipoAviso.Error,
                modifier = Modifier.padding(top = 18.dp),
            )
        }

        Tarjeta(
            relleno = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            espacio = 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .aparecer(visible, orden = 3),
        ) {
            FilaGarantia(icono = Iconos.Escudo, texto = "Usa una llave segura que nunca sale del teléfono")
            HorizontalDivider(thickness = 1.dp, color = Colores.Linea)
            FilaGarantia(icono = Iconos.Llave, texto = "Siempre podrás entrar con tu contraseña")
            HorizontalDivider(thickness = 1.dp, color = Colores.Linea)
            FilaGarantia(icono = Iconos.Ajustes, texto = "Lo activas o desactivas en Perfil")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.aparecer(visible, orden = 4),
        ) {
            Boton(
                texto = "Activar desbloqueo",
                alPulsar = alActivar,
                icono = Iconos.Huella,
                ocupado = activando,
                modifier = Modifier.fillMaxWidth(),
            )
            Boton(
                texto = "Ahora no",
                alPulsar = alOmitir,
                estilo = EstiloBoton.Transparente,
                habilitado = !activando,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Anillos alrededor de la huella, con las dos formas de desbloqueo flotando a los lados. */
@Composable
private fun ArteHuella(visible: Boolean, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(236.dp)
            .clearAndSetSemantics { },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radio = size.minDimension / 2
            val trazoFino = 1.5.dp.toPx()
            val trazoMenta = 2.dp.toPx()

            // Anillo exterior punteado
            drawCircle(
                color = Colores.Tinta.copy(alpha = 0.10f),
                radius = radio - trazoFino / 2,
                style = Stroke(
                    width = trazoFino,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                ),
            )
            // Anillo menta
            drawCircle(
                color = Colores.Menta.copy(alpha = 0.9f),
                radius = radio - 28.dp.toPx() - trazoMenta / 2,
                style = Stroke(width = trazoMenta),
            )
            // Disco menta suave
            drawCircle(color = Colores.MentaSuave, radius = radio - 52.dp.toPx())
        }

        CirculoHuella(tamano = 96.dp)

        Etiqueta(
            texto = "Huella",
            tipo = TipoEtiqueta.Blanca,
            icono = Iconos.Huella,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-8).dp, y = 52.dp)
                .aparecer(visible, orden = 3),
        )
        Etiqueta(
            texto = "Rostro",
            tipo = TipoEtiqueta.Blanca,
            icono = Iconos.Rostro,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 6.dp, y = (-46).dp)
                .aparecer(visible, orden = 5),
        )
    }
}

@Composable
private fun FilaGarantia(icono: ImageVector, texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = Colores.MentaTexto,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.Tinta,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 860)
@Composable
private fun VistaPreviaActivarHuella() {
    TemaRinFix {
        PantallaActivarHuella(alActivar = {}, alOmitir = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 960)
@Composable
private fun VistaPreviaActivarHuellaConError() {
    TemaRinFix {
        PantallaActivarHuella(
            alActivar = {},
            alOmitir = {},
            error = "Demasiados intentos. Espera un momento o hazlo luego desde Perfil.",
        )
    }
}
