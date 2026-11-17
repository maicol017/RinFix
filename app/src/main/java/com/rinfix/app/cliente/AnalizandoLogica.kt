package com.rinfix.app.cliente

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.AvanceAnalisis
import com.rinfix.app.datos.Servicios
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Avance de un análisis en curso. El id llega en la ruta: «analizando/{analisis}». */
class AnalizandoLogica(guardado: SavedStateHandle) : ViewModel() {

    private val idAnalisis: String = guardado.get<String>("analisis").orEmpty()

    val avance: StateFlow<AvanceAnalisis> = Servicios.analisis.avance(idAnalisis)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AvanceAnalisis())
}
