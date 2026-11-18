package com.rinfix.app.datos

import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * Asistente con Claude: cada pregunta va al servidor de RinFix con la conversación y los datos
 * de la persona (órdenes, diagnósticos, garaje, catálogo y taller), y Claude responde libre.
 *
 * Hereda la conversación de [AsistenteDeEjemplo]. Si el servidor falla (sin internet, por ejemplo),
 * responde con las reglas de ejemplo y lo dice, para que la persona no se quede sin respuesta.
 */
class AsistenteConClaude(
    ordenes: ServicioOrdenes,
    analisis: ServicioAnalisis,
    garaje: ServicioGaraje,
) : AsistenteDeEjemplo(ordenes, analisis, garaje) {

    override suspend fun responder(
        usuario: Usuario,
        pregunta: String,
        contexto: ContextoPregunta?,
        historial: List<Mensaje>,
    ): Respuesta {
        val misOrdenes = ordenes.ordenes(usuario).first()
        val diagnosticos = analisis.historial(usuario).first()
        val vehiculos = garaje.vehiculos(usuario).first()

        val cuerpo = JSONObject()
            .put("mensajes", JSONArray(historial.takeLast(MaximoMensajes).map(::comoJson)))
            .put("contexto", contextoDe(usuario, misOrdenes, diagnosticos, vehiculos, contexto))

        val respuesta = try {
            Servidor.enviar("/api/asistente", cuerpo, esperaMaximaMs = 45_000)
        } catch (e: Servidor.Error) {
            val local = super.responder(usuario, pregunta, contexto, historial)
            return local.copy(texto = "No pude conectarme con el asistente (${e.message?.trimEnd('.')}). " +
                "Te respondo con lo que tengo en el teléfono:\n\n${local.texto}")
        }

        val acciones = leerAcciones(respuesta.optJSONArray("acciones"), misOrdenes, diagnosticos)
        // Si Claude sugiere ver una orden o un diagnóstico, se muestra también su tarjeta.
        val piezas = acciones.mapNotNull { accion ->
            when (val destino = accion.destino) {
                is Destino.Seguimiento -> Pieza.OrdenRelacionada(destino.idOrden)
                is Destino.VerDiagnostico -> Pieza.DiagnosticoRelacionado(destino.idDiagnostico)
                else -> null
            }
        }.distinct().take(2)

        return Respuesta(
            texto = respuesta.optString("texto").ifBlank { "No me llegó la respuesta. Pregúntame de nuevo." },
            piezas = piezas,
            acciones = acciones,
            sugerencias = leerTextos(respuesta.optJSONArray("sugerencias")).take(3),
            deEjemplo = false,
        )
    }

    private fun comoJson(mensaje: Mensaje): JSONObject = JSONObject()
        .put("autor", if (mensaje.autor == Autor.Persona) "persona" else "asistente")
        .put("texto", mensaje.texto)

    /** Lo que Claude necesita saber de la persona. Solo datos de la app, nada del teléfono. */
    private fun contextoDe(
        usuario: Usuario,
        misOrdenes: List<Orden>,
        diagnosticos: List<Diagnostico>,
        vehiculos: List<Vehiculo>,
        contexto: ContextoPregunta?,
    ): JSONObject = JSONObject().apply {
        put("nombre", usuario.nombre.substringBefore(' '))
        put("ordenes", JSONArray(misOrdenes.take(10).map { orden ->
            JSONObject()
                .put("id", orden.id)
                .put("codigo", orden.codigo)
                .put("vehiculo", orden.vehiculo)
                .put("rin", orden.rin)
                .put("servicio", orden.servicio)
                .put("etapa", orden.etapa.titulo)
                .put("paso", "${orden.etapa.numero} de ${EtapaOrden.total}")
                .put("ahoraMismo", orden.detalleEtapa ?: "")
                .put("entregaEstimada", orden.entregaEstimada?.let(Formato::diaYHora) ?: "")
                .put("total", orden.total?.let(Formato::pesos) ?: "sin cotizar")
                .put("pagada", orden.pagada)
                .put("pagoPendiente", orden.pagoPendiente)
        }))
        put("diagnosticos", JSONArray(diagnosticos.take(5).map { diagnostico ->
            JSONObject()
                .put("id", diagnostico.id)
                .put("fecha", Formato.fechaCorta(diagnostico.fecha))
                .put("vehiculo", diagnostico.vehiculo ?: "")
                .put("danos", JSONArray(diagnostico.danos.map { dano ->
                    "${dano.descripcion} (${dano.gravedad.nombre.lowercase()}${if (dano.confirmado) "" else ", por confirmar"})"
                }))
                .put("rangoPrecio", if (diagnostico.danos.isEmpty()) "sin reparación" else
                    Formato.rangoPesos(diagnostico.precioMinimo, diagnostico.precioMaximo))
                .put("requiereInspeccion", diagnostico.requiereInspeccion)
                .put("deEjemplo", diagnostico.deEjemplo)
        }))
        put("vehiculos", JSONArray(vehiculos.take(5).map { "${it.titulo}, ${it.descripcionRin.lowercase()}" }))
        put("catalogo", JSONArray(Catalogo.servicios.values.map { servicio ->
            "${servicio.nombre}: ${Formato.rangoPesos(servicio.minimo, servicio.maximo)}, " +
                "${servicio.diasMinimos} a ${servicio.diasMaximos} días hábiles"
        }))
        put("notaCatalogo", "Precios base para un rin de 17\" de aleación. Rines de 14\" o menos cuestan 15 % menos; " +
            "de 15\" o 16\", 5 % menos; de 18\", 12 % más; de 19\" o más, 25 % más. El acero cuesta 20 % menos. Incluido sin costo: " +
            Catalogo.incluidos.joinToString(", ") + ".")
        put("taller", JSONObject()
            .put("nombre", Catalogo.Taller)
            .put("direccion", Catalogo.DireccionTaller)
            .put("horario", "Lunes a viernes de 8:00 a. m. a 12:00 m. y de 2:00 a 6:00 p. m.; sábados de 8:00 a. m. a 12:00 m.; domingos cerrado"))
        contexto?.idDiagnostico?.let { put("preguntaSobreDiagnostico", it) }
        contexto?.idOrden?.let { put("preguntaSobreOrden", it) }
    }

    /** Solo acciones que llevan a algo que existe: órdenes y diagnósticos de la persona. */
    private fun leerAcciones(lista: JSONArray?, misOrdenes: List<Orden>, diagnosticos: List<Diagnostico>): List<AccionRespuesta> {
        if (lista == null) return emptyList()
        return (0 until lista.length()).mapNotNull { indice ->
            val accion = lista.optJSONObject(indice) ?: return@mapNotNull null
            val texto = accion.optString("texto").ifBlank { return@mapNotNull null }
            val id = accion.optString("id")
            val destino = when (accion.optString("destino")) {
                "analizar" -> Destino.Analizar
                "agendar" -> Destino.Agendar(id.takeIf { buscado -> diagnosticos.any { it.id == buscado } })
                "seguimiento" -> misOrdenes.firstOrNull { it.id == id }?.let { Destino.Seguimiento(it.id) }
                "diagnostico" -> diagnosticos.firstOrNull { it.id == id }?.let { Destino.VerDiagnostico(it.id) }
                "pagar" -> misOrdenes.firstOrNull { it.id == id && it.pagoPendiente }?.let { Destino.Pagar(it.id) }
                "como_llegar" -> Destino.ComoLlegar
                else -> null
            } ?: return@mapNotNull null
            AccionRespuesta(texto, destino)
        }.distinctBy { it.destino }.take(3)
    }

    private fun leerTextos(lista: JSONArray?): List<String> =
        if (lista == null) emptyList() else (0 until lista.length()).mapNotNull { lista.optString(it).ifBlank { null } }

    private companion object {
        const val MaximoMensajes = 16
    }
}
