package com.rinfix.app.taller

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.NuevoAvance
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.ResultadoAvance
import com.rinfix.app.datos.Servicios
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Un trabajo del taller. El id llega en la ruta: «trabajo/{orden}». */
@OptIn(ExperimentalCoroutinesApi::class)
class TrabajoLogica(guardado: SavedStateHandle) : ViewModel() {

    data class Estado(
        val cargando: Boolean = true,
        val orden: Orden? = null,
        val guardando: Boolean = false,
        val error: String? = null,
    )

    private data class Proceso(val guardando: Boolean = false, val error: String? = null)

    private val idOrden: String = guardado.get<String>("orden").orEmpty()
    private val proceso = MutableStateFlow(Proceso())

    val estado: StateFlow<Estado> = combine(
        Servicios.cuentas.usuarioActual
            .flatMapLatest { usuario -> if (usuario == null) flowOf(emptyList()) else Servicios.ordenes.ordenes(usuario) }
            .map { lista -> lista.firstOrNull { it.id == idOrden } },
        proceso,
    ) { orden, actual ->
        Estado(cargando = false, orden = orden, guardando = actual.guardando, error = actual.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    /** Registra el cambio. `alTerminar` se llama solo si quedó guardado. */
    fun registrar(avance: NuevoAvance, alTerminar: () -> Unit) {
        val usuario = Servicios.cuentas.usuarioActual.value ?: return
        if (proceso.value.guardando) return
        proceso.value = Proceso(guardando = true)
        viewModelScope.launch {
            when (val resultado = Servicios.ordenes.registrarAvance(usuario, idOrden, avance)) {
                is ResultadoAvance.Hecho -> {
                    proceso.value = Proceso()
                    alTerminar()
                }
                is ResultadoAvance.Fallo -> proceso.value = Proceso(error = resultado.mensaje)
            }
        }
    }

    fun limpiarError() {
        proceso.update { it.copy(error = null) }
    }
}
