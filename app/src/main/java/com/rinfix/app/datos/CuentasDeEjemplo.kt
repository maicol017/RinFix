package com.rinfix.app.datos

import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Cuentas de ejemplo mientras no existe el backend.
 *
 * Todo se guarda en el teléfono para que sobreviva al cerrar la app: la sesión abierta y las
 * cuentas creadas. De las contraseñas nuevas solo se guarda su resumen PBKDF2 con sal, nunca
 * el texto. Con Supabase, las contraseñas las maneja el servidor y esto desaparece.
 *
 * Cuentas cargadas (sus contraseñas son públicas, son de prueba):
 * - Cliente: laura@correo.com / Rines2026!
 * - Taller:  taller@rinfix.co / Taller2026!
 *
 * Código de invitación de taller: RINFIX-TALLER
 *
 * «Continuar con Google» entra como la cliente de ejemplo; el inicio real con Google llega
 * en la fase de backend. Las esperas imitan el tiempo de respuesta de un servidor.
 */
class CuentasDeEjemplo(private val preferencias: SharedPreferences) : ServicioCuentas {

    /**
     * Una cuenta. Las dos de ejemplo tienen `contrasenaPublica`; las creadas en el teléfono
     * tienen `sal` y `resumen`.
     */
    private class Cuenta(
        val usuario: Usuario,
        val contrasenaPublica: String? = null,
        val sal: ByteArray? = null,
        val resumen: ByteArray? = null,
    ) {
        fun con(nuevo: Usuario) = Cuenta(nuevo, contrasenaPublica, sal, resumen)
    }

    private val cuentas: MutableMap<String, Cuenta> = cargarCuentas()

    private val codigosValidos = mutableSetOf("RINFIX-TALLER")

    private val usuario = MutableStateFlow(leerSesion())
    override val usuarioActual: StateFlow<Usuario?> = usuario.asStateFlow()

    override suspend fun entrar(correo: String, contrasena: String): ResultadoCuenta {
        delay(700)
        val cuenta = cuentas[correo.trim().lowercase()]
        if (cuenta == null || !coincide(cuenta, contrasena)) {
            return ResultadoCuenta.Fallo("El correo o la contraseña no coinciden.")
        }
        abrirSesion(cuenta.usuario)
        return ResultadoCuenta.Exito(cuenta.usuario)
    }

    override suspend fun crearCuenta(nombre: String, correo: String, contrasena: String): ResultadoCuenta {
        delay(800)
        val clave = correo.trim().lowercase()
        if (clave in cuentas) {
            return ResultadoCuenta.Fallo("Ya existe una cuenta con ese correo. Inicia sesión.")
        }
        val nuevo = Usuario(id = nuevoId("cliente"), nombre = nombre.trim(), correo = clave, rol = Rol.Cliente)
        guardarNueva(nuevo, contrasena)
        abrirSesion(nuevo)
        return ResultadoCuenta.Exito(nuevo)
    }

    override suspend fun entrarConGoogle(): ResultadoCuenta {
        delay(700)
        val laura = cuentas.getValue(CorreoLaura).usuario
        abrirSesion(laura)
        return ResultadoCuenta.Exito(laura)
    }

    override suspend fun unirseATaller(
        codigo: String,
        nombre: String,
        correo: String,
        contrasena: String,
    ): ResultadoCuenta {
        delay(800)
        if (codigo.trim().uppercase() !in codigosValidos) {
            return ResultadoCuenta.Fallo("Ese código no existe o ya se usó. Pídele uno nuevo al administrador del taller.")
        }
        val clave = correo.trim().lowercase()
        if (clave in cuentas) {
            return ResultadoCuenta.Fallo("Ya existe una cuenta con ese correo. Inicia sesión.")
        }
        val nuevo = Usuario(
            id = nuevoId("taller"),
            nombre = nombre.trim(),
            correo = clave,
            rol = Rol.Taller,
            taller = Catalogo.Taller,
        )
        guardarNueva(nuevo, contrasena)
        abrirSesion(nuevo)
        return ResultadoCuenta.Exito(nuevo)
    }

    override suspend fun actualizarDatos(nombre: String, telefono: String?): ResultadoCuenta {
        delay(600)
        val actual = usuario.value ?: return ResultadoCuenta.Fallo("Tu sesión se cerró. Vuelve a entrar.")
        val nuevo = actual.copy(nombre = nombre.trim(), telefono = telefono?.takeIf { it.isNotBlank() })
        cuentas[actual.correo]?.let { cuentas[actual.correo] = it.con(nuevo) }
        guardarCuentas()
        abrirSesion(nuevo)
        return ResultadoCuenta.Exito(nuevo)
    }

    /** Cierra la sesión. Las cuentas creadas se quedan guardadas para volver a entrar. */
    override suspend fun salir() {
        delay(200)
        preferencias.edit { ClavesSesion.forEach { remove(it) } }
        usuario.value = null
    }

    // ---------- Contraseñas ----------

    private suspend fun guardarNueva(nuevo: Usuario, contrasena: String) {
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        cuentas[nuevo.correo] = Cuenta(nuevo, sal = sal, resumen = resumir(contrasena, sal))
        guardarCuentas()
    }

    private suspend fun coincide(cuenta: Cuenta, contrasena: String): Boolean {
        cuenta.contrasenaPublica?.let { return it == contrasena }
        val sal = cuenta.sal ?: return false
        val resumen = cuenta.resumen ?: return false
        return MessageDigest.isEqual(resumir(contrasena, sal), resumen)
    }

    /** PBKDF2 tarda a propósito; se hace fuera del hilo de la pantalla. */
    private suspend fun resumir(contrasena: String, sal: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        val datos = PBEKeySpec(contrasena.toCharArray(), sal, Iteraciones, 256)
        try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(datos).encoded
        } finally {
            datos.clearPassword()
        }
    }

    private fun nuevoId(prefijo: String) = "$prefijo-${UUID.randomUUID().toString().take(8)}"

    // ---------- Guardado en el teléfono ----------

    /** Las de ejemplo, con los datos que la persona les haya cambiado, más las creadas. */
    private fun cargarCuentas(): MutableMap<String, Cuenta> {
        val todas = cuentasDeEjemplo().associateBy { it.usuario.correo }.toMutableMap()
        val texto = preferencias.getString(CUENTAS, null) ?: return todas
        try {
            val lista = JSONArray(texto)
            for (i in 0 until lista.length()) {
                val json = lista.getJSONObject(i)
                val guardado = usuarioDe(json) ?: continue
                val deEjemplo = todas[guardado.correo]
                when {
                    deEjemplo != null -> todas[guardado.correo] = deEjemplo.con(guardado)
                    json.has(SAL) && json.has(RESUMEN) -> todas[guardado.correo] = Cuenta(
                        usuario = guardado,
                        sal = Base64.decode(json.getString(SAL), Base64.NO_WRAP),
                        resumen = Base64.decode(json.getString(RESUMEN), Base64.NO_WRAP),
                    )
                }
            }
        } catch (e: JSONException) {
            // Si el guardado se dañó, quedan solo las cuentas de ejemplo.
        } catch (e: IllegalArgumentException) {
            // Base64 inválido: igual.
        }
        return todas
    }

    private fun guardarCuentas() {
        val lista = JSONArray()
        cuentas.values.forEach { cuenta ->
            val u = cuenta.usuario
            val json = JSONObject()
                .put(ID, u.id)
                .put(NOMBRE, u.nombre)
                .put(CORREO, u.correo)
                .put(ROL, u.rol.name)
                .put(TALLER, u.taller)
                .put(TELEFONO, u.telefono)
            if (cuenta.sal != null && cuenta.resumen != null) {
                json.put(SAL, Base64.encodeToString(cuenta.sal, Base64.NO_WRAP))
                json.put(RESUMEN, Base64.encodeToString(cuenta.resumen, Base64.NO_WRAP))
            }
            lista.put(json)
        }
        preferencias.edit { putString(CUENTAS, lista.toString()) }
    }

    private fun usuarioDe(json: JSONObject): Usuario? {
        val rol = Rol.entries.firstOrNull { it.name == json.optString(ROL) } ?: return null
        val correo = json.optString(CORREO).takeIf { it.isNotBlank() } ?: return null
        return Usuario(
            id = json.optString(ID).takeIf { it.isNotBlank() } ?: return null,
            nombre = json.optString(NOMBRE),
            correo = correo,
            rol = rol,
            taller = json.optString(TALLER).takeIf { it.isNotBlank() && json.has(TALLER) },
            telefono = json.optString(TELEFONO).takeIf { it.isNotBlank() && json.has(TELEFONO) },
        )
    }

    private fun abrirSesion(nuevo: Usuario) {
        preferencias.edit {
            putString(SESION_ID, nuevo.id)
            putString(SESION_NOMBRE, nuevo.nombre)
            putString(SESION_CORREO, nuevo.correo)
            putString(SESION_ROL, nuevo.rol.name)
            putString(SESION_TALLER, nuevo.taller)
            putString(SESION_TELEFONO, nuevo.telefono)
        }
        usuario.value = nuevo
    }

    private fun leerSesion(): Usuario? {
        val id = preferencias.getString(SESION_ID, null) ?: return null
        val rol = Rol.entries.firstOrNull { it.name == preferencias.getString(SESION_ROL, null) } ?: return null
        return Usuario(
            id = id,
            nombre = preferencias.getString(SESION_NOMBRE, null).orEmpty(),
            correo = preferencias.getString(SESION_CORREO, null).orEmpty(),
            rol = rol,
            taller = preferencias.getString(SESION_TALLER, null),
            telefono = preferencias.getString(SESION_TELEFONO, null),
        )
    }

    private companion object {
        const val CorreoLaura = "laura@correo.com"
        const val Iteraciones = 60_000

        // Sesión abierta
        const val SESION_ID = "sesion_id"
        const val SESION_NOMBRE = "sesion_nombre"
        const val SESION_CORREO = "sesion_correo"
        const val SESION_ROL = "sesion_rol"
        const val SESION_TALLER = "sesion_taller"
        const val SESION_TELEFONO = "sesion_telefono"
        val ClavesSesion = listOf(SESION_ID, SESION_NOMBRE, SESION_CORREO, SESION_ROL, SESION_TALLER, SESION_TELEFONO)

        // Cuentas guardadas
        const val CUENTAS = "cuentas_guardadas"
        const val ID = "id"
        const val NOMBRE = "nombre"
        const val CORREO = "correo"
        const val ROL = "rol"
        const val TALLER = "taller"
        const val TELEFONO = "telefono"
        const val SAL = "sal"
        const val RESUMEN = "resumen"

        fun cuentasDeEjemplo() = listOf(
            Cuenta(
                Usuario(id = "cliente-1", nombre = "Laura Méndez", correo = CorreoLaura, rol = Rol.Cliente),
                contrasenaPublica = "Rines2026!",
            ),
            Cuenta(
                Usuario(
                    id = "taller-1",
                    nombre = "Carlos Rueda",
                    correo = "taller@rinfix.co",
                    rol = Rol.Taller,
                    taller = Catalogo.Taller,
                ),
                contrasenaPublica = "Taller2026!",
            ),
        )
    }
}
