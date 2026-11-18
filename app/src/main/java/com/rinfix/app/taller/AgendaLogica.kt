package com.rinfix.app.taller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.Catalogo
import com.rinfix.app.datos.EtapaOrden
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.Servicios
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Agenda del taller para las próximas dos semanas: a qué hora entra cada rin, qué horas quedan
 * libres, cuáles están bloqueadas y qué se entrega cada día.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AgendaLogica : ViewModel() {

    data class ResumenDia(val fecha: LocalDate, val ingresos: Int, val entregas: Int, val cerrado: Boolean)

    /** Una hora de ingreso del día elegido. */
    data class Franja(
        val inicio: LocalDateTime,
        val ingresos: List<Orden>,
        val bloqueada: Boolean,
        val pasada: Boolean,
        /** `false` si es una cita fuera del horario de ingreso: se muestra pero no se puede bloquear. */
        val deAtencion: Boolean = true,
    ) {
        val libre: Boolean get() = ingresos.isEmpty() && !bloqueada
    }

    data class Estado(
        val cargando: Boolean = true,
        val hoy: LocalDate = LocalDate.now(),
        val dias: List<ResumenDia> = emptyList(),
        val elegido: LocalDate = LocalDate.now(),
        val franjas: List<Franja> = emptyList(),
        val entregas: List<Orden> = emptyList(),
        /** Hora que se está bloqueando o liberando. */
        val cambiando: LocalDateTime? = null,
        val error: String? = null,
    ) {
        val cerrado: Boolean get() = Catalogo.horasDeIngreso(elegido).isEmpty()
        val ingresos: Int get() = franjas.sumOf { it.ingresos.size }
        val libres: Int get() = franjas.count { it.libre && !it.pasada && it.deAtencion }
    }

    private data class Proceso(val cambiando: LocalDateTime? = null, val error: String? = null)

    private val elegido = MutableStateFlow(LocalDate.now())
    private val proceso = MutableStateFlow(Proceso())

    val estado: StateFlow<Estado> = Servicios.cuentas.usuarioActual
        .flatMapLatest { usuario ->
            if (usuario == null) {
                flowOf(Estado(cargando = false))
            } else {
                combine(
                    Servicios.ordenes.ordenes(usuario),
                    Servicios.ordenes.horasBloqueadas(usuario),
                    elegido,
                    proceso,
                ) { ordenes, bloqueadas, dia, actual -> armar(ordenes, bloqueadas, dia, actual) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun elegirDia(fecha: LocalDate) {
        elegido.value = fecha
        proceso.update { it.copy(error = null) }
    }

    /** Bloquea una hora libre o libera una bloqueada. `alTerminar` recibe cómo quedó. */
    fun cambiarBloqueo(franja: Franja, alTerminar: (bloqueada: Boolean) -> Unit) {
        val usuario = Servicios.cuentas.usuarioActual.value ?: return
        if (proceso.value.cambiando != null) return
        val bloquear = !franja.bloqueada
        proceso.value = Proceso(cambiando = franja.inicio)
        viewModelScope.launch {
            val error = Servicios.ordenes.cambiarBloqueo(usuario, franja.inicio, bloquear)
            proceso.value = Proceso(error = error)
            if (error == null) alTerminar(bloquear)
        }
    }

    fun limpiarError() {
        proceso.update { it.copy(error = null) }
    }

    private fun armar(ordenes: List<Orden>, bloqueadas: Set<LocalDateTime>, dia: LocalDate, actual: Proceso): Estado {
        val ahora = LocalDateTime.now()
        val hoy = ahora.toLocalDate()
        val conCita = ordenes.filter { it.cita != null }
        val porEntregar = ordenes.filter { it.activa && it.etapa > EtapaOrden.Solicitud && it.entregaEstimada != null }

        val dias = (0 until DiasVisibles).map { hoy.plusDays(it.toLong()) }.map { fecha ->
            ResumenDia(
                fecha = fecha,
                ingresos = conCita.count { it.cita?.toLocalDate() == fecha },
                entregas = porEntregar.count { it.entregaEstimada?.toLocalDate() == fecha },
                cerrado = Catalogo.horasDeIngreso(fecha).isEmpty(),
            )
        }

        // Horas de atención del día más cualquier cita agendada fuera de ellas.
        val horasDeAtencion = Catalogo.horasDeIngreso(dia).map { dia.atTime(it) }
        val citasDelDia = conCita.mapNotNull { it.cita }.filter { it.toLocalDate() == dia }
            .map { it.truncatedTo(ChronoUnit.HOURS) }
        val franjas = (horasDeAtencion + citasDelDia).distinct().sorted().map { inicio ->
            Franja(
                inicio = inicio,
                ingresos = conCita.filter { it.cita?.truncatedTo(ChronoUnit.HOURS) == inicio },
                bloqueada = inicio in bloqueadas,
                pasada = inicio.isBefore(ahora),
                deAtencion = inicio in horasDeAtencion,
            )
        }

        return Estado(
            cargando = false,
            hoy = hoy,
            dias = dias,
            elegido = dia,
            franjas = franjas,
            entregas = porEntregar.filter { it.entregaEstimada?.toLocalDate() == dia }.sortedBy { it.entregaEstimada },
            cambiando = actual.cambiando,
            error = actual.error,
        )
    }

    companion object {
        const val DiasVisibles = 14
    }
}
