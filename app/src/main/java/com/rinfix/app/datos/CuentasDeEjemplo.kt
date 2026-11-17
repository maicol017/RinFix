package com.rinfix.app.datos

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cuentas de ejemplo mientras no existe el backend.
 *
 * La sesión abierta se guarda en el teléfono para que siga activa al cerrar la app, como hará
 * Supabase. Las cuentas nuevas, en cambio, viven en memoria: al cerrar la app del todo se
 * pierden (la sesión sigue abierta, pero no se puede volver a entrar con esa contraseña).
 *
 * Cuentas cargadas:
 * - Cliente: laura@correo.com / Rines2026!
 * - Taller:  taller@rinfix.co / Taller2026!
 *
 * Código de invitación de taller: RINFIX-TALLER
 *
 * «Continuar con Google» entra como la cliente de ejemplo; el inicio real con Google llega
 * en la fase de backend. Las esperas imitan el tiempo de respuesta de un servidor.
 */
class CuentasDeEjemplo(private val preferencias: SharedPreferences) : ServicioCuentas {

    private data class CuentaGuardada(val usuario: Usuario, val contrasena: String)

    private val cuentas = mutableMapOf(
        "laura@correo.com" to CuentaGuardada(
            Usuario(id = "cliente-1", nombre = "Laura Méndez", correo = "laura@correo.com", rol = Rol.Cliente),
            contrasena = "Rines2026!",
        ),
        "taller@rinfix.co" to CuentaGuardada(
            Usuario(
                id = "taller-1",
                nombre = "Carlos Rueda",
                correo = "taller@rinfix.co",
                rol = Rol.Taller,
                taller = "Taller RinFix Cabecera",
            ),
            contrasena = "Taller2026!",
        ),
    )

    private val codigosValidos = mutableSetOf("RINFIX-TALLER")

    private val usuario = MutableStateFlow(leerSesion())
    override val usuarioActual: StateFlow<Usuario?> = usuario.asStateFlow()

    override suspend fun entrar(correo: String, contrasena: String): ResultadoCuenta {
        delay(900)
        val guardada = cuentas[correo.trim().lowercase()]
        if (guardada == null || guardada.contrasena != contrasena) {
            return ResultadoCuenta.Fallo("El correo o la contraseña no coinciden.")
        }
        abrirSesion(guardada.usuario)
        return ResultadoCuenta.Exito(guardada.usuario)
    }

    override suspend fun crearCuenta(nombre: String, correo: String, contrasena: String): ResultadoCuenta {
        delay(1100)
        val clave = correo.trim().lowercase()
        if (clave in cuentas) {
            return ResultadoCuenta.Fallo("Ya existe una cuenta con ese correo. Inicia sesión.")
        }
        val nuevo = Usuario(id = "cliente-${cuentas.size + 1}", nombre = nombre, correo = clave, rol = Rol.Cliente)
        cuentas[clave] = CuentaGuardada(nuevo, contrasena)
        abrirSesion(nuevo)
        return ResultadoCuenta.Exito(nuevo)
    }

    override suspend fun entrarConGoogle(): ResultadoCuenta {
        delay(700)
        val laura = cuentas.getValue("laura@correo.com").usuario
        abrirSesion(laura)
        return ResultadoCuenta.Exito(laura)
    }

    override suspend fun unirseATaller(
        codigo: String,
        nombre: String,
        correo: String,
        contrasena: String,
    ): ResultadoCuenta {
        delay(1000)
        if (codigo.trim().uppercase() !in codigosValidos) {
            return ResultadoCuenta.Fallo("Ese código no existe o ya se usó. Pídele uno nuevo al administrador del taller.")
        }
        val clave = correo.trim().lowercase()
        if (clave in cuentas) {
            return ResultadoCuenta.Fallo("Ya existe una cuenta con ese correo. Inicia sesión.")
        }
        val nuevo = Usuario(
            id = "taller-${cuentas.size + 1}",
            nombre = nombre,
            correo = clave,
            rol = Rol.Taller,
            taller = "Taller RinFix Cabecera",
        )
        cuentas[clave] = CuentaGuardada(nuevo, contrasena)
        abrirSesion(nuevo)
        return ResultadoCuenta.Exito(nuevo)
    }

    override suspend fun salir() {
        delay(200)
        preferencias.edit { clear() }
        usuario.value = null
    }

    private fun abrirSesion(nuevo: Usuario) {
        preferencias.edit {
            putString(ID, nuevo.id)
            putString(NOMBRE, nuevo.nombre)
            putString(CORREO, nuevo.correo)
            putString(ROL, nuevo.rol.name)
            putString(TALLER, nuevo.taller)
        }
        usuario.value = nuevo
    }

    private fun leerSesion(): Usuario? {
        val id = preferencias.getString(ID, null) ?: return null
        val rol = Rol.entries.firstOrNull { it.name == preferencias.getString(ROL, null) } ?: return null
        return Usuario(
            id = id,
            nombre = preferencias.getString(NOMBRE, null).orEmpty(),
            correo = preferencias.getString(CORREO, null).orEmpty(),
            rol = rol,
            taller = preferencias.getString(TALLER, null),
        )
    }

    private companion object {
        const val ID = "sesion_id"
        const val NOMBRE = "sesion_nombre"
        const val CORREO = "sesion_correo"
        const val ROL = "sesion_rol"
        const val TALLER = "sesion_taller"
    }
}
