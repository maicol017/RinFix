package com.rinfix.app.cuenta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.ResultadoCuenta
import com.rinfix.app.datos.ServicioCuentas
import com.rinfix.app.datos.Servicios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lógica de las pantallas de cuenta: inicio de sesión, registro, Google y código de taller.
 *
 * La pantalla solo muestra el [Estado]; esta clase habla con el servicio de cuentas.
 * Si la persona entra con éxito, el servicio cambia el usuario actual y la app pasa sola
 * a la navegación de su rol.
 */
class CuentaLogica(
    private val cuentas: ServicioCuentas = Servicios.cuentas,
) : ViewModel() {

    data class Estado(
        val cargando: Boolean = false,
        val error: String? = null,
        val intentosFallidos: Int = 0,
    )

    private val estadoInterno = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = estadoInterno.asStateFlow()

    fun entrar(correo: String, contrasena: String) = ejecutar { cuentas.entrar(correo, contrasena) }

    fun crearCuenta(nombre: String, correo: String, contrasena: String) =
        ejecutar { cuentas.crearCuenta(nombre, correo, contrasena) }

    fun entrarConGoogle() = ejecutar { cuentas.entrarConGoogle() }

    fun unirseATaller(codigo: String, nombre: String, correo: String, contrasena: String) =
        ejecutar { cuentas.unirseATaller(codigo, nombre, correo, contrasena) }

    fun limpiarError() = estadoInterno.update { it.copy(error = null) }

    private fun ejecutar(accion: suspend () -> ResultadoCuenta) {
        if (estadoInterno.value.cargando) return
        viewModelScope.launch {
            estadoInterno.update { it.copy(cargando = true, error = null) }
            when (val resultado = accion()) {
                is ResultadoCuenta.Exito -> estadoInterno.update { it.copy(cargando = false) }
                is ResultadoCuenta.Fallo -> estadoInterno.update {
                    it.copy(
                        cargando = false,
                        error = resultado.mensaje,
                        intentosFallidos = it.intentosFallidos + 1,
                    )
                }
            }
        }
    }
}
