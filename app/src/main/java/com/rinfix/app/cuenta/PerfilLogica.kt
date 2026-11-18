package com.rinfix.app.cuenta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.Comprobante
import com.rinfix.app.datos.Diagnostico
import com.rinfix.app.datos.EtapaOrden
import com.rinfix.app.datos.ResultadoCuenta
import com.rinfix.app.datos.Servicios
import com.rinfix.app.datos.Usuario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Perfil de cliente o taller: datos de la cuenta, resumen de lo que tiene en la app y edición de datos. */
@OptIn(ExperimentalCoroutinesApi::class)
class PerfilLogica : ViewModel() {

    data class Estado(
        val cargando: Boolean = true,
        val usuario: Usuario? = null,
        val vehiculos: Int = 0,
        val diagnosticos: List<Diagnostico> = emptyList(),
        val comprobantes: List<Comprobante> = emptyList(),
        /** Órdenes sin terminar (incluye las listas para entregar). */
        val enCurso: Int = 0,
        val listas: Int = 0,
        val entregadas: Int = 0,
    )

    data class Edicion(
        val guardando: Boolean = false,
        val errorNombre: String? = null,
        val errorTelefono: String? = null,
        val error: String? = null,
    )

    val estado: StateFlow<Estado> = Servicios.cuentas.usuarioActual
        .flatMapLatest { usuario ->
            if (usuario == null) {
                flowOf(Estado(cargando = false))
            } else {
                combine(
                    Servicios.garaje.vehiculos(usuario),
                    Servicios.ordenes.ordenes(usuario),
                    Servicios.analisis.historial(usuario),
                    Servicios.pagos.comprobantes(usuario),
                ) { vehiculos, ordenes, diagnosticos, comprobantes ->
                    Estado(
                        cargando = false,
                        usuario = usuario,
                        vehiculos = vehiculos.size,
                        diagnosticos = diagnosticos,
                        comprobantes = comprobantes,
                        enCurso = ordenes.count { it.activa },
                        listas = ordenes.count { it.etapa == EtapaOrden.Lista },
                        entregadas = ordenes.count { it.etapa == EtapaOrden.Entregada },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    private val _edicion = MutableStateFlow(Edicion())
    val edicion: StateFlow<Edicion> = _edicion.asStateFlow()

    /** Valida y guarda. `alGuardar` se llama solo si todo salió bien. */
    fun guardarDatos(nombre: String, telefono: String, alGuardar: () -> Unit) {
        if (_edicion.value.guardando) return
        val nombreLimpio = nombre.trim().replace(Regex("\\s+"), " ")
        val celular = telefono.filter { it.isDigit() }
        val errorNombre = when {
            nombreLimpio.length < 3 -> "Escribe tu nombre completo."
            nombreLimpio.length > MaximoNombre -> "Usa máximo $MaximoNombre caracteres."
            else -> null
        }
        val errorTelefono = when {
            celular.isEmpty() -> null
            celular.length != 10 || !celular.startsWith("3") -> "Escribe un celular de 10 dígitos, por ejemplo 300 123 4567."
            else -> null
        }
        if (errorNombre != null || errorTelefono != null) {
            _edicion.value = Edicion(errorNombre = errorNombre, errorTelefono = errorTelefono)
            return
        }
        _edicion.value = Edicion(guardando = true)
        viewModelScope.launch {
            when (val resultado = Servicios.cuentas.actualizarDatos(nombreLimpio, celular.ifEmpty { null })) {
                is ResultadoCuenta.Exito -> {
                    _edicion.value = Edicion()
                    alGuardar()
                }
                is ResultadoCuenta.Fallo -> _edicion.value = Edicion(error = resultado.mensaje)
            }
        }
    }

    fun limpiarEdicion() {
        _edicion.update { Edicion(guardando = it.guardando) }
    }

    companion object {
        const val MaximoNombre = 60
    }
}
