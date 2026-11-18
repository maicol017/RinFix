package com.rinfix.app.datos

import java.time.LocalDateTime

/** Quién escribió un mensaje. */
enum class Autor { Persona, Asistente }

/** A dónde lleva un botón dentro de una respuesta del asistente. */
sealed interface Destino {
    data object Analizar : Destino
    data class Agendar(val idDiagnostico: String? = null) : Destino
    data class Seguimiento(val idOrden: String) : Destino
    data class VerDiagnostico(val idDiagnostico: String) : Destino
    data class Pagar(val idOrden: String) : Destino
    data object ComoLlegar : Destino
}

/** Botón que acompaña una respuesta: «Ver seguimiento», «Agendar reparación»… */
data class AccionRespuesta(val texto: String, val destino: Destino)

/** Fila de una tabla: «Enderezado del borde» — «$60.000 – $90.000». */
data class FilaTabla(val nombre: String, val valor: String, val detalle: String? = null)

/**
 * Bloques que acompañan el texto de una respuesta.
 * Las órdenes y los diagnósticos van por id: la pantalla los muestra con sus datos al día.
 */
sealed interface Pieza {
    data class OrdenRelacionada(val idOrden: String) : Pieza
    data class DiagnosticoRelacionado(val idDiagnostico: String) : Pieza

    /** Datos en filas. Con `dePrecios` se pinta con el color de las cotizaciones. */
    data class Tabla(
        val titulo: String,
        val filas: List<FilaTabla>,
        val nota: String? = null,
        val dePrecios: Boolean = false,
    ) : Pieza

    /** Lista corta de consejos o señales. */
    data class Puntos(val puntos: List<String>) : Pieza

    /** Aviso de seguridad destacado. */
    data class Cuidado(val texto: String) : Pieza
}

/** Un mensaje de la conversación con el asistente. */
data class Mensaje(
    val id: String,
    val autor: Autor,
    val texto: String,
    val fecha: LocalDateTime,
    val piezas: List<Pieza> = emptyList(),
    val acciones: List<AccionRespuesta> = emptyList(),
    /** Preguntas que la persona puede tocar para seguir la conversación. */
    val sugerencias: List<String> = emptyList(),
    /** `true` si la respuesta viene de las reglas de ejemplo y no del asistente con IA. */
    val deEjemplo: Boolean = false,
)

/** Conversación de una persona con el asistente. */
data class Conversacion(
    val mensajes: List<Mensaje> = emptyList(),
    /** `true` mientras el asistente prepara la respuesta. */
    val escribiendo: Boolean = false,
)

/** De qué diagnóstico u orden viene una pregunta, por ejemplo desde «Preguntar al asistente». */
data class ContextoPregunta(val idDiagnostico: String? = null, val idOrden: String? = null)
