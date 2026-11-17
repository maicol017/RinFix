package com.rinfix.app.seguridad

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Llave del teléfono que protege la sesión guardada.
 *
 * Vive en Android Keystore: no se puede copiar ni leer, y solo se puede usar justo después
 * de que Android confirma la huella (biometría fuerte, clase 3). Si la persona registra una
 * huella nueva, Android la invalida y hay que volver a entrar con la contraseña.
 */
object LlaveHuella {

    private const val ALMACEN = "AndroidKeyStore"
    private const val NOMBRE = "rinfix_sesion_huella"
    private const val TRANSFORMACION = "AES/GCM/NoPadding"

    /** Cifrador listo para guardar la sesión. Se completa después de confirmar la huella. */
    fun cifradorParaGuardar(): Cipher =
        Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, obtenerOCrear()) }

    /**
     * Cifrador listo para leer la sesión guardada.
     * Lanza `KeyPermanentlyInvalidatedException` si se registró una huella nueva.
     */
    fun cifradorParaLeer(vectorInicial: ByteArray): Cipher =
        Cipher.getInstance(TRANSFORMACION).apply {
            init(Cipher.DECRYPT_MODE, obtenerOCrear(), GCMParameterSpec(128, vectorInicial))
        }

    fun borrar() {
        runCatching {
            KeyStore.getInstance(ALMACEN).apply { load(null) }.deleteEntry(NOMBRE)
        }
    }

    private fun obtenerOCrear(): SecretKey {
        val almacen = KeyStore.getInstance(ALMACEN).apply { load(null) }
        (almacen.getKey(NOMBRE, null) as? SecretKey)?.let { return it }

        val especificacion = KeyGenParameterSpec.Builder(
            NOMBRE,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Cada uso exige una confirmación con biometría fuerte.
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                } else {
                    @Suppress("DEPRECATION")
                    setUserAuthenticationValidityDurationSeconds(-1)
                }
            }
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ALMACEN)
            .apply { init(especificacion) }
            .generateKey()
    }
}
