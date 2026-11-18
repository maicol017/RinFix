package com.rinfix.app.ia

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Deja una foto lista para enviarla al análisis: derecha (según cómo se tomó), con máximo
 * 1280 px por lado y en JPEG liviano. Así pesa unos cientos de KB en vez de varios MB,
 * sube rápido con datos móviles y Claude la ve con detalle suficiente.
 */
object PrepararFoto {

    const val LadoMaximo = 1280
    private const val Calidad = 82

    /** La foto en base64 (JPEG), o `null` si no se pudo leer. */
    fun comoBase64(ruta: String): String? = runCatching {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, medidas)
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null

        // Primero se reduce al leer (rápido y con poca memoria) y luego al tamaño exacto.
        var muestra = 1
        while (max(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= LadoMaximo) muestra *= 2
        val leida = BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = muestra })
            ?: return null

        val escala = minOf(1f, LadoMaximo.toFloat() / max(leida.width, leida.height))
        val matriz = Matrix().apply {
            postScale(escala, escala)
            postRotate(giroSegunExif(ruta))
        }
        val lista = Bitmap.createBitmap(leida, 0, 0, leida.width, leida.height, matriz, true)

        val bytes = ByteArrayOutputStream().use { salida ->
            lista.compress(Bitmap.CompressFormat.JPEG, Calidad, salida)
            salida.toByteArray()
        }
        if (lista !== leida) lista.recycle()
        leida.recycle()
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    }.getOrNull()

    private fun giroSegunExif(ruta: String): Float =
        when (ExifInterface(ruta).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

    /** Tamaño aproximado en KB del texto base64, para mostrarlo mientras se envía. */
    fun kilobytes(base64: String): Int = (base64.length * 3 / 4 / 1024f).roundToInt()
}
