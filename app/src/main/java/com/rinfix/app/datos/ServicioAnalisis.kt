package com.rinfix.app.datos

import kotlinx.coroutines.flow.Flow

/**
 * Análisis de fotos de rines.
 * Con el backend, el análisis lo hace un servidor en Vercel que llama a Claude;
 * la clave de la IA nunca está en el teléfono.
 */
interface ServicioAnalisis {

    /** Diagnósticos anteriores de la persona, el más reciente primero. */
    fun historial(usuario: Usuario): Flow<List<Diagnostico>>
}
