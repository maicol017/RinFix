package com.rinfix.app.cliente

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.Comprobante
import com.rinfix.app.datos.MetodoPago
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.Servicios
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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

/**
 * Pago simulado de una orden. El id llega en la ruta: «pago/{orden}».
 * Recorre las mismas fases que una pasarela real (validar, autorizar, aprobar) sin cobrar nada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PagoLogica(guardado: SavedStateHandle) : ViewModel() {

    enum class Fase(val texto: String) {
        Eligiendo(""),
        Validando("Validando datos de prueba"),
        Autorizando("Autorizando el pago simulado"),
        Aprobado("Pago aprobado"),
    }

    data class Estado(
        val cargando: Boolean = true,
        val orden: Orden? = null,
        val metodo: MetodoPago = MetodoPago.Tarjeta,
        val fase: Fase = Fase.Eligiendo,
        val error: String? = null,
        /** Comprobante listo: la pantalla pasa a mostrarlo. */
        val comprobante: Comprobante? = null,
    )

    private data class Seleccion(
        val metodo: MetodoPago = MetodoPago.Tarjeta,
        val fase: Fase = Fase.Eligiendo,
        val error: String? = null,
        val comprobante: Comprobante? = null,
    )

    private val idOrden: String = guardado.get<String>("orden").orEmpty()
    private val seleccion = MutableStateFlow(Seleccion())

    val estado: StateFlow<Estado> = combine(
        Servicios.cuentas.usuarioActual
            .flatMapLatest { usuario -> if (usuario == null) flowOf(emptyList()) else Servicios.ordenes.ordenes(usuario) }
            .map { lista -> lista.firstOrNull { it.id == idOrden } },
        seleccion,
    ) { orden, actual ->
        Estado(
            cargando = false,
            orden = orden,
            metodo = actual.metodo,
            fase = actual.fase,
            error = actual.error,
            comprobante = actual.comprobante,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun elegirMetodo(metodo: MetodoPago) {
        seleccion.update { it.copy(metodo = metodo, error = null) }
    }

    fun pagar() {
        val usuario = Servicios.cuentas.usuarioActual.value ?: return
        val orden = estado.value.orden ?: return
        if (seleccion.value.fase != Fase.Eligiendo) return
        val metodo = seleccion.value.metodo
        viewModelScope.launch {
            try {
                seleccion.update { it.copy(fase = Fase.Validando, error = null) }
                delay(850)
                seleccion.update { it.copy(fase = Fase.Autorizando) }
                val comprobante = Servicios.pagos.pagar(usuario, orden, metodo)
                seleccion.update { it.copy(fase = Fase.Aprobado) }
                delay(1_000)
                seleccion.update { it.copy(comprobante = comprobante) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                seleccion.update {
                    it.copy(fase = Fase.Eligiendo, error = "No pudimos completar el pago simulado. Inténtalo de nuevo.")
                }
            }
        }
    }
}

/** Un comprobante de la persona. El id llega en la ruta: «comprobante/{comprobante}». */
@OptIn(ExperimentalCoroutinesApi::class)
class ComprobanteLogica(guardado: SavedStateHandle) : ViewModel() {

    data class Estado(val cargando: Boolean = true, val comprobante: Comprobante? = null)

    private val idComprobante: String = guardado.get<String>("comprobante").orEmpty()

    val estado: StateFlow<Estado> = Servicios.cuentas.usuarioActual
        .flatMapLatest { usuario ->
            if (usuario == null) flowOf(null) else Servicios.pagos.comprobante(usuario, idComprobante)
        }
        .map { Estado(cargando = false, comprobante = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())
}
