@file:OptIn(ExperimentalTextApi::class)

package com.rinfix.app.diseno

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rinfix.app.R

/**
 * Tipografía de RinFix: Archivo, una sola fuente variable (res/font/archivo.ttf)
 * que permite cambiar el grosor (100 a 900) y el ancho (62 % a 125 %).
 *
 * - Archivo expandida (125 %): títulos y cifras. Recuerda la rotulación de placas
 *   y del automovilismo, y es el rasgo más reconocible de la marca.
 * - Archivo semiexpandida (112 %): subtítulos de tarjetas y secciones.
 * - Archivo normal (100 %): textos, botones y formularios.
 *
 * Licencia de la fuente: SIL Open Font License (assets/licencias/Archivo-OFL.txt).
 */

/** Crea una variante de Archivo con el grosor y el ancho indicados. */
private fun archivo(grosor: Int, ancho: Float) = Font(
    resId = R.font.archivo,
    weight = FontWeight(grosor),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(grosor),
        FontVariation.width(ancho),
    ),
)

object Letras {

    /** Textos, botones y formularios. */
    val Normal = FontFamily(
        archivo(400, 100f),
        archivo(500, 100f),
        archivo(600, 100f),
        archivo(700, 100f),
    )

    /** Subtítulos de tarjetas y secciones. */
    val Semiexpandida = FontFamily(
        archivo(600, 112f),
        archivo(700, 112f),
    )

    /** Títulos y cifras. */
    val Expandida = FontFamily(
        archivo(600, 125f),
        archivo(700, 125f),
        archivo(800, 125f),
    )

    /**
     * Precios, fechas y contadores. Los números tienen el mismo ancho,
     * así no «bailan» cuando una cifra se anima.
     */
    val Cifra = TextStyle(
        fontFamily = Expandida,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em,
        fontFeatureSettings = "tnum",
    )

    /** Nombre de la marca en la apertura y en el encabezado. */
    val Marca = TextStyle(
        fontFamily = Expandida,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.02).em,
    )
}

/** Escala de textos que usa el tema de la app. */
val TipografiaRinFix = Typography(
    // Títulos grandes: expandida
    headlineLarge = TextStyle(
        fontFamily = Letras.Expandida,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.018).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = Letras.Expandida,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.018).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = Letras.Expandida,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.015).em,
    ),

    // Subtítulos: semiexpandida y normal
    titleLarge = TextStyle(
        fontFamily = Letras.Semiexpandida,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.005).em,
    ),
    titleMedium = TextStyle(
        fontFamily = Letras.Normal,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Letras.Normal,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),

    // Textos
    bodyLarge = TextStyle(
        fontFamily = Letras.Normal,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Letras.Normal,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Letras.Normal,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),

    // Botones, etiquetas y textos pequeños
    labelLarge = TextStyle(
        fontFamily = Letras.Normal,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Letras.Normal,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Letras.Normal,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
