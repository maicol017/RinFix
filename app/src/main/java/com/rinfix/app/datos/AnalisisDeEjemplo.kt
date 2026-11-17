package com.rinfix.app.datos

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Análisis de ejemplo mientras no existe el backend.
 *
 * Recorre los cuatro pasos con los tiempos que tendrá el análisis real y siempre llega al mismo
 * hallazgo (un rayón y una posible deformación), con el precio calculado por el [Catalogo] según
 * el tamaño y el material elegidos. Todo lleva `deEjemplo = true`: los daños NO salen de la foto.
 */
class AnalisisDeEjemplo(ahora: LocalDateTime = LocalDateTime.now()) : ServicioAnalisis {

    /** Diagnósticos por id de usuario. */
    private val porUsuario = MutableStateFlow(diagnosticosIniciales(ahora))

    /** Análisis iniciados, por id. Siguen corriendo aunque la persona cambie de pantalla. */
    private val avances = ConcurrentHashMap<String, MutableStateFlow<AvanceAnalisis>>()
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var siguiente = 1

    override fun historial(usuario: Usuario): Flow<List<Diagnostico>> =
        porUsuario.map { mapa -> mapa[usuario.id].orEmpty().sortedByDescending { it.fecha } }

    override fun diagnostico(usuario: Usuario, idDiagnostico: String): Flow<Diagnostico?> =
        porUsuario.map { mapa -> mapa[usuario.id].orEmpty().firstOrNull { it.id == idDiagnostico } }

    override suspend fun iniciar(usuario: Usuario, solicitud: SolicitudAnalisis): String {
        delay(400)
        val id = "analisis-${siguiente++}"
        val avance = MutableStateFlow(AvanceAnalisis(foto = solicitud.fotos.firstOrNull(), deEjemplo = true))
        avances[id] = avance
        alcance.launch { simular(usuario, id, solicitud, avance) }
        return id
    }

    override fun avance(idAnalisis: String): Flow<AvanceAnalisis> =
        avances[idAnalisis] ?: flowOf(AvanceAnalisis(error = "No encontramos este análisis. Toma las fotos de nuevo."))

    private suspend fun simular(
        usuario: Usuario,
        id: String,
        solicitud: SolicitudAnalisis,
        avance: MutableStateFlow<AvanceAnalisis>,
    ) {
        fun paso(paso: PasoAnalisis, estado: EstadoPaso, detalle: String) {
            avance.update { actual ->
                actual.copy(pasos = actual.pasos.map { if (it.paso == paso) PasoEnCurso(paso, estado, detalle) else it })
            }
        }

        delay(250)
        paso(PasoAnalisis.Foto, EstadoPaso.EnCurso, "Revisando nitidez y luz")
        delay(650)
        paso(PasoAnalisis.Foto, EstadoPaso.Listo, "Nitidez y luz correctas")
        avance.update { it.copy(fotoRevisada = true) }

        delay(150)
        paso(PasoAnalisis.Deteccion, EstadoPaso.EnCurso, "Buscando zonas con daño visible")
        delay(750)
        val danos = danosDeEjemplo()
        paso(PasoAnalisis.Deteccion, EstadoPaso.Listo, "${danos.size} zonas posibles")
        avance.update { actual ->
            actual.copy(zonas = danos.mapNotNull { dano -> dano.caja?.let { caja -> etiquetaZona(dano) to caja } })
        }

        delay(150)
        paso(PasoAnalisis.Interpretacion, EstadoPaso.EnCurso, "Enviando solo la zona del rin")
        delay(600)
        paso(PasoAnalisis.Interpretacion, EstadoPaso.EnCurso, "Interpretando la zona con Claude")
        avance.update { it.copy(interpretando = true) }
        delay(1_400)
        paso(PasoAnalisis.Interpretacion, EstadoPaso.Listo, "Explicación lista")
        avance.update { it.copy(interpretando = false) }

        delay(100)
        paso(PasoAnalisis.Cotizacion, EstadoPaso.EnCurso, "Aplicando reglas y tarifas")
        delay(600)
        val diagnostico = armarDiagnostico(id, solicitud, danos)
        porUsuario.update { mapa -> mapa + (usuario.id to mapa[usuario.id].orEmpty() + diagnostico) }
        paso(PasoAnalisis.Cotizacion, EstadoPaso.Listo, "Rango calculado")
        avance.update { it.copy(idDiagnostico = diagnostico.id) }
    }

    private fun etiquetaZona(dano: Dano): String =
        if (dano.confirmado) dano.tipo.nombre else "¿${dano.tipo.nombre}?"

    internal companion object {

        fun danosDeEjemplo(): List<Dano> = listOf(
            Dano(
                tipo = TipoDano.Rayon,
                descripcion = "Rayón profundo en el borde exterior",
                gravedad = Gravedad.Moderada,
                confianza = 0.86f,
                caja = Caja(0.58f, 0.18f, 0.24f, 0.24f),
                explicacion = "Entre las 12 y las 3 en punto. Afecta la pintura y una capa del aluminio.",
                confirmado = true,
            ),
            Dano(
                tipo = TipoDano.Deformacion,
                descripcion = "Posible deformación del borde",
                gravedad = Gravedad.Leve,
                confianza = 0.58f,
                caja = Caja(0.2f, 0.62f, 0.2f, 0.2f),
                explicacion = "Una foto no permite medirla. El modelo marcó la zona y la IA no puede confirmarla: " +
                    "el taller la mide en la balanceadora.",
                confirmado = false,
            ),
        )

        const val AlertaDeformacion = "La posible deformación no se puede medir con fotos. Si notas vibración o el " +
            "carro se desvía, evita viajar en carretera hasta revisarlo."

        fun armarDiagnostico(
            id: String,
            solicitud: SolicitudAnalisis,
            danos: List<Dano>,
            fecha: LocalDateTime = LocalDateTime.now(),
        ): Diagnostico {
            val dias = Catalogo.dias(danos)
            return Diagnostico(
                id = "diagnostico-$id",
                fecha = fecha,
                vehiculo = solicitud.vehiculo,
                material = solicitud.material,
                pulgadas = solicitud.pulgadas,
                danos = danos,
                cotizacion = Catalogo.cotizar(danos, solicitud.material, solicitud.pulgadas),
                diasMinimos = dias.first,
                diasMaximos = dias.last,
                recomendaciones = listOf(
                    "Pide revisar el balanceo después del enderezado.",
                    "Evita andenes y huecos hasta la reparación.",
                ),
                requiereInspeccion = danos.any { !it.confirmado },
                deEjemplo = true,
                fotos = solicitud.fotos,
                gravedad = Gravedad.Moderada,
                indiceGravedad = 0.52f,
                alerta = AlertaDeformacion,
            )
        }

        fun diagnosticosIniciales(ahora: LocalDateTime): Map<String, List<Diagnostico>> = mapOf(
            "cliente-1" to listOf(
                armarDiagnostico(
                    id = "1",
                    solicitud = SolicitudAnalisis(
                        fotos = emptyList(),
                        material = MaterialRin.Aleacion,
                        pulgadas = 17,
                        vehiculo = "Mazda 3 2021",
                    ),
                    danos = danosDeEjemplo(),
                    fecha = ahora.toLocalDate().minusDays(5).atTime(15, 20),
                ),
            ),
        )
    }
}
