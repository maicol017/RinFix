package com.rinfix.app.cliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.Diagnostico
import com.rinfix.app.datos.Notificacion
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.ServicioAnalisis
import com.rinfix.app.datos.ServicioCuentas
import com.rinfix.app.datos.ServicioGaraje
import com.rinfix.app.datos.ServicioOrdenes
import com.rinfix.app.datos.Servicios
import com.rinfix.app.datos.Vehiculo
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Datos del Inicio del cliente: la reparación en curso, su garaje, lo último que pasó
 * y sus avisos. Todo se actualiza solo cuando cambia algo (por ejemplo, el taller sube fotos).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioLogica(
    private val cuentas: ServicioCuentas = Servicios.cuentas,
    private val ordenes: ServicioOrdenes = Servicios.ordenes,
    garaje: ServicioGaraje = Servicios.garaje,
    analisis: ServicioAnalisis = Servicios.analisis,
) : ViewModel() {

    /** Algo que pasó: un diagnóstico o una reparación entregada. */
    sealed interface Actividad {
        val fecha: LocalDateTime

        data class Analisis(val diagnostico: Diagnostico) : Actividad {
            override val fecha: LocalDateTime get() = diagnostico.fecha
        }

        data class Entrega(val orden: Orden) : Actividad {
            override val fecha: LocalDateTime get() = orden.ultimaActualizacion
        }
    }

    data class Estado(
        val cargando: Boolean = true,
        val nombre: String = "",
        /** La reparación activa más reciente. */
        val ordenActiva: Orden? = null,
        /** Primera orden con un total aprobado sin pagar. */
        val pagoPendiente: Orden? = null,
        val vehiculos: List<Vehiculo> = emptyList(),
        /** Las 3 cosas más recientes. */
        val actividad: List<Actividad> = emptyList(),
        val notificaciones: List<Notificacion> = emptyList(),
    ) {
        val sinLeer: Int get() = notificaciones.count { !it.leida }
    }

    val estado: StateFlow<Estado> = cuentas.usuarioActual
        .flatMapLatest { usuario ->
            if (usuario == null) {
                flowOf(Estado(cargando = false))
            } else {
                combine(
                    ordenes.ordenes(usuario),
                    ordenes.notificaciones(usuario),
                    garaje.vehiculos(usuario),
                    analisis.historial(usuario),
                ) { lista, avisos, carros, diagnosticos ->
                    val activas = lista.filter { it.activa }
                    val actividad = diagnosticos.map { Actividad.Analisis(it) } +
                        lista.filterNot { it.activa }.map { Actividad.Entrega(it) }
                    Estado(
                        cargando = false,
                        nombre = usuario.nombre,
                        ordenActiva = activas.firstOrNull(),
                        pagoPendiente = activas.firstOrNull { it.pagoPendiente },
                        vehiculos = carros,
                        actividad = actividad.sortedByDescending { it.fecha }.take(3),
                        notificaciones = avisos,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun marcarLeidas() {
        val usuario = cuentas.usuarioActual.value ?: return
        viewModelScope.launch { ordenes.marcarNotificacionesLeidas(usuario) }
    }
}
