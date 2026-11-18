package com.rinfix.app.datos

import java.time.LocalDateTime

/**
 * Etapas de una reparación, en orden. El trabajador del taller las avanza
 * y el cliente las ve en la línea de tiempo.
 */
enum class EtapaOrden(val titulo: String, val estado: String) {
    Solicitud("Solicitud recibida", "Solicitada"),
    Inspeccion("Inspección en el taller", "En inspección"),
    Cotizacion("Cotización aprobada", "Aprobada"),
    Reparacion("Reparación en curso", "En reparación"),
    Calidad("Control de calidad", "Revisión final"),
    Lista("Listo para entrega", "Lista para recoger"),
    Entregada("Entregado", "Entregada"),
    ;

    /** 1 a 7 */
    val numero: Int get() = ordinal + 1

    /** La etapa que sigue, o `null` si ya se entregó. */
    val siguiente: EtapaOrden? get() = entries.getOrNull(ordinal + 1)

    companion object {
        val total: Int get() = entries.size
    }
}

/** Cuál rin del carro se va a reparar. */
enum class PosicionRin(val nombre: String) {
    DelanteroIzquierdo("Delantero izquierdo"),
    DelanteroDerecho("Delantero derecho"),
    TraseroIzquierdo("Trasero izquierdo"),
    TraseroDerecho("Trasero derecho"),
    Repuesto("Repuesto"),
    Varios("Varios rines"),
    ;

    /** «rin delantero izquierdo» o «varios rines», para mostrar junto al vehículo. */
    val descripcion: String
        get() = if (this == Varios) "varios rines" else "rin ${nombre.lowercase()}"
}

/** Cuándo se tomó una foto del proceso. */
enum class MomentoFoto(val nombre: String) {
    Antes("Antes"),
    Durante("Durante"),
    Despues("Después"),
}

/**
 * Foto de avance. `direccion` será la dirección del archivo en Supabase Storage;
 * en los datos de ejemplo es `null` y la app dibuja un rin en su lugar.
 */
data class FotoAvance(
    val id: String,
    val momento: MomentoFoto,
    val direccion: String? = null,
)

/** Un paso registrado en la línea de tiempo. */
data class Avance(
    val etapa: EtapaOrden,
    val fecha: LocalDateTime,
    val nota: String? = null,
    val fotos: List<FotoAvance> = emptyList(),
    /** «Carlos, técnico». `null` cuando lo registra el sistema. */
    val registradoPor: String? = null,
)

/** Orden de reparación de un rin. */
data class Orden(
    val id: String,
    /** Código que ven el cliente y el taller: «RF-1042». */
    val codigo: String,
    val clienteId: String,
    val cliente: String,
    /** «Mazda 3» */
    val vehiculo: String,
    /** «rin delantero izquierdo» */
    val rin: String,
    /** Servicio principal: «Enderezado», «Diamantado»… */
    val servicio: String,
    val taller: String,
    val tecnico: String?,
    val etapa: EtapaOrden,
    /** Detalle de la etapa actual: «Pintura y curado». */
    val detalleEtapa: String?,
    val creada: LocalDateTime,
    val entregaEstimada: LocalDateTime?,
    /** Total aprobado en pesos; `null` mientras no hay cotización. */
    val total: Long?,
    val pagada: Boolean,
    val avances: List<Avance>,
    val diagnosticoId: String? = null,
    /** Fecha y hora en que el cliente lleva el rin al taller. */
    val cita: LocalDateTime? = null,
    /** Lo que el cliente le contó al taller al pedir la reparación. */
    val notas: String? = null,
    /** Cotización que el taller confirmó tras la inspección: valores exactos, ya no rangos. */
    val cotizacion: List<LineaCotizacion> = emptyList(),
) {
    val activa: Boolean get() = etapa != EtapaOrden.Entregada

    val ultimaActualizacion: LocalDateTime
        get() = avances.maxOfOrNull { it.fecha } ?: creada

    /** Detalle del total: la cotización confirmada o, si no la hay, una sola línea con el servicio. */
    val detalleTotal: List<LineaCotizacion>
        get() = cotizacion.ifEmpty {
            total?.let { listOf(LineaCotizacion(servicio = servicio, minimo = it, maximo = it)) }.orEmpty()
        }

    /** Hay un total aprobado sin pagar. */
    val pagoPendiente: Boolean
        get() = total != null && !pagada && etapa >= EtapaOrden.Cotizacion
}

enum class TipoNotificacion { Avance, Fotos, Cotizacion, Pago, Entrega }

data class Notificacion(
    val id: String,
    val usuarioId: String,
    val tipo: TipoNotificacion,
    val titulo: String,
    val detalle: String,
    val fecha: LocalDateTime,
    val leida: Boolean,
    val ordenId: String? = null,
)
