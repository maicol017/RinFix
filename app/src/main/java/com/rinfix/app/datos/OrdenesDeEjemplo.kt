package com.rinfix.app.datos

import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Órdenes de ejemplo mientras no existe el backend. Viven en memoria y las fechas se calculan
 * desde hoy, para que siempre se vean recientes.
 *
 * - Laura (cliente-1): una reparación en curso (RF-1042) y una entregada (RF-0987).
 * - Taller RinFix Cabecera: además, tres órdenes de otros clientes en distintas etapas.
 */
class OrdenesDeEjemplo(ahora: LocalDateTime = LocalDateTime.now()) : ServicioOrdenes {

    private val todas = MutableStateFlow(ordenesIniciales(ahora))
    private val avisos = MutableStateFlow(notificacionesIniciales(ahora))

    override fun ordenes(usuario: Usuario): Flow<List<Orden>> = todas.map { lista ->
        lista
            .filter {
                when (usuario.rol) {
                    Rol.Cliente -> it.clienteId == usuario.id
                    Rol.Taller -> it.taller == usuario.taller
                }
            }
            .sortedByDescending { it.ultimaActualizacion }
    }

    override fun notificaciones(usuario: Usuario): Flow<List<Notificacion>> = avisos.map { lista ->
        lista.filter { it.usuarioId == usuario.id }.sortedByDescending { it.fecha }
    }

    override suspend fun marcarNotificacionesLeidas(usuario: Usuario) {
        delay(150)
        avisos.update { lista ->
            lista.map { if (it.usuarioId == usuario.id) it.copy(leida = true) else it }
        }
    }

    internal companion object {

        const val Taller = "Taller RinFix Cabecera"

        fun ordenesIniciales(ahora: LocalDateTime): List<Orden> {
            val hoy = ahora.toLocalDate()
            fun dia(dias: Long, hora: Int, minuto: Int = 0) = hoy.plusDays(dias).atTime(hora, minuto)

            val rf1042 = Orden(
                id = "orden-1042",
                codigo = "RF-1042",
                clienteId = "cliente-1",
                cliente = "Laura Méndez",
                vehiculo = "Mazda 3",
                rin = "rin delantero izquierdo",
                servicio = "Rayones, pintura y enderezado",
                taller = Taller,
                tecnico = "Carlos",
                etapa = EtapaOrden.Reparacion,
                detalleEtapa = "Pintura y curado",
                creada = dia(-3, 9, 12),
                entregaEstimada = dia(2, 16),
                total = 210_000,
                pagada = false,
                diagnosticoId = "diagnostico-1",
                avances = listOf(
                    Avance(EtapaOrden.Solicitud, dia(-3, 9, 12), "El taller aceptó y agendó tu ingreso."),
                    Avance(
                        EtapaOrden.Inspeccion,
                        dia(-2, 10, 40),
                        "Se confirmó el rayón y una deformación leve del borde.",
                        fotos = listOf(
                            FotoAvance("foto-1042-1", MomentoFoto.Antes),
                            FotoAvance("foto-1042-2", MomentoFoto.Antes),
                        ),
                        registradoPor = "Carlos, técnico",
                    ),
                    Avance(EtapaOrden.Cotizacion, dia(-2, 11, 5), "Aprobaste \$210.000 en la app."),
                    Avance(
                        EtapaOrden.Reparacion,
                        ahora.minusMinutes(25),
                        "Enderezado terminado. Ahora en pintura y curado.",
                        fotos = listOf(
                            FotoAvance("foto-1042-3", MomentoFoto.Durante),
                            FotoAvance("foto-1042-4", MomentoFoto.Durante),
                            FotoAvance("foto-1042-5", MomentoFoto.Durante),
                        ),
                        registradoPor = "Carlos, técnico",
                    ),
                ),
            )

            val rf0987 = Orden(
                id = "orden-0987",
                codigo = "RF-0987",
                clienteId = "cliente-1",
                cliente = "Laura Méndez",
                vehiculo = "Kia Picanto",
                rin = "rin trasero derecho",
                servicio = "Enderezado",
                taller = Taller,
                tecnico = "Carlos",
                etapa = EtapaOrden.Entregada,
                detalleEtapa = null,
                creada = dia(-52, 8, 30),
                entregaEstimada = dia(-50, 17),
                total = 95_000,
                pagada = true,
                avances = listOf(
                    Avance(EtapaOrden.Solicitud, dia(-52, 8, 30)),
                    Avance(EtapaOrden.Inspeccion, dia(-52, 10, 15), registradoPor = "Carlos, técnico"),
                    Avance(EtapaOrden.Cotizacion, dia(-52, 10, 40), "Aprobaste \$95.000 en la app."),
                    Avance(EtapaOrden.Reparacion, dia(-51, 9, 0), registradoPor = "Carlos, técnico"),
                    Avance(EtapaOrden.Calidad, dia(-51, 15, 20), registradoPor = "Carlos, técnico"),
                    Avance(EtapaOrden.Lista, dia(-50, 11, 0)),
                    Avance(EtapaOrden.Entregada, dia(-50, 16, 45), "Pagaste \$95.000 al recoger."),
                ),
            )

            val rf1041 = Orden(
                id = "orden-1041",
                codigo = "RF-1041",
                clienteId = "cliente-ejemplo-2",
                cliente = "Mónica Salazar",
                vehiculo = "Hyundai Tucson",
                rin = "rin 18\" delantero derecho",
                servicio = "Diamantado",
                taller = Taller,
                tecnico = "Carlos",
                etapa = EtapaOrden.Reparacion,
                detalleEtapa = "Diamantado en torno",
                creada = dia(-4, 14, 0),
                entregaEstimada = dia(2, 11),
                total = 320_000,
                pagada = true,
                avances = listOf(
                    Avance(EtapaOrden.Solicitud, dia(-4, 14, 0)),
                    Avance(EtapaOrden.Inspeccion, dia(-3, 9, 30), registradoPor = "Carlos, técnico"),
                    Avance(EtapaOrden.Cotizacion, dia(-3, 10, 10)),
                    Avance(EtapaOrden.Reparacion, dia(-1, 8, 45), registradoPor = "Carlos, técnico"),
                ),
            )

            val rf1036 = Orden(
                id = "orden-1036",
                codigo = "RF-1036",
                clienteId = "cliente-ejemplo-3",
                cliente = "Diana Ortiz",
                vehiculo = "Renault Duster",
                rin = "2 rines traseros",
                servicio = "Enderezado y balanceo",
                taller = Taller,
                tecnico = "Carlos",
                etapa = EtapaOrden.Lista,
                detalleEtapa = "Cobro en el taller",
                creada = dia(-6, 11, 0),
                entregaEstimada = dia(0, 13),
                total = 180_000,
                pagada = false,
                avances = listOf(
                    Avance(EtapaOrden.Solicitud, dia(-6, 11, 0)),
                    Avance(EtapaOrden.Inspeccion, dia(-5, 9, 0), registradoPor = "Carlos, técnico"),
                    Avance(EtapaOrden.Cotizacion, dia(-5, 9, 50)),
                    Avance(EtapaOrden.Reparacion, dia(-3, 8, 30), registradoPor = "Carlos, técnico"),
                    Avance(EtapaOrden.Calidad, dia(-1, 16, 0), registradoPor = "Carlos, técnico"),
                    Avance(EtapaOrden.Lista, dia(-1, 17, 10), registradoPor = "Carlos, técnico"),
                ),
            )

            val rf1045 = Orden(
                id = "orden-1045",
                codigo = "RF-1045",
                clienteId = "cliente-ejemplo-4",
                cliente = "Jorge Prada",
                vehiculo = "Chevrolet Onix",
                rin = "2 rines 15\" delanteros",
                servicio = "Inspección",
                taller = Taller,
                tecnico = null,
                etapa = EtapaOrden.Solicitud,
                detalleEtapa = "Ingreso agendado",
                creada = ahora.minusHours(3),
                entregaEstimada = null,
                total = null,
                pagada = false,
                avances = listOf(
                    Avance(EtapaOrden.Solicitud, ahora.minusHours(3), "Solicitud nueva desde la app."),
                ),
            )

            return listOf(rf1042, rf0987, rf1041, rf1036, rf1045)
        }

        fun notificacionesIniciales(ahora: LocalDateTime): List<Notificacion> {
            val hoy = ahora.toLocalDate()
            return listOf(
                Notificacion(
                    id = "aviso-1",
                    usuarioId = "cliente-1",
                    tipo = TipoNotificacion.Fotos,
                    titulo = "Nuevas fotos de tu reparación",
                    detalle = "Pintura y curado",
                    fecha = ahora.minusMinutes(25),
                    leida = false,
                    ordenId = "orden-1042",
                ),
                Notificacion(
                    id = "aviso-2",
                    usuarioId = "cliente-1",
                    tipo = TipoNotificacion.Cotizacion,
                    titulo = "Cotización confirmada por el taller",
                    detalle = "\$210.000",
                    fecha = hoy.minusDays(2).atTime(11, 5),
                    leida = false,
                    ordenId = "orden-1042",
                ),
                Notificacion(
                    id = "aviso-3",
                    usuarioId = "cliente-1",
                    tipo = TipoNotificacion.Entrega,
                    titulo = "Recogiste tu rin",
                    detalle = "Kia Picanto, enderezado",
                    fecha = hoy.minusDays(50).atTime(16, 45),
                    leida = true,
                    ordenId = "orden-0987",
                ),
            )
        }
    }
}
