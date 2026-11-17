package com.rinfix.app.cuenta

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.Enlace
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.LineasTecnicas
import com.rinfix.app.componentes.Logo
import com.rinfix.app.componentes.RinIlustrado
import com.rinfix.app.componentes.TipoEtiqueta
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.Letras
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import com.rinfix.app.diseno.sombra

/**
 * Primera pantalla para quien no ha iniciado sesión.
 *
 * Arriba, una tarjeta lavanda con un rin y tres etiquetas que muestran, como ejemplo,
 * lo que entrega la app: un hallazgo, una cotización estimada y una fecha de entrega.
 * Abajo, las formas de entrar: Google, correo o código de taller.
 */
@Composable
fun PantallaBienvenida(
    alContinuarConGoogle: () -> Unit,
    alCrearCuenta: () -> Unit,
    alIniciarSesion: () -> Unit,
    alEntrarConCodigo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = recordarEntrada()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Logo(tamano = 42.dp)
        }

        TarjetaEjemplo(visible = visible)

        Text(
            text = "Tu rin, revisado antes de llegar al taller",
            style = MaterialTheme.typography.headlineLarge,
            color = Colores.Tinta,
            modifier = Modifier
                .padding(top = 22.dp)
                .aparecer(visible, orden = 2),
        )
        Text(
            text = "Toma una foto, recibe un diagnóstico preliminar y sigue la reparación paso a paso.",
            style = MaterialTheme.typography.bodyLarge,
            color = Colores.Tinta2,
            modifier = Modifier.aparecer(visible, orden = 3),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(top = 4.dp)
                .aparecer(visible, orden = 4),
        ) {
            // En la fase 1 este botón se reemplaza por el botón oficial de Google,
            // con su logo y estilo, como exigen sus normas de marca.
            Boton(
                texto = "Continuar con Google",
                alPulsar = alContinuarConGoogle,
                estilo = EstiloBoton.Blanco,
                icono = Iconos.UsuarioCirculo,
                modifier = Modifier.fillMaxWidth(),
            )
            Boton(
                texto = "Crear cuenta con correo",
                alPulsar = alCrearCuenta,
                icono = Iconos.Correo,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aparecer(visible, orden = 5),
        ) {
            Text(
                text = "¿Ya tienes cuenta?",
                style = MaterialTheme.typography.bodyMedium,
                color = Colores.Tinta2,
            )
            Enlace(texto = "Inicia sesión", alPulsar = alIniciarSesion)
        }

        Enlace(
            texto = "¿Trabajas en un taller? Entra con tu código",
            alPulsar = alEntrarConCodigo,
            estilo = MaterialTheme.typography.labelMedium,
            color = Colores.Tinta2,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .aparecer(visible, orden = 6),
        )
    }
}

/** Fondo de la tarjeta: lavanda con un degradado suave en diagonal. */
private val FondoLavanda = Brush.linearGradient(
    0.0f to Color(0xFFC6D4F7),
    0.6f to Colores.Lavanda,
    1.0f to Color(0xFFA9BDEF),
    start = Offset.Zero,
    end = Offset.Infinite,
)

private val FormaTarjeta = RoundedCornerShape(32.dp)

private val SombraRin = Shadow(
    radius = 22.dp,
    spread = (-10).dp,
    color = Colores.Tinta.copy(alpha = 0.35f),
    offset = DpOffset(x = 0.dp, y = 22.dp),
)

/** Tarjeta lavanda con el rin de ejemplo y lo que la app entrega. */
@Composable
private fun TarjetaEjemplo(visible: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(318.dp),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(FormaTarjeta)
                .background(FondoLavanda)
                .border(1.dp, Colores.LavandaTexto.copy(alpha = 0.08f), FormaTarjeta),
        ) {
            LineasTecnicas(
                tamano = 330.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 70.dp, y = (-20).dp)
                    .alpha(0.9f),
            )
        }

        // El rin sobresale un poco por abajo de la tarjeta.
        RinIlustrado(
            tamano = 250.dp,
            conDano = true,
            inclinacion = 12f,
            descripcion = "Ejemplo: rin de aleación con un rayón en el borde",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 22.dp)
                .aparecer(visible, orden = 1)
                .dropShadow(CircleShape, SombraRin),
        )

        Etiqueta(
            texto = "Posible rayón en el borde",
            tipo = TipoEtiqueta.Blanca,
            icono = Iconos.Ia,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 18.dp)
                .aparecer(visible, orden = 4),
        )
        Etiqueta(
            texto = "Listo el jueves",
            tipo = TipoEtiqueta.Listo,
            icono = Iconos.Hecho,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 58.dp)
                .aparecer(visible, orden = 8),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 22.dp)
                .aparecer(visible, orden = 6)
                .sombra(Sombra.Media, Formas.Campo)
                .background(Colores.Papel, Formas.Campo)
                .padding(horizontal = 13.dp, vertical = 9.dp),
        ) {
            Text(
                text = "Cotización estimada",
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Tinta3,
            )
            Text(
                text = "$180.000 – $260.000",
                style = Letras.Cifra.copy(fontSize = 16.sp, lineHeight = 20.sp),
                color = Colores.Tinta,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, widthDp = 380, heightDp = 860)
@Composable
private fun VistaPreviaBienvenida() {
    TemaRinFix {
        PantallaBienvenida(
            alContinuarConGoogle = {},
            alCrearCuenta = {},
            alIniciarSesion = {},
            alEntrarConCodigo = {},
        )
    }
}
