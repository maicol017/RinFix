package com.rinfix.app.cliente

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.Servicios
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** «Mis órdenes»: las reparaciones en curso y las terminadas, de la más reciente a la más antigua. */
@OptIn(ExperimentalCoroutinesApi::class)
class OrdenesLogica : ViewModel() {

    data class Estado(
        val cargando: Boolean = true,
        val enCurso: List<Orden> = emptyList(),
        val terminadas: List<Orden> = emptyList(),
    )

    val estado: StateFlow<Estado> = Servicios.cuentas.usuarioActual
        .flatMapLatest { usuario -> if (usuario == null) flowOf(emptyList()) else Servicios.ordenes.ordenes(usuario) }
        .map { lista ->
            Estado(
                cargando = false,
                enCurso = lista.filter { it.activa },
                terminadas = lista.filterNot { it.activa },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())
}

/** Una orden de la persona, con todos sus avances. El id llega en la ruta: «seguimiento/{orden}». */
@OptIn(ExperimentalCoroutinesApi::class)
class SeguimientoLogica(guardado: SavedStateHandle) : ViewModel() {

    data class Estado(val cargando: Boolean = true, val orden: Orden? = null)

    private val idOrden: String = guardado.get<String>("orden").orEmpty()

    val estado: StateFlow<Estado> = Servicios.cuentas.usuarioActual
        .flatMapLatest { usuario -> if (usuario == null) flowOf(emptyList()) else Servicios.ordenes.ordenes(usuario) }
        .map { lista -> Estado(cargando = false, orden = lista.firstOrNull { it.id == idOrden }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())
}
