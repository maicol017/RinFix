package com.rinfix.app.taller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.EtapaOrden
import com.rinfix.app.datos.Notificacion
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.Servicios
import java.time.Duration
import java.time.LocalDate
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
 * Panel del taller: cómo va el día, solicitudes nuevas, ingresos y entregas de hoy y mañana,
 * y unas estadísticas básicas. Se actualiza solo cuando cambia una orden.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PanelLogica : ViewModel() {

    enum class TipoEvento { Ingreso, Entrega }

    /** Un rin que entra o sale del taller a cierta hora. */
    data class Evento(val fecha: LocalDateTime, val tipo: TipoEvento, val orden: Orden)

    data class Estadisticas(
        val entregados: Int = 0,
        val cobrado: Long = 0,
        /** Días desde la solicitud hasta la entrega, en promedio. `null` si no hay entregas. */
        val diasPromedio: Int? = null,
    )

    data class Estado(
        val cargando: Boolean = true,
        val nombre: String = "",
        val taller: String = "",
        val nuevas: List<Orden> = emptyList(),
        val enTaller: Int = 0,
        val listas: Int = 0,
        val atrasadas: Int = 0,
        /** Ingresos y entregas de hoy y mañana, en orden. */
        val proximos: List<Evento> = emptyList(),
        val estadisticas: Estadisticas = Estadisticas(),
        val notificaciones: List<Notificacion> = emptyList(),
    ) {
        val sinLeer: Int get() = notificaciones.count { !it.leida }
    }

    val estado: StateFlow<Estado> = Servicios.cuentas.usuarioActual
        .flatMapLatest { usuario ->
            if (usuario == null) {
                flowOf(Estado(cargando = false))
            } else {
                combine(
                    Servicios.ordenes.ordenes(usuario),
                    Servicios.ordenes.notificaciones(usuario),
                ) { ordenes, avisos ->
                    val ahora = LocalDateTime.now()
                    Estado(
                        cargando = false,
                        nombre = usuario.nombre,
                        taller = usuario.taller.orEmpty(),
                        nuevas = ordenes
                            .filter { it.etapa == EtapaOrden.Solicitud }
                            .sortedWith(compareBy(nullsLast()) { it.cita }),
                        enTaller = ordenes.count { it.etapa > EtapaOrden.Solicitud && it.etapa < EtapaOrden.Lista },
                        listas = ordenes.count { it.etapa == EtapaOrden.Lista },
                        atrasadas = ordenes.count { it.atrasada(ahora) },
                        proximos = proximos(ordenes, ahora.toLocalDate()),
                        estadisticas = estadisticas(ordenes, ahora),
                        notificaciones = avisos,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun marcarLeidas() {
        val usuario = Servicios.cuentas.usuarioActual.value ?: return
        viewModelScope.launch { Servicios.ordenes.marcarNotificacionesLeidas(usuario) }
    }

    private fun proximos(ordenes: List<Orden>, hoy: LocalDate): List<Evento> {
        val dias = setOf(hoy, hoy.plusDays(1))
        val ingresos = ordenes.mapNotNull { orden ->
            val cita = orden.cita
            if (orden.etapa == EtapaOrden.Solicitud && cita != null && cita.toLocalDate() in dias) {
                Evento(cita, TipoEvento.Ingreso, orden)
            } else {
                null
            }
        }
        val entregas = ordenes.mapNotNull { orden ->
            val entrega = orden.entregaEstimada
            if (orden.activa && orden.etapa > EtapaOrden.Solicitud && entrega != null && entrega.toLocalDate() in dias) {
                Evento(entrega, TipoEvento.Entrega, orden)
            } else {
                null
            }
        }
        return (ingresos + entregas).sortedBy { it.fecha }
    }

    /** Entregas de los últimos 90 días: cuántas, cuánto se cobró y cuánto tardaron. */
    private fun estadisticas(ordenes: List<Orden>, ahora: LocalDateTime): Estadisticas {
        val desde = ahora.minusDays(DiasEstadisticas)
        val entregadas = ordenes.mapNotNull { orden ->
            val entrega = orden.avances.lastOrNull { it.etapa == EtapaOrden.Entregada }?.fecha
            if (entrega != null && entrega.isAfter(desde)) orden to entrega else null
        }
        if (entregadas.isEmpty()) return Estadisticas()
        val horas = entregadas.map { (orden, entrega) -> Duration.between(orden.creada, entrega).toHours() }
        return Estadisticas(
            entregados = entregadas.size,
            cobrado = entregadas.sumOf { (orden, _) -> if (orden.pagada) orden.total ?: 0L else 0L },
            diasPromedio = (horas.average() / 24).let { kotlin.math.round(it).toInt().coerceAtLeast(1) },
        )
    }

    companion object {
        const val DiasEstadisticas = 90L
    }
}
