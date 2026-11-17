package com.rinfix.app.seguridad

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

/**
 * Ventana de huella del sistema (BiometricPrompt).
 *
 * Solo acepta biometría fuerte (clase 3), la única que puede liberar una llave del Keystore.
 * RinFix nunca recibe la huella: Android solo responde «aprobado» con el cifrador listo,
 * o un error.
 */
object Huella {

    /**
     * `true` si el teléfono tiene un sensor fuerte y al menos una huella registrada.
     *
     * Desde Android 9: en Android 8 la biblioteca dibuja su propia ventana con estilos de
     * AppCompat, que RinFix no usa, y se cerraría la app.
     */
    fun disponible(contexto: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            BiometricManager.from(contexto).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Pide la huella y, si se confirma, entrega el cifrador desbloqueado.
     *
     * - `alAlterno` se llama con el botón secundario de la ventana (por ejemplo «Usar contraseña»).
     * - Si la huella no coincide, el sistema lo muestra y deja reintentar; aquí no hace falta nada.
     */
    fun pedir(
        actividad: FragmentActivity,
        titulo: String,
        subtitulo: String,
        textoAlterno: String,
        cifrador: Cipher,
        alExito: (Cipher) -> Unit,
        alError: (String) -> Unit,
        alAlterno: () -> Unit,
        alCancelar: () -> Unit,
    ) {
        val ventana = BiometricPrompt(
            actividad,
            ContextCompat.getMainExecutor(actividad),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                    val desbloqueado = resultado.cryptoObject?.cipher
                    if (desbloqueado != null) {
                        alExito(desbloqueado)
                    } else {
                        alError("No pudimos verificar la llave del teléfono. Entra con tu contraseña.")
                    }
                }

                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    when (codigo) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> alAlterno()
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> alCancelar()
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                            alError("Demasiados intentos. Espera un momento o entra con tu contraseña.")
                        else -> alError(mensaje.toString())
                    }
                }
            },
        )
        val informacion = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle(subtitulo)
            .setNegativeButtonText(textoAlterno)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()
        ventana.authenticate(informacion, BiometricPrompt.CryptoObject(cifrador))
    }
}
