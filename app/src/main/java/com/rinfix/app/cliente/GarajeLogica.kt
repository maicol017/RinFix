package com.rinfix.app.cliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.ServicioCuentas
import com.rinfix.app.datos.ServicioGaraje
import com.rinfix.app.datos.Servicios
import com.rinfix.app.datos.Vehiculo
import kotlinx.coroutines.CancellationException
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

/** Vehículos de «Mi garaje»: verlos, agregarlos, editarlos y quitarlos. */
@OptIn(ExperimentalCoroutinesApi::class)
class GarajeLogica(
    private val cuentas: ServicioCuentas = Servicios.cuentas,
    private val garaje: ServicioGaraje = Servicios.garaje,
) : ViewModel() {

    data class Estado(
        val cargando: Boolean = true,
        val vehiculos: List<Vehiculo> = emptyList(),
        val guardando: Boolean = false,
        val error: String? = null,
    )

    private data class Accion(val guardando: Boolean = false, val error: String? = null)

    private val accion = MutableStateFlow(Accion())

    val estado: StateFlow<Estado> = combine(
        cuentas.usuarioActual.flatMapLatest { usuario ->
            if (usuario == null) flowOf(emptyList()) else garaje.vehiculos(usuario)
        },
        accion,
    ) { vehiculos, actual ->
        Estado(
            cargando = false,
            vehiculos = vehiculos,
            guardando = actual.guardando,
            error = actual.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    /**
     * Guarda un vehículo nuevo (id vacío) o los cambios de uno existente.
     * `alGuardar` se llama solo si salió bien, por ejemplo para cerrar el formulario.
     */
    fun guardar(vehiculo: Vehiculo, alGuardar: () -> Unit) {
        val usuario = cuentas.usuarioActual.value ?: return
        if (accion.value.guardando) return
        accion.value = Accion(guardando = true)
        viewModelScope.launch {
            try {
                if (vehiculo.id.isEmpty()) {
                    garaje.agregar(usuario, vehiculo)
                } else {
                    garaje.actualizar(usuario, vehiculo)
                }
                accion.value = Accion()
                alGuardar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                accion.value = Accion(error = "No pudimos guardar el vehículo. Revisa tu conexión e inténtalo de nuevo.")
            }
        }
    }

    fun quitar(idVehiculo: String) {
        val usuario = cuentas.usuarioActual.value ?: return
        viewModelScope.launch {
            try {
                garaje.quitar(usuario, idVehiculo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                accion.value = Accion(error = "No pudimos quitar el vehículo. Revisa tu conexión e inténtalo de nuevo.")
            }
        }
    }

    fun limpiarError() {
        accion.update { it.copy(error = null) }
    }
}
