package com.rinfix.app.datos

import java.time.LocalDateTime

/** Daños visibles que el análisis puede señalar en una foto. */
enum class TipoDano(val nombre: String) {
    Rayon("Rayón"),
    Raspadura("Raspadura de andén"),
    Deformacion("Deformación"),
    Fisura("Posible fisura"),
    Corrosion("Corrosión"),
    Pintura("Pintura desgastada"),
    Otro("Otro daño"),
}

enum class Gravedad(val nombre: String) {
    Leve("Leve"),
    Moderada("Moderada"),
    Alta("Alta"),
}

/** Zona del daño en la foto, en proporciones de 0 a 1 (izquierda, arriba, ancho, alto). */
data class Caja(val izquierda: Float, val arriba: Float, val ancho: Float, val alto: Float)

data class Dano(
    val tipo: TipoDano,
    /** Frase corta para mostrar, por ejemplo «Rayón en el borde exterior». */
    val descripcion: String,
    val gravedad: Gravedad,
    /** Qué tan seguro está el análisis, de 0 a 1. */
    val confianza: Float,
    val caja: Caja? = null,
)

/** Una línea de la cotización, con rango porque la foto no muestra todo. */
data class LineaCotizacion(val servicio: String, val minimo: Long, val maximo: Long)

/**
 * Diagnóstico preliminar de una foto. No reemplaza la inspección del taller:
 * por eso los precios y tiempos son rangos.
 */
data class Diagnostico(
    val id: String,
    val fecha: LocalDateTime,
    /** Vehículo del garaje, si la persona lo eligió: «Mazda 3 2021». */
    val vehiculo: String?,
    val material: MaterialRin,
    val pulgadas: Int?,
    val danos: List<Dano>,
    val cotizacion: List<LineaCotizacion>,
    val diasMinimos: Int,
    val diasMaximos: Int,
    val recomendaciones: List<String>,
    /** `true` si algo puede afectar la seguridad (por ejemplo una fisura): no rodar sin revisión. */
    val requiereInspeccion: Boolean,
    /** `true` mientras los resultados vengan de datos de ejemplo y no del análisis real. */
    val deEjemplo: Boolean,
) {
    val precioMinimo: Long get() = cotizacion.sumOf { it.minimo }
    val precioMaximo: Long get() = cotizacion.sumOf { it.maximo }

    /** «rayón en el borde exterior» o «rayón en el borde exterior y 1 daño más». */
    val resumen: String
        get() {
            val primero = danos.firstOrNull()?.descripcion?.replaceFirstChar { it.lowercase() }
                ?: return "sin daños visibles"
            val otros = danos.size - 1
            return when (otros) {
                0 -> primero
                1 -> "$primero y 1 daño más"
                else -> "$primero y $otros daños más"
            }
        }
}
