package com.rinfix.app.datos

import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Pagos simulados en memoria. Siempre aprueban; con «Pagar en el taller» solo dejan constancia.
 * Al aprobar, avisan a las órdenes para marcar la orden como pagada.
 */
class PagosDeEjemplo(private val ordenes: ServicioOrdenes) : ServicioPagos {

    private val todos = MutableStateFlow<List<Comprobante>>(emptyList())
    private var siguiente = 1

    override suspend fun pagar(usuario: Usuario, orden: Orden, metodo: MetodoPago): Comprobante {
        delay(900)
        val comprobante = Comprobante(
            id = "comprobante-${siguiente++}",
            referencia = "DEMO-" + (1..6).map { CaracteresReferencia.random() }.joinToString(""),
            usuarioId = usuario.id,
            idOrden = orden.id,
            codigoOrden = orden.codigo,
            metodo = metodo,
            monto = orden.total ?: 0,
            fecha = LocalDateTime.now(),
            estado = if (metodo == MetodoPago.EnTaller) EstadoPago.EnTaller else EstadoPago.Aprobado,
        )
        if (comprobante.estado == EstadoPago.Aprobado) {
            ordenes.registrarPago(orden.id, comprobante.referencia)
        }
        todos.update { it + comprobante }
        return comprobante
    }

    override fun comprobante(usuario: Usuario, idComprobante: String): Flow<Comprobante?> =
        todos.map { lista -> lista.firstOrNull { it.id == idComprobante && it.usuarioId == usuario.id } }

    override fun comprobantes(usuario: Usuario): Flow<List<Comprobante>> =
        todos.map { lista -> lista.filter { it.usuarioId == usuario.id }.sortedByDescending { it.fecha } }

    private companion object {
        /** Sin letras ni números que se confundan (O y 0, I y 1). */
        const val CaracteresReferencia = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
