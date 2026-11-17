package com.rinfix.app.seguridad

import android.content.Context
import android.util.Base64
import androidx.core.content.edit

/**
 * Candado del desbloqueo con huella.
 *
 * Al activarlo se guarda el id del usuario cifrado con [LlaveHuella]. Al volver a abrir la app,
 * la huella descifra ese id y se compara con la sesión abierta: si coincide, se desbloquea.
 * Así el candado queda atado a esa cuenta, y una huella registrada después en el teléfono
 * no sirve, porque Android anula la llave.
 *
 * Aquí nunca se guarda nada biométrico: eso lo maneja Android.
 */
class SesionGuardada(contexto: Context) {

    class Candado(val cifrado: ByteArray, val vectorInicial: ByteArray)

    private val preferencias = contexto.getSharedPreferences("rinfix_seguridad", Context.MODE_PRIVATE)

    fun guardar(cifrado: ByteArray, vectorInicial: ByteArray) {
        preferencias.edit {
            putString(CIFRADO, Base64.encodeToString(cifrado, Base64.NO_WRAP))
            putString(VECTOR, Base64.encodeToString(vectorInicial, Base64.NO_WRAP))
        }
    }

    fun leer(): Candado? {
        val cifrado = preferencias.getString(CIFRADO, null) ?: return null
        val vector = preferencias.getString(VECTOR, null) ?: return null
        return Candado(Base64.decode(cifrado, Base64.NO_WRAP), Base64.decode(vector, Base64.NO_WRAP))
    }

    /** `true` si la persona activó el desbloqueo con huella. */
    fun existe(): Boolean = preferencias.contains(CIFRADO)

    fun borrar() {
        preferencias.edit {
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
        const val CIFRADO = "candado_cifrado"
        const val VECTOR = "candado_vector"
        const val OFRECIDA = "huella_ofrecida_"
    }
}
