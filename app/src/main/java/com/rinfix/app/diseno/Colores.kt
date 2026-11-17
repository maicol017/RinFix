package com.rinfix.app.diseno

import androidx.compose.ui.graphics.Color

/**
 * Paleta de RinFix, identidad «Crema de taller».
 *
 * Cada color pastel tiene un significado fijo y un tono oscuro para el texto que va encima,
 * así el contraste siempre es legible:
 * - Menta: listo o completado.
 * - Mantequilla: en proceso.
 * - Lavanda: inteligencia artificial o información.
 * - Freno: error o atención.
 */
object Colores {

    // Fondos y superficies
    val Crema = Color(0xFFFFF3E7)        // fondo general de la app
    val Crema2 = Color(0xFFFAEBDC)       // fondo de secciones y bloques
    val Arena = Color(0xFFF2E0CC)        // separadores y bordes cálidos
    val Papel = Color(0xFFFFFFFF)        // tarjetas
    val Niebla = Color(0xFFF7F7F7)       // campos de texto y fondos neutros

    // Texto, bordes y acciones principales
    val Tinta = Color(0xFF030302)        // texto principal y botones principales
    val Tinta2 = Color(0xFF4F4A44)       // texto secundario
    val Tinta3 = Color(0xFF6F6862)       // texto de apoyo
    val Linea = Color(0x14030302)        // borde suave (8 % de tinta)
    val Linea2 = Color(0x26030302)       // borde marcado (15 % de tinta)

    // Menta: listo o completado
    val Menta = Color(0xFF9BD8A9)
    val MentaTexto = Color(0xFF1F5130)
    val MentaSuave = Color(0xFFE3F4E7)

    // Mantequilla: en proceso
    val Mantequilla = Color(0xFFFDE99B)
    val MantequillaTexto = Color(0xFF5C4800)
    val MantequillaSuave = Color(0xFFFEF6D8)

    // Lavanda: inteligencia artificial o información
    val Lavanda = Color(0xFFB8CAF5)
    val LavandaTexto = Color(0xFF233A75)
    val LavandaSuave = Color(0xFFE8EEFD)

    // Freno: error o atención
    val Freno = Color(0xFFC8341A)
    val FrenoTexto = Color(0xFF8A2410)
    val FrenoSuave = Color(0xFFFBE0D8)
}
