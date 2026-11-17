package com.rinfix.app.datos

import kotlinx.coroutines.flow.StateFlow

/**
 * Lo que la app necesita para manejar cuentas, sin importar quién lo resuelva.
 * Hoy lo cumple [CuentasDeEjemplo]; cuando se conecte el backend lo cumplirá Supabase.
 */
interface ServicioCuentas {

    /**
     * Usuario con sesión iniciada, o `null` si nadie ha entrado.
     *
     * La sesión se conserva al cerrar la app: al volver a abrirla, la persona sigue dentro.
     * Si activó la huella, la app pide desbloquear antes de mostrar nada.
     */
    val usuarioActual: StateFlow<Usuario?>

    suspend fun entrar(correo: String, contrasena: String): ResultadoCuenta

    suspend fun crearCuenta(nombre: String, correo: String, contrasena: String): ResultadoCuenta

    suspend fun entrarConGoogle(): ResultadoCuenta

    /** Crea la cuenta de un trabajador con el código de invitación de su taller. */
    suspend fun unirseATaller(
        codigo: String,
        nombre: String,
        correo: String,
        contrasena: String,
    ): ResultadoCuenta

    suspend fun salir()
}

/** Resultado de una operación de cuenta. Los mensajes de error ya están listos para mostrarse. */
sealed interface ResultadoCuenta {
    data class Exito(val usuario: Usuario) : ResultadoCuenta
    data class Fallo(val mensaje: String) : ResultadoCuenta
}
