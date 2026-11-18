package com.rinfix.app.cliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.Diagnostico
import com.rinfix.app.datos.Mensaje
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.Servicios
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Conversación con el asistente. Las órdenes y diagnósticos van aparte para que las tarjetas
 * dentro del chat siempre muestren sus datos al día.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AsistenteLogica : ViewModel() {

    data class Estado(
        val cargando: Boolean = true,
        val nombre: String = "",
        val mensajes: List<Mensaje> = emptyList(),
        val escribiendo: Boolean = false,
        /** Preguntas para empezar, según lo que la persona tiene en la app. */
        val sugerencias: List<String> = emptyList(),
        val ordenes: Map<String, Orden> = emptyMap(),
        val diagnosticos: Map<String, Diagnostico> = emptyMap(),
    )

    val estado: StateFlow<Estado> = Servicios.cuentas.usuarioActual
        .flatMapLatest { usuario ->
            if (usuario == null) {
                flowOf(Estado(cargando = false))
            } else {
                combine(
                    Servicios.asistente.conversacion(usuario),
                    Servicios.ordenes.ordenes(usuario),
                    Servicios.analisis.historial(usuario),
                ) { conversacion, ordenes, historial ->
                    Estado(
                        cargando = false,
                        nombre = usuario.nombre.substringBefore(" "),
                        mensajes = conversacion.mensajes,
                        escribiendo = conversacion.escribiendo,
                        sugerencias = sugerenciasIniciales(ordenes, historial),
                        ordenes = ordenes.associateBy { it.id },
                        diagnosticos = historial.associateBy { it.id },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    /** Envía la pregunta. Devuelve `true` si salió, para limpiar el campo. */
    fun enviar(texto: String): Boolean {
        val usuario = Servicios.cuentas.usuarioActual.value ?: return false
        val pregunta = texto.trim().take(MaximoTexto)
        if (pregunta.isEmpty() || estado.value.escribiendo) return false
        Servicios.asistente.enviar(usuario, pregunta)
        return true
    }

    fun reiniciar() {
        Servicios.cuentas.usuarioActual.value?.let { Servicios.asistente.reiniciar(it) }
    }

    private fun sugerenciasIniciales(ordenes: List<Orden>, historial: List<Diagnostico>): List<String> = buildList {
        ordenes.firstOrNull { it.activa }?.let { add("¿Cómo va mi orden ${it.codigo}?") }
        if (historial.isNotEmpty()) add("¿Qué significa mi último diagnóstico?")
        add("¿Cuánto cuesta enderezar un rin?")
        add("¿Puedo seguir manejando con el rin dañado?")
        add("¿Cómo tomo buenas fotos del rin?")
    }.take(4)

    companion object {
        const val MaximoTexto = 400
    }
}
