package com.rinfix.app.datos

import kotlinx.coroutines.flow.Flow

/**
 * Órdenes de reparación y sus avisos.
 * Hoy lo cumple [OrdenesDeEjemplo]; con el backend serán tablas de Supabase con tiempo real,
 * para que lo que registra el taller le llegue al cliente al instante.
 */
interface ServicioOrdenes {

    /**
     * Órdenes que ve esta persona: las suyas si es cliente, las de su taller si trabaja allí.
     * La más reciente primero. Se actualiza sola cuando alguien registra un avance.
     */
    fun ordenes(usuario: Usuario): Flow<List<Orden>>

    /** Avisos de la persona, el más reciente primero. */
    fun notificaciones(usuario: Usuario): Flow<List<Notificacion>>

    suspend fun marcarNotificacionesLeidas(usuario: Usuario)
}
