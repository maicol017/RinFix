package com.rinfix.app.taller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.EtapaOrden
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.Servicios
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Trabajos del taller agrupados por filtro, ya ordenados por lo más urgente. */
@OptIn(ExperimentalCoroutinesApi::class)
class TrabajosLogica : ViewModel() {

    enum class Filtro(val nombre: String) {
        Activos("Activos"),
        Nuevos("Nuevos"),
        EnTaller("En el taller"),
        Listos("Listos"),
        Entregados("Entregados"),
        ;

        fun incluye(orden: Orden): Boolean = when (this) {
            Activos -> orden.activa
            Nuevos -> orden.etapa == EtapaOrden.Solicitud
            EnTaller -> orden.etapa > EtapaOrden.Solicitud && orden.etapa < EtapaOrden.Lista
            Listos -> orden.etapa == EtapaOrden.Lista
            Entregados -> orden.etapa == EtapaOrden.Entregada
        }
    }

    data class Estado(
        val cargando: Boolean = true,
        val taller: String = "",
        val porFiltro: Map<Filtro, List<Orden>> = emptyMap(),
    ) {
        fun de(filtro: Filtro): List<Orden> = porFiltro[filtro].orEmpty()
    }

    val estado: StateFlow<Estado> = Servicios.cuentas.usuarioActual
        .flatMapLatest { usuario ->
            if (usuario == null) {
                flowOf(Estado(cargando = false))
            } else {
                Servicios.ordenes.ordenes(usuario).map { ordenes ->
                    val ahora = LocalDateTime.now()
                    Estado(
                        cargando = false,
                        taller = usuario.taller.orEmpty(),
                        porFiltro = Filtro.entries.associateWith { filtro ->
                            ordenar(ordenes.filter(filtro::incluye), filtro, ahora)
                        },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    /**
     * Activos: primero los atrasados, luego por la fecha que más importa (cita o entrega).
     * Entregados: el más reciente primero.
     */
    private fun ordenar(ordenes: List<Orden>, filtro: Filtro, ahora: LocalDateTime): List<Orden> =
        if (filtro == Filtro.Entregados) {
            ordenes.sortedByDescending { it.ultimaActualizacion }
        } else {
            ordenes.sortedWith(
                compareByDescending<Orden> { it.atrasada(ahora) }
                    .thenBy(nullsLast()) { it.fechaClave() },
            )
        }
}
