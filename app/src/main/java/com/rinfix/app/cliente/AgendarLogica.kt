package com.rinfix.app.cliente

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.Catalogo
import com.rinfix.app.datos.Diagnostico
import com.rinfix.app.datos.Horario
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.PosicionRin
import com.rinfix.app.datos.Servicios
import com.rinfix.app.datos.SolicitudReparacion
import com.rinfix.app.datos.Vehiculo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Solicitar una reparación (desde un diagnóstico) o agendar una revisión (sin diagnóstico).
 * El id del diagnóstico llega opcional en la ruta: «agendar?diagnostico={diagnostico}».
 */
class AgendarLogica(guardado: SavedStateHandle) : ViewModel() {

    data class Estado(
        val cargando: Boolean = true,
        val diagnostico: Diagnostico? = null,
        val vehiculos: List<Vehiculo> = emptyList(),
        val idVehiculo: String? = null,
        val posicion: PosicionRin? = null,
        val dias: List<LocalDate> = emptyList(),
        val dia: LocalDate? = null,
        val horas: List<Horario> = emptyList(),
        val cargandoHoras: Boolean = false,
        val hora: LocalTime? = null,
        val notas: String = "",
        val enviando: Boolean = false,
        val error: String? = null,
        /** Orden creada al confirmar: la pantalla muestra la confirmación. */
        val ordenCreada: Orden? = null,
    ) {
        val vehiculo: Vehiculo? get() = vehiculos.firstOrNull { it.id == idVehiculo }
        val listo: Boolean get() = posicion != null && dia != null && hora != null
    }

    private val idDiagnostico: String? = guardado.get<String>("diagnostico")
    private val cuentas = Servicios.cuentas
    private val ordenes = Servicios.ordenes

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    private var cargaHoras: Job? = null

    init {
        viewModelScope.launch {
            val usuario = cuentas.usuarioActual.value
            val diagnostico = if (usuario != null && idDiagnostico != null) {
                Servicios.analisis.diagnostico(usuario, idDiagnostico).first()
            } else {
                null
            }
            val vehiculos = if (usuario != null) Servicios.garaje.vehiculos(usuario).first() else emptyList()
            // El vehículo del diagnóstico, o el único del garaje.
            val elegido = vehiculos.firstOrNull { it.titulo == diagnostico?.vehiculo } ?: vehiculos.singleOrNull()
            val dias = proximosDias(LocalDateTime.now())
            _estado.update {
                it.copy(
                    cargando = false,
                    diagnostico = diagnostico,
                    vehiculos = vehiculos,
                    idVehiculo = elegido?.id,
                    dias = dias,
                )
            }
            dias.firstOrNull()?.let(::elegirDia)
        }
    }

    fun elegirVehiculo(id: String?) {
        _estado.update { it.copy(idVehiculo = id) }
    }

    fun elegirPosicion(posicion: PosicionRin) {
        _estado.update { it.copy(posicion = posicion, error = null) }
    }

    fun elegirDia(dia: LocalDate) {
        _estado.update { it.copy(dia = dia, hora = null, cargandoHoras = true, horas = emptyList()) }
        cargaHoras?.cancel()
        cargaHoras = viewModelScope.launch {
            val horas = runCatching { ordenes.horasDisponibles(dia) }.getOrElse { emptyList() }
            _estado.update { it.copy(horas = horas, cargandoHoras = false) }
        }
    }

    fun elegirHora(hora: LocalTime) {
        _estado.update { it.copy(hora = hora, error = null) }
    }

    fun cambiarNotas(notas: String) {
        _estado.update { it.copy(notas = notas.take(MaximoNotas)) }
    }

    fun enviar() {
        val actual = _estado.value
        val usuario = cuentas.usuarioActual.value ?: return
        val posicion = actual.posicion
        val dia = actual.dia
        val hora = actual.hora
        if (actual.enviando) return
        if (posicion == null || dia == null || hora == null) {
            _estado.update {
                it.copy(error = if (posicion == null) "Elige cuál rin vas a llevar." else "Elige el día y la hora de ingreso.")
            }
            return
        }
        val diagnostico = actual.diagnostico
        val servicio = diagnostico?.danos?.firstOrNull { it.confirmado }
            ?.let { Catalogo.servicios[it.tipo]?.nombre }
            ?: "Revisión en el taller"
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            try {
                val orden = ordenes.solicitar(
                    usuario,
                    SolicitudReparacion(
                        idDiagnostico = diagnostico?.id,
                        vehiculo = actual.vehiculo?.titulo ?: diagnostico?.vehiculo ?: "Vehículo por confirmar",
                        posicion = posicion,
                        servicio = servicio,
                        cita = dia.atTime(hora),
                        notas = actual.notas.trim(),
                    ),
                )
                _estado.update { it.copy(enviando = false, ordenCreada = orden) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update {
                    it.copy(enviando = false, error = "No pudimos enviar la solicitud. Revisa tu conexión e inténtalo de nuevo.")
                }
            }
        }
    }

    companion object {
        const val MaximoNotas = 300

        /**
         * Los próximos 8 días en que el taller recibe rines (no abre los domingos).
         * Después de las 4 p. m. ya no se ofrece el mismo día.
         */
        fun proximosDias(ahora: LocalDateTime): List<LocalDate> {
            var dia = if (ahora.hour >= 16) ahora.toLocalDate().plusDays(1) else ahora.toLocalDate()
            val dias = mutableListOf<LocalDate>()
            while (dias.size < 8) {
                if (dia.dayOfWeek != DayOfWeek.SUNDAY) dias += dia
                dia = dia.plusDays(1)
            }
            return dias
        }
    }
}
