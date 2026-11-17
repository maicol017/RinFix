package com.rinfix.app.cliente

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.MaterialRin
import com.rinfix.app.datos.ServicioAnalisis
import com.rinfix.app.datos.ServicioCuentas
import com.rinfix.app.datos.ServicioGaraje
import com.rinfix.app.datos.Servicios
import com.rinfix.app.datos.SolicitudAnalisis
import com.rinfix.app.datos.Vehiculo
import com.rinfix.app.ia.Encuadre
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Cámara para analizar un rin: fotos tomadas (hasta 3), datos del rin y envío al análisis.
 * Las fotos se guardan en la carpeta temporal de la app; nunca en la galería del teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalizarLogica(aplicacion: Application) : AndroidViewModel(aplicacion) {

    data class Estado(
        val vehiculos: List<Vehiculo> = emptyList(),
        val idVehiculo: String? = null,
        val material: MaterialRin = MaterialRin.Desconocido,
        val pulgadas: Int? = null,
        val fotos: List<String> = emptyList(),
        val tomando: Boolean = false,
        val enviando: Boolean = false,
        val error: String? = null,
        val encuadre: Encuadre = Encuadre(),
        val linterna: Boolean = false,
    ) {
        val puedeTomarMas: Boolean get() = fotos.size < MaximoFotos
        val vehiculo: Vehiculo? get() = vehiculos.firstOrNull { it.id == idVehiculo }
    }

    private val cuentas: ServicioCuentas = Servicios.cuentas
    private val garaje: ServicioGaraje = Servicios.garaje
    private val analisis: ServicioAnalisis = Servicios.analisis

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    private val carpeta: File by lazy {
        File(aplicacion.cacheDir, "fotos").apply { mkdirs() }
    }

    /** Vehículo pedido al abrir la pantalla (desde «Analizar este rin»), mientras llega el garaje. */
    private var vehiculoPedido: String? = null
    private var preparada = false

    init {
        viewModelScope.launch {
            cuentas.usuarioActual
                .flatMapLatest { usuario -> if (usuario == null) flowOf(emptyList()) else garaje.vehiculos(usuario) }
                .collect { lista ->
                    _estado.update { it.copy(vehiculos = lista) }
                    elegirVehiculoInicial(lista)
                }
        }
    }

    /** Se llama al abrir la pantalla. Con `idVehiculo` deja ese vehículo elegido. */
    fun preparar(idVehiculo: String?) {
        if (preparada) return
        preparada = true
        vehiculoPedido = idVehiculo
        elegirVehiculoInicial(_estado.value.vehiculos)
    }

    private fun elegirVehiculoInicial(lista: List<Vehiculo>) {
        if (!preparada || lista.isEmpty() || _estado.value.idVehiculo != null) return
        val pedido = vehiculoPedido?.let { id -> lista.firstOrNull { it.id == id } }
        // Sin pedido, si solo hay un vehículo se elige ese; con varios, la persona decide.
        val elegido = pedido ?: lista.singleOrNull()
        vehiculoPedido = null
        if (elegido != null) elegirVehiculo(elegido.id)
    }

    /** Elegir un vehículo llena material y tamaño con los de su rin. `null` es «Otro carro». */
    fun elegirVehiculo(id: String?) {
        val vehiculo = _estado.value.vehiculos.firstOrNull { it.id == id }
        _estado.update {
            if (vehiculo == null) {
                it.copy(idVehiculo = null)
            } else {
                it.copy(idVehiculo = vehiculo.id, material = vehiculo.materialRin, pulgadas = vehiculo.pulgadasRin)
            }
        }
    }

    fun elegirMaterial(material: MaterialRin) {
        _estado.update { it.copy(material = material) }
    }

    fun elegirTamano(pulgadas: Int?) {
        _estado.update { it.copy(pulgadas = pulgadas) }
    }

    fun revisarEncuadre(encuadre: Encuadre) {
        if (encuadre != _estado.value.encuadre) _estado.update { it.copy(encuadre = encuadre) }
    }

    fun cambiarLinterna() {
        _estado.update { it.copy(linterna = !it.linterna) }
    }

    /** Archivo nuevo donde la cámara guarda la próxima foto. */
    fun archivoNuevo(): File = File(carpeta, "rin_${System.currentTimeMillis()}.jpg")

    fun empezarFoto() {
        _estado.update { it.copy(tomando = true, error = null) }
    }

    fun fotoTomada(archivo: File?) {
        _estado.update {
            if (archivo == null) {
                it.copy(tomando = false, error = "No pudimos tomar la foto. Inténtalo de nuevo.")
            } else {
                it.copy(tomando = false, fotos = (it.fotos + archivo.absolutePath).take(MaximoFotos))
            }
        }
    }

    /** Copia la foto elegida en la galería a la carpeta de la app, para poder enviarla después. */
    fun agregarDeGaleria(uri: Uri) {
        if (!_estado.value.puedeTomarMas) return
        _estado.update { it.copy(tomando = true, error = null) }
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
                _estado.update { it.copy(tomando = false, error = "No pudimos abrir esa foto. Elige otra.") }
            } else {
                fotoTomada(copia)
            }
        }
    }

    fun quitarFoto(ruta: String) {
        _estado.update { it.copy(fotos = it.fotos - ruta) }
        viewModelScope.launch(Dispatchers.IO) { File(ruta).delete() }
    }

    fun limpiarError() {
        _estado.update { it.copy(error = null) }
    }

    /** Envía las fotos al análisis. `alIniciar` recibe el id para abrir «Analizando». */
    fun analizar(alIniciar: (idAnalisis: String) -> Unit) {
        val actual = _estado.value
        val usuario = cuentas.usuarioActual.value ?: return
        if (actual.fotos.isEmpty() || actual.enviando) return
        _estado.update { it.copy(enviando = true, error = null, linterna = false) }
        viewModelScope.launch {
            try {
                val id = analisis.iniciar(
                    usuario,
                    SolicitudAnalisis(
                        fotos = actual.fotos,
                        material = actual.material,
                        pulgadas = actual.pulgadas,
                        vehiculo = actual.vehiculo?.titulo,
                    ),
                )
                _estado.update { it.copy(enviando = false) }
                alIniciar(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update {
                    it.copy(enviando = false, error = "No pudimos enviar las fotos. Revisa tu conexión e inténtalo de nuevo.")
                }
            }
        }
    }

    companion object {
        const val MaximoFotos = 3
    }
}
