package com.rinfix.app.datos

import com.rinfix.app.ia.PrepararFoto
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Análisis REAL: las fotos van al servidor de RinFix en Vercel y Claude identifica los daños
 * visibles. El precio y los días salen siempre del [Catalogo] del taller, nunca de la IA.
 *
 * Los diagnósticos se guardan en memoria mientras llega Supabase. El historial arranca con
 * el diagnóstico de ejemplo de Laura, que sigue marcado como de ejemplo.
 */
class AnalisisConClaude(ahora: LocalDateTime = LocalDateTime.now()) : ServicioAnalisis {

    private val porUsuario = MutableStateFlow(AnalisisDeEjemplo.diagnosticosIniciales(ahora))
    private val avances = ConcurrentHashMap<String, MutableStateFlow<AvanceAnalisis>>()
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val siguiente = AtomicInteger(1)

    override fun historial(usuario: Usuario): Flow<List<Diagnostico>> =
        porUsuario.map { mapa -> mapa[usuario.id].orEmpty().sortedByDescending { it.fecha } }

    override fun diagnostico(usuario: Usuario, idDiagnostico: String): Flow<Diagnostico?> =
        porUsuario.map { mapa -> mapa[usuario.id].orEmpty().firstOrNull { it.id == idDiagnostico } }

    override suspend fun iniciar(usuario: Usuario, solicitud: SolicitudAnalisis): String {
        val id = "real-${System.currentTimeMillis()}-${siguiente.getAndIncrement()}"
        val avance = MutableStateFlow(
            AvanceAnalisis(
                // Sin el modelo del teléfono todavía: Claude encuentra y explica los daños.
                pasos = listOf(PasoAnalisis.Foto, PasoAnalisis.Interpretacion, PasoAnalisis.Cotizacion).map { PasoEnCurso(it) },
                foto = solicitud.fotos.firstOrNull(),
                deEjemplo = false,
            ),
        )
        avances[id] = avance
        alcance.launch { analizar(usuario, id, solicitud, avance) }
        return id
    }

    override fun avance(idAnalisis: String): Flow<AvanceAnalisis> =
        avances[idAnalisis] ?: flowOf(AvanceAnalisis(error = "No encontramos este análisis. Toma las fotos de nuevo."))

    private suspend fun analizar(
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

        fun fallar(paso: PasoAnalisis, detalle: String, mensaje: String) {
            paso(paso, EstadoPaso.Fallo, detalle)
            avance.update { it.copy(interpretando = false, error = mensaje) }
        }

        // 1. Preparar las fotos en el teléfono.
        paso(PasoAnalisis.Foto, EstadoPaso.EnCurso, "Enderezando y aligerando las fotos")
        val fotos = withContext(Dispatchers.IO) { solicitud.fotos.mapNotNull { PrepararFoto.comoBase64(it) } }
        if (fotos.isEmpty()) {
            fallar(PasoAnalisis.Foto, "No se pudieron leer", "No pudimos leer las fotos. Tómalas de nuevo.")
            return
        }
        val peso = fotos.sumOf { PrepararFoto.kilobytes(it) }
        paso(PasoAnalisis.Foto, EstadoPaso.Listo, "${cantidad(fotos.size, "foto lista", "fotos listas")}, $peso KB")
        avance.update { it.copy(fotoRevisada = true) }

        // 2. Claude revisa las fotos en el servidor.
        paso(PasoAnalisis.Interpretacion, EstadoPaso.EnCurso, "Enviando al servidor seguro")
        avance.update { it.copy(interpretando = true) }
        val cuerpo = JSONObject()
            .put("fotos", JSONArray(fotos))
            .put("material", solicitud.material.name)
            .put("pulgadas", solicitud.pulgadas ?: JSONObject.NULL)
            .put("vehiculo", solicitud.vehiculo ?: JSONObject.NULL)
        val respuesta = try {
            paso(PasoAnalisis.Interpretacion, EstadoPaso.EnCurso, "Claude está revisando el rin")
            Servidor.enviar("/api/analizar", cuerpo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Servidor.Error) {
            fallar(PasoAnalisis.Interpretacion, "No se completó", e.message ?: "No pudimos analizar las fotos.")
            return
        }

        if (!respuesta.optBoolean("esRin", false)) {
            fallar(
                PasoAnalisis.Interpretacion,
                "No se ve un rin",
                "No vemos un rin de carro en la foto. Tómala de frente al rin, completo y con buena luz.",
            )
            return
        }
        val danos = leerDanos(respuesta.optJSONArray("danos"))
        val calidad = respuesta.optString("calidadFoto", "buena")
        if (danos.isEmpty() && calidad != "buena") {
            fallar(PasoAnalisis.Interpretacion, "Foto ${calidad}", mensajeCalidad(calidad))
            return
        }
        paso(
            PasoAnalisis.Interpretacion,
            EstadoPaso.Listo,
            if (danos.isEmpty()) "Sin daños visibles" else cantidad(danos.size, "daño encontrado", "daños encontrados"),
        )
        avance.update { actual ->
            actual.copy(
                interpretando = false,
                zonas = danos.mapNotNull { dano -> dano.caja?.let { (if (dano.confirmado) dano.tipo.nombre else "¿${dano.tipo.nombre}?") to it } },
            )
        }

        // 3. Precio y días con el catálogo del taller.
        paso(PasoAnalisis.Cotizacion, EstadoPaso.EnCurso, "Aplicando las tarifas del taller")
        val diagnostico = armarDiagnostico(id, solicitud, danos, respuesta, calidad)
        porUsuario.update { mapa -> mapa + (usuario.id to mapa[usuario.id].orEmpty() + diagnostico) }
        paso(
            PasoAnalisis.Cotizacion,
            EstadoPaso.Listo,
            if (danos.isEmpty()) "Sin reparación sugerida" else Formato.rangoPesos(diagnostico.precioMinimo, diagnostico.precioMaximo),
        )
        avance.update { it.copy(idDiagnostico = diagnostico.id) }
    }

    /** Daños de la respuesta. El recuadro solo se guarda si es de la primera foto, la que se muestra. */
    private fun leerDanos(lista: JSONArray?): List<Dano> {
        if (lista == null) return emptyList()
        return (0 until lista.length()).mapNotNull { indice ->
            val dano = lista.optJSONObject(indice) ?: return@mapNotNull null
            val caja = dano.optJSONObject("caja")
                ?.takeIf { dano.optInt("foto", 0) == 0 }
                ?.let {
                    Caja(
                        izquierda = it.optDouble("x", 0.0).toFloat(),
                        arriba = it.optDouble("y", 0.0).toFloat(),
                        ancho = it.optDouble("ancho", 0.0).toFloat(),
                        alto = it.optDouble("alto", 0.0).toFloat(),
                    )
                }
                ?.takeIf { it.ancho > 0f && it.alto > 0f }
            Dano(
                tipo = TipoDano.entries.firstOrNull { it.name == dano.optString("tipo") } ?: TipoDano.Otro,
                descripcion = dano.optString("descripcion").ifBlank { "Daño visible en el rin" },
                gravedad = Gravedad.entries.firstOrNull { it.name == dano.optString("gravedad") } ?: Gravedad.Moderada,
                confianza = dano.optDouble("confianza", 0.5).toFloat().coerceIn(0f, 1f),
                caja = caja,
                explicacion = dano.optString("explicacion"),
                confirmado = dano.optBoolean("confirmado", false),
            )
        }
    }

    private fun armarDiagnostico(
        id: String,
        solicitud: SolicitudAnalisis,
        danos: List<Dano>,
        respuesta: JSONObject,
        calidad: String,
    ): Diagnostico {
        val dias = Catalogo.dias(danos)
        val gravedad = danos.maxOfOrNull { it.gravedad } ?: Gravedad.Leve
        val recomendaciones = buildList {
            val lista = respuesta.optJSONArray("recomendaciones")
            if (lista != null) for (i in 0 until lista.length()) lista.optString(i).takeIf { it.isNotBlank() }?.let(::add)
            if (calidad != "buena") add("La foto salió ${calidad}: el taller confirmará los daños al ver el rin.")
            if (isEmpty()) add("Si notas vibración o pérdida de aire, pide una revisión en el taller.")
        }
        return Diagnostico(
            id = "diagnostico-$id",
            fecha = LocalDateTime.now(),
            vehiculo = solicitud.vehiculo,
            material = solicitud.material,
            pulgadas = solicitud.pulgadas,
            danos = danos,
            cotizacion = if (danos.isEmpty()) emptyList() else Catalogo.cotizar(danos, solicitud.material, solicitud.pulgadas),
            diasMinimos = if (danos.isEmpty()) 0 else dias.first,
            diasMaximos = if (danos.isEmpty()) 0 else dias.last,
            recomendaciones = recomendaciones,
            requiereInspeccion = respuesta.optBoolean("requiereInspeccion", false) || danos.any { !it.confirmado },
            deEjemplo = false,
            fotos = solicitud.fotos,
            gravedad = gravedad,
            indiceGravedad = when {
                danos.isEmpty() -> 0.08f
                gravedad == Gravedad.Alta -> 0.86f
                gravedad == Gravedad.Moderada -> 0.54f
                else -> 0.24f
            },
            alerta = respuesta.optString("alerta").takeIf { it.isNotBlank() && it != "null" },
        )
    }

    private fun mensajeCalidad(calidad: String): String = when (calidad) {
        "borrosa" -> "La foto salió borrosa. Apoya bien el celular y toma la foto de nuevo."
        "oscura" -> "La foto salió muy oscura. Busca más luz o prende la linterna."
        "lejana" -> "El rin se ve muy lejos. Acércate hasta que ocupe casi toda la foto."
        "tapada" -> "Algo tapa el rin en la foto. Despeja la vista y toma la foto de nuevo."
        else -> "No pudimos evaluar la foto. Tómala de nuevo con buena luz."
    }

    private fun cantidad(numero: Int, uno: String, varios: String): String =
        if (numero == 1) "1 $uno" else "$numero $varios"
}
