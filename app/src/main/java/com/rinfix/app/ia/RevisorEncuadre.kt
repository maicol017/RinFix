package com.rinfix.app.ia

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/** Qué tan buena es la luz para fotografiar el rin. */
enum class Luz(val mensaje: String) {
    Revisando("Revisando la luz"),
    Poca("Poca luz: prende la linterna"),
    Buena("Luz suficiente"),
    Reflejo("Mucho brillo: busca sombra"),
}

/**
 * Cómo se ve la imagen de la cámara en este momento.
 * Más adelante sumará si el rin está completo dentro del círculo (modelo de detección).
 */
data class Encuadre(val luz: Luz = Luz.Revisando)

/**
 * Revisa la imagen de la cámara en el teléfono, sin internet, unas dos veces por segundo.
 *
 * Por ahora mide la luz: promedia el brillo de una muestra de píxeles del canal de luminancia.
 * `alRevisar` se llama desde el hilo de la cámara; quien lo usa decide cómo pasarlo a la pantalla.
 */
class RevisorEncuadre(private val alRevisar: (Encuadre) -> Unit) : ImageAnalysis.Analyzer {

    private var ultimaRevision = 0L

    override fun analyze(imagen: ImageProxy) {
        imagen.use {
            val ahora = SystemClock.elapsedRealtime()
            if (ahora - ultimaRevision < IntervaloMs) return
            ultimaRevision = ahora
            alRevisar(Encuadre(luz = luzSegun(brilloPromedio(it))))
        }
    }

    /** Brillo de 0 (negro) a 255 (blanco), con un píxel de cada 16 en cada sentido. */
    private fun brilloPromedio(imagen: ImageProxy): Int {
        val plano = imagen.planes[0]
        val datos = plano.buffer
        val fila = plano.rowStride
        val pixel = plano.pixelStride
        var suma = 0L
        var cuenta = 0
        var y = 0
        while (y < imagen.height) {
            var x = 0
            while (x < imagen.width) {
                suma += datos.get(y * fila + x * pixel).toInt() and 0xFF
                cuenta++
                x += Paso
            }
            y += Paso
        }
        return if (cuenta == 0) 0 else (suma / cuenta).toInt()
    }

    private fun luzSegun(brillo: Int): Luz = when {
        brillo < BrilloMinimo -> Luz.Poca
        brillo > BrilloMaximo -> Luz.Reflejo
        else -> Luz.Buena
    }

    private companion object {
        const val IntervaloMs = 400L
        const val Paso = 16
        const val BrilloMinimo = 60
        const val BrilloMaximo = 225
    }
}
