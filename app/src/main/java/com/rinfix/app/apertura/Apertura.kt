package com.rinfix.app.apertura

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Letras
import com.rinfix.app.diseno.TemaRinFix
import kotlinx.coroutines.delay

/*
 * Animación de apertura de RinFix (unos 1,9 segundos). Línea de tiempo en milisegundos:
 *
 *    0 –  500  Un trazo fino dibuja la circunferencia guía.
 *  150 –  780  Los cinco rayos aparecen uno a uno, girando un poco hasta su lugar.
 *  380 –  720  Aparece el aro.
 *  520 –  860  Aparece el disco oscuro detrás; rayos y aro pasan a color crema.
 *  680 –  980  El centro menta aparece con un pequeño rebote.
 *  780 – 1400  El rin da una vuelta completa y se asienta con un rebote corto.
 *  900 – 1520  Una luz menta recorre el borde.
 * 1050 – 1470  Entra el nombre RinFix; a los 1200 entra la frase.
 * 1500 – 1920  El rin se encoge y sube mientras aparece la app.
 *
 * Se dibuja con Canvas a partir de un solo reloj, así cada cuadro solo se repinta
 * sin recomponer la pantalla. La geometría es la del logo: una cuadrícula de 100 × 100.
 */

private const val DURACION = 1920f
private const val INICIO_SALIDA = 1500f

/** Curva del prototipo: arranca rápido y frena suave. */
private val CurvaSuave = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/** Curva con rebote: se pasa un poco y vuelve. */
private val CurvaRebote = CubicBezierEasing(0.34f, 1.5f, 0.64f, 1f)

/** Curva del giro. */
private val CurvaGiro = CubicBezierEasing(0.3f, 0.7f, 0.3f, 1f)

/** Avance de 0 a 1 de una parte de la animación que empieza en `inicio` y dura `duracion`. */
private fun avance(tiempo: Float, inicio: Float, duracion: Float, curva: Easing = LinearEasing): Float =
    curva.transform(((tiempo - inicio) / duracion).coerceIn(0f, 1f))

/**
 * Pantalla de apertura. Se muestra encima de la app y llama a `alTerminar` al acabar.
 * Tocar en cualquier parte la adelanta hasta la salida.
 */
@Composable
fun Apertura(alTerminar: () -> Unit, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val sinAnimaciones = remember {
        Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val tiempo = remember { Animatable(0f) }
    var saltar by remember { mutableStateOf(false) }

    LaunchedEffect(saltar) {
        if (sinAnimaciones) {
            // Con «Quitar animaciones» se muestra el logo armado un momento y se pasa a la app.
            tiempo.snapTo(INICIO_SALIDA - 50f)
            delay(500)
            alTerminar()
            return@LaunchedEffect
        }
        if (saltar) tiempo.snapTo(maxOf(tiempo.value, INICIO_SALIDA))
        tiempo.animateTo(
            targetValue = DURACION,
            animationSpec = tween(
                durationMillis = (DURACION - tiempo.value).toInt().coerceAtLeast(1),
                easing = LinearEasing,
            ),
        )
        alTerminar()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - avance(tiempo.value, 1600f, 320f) }
            .background(Colores.Crema)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Saltar la animación",
                onClick = { saltar = true },
            )
            .semantics { contentDescription = "RinFix. Diagnóstico, reparación y seguimiento de rines" },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.offset(y = (-20).dp),
        ) {
            Canvas(
                modifier = Modifier
                    .size(210.dp)
                    .graphicsLayer {
                        val salida = avance(tiempo.value, INICIO_SALIDA, 420f, CurvaSuave)
                        translationY = -salida * 260.dp.toPx()
                        scaleX = 1f - 0.8f * salida
                        scaleY = 1f - 0.8f * salida
                    },
            ) {
                dibujarRin(tiempo.value)
            }

            // Nombre: sube desde abajo dentro de una ventana recortada.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .height(56.dp)
                    .clipToBounds(),
            ) {
                Text(
                    text = "RinFix",
                    style = Letras.Marca,
                    color = Colores.Tinta,
                    modifier = Modifier.graphicsLayer {
                        val entrada = avance(tiempo.value, 1050f, 420f, CurvaSuave)
                        translationY = (1f - entrada) * 56.dp.toPx()
                        alpha = entrada * (1f - avance(tiempo.value, INICIO_SALIDA, 300f))
                    },
                )
            }

            Text(
                text = "Diagnóstico, reparación y seguimiento de rines",
                style = MaterialTheme.typography.bodyMedium,
                color = Colores.Tinta2,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .graphicsLayer {
                        val entrada = avance(tiempo.value, 1200f, 400f, CurvaSuave)
                        translationY = (1f - entrada) * 12.dp.toPx()
                        alpha = entrada * (1f - avance(tiempo.value, INICIO_SALIDA, 300f))
                    },
            )
        }

        Text(
            text = "Toca para saltar",
            style = MaterialTheme.typography.labelSmall,
            color = Colores.Tinta3,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .graphicsLayer {
                    alpha = 0.8f * avance(tiempo.value, 600f, 300f) * (1f - avance(tiempo.value, INICIO_SALIDA, 200f))
                },
        )
    }
}

/** Dibuja el rin en el instante `t` (milisegundos). */
private fun DrawScope.dibujarRin(t: Float) {
    val u = size.minDimension / 100f // una unidad de la cuadrícula del logo
    val c = center

    // Sombra en el piso
    val sombra = avance(t, 560f, 400f)
    if (sombra > 0f) {
        drawOval(
            color = Colores.Tinta.copy(alpha = 0.10f * sombra),
            topLeft = Offset(c.x - 34f * u, c.y + 50f * u),
            size = Size(68f * u, 9f * u),
        )
    }

    // Circunferencia guía
    val guia = avance(t, 0f, 500f, CurvaSuave)
    val guiaOpacidad = 0.55f * (guia * 5f).coerceAtMost(1f) * (1f - avance(t, 760f, 300f))
    if (guiaOpacidad > 0f) {
        drawArc(
            color = Colores.Tinta.copy(alpha = guiaOpacidad),
            startAngle = -90f,
            sweepAngle = 360f * guia,
            useCenter = false,
            topLeft = Offset(c.x - 48f * u, c.y - 48f * u),
            size = Size(96f * u, 96f * u),
            style = Stroke(width = 1.2.dp.toPx()),
        )
    }

    // Giro: una vuelta que se pasa 12 grados y vuelve
    val g = avance(t, 780f, 620f)
    val giro = if (g < 0.72f) {
        372f * CurvaGiro.transform(g / 0.72f)
    } else {
        372f - 12f * CurvaSuave.transform((g - 0.72f) / 0.28f)
    }

    // Al aparecer el disco oscuro, rayos y aro pasan de tinta a crema
    val disco = avance(t, 520f, 340f, CurvaSuave)
    val colorPiezas = lerp(Colores.Tinta, Colores.Crema, disco)

    rotate(degrees = giro, pivot = c) {
        if (disco > 0f) {
            scale(scale = 1.14f - 0.14f * disco, pivot = c) {
                drawCircle(color = Colores.Tinta.copy(alpha = disco), radius = 48f * u, center = c)
            }
        }

        val aro = avance(t, 380f, 340f, CurvaSuave)
        if (aro > 0f) {
            scale(scale = 0.84f + 0.16f * aro, pivot = c) {
                drawCircle(
                    color = colorPiezas.copy(alpha = aro),
                    radius = 36f * u,
                    center = c,
                    style = Stroke(width = 6f * u),
                )
            }
        }

        for (i in 0 until 5) {
            val rayo = avance(t, 150f + i * 70f, 300f, CurvaSuave)
            if (rayo <= 0f) continue
            rotate(degrees = i * 72f - 26f * (1f - rayo), pivot = c) {
                scale(scale = 0.5f + 0.5f * rayo, pivot = c) {
                    drawLine(
                        color = colorPiezas.copy(alpha = rayo),
                        start = Offset(c.x, c.y - 13f * u),
                        end = Offset(c.x, c.y - 31f * u),
                        strokeWidth = 7f * u,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        // Luz menta que recorre el aro
        val b = avance(t, 900f, 620f)
        val brillo = when {
            b <= 0f || b >= 1f -> 0f
            b < 0.25f -> b / 0.25f
            b < 0.78f -> 1f
            else -> 1f - (b - 0.78f) / 0.22f
        }
        if (brillo > 0f) {
            rotate(degrees = -60f + 360f * b, pivot = c) {
                val esquina = Offset(c.x - 36f * u, c.y - 36f * u)
                val lado = Size(72f * u, 72f * u)
                drawArc(
                    color = Colores.Menta.copy(alpha = 0.35f * brillo),
                    startAngle = -125f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = esquina,
                    size = lado,
                    style = Stroke(width = 14f * u, cap = StrokeCap.Round),
                )
                drawArc(
                    color = Colores.Menta.copy(alpha = brillo),
                    startAngle = -125f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = esquina,
                    size = lado,
                    style = Stroke(width = 6f * u, cap = StrokeCap.Round),
                )
            }
        }

        // Centro menta con rebote
        val centro = avance(t, 680f, 300f)
        if (centro > 0f) {
            scale(scale = 0.2f + 0.8f * CurvaRebote.transform(centro), pivot = c) {
                drawCircle(
                    color = Colores.Menta.copy(alpha = (centro * 3f).coerceAtMost(1f)),
                    radius = 9f * u,
                    center = c,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, widthDp = 360, heightDp = 640)
@Composable
private fun VistaPreviaApertura() {
    TemaRinFix {
        Apertura(alTerminar = {})
    }
}
