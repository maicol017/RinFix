package com.rinfix.app.datos

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    private var siguienteCodigo = 1046

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

    override suspend fun registrarPago(idOrden: String, referencia: String) {
        val ahora = LocalDateTime.now()
        var pagada: Orden? = null
        todas.update { lista ->
            lista.map { orden ->
                if (orden.id == idOrden) orden.copy(pagada = true).also { pagada = it } else orden
            }
        }
        val orden = pagada ?: return
        avisos.update { lista ->
            lista + Notificacion(
                id = "aviso-pago-$referencia",
                usuarioId = orden.clienteId,
                tipo = TipoNotificacion.Pago,
                titulo = "Pago aprobado",
                detalle = "${orden.codigo}, ${orden.total?.let(Formato::pesos).orEmpty()}",
                fecha = ahora,
                leida = true,
                ordenId = orden.id,
            )
        }
    }

    override suspend fun horasDisponibles(fecha: LocalDate): List<Horario> {
        delay(300)
        val ahora = LocalDateTime.now()
        val horas = if (fecha.dayOfWeek == DayOfWeek.SATURDAY) HorasSabado else HorasSemana
        return horas.map { hora ->
            // Ocupadas de ejemplo: cambian con el día para que la agenda no se vea siempre igual.
            val ocupada = (fecha.dayOfMonth + hora.hour) % 3 == 0
            // Hoy, solo con al menos 2 horas de anticipación.
            val pasada = fecha.atTime(hora).isBefore(ahora.plusHours(2))
            Horario(hora, disponible = !ocupada && !pasada && fecha.dayOfWeek != DayOfWeek.SUNDAY)
        }
    }

    override suspend fun solicitar(usuario: Usuario, solicitud: SolicitudReparacion): Orden {
        delay(900)
        val ahora = LocalDateTime.now()
        val codigo = "RF-${siguienteCodigo++}"
        val orden = Orden(
            id = "orden-${codigo.removePrefix("RF-")}",
            codigo = codigo,
            clienteId = usuario.id,
            cliente = usuario.nombre,
            vehiculo = solicitud.vehiculo,
            rin = solicitud.posicion.descripcion,
            servicio = solicitud.servicio,
            taller = Taller,
            tecnico = null,
            etapa = EtapaOrden.Solicitud,
            detalleEtapa = "Ingreso agendado",
            creada = ahora,
            entregaEstimada = null,
            total = null,
            pagada = false,
            diagnosticoId = solicitud.idDiagnostico,
            cita = solicitud.cita,
            notas = solicitud.notas.ifBlank { null },
            avances = listOf(
                Avance(
                    etapa = EtapaOrden.Solicitud,
                    fecha = ahora,
                    nota = "Solicitud enviada desde la app. Ingreso agendado para " +
                        "${Formato.diaYHora(solicitud.cita).replaceFirstChar { it.lowercase() }}.",
                ),
            ),
        )
        todas.update { it + orden }
        avisos.update { lista ->
            lista + Notificacion(
                id = "aviso-${orden.id}-cliente",
                usuarioId = usuario.id,
                tipo = TipoNotificacion.Avance,
                titulo = "El taller recibió tu solicitud",
                detalle = "${orden.codigo}, ${orden.vehiculo}",
                fecha = ahora,
                leida = false,
                ordenId = orden.id,
            ) + Notificacion(
                id = "aviso-${orden.id}-taller",
                usuarioId = "taller-1",
                tipo = TipoNotificacion.Avance,
                titulo = "Nueva solicitud ${orden.codigo}",
                detalle = "${orden.vehiculo}, ${usuario.nombre}",
                fecha = ahora,
                leida = false,
                ordenId = orden.id,
            )
        }
        return orden
    }

    override suspend fun registrarAvance(usuario: Usuario, idOrden: String, avance: NuevoAvance): ResultadoAvance {
        delay(700)
        val orden = todas.value.firstOrNull { it.id == idOrden }
            ?: return ResultadoAvance.Fallo("No encontramos este trabajo. Vuelve a la lista e inténtalo de nuevo.")
        if (usuario.rol != Rol.Taller || orden.taller != usuario.taller) {
            return ResultadoAvance.Fallo("Solo el taller de esta orden puede registrar avances.")
        }
        if (orden.etapa == EtapaOrden.Entregada) {
            return ResultadoAvance.Fallo("Este trabajo ya se entregó.")
        }
        val cambia = avance.etapa != orden.etapa
        if (cambia && avance.etapa != orden.etapa.siguiente) {
            return ResultadoAvance.Fallo("Las etapas van en orden: la siguiente es «${orden.etapa.siguiente?.titulo}».")
        }
        val cotizacion = avance.cotizacion.orEmpty()
        val cotizando = cambia && avance.etapa == EtapaOrden.Cotizacion
        if (cotizando && cotizacion.sumOf { it.minimo } <= 0L) {
            return ResultadoAvance.Fallo("Agrega al menos un servicio con su valor.")
        }
        val pagada = orden.pagada || avance.cobradoEnTaller
        if (avance.etapa == EtapaOrden.Entregada && !pagada) {
            return ResultadoAvance.Fallo("Registra el cobro antes de entregar el rin.")
        }
        val nota = avance.nota?.trim()?.ifBlank { null }
        val detalle = avance.detalleEtapa?.trim()
        if (!cambia && avance.fotos.isEmpty() && nota == null && (detalle == null || detalle == orden.detalleEtapa)) {
            return ResultadoAvance.Fallo("Agrega una foto, una nota o lo que se está haciendo.")
        }

        val ahora = LocalDateTime.now()
        val nombre = usuario.nombre.trim().substringBefore(' ')
        val total = if (cotizando) cotizacion.sumOf { it.minimo } else orden.total
        val registro = Avance(
            etapa = avance.etapa,
            fecha = ahora,
            nota = nota ?: notaAutomatica(avance.etapa, cambia, total, avance.cobradoEnTaller),
            fotos = avance.fotos,
            registradoPor = "$nombre, técnico",
        )
        val actualizada = orden.copy(
            etapa = avance.etapa,
            detalleEtapa = when {
                detalle != null -> detalle.ifBlank { null }
                cambia -> detallePorDefecto(avance.etapa, pagada)
                else -> orden.detalleEtapa
            },
            tecnico = orden.tecnico ?: nombre,
            total = total,
            cotizacion = if (cotizando) cotizacion else orden.cotizacion,
            entregaEstimada = avance.entregaEstimada ?: orden.entregaEstimada,
            pagada = pagada,
            avances = orden.avances + registro,
        )
        todas.update { lista -> lista.map { if (it.id == idOrden) actualizada else it } }
        avisos.update { it + avisoAlCliente(actualizada, cambia, avance.fotos.isNotEmpty(), ahora) }
        return ResultadoAvance.Hecho(actualizada)
    }

    private fun notaAutomatica(etapa: EtapaOrden, cambia: Boolean, total: Long?, cobrado: Boolean): String? = when {
        !cambia -> null
        etapa == EtapaOrden.Cotizacion && total != null -> "Cotización confirmada: ${Formato.pesos(total)}."
        etapa == EtapaOrden.Entregada && cobrado && total != null -> "Pagaste ${Formato.pesos(total)} en el taller."
        else -> null
    }

    private fun detallePorDefecto(etapa: EtapaOrden, pagada: Boolean): String? = when (etapa) {
        EtapaOrden.Solicitud -> "Ingreso agendado"
        EtapaOrden.Inspeccion -> "Revisión del rin"
        EtapaOrden.Cotizacion -> "Esperando turno de reparación"
        EtapaOrden.Reparacion -> "En reparación"
        EtapaOrden.Calidad -> "Balanceo y revisión final"
        EtapaOrden.Lista -> if (pagada) "Listo para recoger" else "Cobro en el taller"
        EtapaOrden.Entregada -> null
    }

    private fun avisoAlCliente(orden: Orden, cambia: Boolean, conFotos: Boolean, ahora: LocalDateTime): Notificacion {
        val (tipo, titulo) = when {
            !cambia && conFotos -> TipoNotificacion.Fotos to "Nuevas fotos de tu reparación"
            !cambia -> TipoNotificacion.Avance to "Novedad en tu reparación"
            else -> when (orden.etapa) {
                EtapaOrden.Solicitud -> TipoNotificacion.Avance to "El taller recibió tu solicitud"
                EtapaOrden.Inspeccion -> TipoNotificacion.Avance to "El taller recibió tu rin"
                EtapaOrden.Cotizacion -> TipoNotificacion.Cotizacion to "Cotización confirmada por el taller"
                EtapaOrden.Reparacion -> TipoNotificacion.Avance to "Empezó la reparación de tu rin"
                EtapaOrden.Calidad -> TipoNotificacion.Avance to "Tu rin está en control de calidad"
                EtapaOrden.Lista -> TipoNotificacion.Entrega to "Tu rin está listo para recoger"
                EtapaOrden.Entregada -> TipoNotificacion.Entrega to "Recogiste tu rin"
            }
        }
        val detalle = when {
            cambia && orden.etapa == EtapaOrden.Cotizacion -> "${orden.codigo}, ${Formato.pesos(orden.total ?: 0)}"
            else -> orden.detalleEtapa ?: "${orden.codigo}, ${orden.vehiculo}"
        }
        return Notificacion(
            id = "aviso-${orden.id}-${orden.avances.size}",
            usuarioId = orden.clienteId,
            tipo = tipo,
            titulo = titulo,
            detalle = detalle,
            fecha = ahora,
            leida = false,
            ordenId = orden.id,
        )
    }

    internal companion object {

        const val Taller = "Taller RinFix Cabecera"

        /** Horas para recibir rines: de lunes a viernes mañana y tarde; sábados solo en la mañana. */
        val HorasSemana: List<LocalTime> = listOf(8, 9, 10, 11, 14, 15, 16, 17).map { LocalTime.of(it, 0) }
        val HorasSabado: List<LocalTime> = listOf(8, 9, 10, 11).map { LocalTime.of(it, 0) }

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
                cotizacion = listOf(
                    LineaCotizacion("Reparación de rayones y pintura", 140_000, 140_000, "Rin 17\", aleación, acabado pintado"),
                    LineaCotizacion("Enderezado del borde", 70_000, 70_000, "El taller confirmó la deformación"),
                    LineaCotizacion("Balanceo", 0, 0),
                ),
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
                cotizacion = listOf(
                    LineaCotizacion("Enderezado del borde", 95_000, 95_000, "Rin 14\", acero"),
                    LineaCotizacion("Balanceo", 0, 0),
                ),
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
                cotizacion = listOf(
                    LineaCotizacion("Enderezado de 2 rines", 150_000, 150_000, "Rines 16\", aleación"),
                    LineaCotizacion("Balanceo de 4 ruedas", 30_000, 30_000),
                ),
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
                // Ingreso en un par de horas si aún es temprano; si no, mañana a primera hora.
                cita = if (ahora.hour in 7..15) dia(0, ahora.hour + 2) else dia(1, 9),
                notas = "Cayó en un hueco y el timón vibra a más de 60 km/h.",
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
                    id = "aviso-taller-1",
                    usuarioId = "taller-1",
                    tipo = TipoNotificacion.Avance,
                    titulo = "Nueva solicitud RF-1045",
                    detalle = "Chevrolet Onix, Jorge Prada",
                    fecha = ahora.minusHours(3),
                    leida = false,
                    ordenId = "orden-1045",
                ),
                Notificacion(
                    id = "aviso-taller-2",
                    usuarioId = "taller-1",
                    tipo = TipoNotificacion.Pago,
                    titulo = "Mónica Salazar pagó en la app",
                    detalle = "RF-1041, \$320.000",
                    fecha = hoy.minusDays(3).atTime(10, 20),
                    leida = true,
                    ordenId = "orden-1041",
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
