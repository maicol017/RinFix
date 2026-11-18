package com.rinfix.app.taller

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.EtapaOrden
import com.rinfix.app.datos.FotoAvance
import com.rinfix.app.datos.MomentoFoto
import com.rinfix.app.datos.NuevoAvance
import com.rinfix.app.datos.Orden
import com.rinfix.app.datos.ResultadoAvance
import com.rinfix.app.datos.Servicios
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Novedad de un trabajo sin cambiar de etapa: fotos del antes, durante o después,
 * lo que se está haciendo y una nota para el cliente. El id llega en la ruta: «avance/{orden}».
 *
 * Las fotos se guardan en la carpeta privada de la app, nunca en la galería del teléfono.
 * Si la persona sale sin publicar, se borran.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistrarAvanceLogica(aplicacion: Application, guardado: SavedStateHandle) : AndroidViewModel(aplicacion) {

    data class Formulario(
        val fotos: List<String> = emptyList(),
        val momento: MomentoFoto = MomentoFoto.Durante,
        val detalle: String = "",
        val nota: String = "",
        val tomando: Boolean = false,
        val guardando: Boolean = false,
        val error: String? = null,
    ) {
        val puedeTomarMas: Boolean get() = fotos.size < MaximoFotos
    }

    data class Estado(
        val cargando: Boolean = true,
        val orden: Orden? = null,
        val formulario: Formulario = Formulario(),
    ) {
        /** Hay algo nuevo para publicar: una foto, una nota o un cambio en lo que se está haciendo. */
        val hayAlgo: Boolean
            get() = formulario.fotos.isNotEmpty() ||
                formulario.nota.isNotBlank() ||
                (formulario.detalle.isNotBlank() && formulario.detalle.trim() != orden?.detalleEtapa)
    }

    private val idOrden: String = guardado.get<String>("orden").orEmpty()
    private val formulario = MutableStateFlow(Formulario())
    private var publicado = false

    private val carpeta: File by lazy {
        File(getApplication<Application>().filesDir, "avances").apply { mkdirs() }
    }

    private val orden = Servicios.cuentas.usuarioActual
        .flatMapLatest { usuario -> if (usuario == null) flowOf(emptyList()) else Servicios.ordenes.ordenes(usuario) }
        .map { lista -> lista.firstOrNull { it.id == idOrden } }

    val estado: StateFlow<Estado> = combine(orden, formulario) { actual, datos ->
        Estado(cargando = false, orden = actual, formulario = datos)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    init {
        // Al abrir: el momento según la etapa y lo que se está haciendo, para editarlo.
        viewModelScope.launch {
            val inicial = orden.filterNotNull().first()
            formulario.update {
                it.copy(momento = momentoPara(inicial.etapa), detalle = inicial.detalleEtapa.orEmpty())
            }
        }
    }

    fun archivoNuevo(): File = File(carpeta, "avance_${System.currentTimeMillis()}.jpg")

    fun empezarFoto() {
        formulario.update { it.copy(tomando = true, error = null) }
    }

    fun fotoTomada(archivo: File?) {
        formulario.update {
            if (archivo == null) {
                it.copy(tomando = false, error = "No pudimos tomar la foto. Inténtalo de nuevo.")
            } else {
                it.copy(tomando = false, fotos = (it.fotos + archivo.absolutePath).take(MaximoFotos))
            }
        }
    }

    /** Copia la foto elegida en la galería a la carpeta de la app. */
    fun agregarDeGaleria(uri: Uri) {
        if (!formulario.value.puedeTomarMas) return
        formulario.update { it.copy(tomando = true, error = null) }
        viewModelScope.launch {
            val copia = withContext(Dispatchers.IO) {
                runCatching {
                    val destino = archivoNuevo()
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { entrada ->
                        destino.outputStream().use { salida -> entrada.copyTo(salida) }
                    } ?: return@runCatching null
                    destino
                }.getOrNull()
            }
            if (copia == null) {
                formulario.update { it.copy(tomando = false, error = "No pudimos abrir esa foto. Elige otra.") }
            } else {
                fotoTomada(copia)
            }
        }
    }

    fun quitarFoto(ruta: String) {
        formulario.update { it.copy(fotos = it.fotos - ruta) }
        viewModelScope.launch(Dispatchers.IO) { File(ruta).delete() }
    }

    fun elegirMomento(momento: MomentoFoto) {
        formulario.update { it.copy(momento = momento) }
    }

    fun cambiarDetalle(texto: String) {
        formulario.update { it.copy(detalle = texto.take(MaximoDetalle), error = null) }
    }

    fun cambiarNota(texto: String) {
        formulario.update { it.copy(nota = texto.take(MaximoNota), error = null) }
    }

    fun mostrarError(mensaje: String) {
        formulario.update { it.copy(error = mensaje) }
    }

    fun limpiarError() {
        formulario.update { it.copy(error = null) }
    }

    /** Publica la novedad. `alTerminar` se llama solo si quedó guardada. */
    fun publicar(alTerminar: () -> Unit) {
        val usuario = Servicios.cuentas.usuarioActual.value ?: return
        val actual = estado.value
        val ordenActual = actual.orden ?: return
        val datos = formulario.value
        if (datos.guardando || !actual.hayAlgo) return
        formulario.update { it.copy(guardando = true, error = null) }
        val marca = System.currentTimeMillis()
        val avance = NuevoAvance(
            etapa = ordenActual.etapa,
            nota = datos.nota,
            fotos = datos.fotos.mapIndexed { indice, ruta ->
                FotoAvance(id = "foto-${ordenActual.id}-$marca-$indice", momento = datos.momento, direccion = ruta)
            },
            detalleEtapa = datos.detalle.trim().takeIf { it.isNotEmpty() && it != ordenActual.detalleEtapa },
        )
        viewModelScope.launch {
            when (val resultado = Servicios.ordenes.registrarAvance(usuario, idOrden, avance)) {
                is ResultadoAvance.Hecho -> {
                    publicado = true
                    formulario.update { it.copy(guardando = false) }
                    alTerminar()
                }
                is ResultadoAvance.Fallo -> formulario.update { it.copy(guardando = false, error = resultado.mensaje) }
            }
        }
    }

    override fun onCleared() {
        // Salió sin publicar: las fotos no se usan en ningún lado.
        if (!publicado) formulario.value.fotos.forEach { File(it).delete() }
    }

    companion object {
        const val MaximoFotos = 6
        const val MaximoDetalle = 60
        const val MaximoNota = 300

        fun momentoPara(etapa: EtapaOrden): MomentoFoto = when (etapa) {
            EtapaOrden.Solicitud, EtapaOrden.Inspeccion -> MomentoFoto.Antes
            EtapaOrden.Cotizacion, EtapaOrden.Reparacion -> MomentoFoto.Durante
            EtapaOrden.Calidad, EtapaOrden.Lista, EtapaOrden.Entregada -> MomentoFoto.Despues
        }
    }
}
