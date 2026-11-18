package com.rinfix.app.datos

import kotlinx.coroutines.flow.Flow

/**
 * Pasarela de pagos SIMULADA. No procesa dinero real ni guarda datos financieros:
 * así lo pide el proyecto. Si algún día se conecta una pasarela real, este es el único
 * contrato que cambia.
 */
interface ServicioPagos {

    /** Paga el total aprobado de la orden con el método elegido y devuelve el comprobante. */
    suspend fun pagar(usuario: Usuario, orden: Orden, metodo: MetodoPago): Comprobante

    fun comprobante(usuario: Usuario, idComprobante: String): Flow<Comprobante?>

    /** Comprobantes de la persona, el más reciente primero. */
    fun comprobantes(usuario: Usuario): Flow<List<Comprobante>>
}
