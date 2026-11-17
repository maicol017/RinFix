package com.rinfix.app.datos

import kotlinx.coroutines.flow.Flow

/** Fotos y datos del rin que la persona envía a analizar. */
data class SolicitudAnalisis(
    /** Rutas de las fotos en el teléfono (de 1 a 3). */
    val fotos: List<String>,
    val material: MaterialRin,
    val pulgadas: Int?,
    /** Vehículo del garaje, si lo eligió: «Mazda 3 2021». */
    val vehiculo: String?,
)

/**
 * Análisis de fotos de rines.
 * Con el backend, el análisis lo hace un servidor en Vercel que llama a Claude;
 * la clave de la IA nunca está en el teléfono.
 */
interface ServicioAnalisis {

    /** Diagnósticos anteriores de la persona, el más reciente primero. */
    fun historial(usuario: Usuario): Flow<List<Diagnostico>>

    /**
     * Recibe las fotos y empieza el análisis. Devuelve el id del análisis en curso,
     * para seguir su avance en la pantalla «Analizando».
     */
    suspend fun iniciar(usuario: Usuario, solicitud: SolicitudAnalisis): String
}
