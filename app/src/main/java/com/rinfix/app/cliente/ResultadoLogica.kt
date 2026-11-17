package com.rinfix.app.cliente

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.Diagnostico
import com.rinfix.app.datos.Servicios
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Un diagnóstico de la persona. El id llega en la ruta: «resultado/{diagnostico}». */
@OptIn(ExperimentalCoroutinesApi::class)
class ResultadoLogica(guardado: SavedStateHandle) : ViewModel() {

    data class Estado(val cargando: Boolean = true, val diagnostico: Diagnostico? = null)

    private val idDiagnostico: String = guardado.get<String>("diagnostico").orEmpty()

    val estado: StateFlow<Estado> = Servicios.cuentas.usuarioActual
        .flatMapLatest { usuario ->
            if (usuario == null) flowOf(null) else Servicios.analisis.diagnostico(usuario, idDiagnostico)
        }
        .map { Estado(cargando = false, diagnostico = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())
}
