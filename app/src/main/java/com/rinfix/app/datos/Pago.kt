package com.rinfix.app.datos

import java.time.LocalDateTime

/**
 * Formas de pago de la pasarela simulada. Ninguna pide ni guarda datos reales:
 * la tarjeta es siempre la de prueba terminada en 4242.
 */
enum class MetodoPago(val nombre: String, val detalle: String, val enComprobante: String) {
    Tarjeta("Tarjeta de prueba", "Terminada en 4242", "Tarjeta de prueba •••• 4242"),
    Pse("PSE simulado", "Banco de prueba, sin datos reales", "PSE, banco de prueba"),
    Billetera("Billetera digital simulada", "Aprobación de prueba", "Billetera digital de prueba"),
    EnTaller("Pagar en el taller", "Al recoger tu rin", "Pago en el taller"),
}

enum class EstadoPago {
    /** Pago simulado aprobado: la orden queda pagada. */
    Aprobado,

    /** La persona pagará al recoger: la orden sigue con pago pendiente. */
    EnTaller,
}

/** Comprobante de la pasarela simulada. La referencia empieza por «DEMO» para que nunca parezca real. */
data class Comprobante(
    val id: String,
    val referencia: String,
    val usuarioId: String,
    val idOrden: String,
    val codigoOrden: String,
    val metodo: MetodoPago,
    val monto: Long,
    val fecha: LocalDateTime,
    val estado: EstadoPago,
) {
    val simulado: Boolean get() = true
}
