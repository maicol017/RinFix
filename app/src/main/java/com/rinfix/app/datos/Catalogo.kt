package com.rinfix.app.datos

import kotlin.math.roundToLong

/** Servicio del taller con su rango base para un rin de 17" de aleación. */
data class ServicioTaller(
    val nombre: String,
    val minimo: Long,
    val maximo: Long,
    val diasMinimos: Int,
    val diasMaximos: Int,
)

/**
 * Catálogo de servicios y reglas de precio. La IA solo dice qué daños ve:
 * el precio sale siempre de aquí, nunca de la IA.
 *
 * Hoy vive en el teléfono con tarifas de ejemplo. Con el backend, cada taller configura
 * el suyo en Supabase y el servidor aplica estas mismas reglas.
 */
object Catalogo {

    const val Taller = "Taller RinFix Cabecera"

    val servicios: Map<TipoDano, ServicioTaller> = mapOf(
        TipoDano.Rayon to ServicioTaller("Reparación de rayones y pintura", 120_000, 170_000, 1, 2),
        TipoDano.Raspadura to ServicioTaller("Reparación de raspaduras de andén", 90_000, 140_000, 1, 2),
        TipoDano.Deformacion to ServicioTaller("Enderezado del borde", 60_000, 90_000, 1, 1),
        TipoDano.Fisura to ServicioTaller("Soldadura de fisura con revisión", 180_000, 320_000, 2, 3),
        TipoDano.Corrosion to ServicioTaller("Limpieza y pintura anticorrosiva", 130_000, 200_000, 2, 3),
        TipoDano.Pintura to ServicioTaller("Pintura completa del rin", 150_000, 230_000, 2, 3),
        TipoDano.Otro to ServicioTaller("Revisión en el taller", 40_000, 60_000, 1, 1),
    )

    /** Servicios que el taller incluye sin costo con cualquier reparación. */
    val incluidos: List<String> = listOf("Balanceo")

    /** Rines más grandes llevan más material y tiempo. */
    fun factorTamano(pulgadas: Int?): Double = when {
        pulgadas == null -> 1.0
        pulgadas <= 14 -> 0.85
        pulgadas <= 16 -> 0.95
        pulgadas == 17 -> 1.0
        pulgadas == 18 -> 1.12
        else -> 1.25
    }

    /** El acero se repara más rápido y con materiales más baratos que la aleación. */
    fun factorMaterial(material: MaterialRin): Double = when (material) {
        MaterialRin.Acero -> 0.8
        MaterialRin.Aleacion, MaterialRin.Desconocido -> 1.0
    }

    /** Una línea por cada tipo de daño, más lo incluido. Precios redondeados a $5.000. */
    fun cotizar(danos: List<Dano>, material: MaterialRin, pulgadas: Int?): List<LineaCotizacion> {
        val factor = factorTamano(pulgadas) * factorMaterial(material)
        val datosRin = descripcionRin(material, pulgadas)
        val lineas = danos.distinctBy { it.tipo }.mapNotNull { dano ->
            val servicio = servicios[dano.tipo] ?: return@mapNotNull null
            LineaCotizacion(
                servicio = servicio.nombre,
                minimo = redondear(servicio.minimo * factor),
                maximo = redondear(servicio.maximo * factor),
                detalle = if (dano.confirmado) datosRin else "Solo si el taller lo confirma",
                condicional = !dano.confirmado,
            )
        }
        return lineas + incluidos.map { LineaCotizacion(servicio = it, minimo = 0, maximo = 0) }
    }

    /** Días hábiles: el servicio más largo, más un día por cada servicio adicional. */
    fun dias(danos: List<Dano>): IntRange {
        val usados = danos.distinctBy { it.tipo }.mapNotNull { servicios[it.tipo] }
        if (usados.isEmpty()) return 1..1
        val minimo = usados.maxOf { it.diasMinimos }
        val maximo = usados.maxOf { it.diasMaximos } + (usados.size - 1)
        return minimo..maximo
    }

    private fun redondear(valor: Double): Long = (valor / 5_000).roundToLong() * 5_000

    private fun descripcionRin(material: MaterialRin, pulgadas: Int?): String {
        val partes = listOfNotNull(
            pulgadas?.let { "Rin $it\"" },
            material.takeIf { it != MaterialRin.Desconocido }?.nombre?.lowercase(),
        )
        return if (partes.isEmpty()) "Tamaño y material por confirmar" else partes.joinToString(", ")
    }
}
