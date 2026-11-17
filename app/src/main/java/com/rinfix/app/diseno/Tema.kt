package com.rinfix.app.diseno

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Traduce la paleta de RinFix a los nombres que usan los componentes de Material.
 *
 * - Primario (tinta): botones principales y elementos activos.
 * - Secundario (lavanda): todo lo relacionado con la IA.
 * - Terciario (mantequilla): estados en proceso.
 * - Error (freno): errores y avisos de atención.
 */
private val EsquemaRinFix = lightColorScheme(
    primary = Colores.Tinta,
    onPrimary = Colores.Crema,
    primaryContainer = Colores.Menta,
    onPrimaryContainer = Colores.MentaTexto,

    secondary = Colores.Lavanda,
    onSecondary = Colores.LavandaTexto,
    secondaryContainer = Colores.LavandaSuave,
    onSecondaryContainer = Colores.LavandaTexto,

    tertiary = Colores.Mantequilla,
    onTertiary = Colores.MantequillaTexto,
    tertiaryContainer = Colores.MantequillaSuave,
    onTertiaryContainer = Colores.MantequillaTexto,

    background = Colores.Crema,
    onBackground = Colores.Tinta,
    surface = Colores.Papel,
    onSurface = Colores.Tinta,
    surfaceVariant = Colores.Crema2,
    onSurfaceVariant = Colores.Tinta2,
    surfaceContainerLowest = Colores.Papel,
    surfaceContainerLow = Colores.Papel,
    surfaceContainer = Colores.Crema2,
    surfaceContainerHigh = Colores.Crema2,
    surfaceContainerHighest = Colores.Arena,

    outline = Colores.Linea2,
    outlineVariant = Colores.Linea,

    error = Colores.Freno,
    onError = Colores.Papel,
    errorContainer = Colores.FrenoSuave,
    onErrorContainer = Colores.FrenoTexto,

    inverseSurface = Colores.Tinta,
    inverseOnSurface = Colores.Crema,
    scrim = Colores.Tinta,
)

/**
 * Tema de RinFix. Envuelve toda la app.
 *
 * La identidad «Crema de taller» es clara, así que la app no cambia a modo oscuro
 * aunque el teléfono lo tenga activado. Un modo oscuro propio se puede diseñar más adelante.
 */
@Composable
fun TemaRinFix(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaRinFix,
        typography = TipografiaRinFix,
        shapes = FormasRinFix,
        content = content,
    )
}
