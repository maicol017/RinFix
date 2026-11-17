package com.rinfix.app.seguridad

import android.content.Context
import android.util.Base64
import androidx.core.content.edit

/**
 * Sesión guardada en el teléfono para el desbloqueo con huella.
 *
 * Lo que se guarda va cifrado con [LlaveHuella]; sin la huella no se puede leer.
 * El nombre de pila se guarda aparte, sin cifrar, solo para saludar («Hola de nuevo, Laura»).
 * Aquí nunca se guarda nada biométrico: eso lo maneja Android.
 */
class SesionGuardada(contexto: Context) {

    data class Datos(val nombre: String, val cifrado: ByteArray, val vectorInicial: ByteArray)

    private val preferencias = contexto.getSharedPreferences("rinfix_seguridad", Context.MODE_PRIVATE)

    fun guardar(nombre: String, cifrado: ByteArray, vectorInicial: ByteArray) {
        preferencias.edit {
            putString(NOMBRE, nombre)
            putString(CIFRADO, Base64.encodeToString(cifrado, Base64.NO_WRAP))
            putString(VECTOR, Base64.encodeToString(vectorInicial, Base64.NO_WRAP))
        }
    }

    fun leer(): Datos? {
        val nombre = preferencias.getString(NOMBRE, null) ?: return null
        val cifrado = preferencias.getString(CIFRADO, null) ?: return null
        val vector = preferencias.getString(VECTOR, null) ?: return null
        return Datos(nombre, Base64.decode(cifrado, Base64.NO_WRAP), Base64.decode(vector, Base64.NO_WRAP))
    }

    fun existe(): Boolean = preferencias.contains(CIFRADO)

    fun borrar() {
        preferencias.edit {
            remove(NOMBRE)
            remove(CIFRADO)
            remove(VECTOR)
        }
    }

    /** Para no ofrecer la huella cada vez que alguien entra. */
    fun huellaOfrecida(idUsuario: String): Boolean = preferencias.getBoolean(OFRECIDA + idUsuario, false)

    fun marcarHuellaOfrecida(idUsuario: String, ofrecida: Boolean = true) {
        preferencias.edit { putBoolean(OFRECIDA + idUsuario, ofrecida) }
    }

    private companion object {
        const val NOMBRE = "nombre"
        const val CIFRADO = "sesion_cifrada"
        const val VECTOR = "vector_inicial"
        const val OFRECIDA = "huella_ofrecida_"
    }
}
