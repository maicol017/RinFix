package com.rinfix.app.datos

import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Diagnósticos de ejemplo. Todos llevan `deEjemplo = true` para que la app los marque:
 * no son resultados reales de un análisis.
 */
class AnalisisDeEjemplo(ahora: LocalDateTime = LocalDateTime.now()) : ServicioAnalisis {

    /** Diagnósticos por id de usuario. */
    private val porUsuario = MutableStateFlow(diagnosticosIniciales(ahora))

    override fun historial(usuario: Usuario): Flow<List<Diagnostico>> =
        porUsuario.map { mapa -> mapa[usuario.id].orEmpty().sortedByDescending { it.fecha } }

    internal companion object {
        fun diagnosticosIniciales(ahora: LocalDateTime): Map<String, List<Diagnostico>> = mapOf(
            "cliente-1" to listOf(
                Diagnostico(
                    id = "diagnostico-1",
                    fecha = ahora.toLocalDate().minusDays(5).atTime(15, 20),
                    vehiculo = "Mazda 3 2021",
                    material = MaterialRin.Aleacion,
                    pulgadas = 17,
                    danos = listOf(
                        Dano(
                            tipo = TipoDano.Rayon,
                            descripcion = "Rayón en el borde exterior",
                            gravedad = Gravedad.Moderada,
                            confianza = 0.86f,
                            caja = Caja(0.58f, 0.18f, 0.24f, 0.24f),
                        ),
                        Dano(
                            tipo = TipoDano.Deformacion,
                            descripcion = "Posible deformación leve del borde",
                            gravedad = Gravedad.Leve,
                            confianza = 0.58f,
                            caja = Caja(0.2f, 0.62f, 0.2f, 0.2f),
                        ),
                    ),
                    cotizacion = listOf(
                        LineaCotizacion("Reparación de rayones y pintura", 120_000, 170_000),
                        LineaCotizacion("Enderezado del borde", 60_000, 90_000),
                    ),
                    diasMinimos = 2,
                    diasMaximos = 3,
                    recomendaciones = listOf(
                        "Pide revisar el balanceo después del enderezado.",
                        "Evita andenes y huecos hasta la reparación.",
                    ),
                    requiereInspeccion = false,
                    deEjemplo = true,
                ),
            ),
        )
    }
}
