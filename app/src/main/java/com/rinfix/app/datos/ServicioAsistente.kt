package com.rinfix.app.datos

import kotlinx.coroutines.flow.Flow

/**
 * Asistente conversacional del cliente. Orienta sobre órdenes, diagnósticos, precios, tiempos y
 * cuidados del rin. No reemplaza la inspección del taller y nunca inventa precios: salen del catálogo.
 *
 * Hoy responde con reglas de ejemplo dentro del teléfono. Con el backend, la pregunta irá a un
 * endpoint en Vercel que llama a Claude con el contexto de la persona; la llave queda en el servidor.
 */
interface ServicioAsistente {

    /** La conversación de la persona, al día. */
    fun conversacion(usuario: Usuario): Flow<Conversacion>

    /**
     * Agrega la pregunta y responde en segundo plano, aunque la persona cambie de pantalla.
     * Si el asistente todavía está respondiendo, no hace nada.
     */
    fun enviar(usuario: Usuario, texto: String, contexto: ContextoPregunta? = null)

    /** Borra la conversación y empieza de cero. */
    fun reiniciar(usuario: Usuario)
}
