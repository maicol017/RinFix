package com.rinfix.app.datos

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

/** Lo que el cliente pide al solicitar una reparación o una revisión. */
data class SolicitudReparacion(
    /** Diagnóstico del que sale la solicitud; `null` si es una revisión sin análisis. */
    val idDiagnostico: String?,
    /** «Mazda 3 2021» */
    val vehiculo: String,
    val posicion: PosicionRin,
    /** Servicio principal: «Reparación de rayones y pintura» o «Revisión en el taller». */
    val servicio: String,
    /** Cuándo lleva el rin al taller. */
    val cita: LocalDateTime,
    val notas: String,
)

/** Una hora del día para llevar el rin, libre u ocupada. */
data class Horario(val hora: LocalTime, val disponible: Boolean)

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

    /** Horas del taller para recibir el rin ese día, con las que ya están ocupadas. */
    suspend fun horasDisponibles(fecha: LocalDate): List<Horario>

    /** Marca la orden como pagada. Lo usa el servicio de pagos cuando un pago queda aprobado. */
    suspend fun registrarPago(idOrden: String, referencia: String)

    /** Crea la orden con la etapa «Solicitud recibida» y avisa al taller. */
    suspend fun solicitar(usuario: Usuario, solicitud: SolicitudReparacion): Orden
}
