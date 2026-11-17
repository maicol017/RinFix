package com.rinfix.app.datos

/** Material del rin. «No sé» es una respuesta válida: el análisis lo tiene en cuenta. */
enum class MaterialRin(val nombre: String) {
    Aleacion("Aleación"),
    Acero("Acero"),
    Desconocido("No sé"),
}

/** Vehículo guardado en «Mi garaje». */
data class Vehiculo(
    val id: String,
    /** Marca y modelo, por ejemplo «Mazda 3». */
    val nombre: String,
    val anio: Int? = null,
    val pulgadasRin: Int? = null,
    val materialRin: MaterialRin = MaterialRin.Desconocido,
) {
    /** Mazda 3 2021 */
    val titulo: String
        get() = listOfNotNull(nombre, anio?.toString()).joinToString(" ")

    /** Rin 17", aleación */
    val descripcionRin: String
        get() {
            val material = materialRin.takeIf { it != MaterialRin.Desconocido }?.nombre?.lowercase()
            return when {
                pulgadasRin != null && material != null -> "Rin $pulgadasRin\", $material"
                pulgadasRin != null -> "Rin $pulgadasRin\""
                material != null -> "Rin de $material"
                else -> "Rin sin datos"
            }
        }
}
