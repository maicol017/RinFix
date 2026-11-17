package com.rinfix.app.navegacion

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinfix.app.datos.ServicioCuentas
import com.rinfix.app.datos.Servicios
import com.rinfix.app.datos.Usuario
import com.rinfix.app.seguridad.Huella
import com.rinfix.app.seguridad.LlaveHuella
import com.rinfix.app.seguridad.SesionGuardada
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Decide qué ve la persona al abrir RinFix:
 *
 * - Sin sesión → pantallas de cuenta.
 * - Con sesión y huella activada → desbloqueo (al abrir la app desde cero, o al volver
 *   después de 5 minutos fuera).
 * - Recién entró y aún no se le ofreció la huella → «Activar huella».
 * - Si no → la app de su rol.
 *
 * Vive mientras la app está abierta: girar el teléfono no vuelve a bloquear, pero si Android
 * cierra la app para liberar memoria, al volver se pide la huella otra vez.
 */
class PuertaLogica(aplicacion: Application) : AndroidViewModel(aplicacion) {

    enum class Etapa { Cuenta, Desbloqueo, OfrecerHuella, App }

    data class Estado(
        val usuario: Usuario? = null,
        val bloqueada: Boolean = false,
        val ofrecerHuella: Boolean = false,
        val activando: Boolean = false,
        val errorActivacion: String? = null,
        val errorDesbloqueo: String? = null,
        val huellaInvalida: Boolean = false,
        /** Correo para «Iniciar sesión» cuando la persona eligió «Usar contraseña». */
        val correoSugerido: String? = null,
    ) {
        val etapa: Etapa
            get() = when {
                usuario == null -> Etapa.Cuenta
                bloqueada -> Etapa.Desbloqueo
                ofrecerHuella -> Etapa.OfrecerHuella
                else -> Etapa.App
            }
    }

    private val cuentas: ServicioCuentas = Servicios.cuentas
    private val candado = SesionGuardada(aplicacion)

    private val _estado = MutableStateFlow(estadoInicial())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    /** Para abrir la ventana de huella sola una única vez por cada bloqueo. */
    private var yaSePidioSola = false
    private var salioEn: Long? = null

    init {
        viewModelScope.launch {
            cuentas.usuarioActual.collect { actual -> alCambiarUsuario(actual) }
        }
    }

    // ---- Activar ----

    /** Prepara una llave nueva. Devuelve `null` (y deja el error listo) si el teléfono no pudo. */
    fun empezarActivacion(): Cipher? {
        LlaveHuella.borrar()
        return try {
            val cifrador = LlaveHuella.cifradorParaGuardar()
            _estado.update { it.copy(activando = true, errorActivacion = null) }
            cifrador
        } catch (e: Exception) {
            // El Keystore lanza varios tipos según el fabricante; todos significan lo mismo aquí.
            LlaveHuella.borrar()
            _estado.update { it.copy(activando = false, errorActivacion = ErrorLlave) }
            null
        }
    }

    /** Android confirmó la huella: se guarda el candado. Devuelve `true` si quedó activado. */
    fun huellaActivada(cifrador: Cipher): Boolean {
        val usuario = _estado.value.usuario ?: return false
        return try {
            val cifrado = cifrador.doFinal(usuario.id.toByteArray(Charsets.UTF_8))
            candado.guardar(dueno = usuario.id, cifrado = cifrado, vectorInicial = cifrador.iv)
            candado.marcarHuellaOfrecida(usuario.id)
            _estado.update { it.copy(activando = false, ofrecerHuella = false, errorActivacion = null) }
            true
        } catch (e: GeneralSecurityException) {
            olvidarHuella()
            _estado.update { it.copy(activando = false, errorActivacion = ErrorLlave) }
            false
        }
    }

    fun errorAlActivar(mensaje: String) {
        _estado.update { it.copy(activando = false, errorActivacion = mensaje) }
    }

    fun activacionCancelada() {
        _estado.update { it.copy(activando = false) }
    }

    /** «Ahora no»: no se vuelve a ofrecer a esta cuenta en este teléfono. */
    fun omitirHuella() {
        val usuario = _estado.value.usuario ?: return
        candado.marcarHuellaOfrecida(usuario.id)
        _estado.update { it.copy(ofrecerHuella = false, activando = false, errorActivacion = null) }
    }

    // ---- Desbloquear ----

    /** `true` solo la primera vez después de cada bloqueo. */
    fun debePedirSola(): Boolean {
        if (yaSePidioSola) return false
        yaSePidioSola = true
        return true
    }

    /** Prepara la llave para leer el candado. Si Android la anuló, desactiva la huella. */
    fun empezarDesbloqueo(): Cipher? {
        val guardado = candado.leer()
        if (guardado == null) {
            marcarHuellaInvalida()
            return null
        }
        return try {
            val cifrador = LlaveHuella.cifradorParaLeer(guardado.vectorInicial)
            _estado.update { it.copy(errorDesbloqueo = null) }
            cifrador
        } catch (e: GeneralSecurityException) {
            // Incluye la llave anulada por una huella nueva (KeyPermanentlyInvalidatedException).
            marcarHuellaInvalida()
            null
        }
    }

    /** Android confirmó la huella: el candado debe abrir a la misma cuenta que tiene la sesión. */
    fun desbloqueado(cifrador: Cipher) {
        val usuario = _estado.value.usuario
        val guardado = candado.leer()
        val id = try {
            guardado?.let { String(cifrador.doFinal(it.cifrado), Charsets.UTF_8) }
        } catch (e: GeneralSecurityException) {
            null
        }
        if (usuario != null && id == usuario.id) {
            _estado.update { it.copy(bloqueada = false, errorDesbloqueo = null) }
        } else {
            marcarHuellaInvalida()
        }
    }

    fun errorAlDesbloquear(mensaje: String) {
        _estado.update { it.copy(errorDesbloqueo = mensaje) }
    }

    // ---- Sesión ----

    /** Sale y abre «Iniciar sesión» con su correo. La huella sigue activada para esta cuenta. */
    fun usarContrasena() {
        val correo = _estado.value.usuario?.correo
        _estado.update { it.copy(correoSugerido = correo) }
        viewModelScope.launch { cuentas.salir() }
    }

    /** «No soy …»: sale, quita la huella y vuelve a la bienvenida. */
    fun cambiarCuenta() = cerrarSesion()

    /** Cerrar sesión siempre quita la huella del teléfono. */
    fun cerrarSesion() {
        olvidarHuella()
        _estado.update { it.copy(correoSugerido = null) }
        viewModelScope.launch { cuentas.salir() }
    }

    // ---- Salir y volver a la app ----

    fun alSalirDeLaApp() {
        salioEn = SystemClock.elapsedRealtime()
    }

    fun alVolverALaApp() {
        val salio = salioEn ?: return
        salioEn = null
        val fuera = SystemClock.elapsedRealtime() - salio
        if (_estado.value.etapa == Etapa.App && candado.existe() && fuera >= TiempoParaBloquear) {
            yaSePidioSola = false
            _estado.update { it.copy(bloqueada = true, errorDesbloqueo = null) }
        }
    }

    // ---- Interno ----

    private fun estadoInicial(): Estado {
        val actual = cuentas.usuarioActual.value
        if (actual != null && candado.existe() && candado.dueno() != actual.id) olvidarHuella()
        return Estado(usuario = actual, bloqueada = actual != null && candado.existe())
    }

    private fun alCambiarUsuario(actual: Usuario?) {
        val antes = _estado.value.usuario
        when {
            actual == null -> _estado.update { Estado(correoSugerido = it.correoSugerido) }

            // Alguien acaba de entrar con contraseña, Google o código de taller.
            antes == null -> {
                if (candado.existe() && candado.dueno() != actual.id) olvidarHuella()
                val ofrecer = !candado.existe() &&
                    !candado.huellaOfrecida(actual.id) &&
                    Huella.disponible(getApplication<Application>())
                _estado.value = Estado(usuario = actual, ofrecerHuella = ofrecer)
            }

            else -> _estado.update { it.copy(usuario = actual) }
        }
    }

    /** La llave ya no sirve: se quita y se vuelve a ofrecer la huella al entrar con contraseña. */
    private fun marcarHuellaInvalida() {
        _estado.value.usuario?.let { candado.marcarHuellaOfrecida(it.id, ofrecida = false) }
        olvidarHuella()
        _estado.update { it.copy(huellaInvalida = true, errorDesbloqueo = null) }
    }

    private fun olvidarHuella() {
        candado.borrar()
        LlaveHuella.borrar()
    }

    private companion object {
        const val TiempoParaBloquear = 5 * 60 * 1000L
        const val ErrorLlave = "No pudimos preparar la llave del teléfono. Inténtalo de nuevo o hazlo luego desde Perfil."
    }
}
