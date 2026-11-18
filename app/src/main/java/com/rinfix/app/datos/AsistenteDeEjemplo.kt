package com.rinfix.app.datos

import java.text.Normalizer
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Asistente de EJEMPLO: responde con reglas sencillas usando los datos de la app
 * (órdenes, diagnósticos, garaje y catálogo del taller). No usa IA ni internet.
 *
 * Todas sus respuestas van marcadas como de ejemplo. [AsistenteConClaude] hereda de aquí la
 * conversación y cambia solo cómo se arma cada respuesta; si el servidor falla, vuelve a estas reglas.
 */
open class AsistenteDeEjemplo(
    protected val ordenes: ServicioOrdenes,
    protected val analisis: ServicioAnalisis,
    protected val garaje: ServicioGaraje,
) : ServicioAsistente {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val conversaciones = MutableStateFlow<Map<String, Conversacion>>(emptyMap())
    private val respondiendo = ConcurrentHashMap<String, Job>()
    private val siguiente = AtomicInteger(1)

    override fun conversacion(usuario: Usuario): Flow<Conversacion> =
        conversaciones.map { it[usuario.id] ?: Conversacion() }.distinctUntilChanged()

    override fun enviar(usuario: Usuario, texto: String, contexto: ContextoPregunta?) {
        val pregunta = texto.trim()
        if (pregunta.isEmpty() || conversaciones.value[usuario.id]?.escribiendo == true) return
        val mensaje = Mensaje(id = nuevoId(), autor = Autor.Persona, texto = pregunta, fecha = LocalDateTime.now())
        cambiar(usuario.id) { it.copy(mensajes = it.mensajes + mensaje, escribiendo = true) }
        val historial = conversaciones.value[usuario.id]?.mensajes.orEmpty()

        respondiendo[usuario.id] = alcance.launch {
            val respuesta = try {
                responder(usuario, pregunta, contexto, historial)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Respuesta(texto = "No pude armar la respuesta. Inténtalo de nuevo en un momento.")
            }
            // Las reglas de ejemplo responden al instante: una pausa de «escribiendo» las hace naturales.
            if (respuesta.deEjemplo) {
                delay(PausaMinima + minOf(respuesta.texto.length * 5L, PausaExtraMaxima))
            }
            val enviado = Mensaje(
                id = nuevoId(),
                autor = Autor.Asistente,
                texto = respuesta.texto,
                fecha = LocalDateTime.now(),
                piezas = respuesta.piezas,
                acciones = respuesta.acciones,
                sugerencias = respuesta.sugerencias,
                deEjemplo = respuesta.deEjemplo,
            )
            cambiar(usuario.id) { it.copy(mensajes = it.mensajes + enviado, escribiendo = false) }
        }
    }

    override fun reiniciar(usuario: Usuario) {
        respondiendo.remove(usuario.id)?.cancel()
        conversaciones.update { it - usuario.id }
    }

    private fun cambiar(idUsuario: String, cambio: (Conversacion) -> Conversacion) {
        conversaciones.update { mapa -> mapa + (idUsuario to cambio(mapa[idUsuario] ?: Conversacion())) }
    }

    private fun nuevoId(): String = "mensaje-${siguiente.getAndIncrement()}"

    // ---------- Reglas de respuesta ----------

    /**
     * Arma la respuesta a la última pregunta. `historial` trae la conversación completa,
     * incluida la pregunta, para quien la necesite (Claude); las reglas solo miran la pregunta.
     */
    @Suppress("UNUSED_PARAMETER")
    protected open suspend fun responder(
        usuario: Usuario,
        pregunta: String,
        contexto: ContextoPregunta?,
        historial: List<Mensaje>,
    ): Respuesta {
        val misOrdenes = ordenes.ordenes(usuario).first()
        val diagnosticos = analisis.historial(usuario).first()
        val vehiculos = garaje.vehiculos(usuario).first()
        val t = normalizar(pregunta)

        // Preguntas que llegan desde un diagnóstico o una orden.
        contexto?.idDiagnostico?.let { id ->
            diagnosticos.firstOrNull { it.id == id }?.let { return explicarDiagnostico(it) }
        }
        contexto?.idOrden?.let { id ->
            misOrdenes.firstOrNull { it.id == id }?.let { return contarOrdenes(listOf(it), misOrdenes) }
        }

        // «¿Cómo va la RF-1042?»
        CodigoOrden.find(t)?.let { encontrado ->
            val codigo = "RF-${encontrado.groupValues[1]}"
            val orden = misOrdenes.firstOrNull { it.codigo.equals(codigo, ignoreCase = true) }
                ?: return Respuesta(
                    texto = "No encontré la orden $codigo en tu cuenta. Revisa el código en la pestaña Órdenes.",
                    sugerencias = listOf("¿Cómo va mi orden?"),
                )
            return contarOrdenes(listOf(orden), misOrdenes)
        }

        val dano = danoEn(t)
        return when {
            Saludo.matches(t) -> saludar(usuario)
            Agradecimiento.matches(t) -> Respuesta(
                texto = "Con gusto. Si necesitas algo más, aquí estoy.",
                sugerencias = SugerenciasGenerales,
            )
            t.tiene("manejar", "rodar", "conducir", "andar con", "seguro", "peligro", "carretera", "viaje") ->
                seguridad(diagnosticos)
            t.tiene("pagar", "pago", "tarjeta", "pse", "billetera", "nequi", "efectivo") -> pagos(misOrdenes)
            t.tiene("tarda", "demora", "cuantos dias", "tiempo", "entrega", "cuando esta", "cuando me") ->
                tiempos(misOrdenes, dano)
            t.tiene("orden", "seguimiento", "como va", "como vamos", "como esta mi", "estado", "avance", "mi reparacion") ->
                contarOrdenes(misOrdenes.filter { it.activa }, misOrdenes)
            t.tiene("diagnostico", "resultado", "analisis", "significa", "que tiene mi rin") ->
                ultimoDiagnostico(diagnosticos)
            t.tiene("foto", "camara", "imagen", "angulo") -> fotos()
            dano != null || t.tiene("cuanto", "cuesta", "precio", "valor", "cobran", "cotiza", "tarifa") ->
                precios(dano, vehiculoPara(t, vehiculos))
            t.tiene("donde", "direccion", "ubica", "horario", "abren", "cierran", "queda", "taller") -> taller()
            else -> noSe()
        }
    }

    private fun saludar(usuario: Usuario) = Respuesta(
        texto = "Hola, ${usuario.nombre.substringBefore(" ")}. Puedo contarte cómo va tu reparación, " +
            "explicarte un diagnóstico o darte precios y tiempos del taller. ¿En qué te ayudo?",
        sugerencias = SugerenciasGenerales,
    )

    private fun contarOrdenes(lista: List<Orden>, todas: List<Orden>): Respuesta {
        if (lista.isEmpty()) {
            val ultima = todas.maxByOrNull { it.creada }
                ?: return Respuesta(
                    texto = "Todavía no tienes reparaciones. Cuando solicites una, aquí te cuento en qué va.",
                    acciones = listOf(
                        AccionRespuesta("Analizar un rin", Destino.Analizar),
                        AccionRespuesta("Agendar reparación", Destino.Agendar()),
                    ),
                    sugerencias = listOf("¿Qué precios tiene el taller?", "¿Dónde queda el taller?"),
                )
            return Respuesta(
                texto = "No tienes reparaciones en curso. La última fue la ${ultima.codigo}, de tu ${ultima.vehiculo}: " +
                    "${ultima.etapa.estado.lowercase()}.",
                piezas = listOf(Pieza.OrdenRelacionada(ultima.id)),
                acciones = listOf(AccionRespuesta("Ver seguimiento", Destino.Seguimiento(ultima.id))),
                sugerencias = listOf("¿Qué precios tiene el taller?", "¿Cómo tomo buenas fotos del rin?"),
            )
        }
        if (lista.size == 1) {
            val orden = lista.first()
            return Respuesta(
                texto = estadoDe(orden),
                piezas = listOf(Pieza.OrdenRelacionada(orden.id)),
                acciones = buildList {
                    add(AccionRespuesta("Ver seguimiento", Destino.Seguimiento(orden.id)))
                    if (orden.pagoPendiente) {
                        add(AccionRespuesta("Pagar ${Formato.pesos(orden.total ?: 0)}", Destino.Pagar(orden.id)))
                    }
                },
                sugerencias = listOf("¿Cuánto tarda la reparación?", "¿Cómo pago mi reparación?", "¿Dónde queda el taller?"),
            )
        }
        return Respuesta(
            texto = "Tienes ${lista.size} reparaciones en curso. " +
                lista.joinToString(" ") { "La ${it.codigo} va en ${it.etapa.titulo.lowercase()}." },
            piezas = lista.take(3).map { Pieza.OrdenRelacionada(it.id) },
            sugerencias = listOf("¿Cuánto tarda la reparación?", "¿Cómo pago mi reparación?"),
        )
    }

    private fun estadoDe(orden: Orden): String = buildString {
        when (orden.etapa) {
            EtapaOrden.Lista -> append("Tu ${orden.rin} del ${orden.vehiculo} ya está listo para recoger en el taller.")
            EtapaOrden.Entregada -> append("La orden ${orden.codigo} ya se entregó.")
            else -> {
                append(
                    "La orden ${orden.codigo}, de tu ${orden.vehiculo}, va en el paso ${orden.etapa.numero} de " +
                        "${EtapaOrden.total}: ${orden.etapa.titulo.lowercase()}.",
                )
                orden.detalleEtapa?.let { append(" Ahora mismo: ${minuscula(it).trimEnd('.')}.") }
                orden.entregaEstimada?.let { append(" Entrega estimada: ${minuscula(Formato.diaYHora(it))}.") }
            }
        }
        if (orden.pagoPendiente) append(" Queda un pago pendiente de ${Formato.pesos(orden.total ?: 0)}.")
    }

    private fun ultimoDiagnostico(diagnosticos: List<Diagnostico>): Respuesta =
        diagnosticos.firstOrNull()?.let(::explicarDiagnostico) ?: Respuesta(
            texto = "Todavía no has analizado ningún rin. Toma una foto y en unos segundos te muestro " +
                "lo que se ve y cuánto costaría arreglarlo.",
            acciones = listOf(AccionRespuesta("Analizar un rin", Destino.Analizar)),
            sugerencias = listOf("¿Cómo tomo buenas fotos del rin?", "¿Qué precios tiene el taller?"),
        )

    private fun explicarDiagnostico(d: Diagnostico): Respuesta {
        val fecha = Formato.fechaCorta(d.fecha)
        val delVehiculo = d.vehiculo?.let { " de tu $it" }.orEmpty()
        if (d.danos.isEmpty()) {
            return Respuesta(
                texto = "Tu análisis del $fecha$delVehiculo no encontró daños visibles. Si notas vibración o " +
                    "pérdida de aire, igual vale la pena una revisión en el taller.",
                piezas = listOf(Pieza.DiagnosticoRelacionado(d.id)),
                acciones = listOf(AccionRespuesta("Agendar revisión", Destino.Agendar(d.id))),
                sugerencias = SugerenciasGenerales,
            )
        }
        val cuantos = if (d.danos.size == 1) "un posible daño" else "${d.danos.size} posibles daños"
        val texto = buildString {
            append("Tu análisis del $fecha$delVehiculo encontró $cuantos, con gravedad ${d.gravedad.nombre.lowercase()}. ")
            append(
                "Con las tarifas del taller, la reparación saldría entre ${Formato.pesos(d.precioMinimo)} y " +
                    "${Formato.pesos(d.precioMaximo)} y tomaría ${dias(d.diasMinimos, d.diasMaximos)}.",
            )
            if (d.deEjemplo) append(" Ten en cuenta que este diagnóstico es de ejemplo, no de una foto analizada de verdad.")
        }
        return Respuesta(
            texto = texto,
            piezas = listOfNotNull(
                Pieza.Tabla(
                    titulo = "Lo que se ve en la foto",
                    filas = d.danos.map { dano ->
                        FilaTabla(
                            nombre = dano.descripcion,
                            valor = dano.gravedad.nombre,
                            detalle = if (dano.confirmado) null else "Falta que el taller lo confirme",
                        )
                    },
                ),
                if (d.requiereInspeccion) {
                    Pieza.Cuidado(
                        d.alerta ?: "Algo de lo que se ve puede afectar la seguridad. No uses ese rin en " +
                            "carretera hasta que el taller lo revise.",
                    )
                } else {
                    null
                },
                Pieza.DiagnosticoRelacionado(d.id),
            ),
            acciones = listOf(
                AccionRespuesta("Agendar reparación", Destino.Agendar(d.id)),
                AccionRespuesta("Ver diagnóstico", Destino.VerDiagnostico(d.id)),
            ),
            sugerencias = listOf("¿Puedo seguir manejando así?", "¿Cuánto tarda la reparación?", "¿Cómo tomo buenas fotos del rin?"),
        )
    }

    private fun seguridad(diagnosticos: List<Diagnostico>): Respuesta {
        val ultimo = diagnosticos.firstOrNull()
        val cuidado = if (ultimo?.requiereInspeccion == true) {
            "Tu último análisis marcó algo que puede afectar la seguridad. No uses ese rin en carretera " +
                "hasta que el taller lo revise."
        } else {
            "Ante la duda, pide una revisión. Solo el taller puede confirmar que el rin está bien."
        }
        return Respuesta(
            texto = "Desde una foto no puedo confirmar que un rin sea seguro. Deja de rodar y pide una revisión " +
                "si notas alguna de estas señales:",
            piezas = listOf(
                Pieza.Puntos(
                    listOf(
                        "El timón vibra o el carro jala hacia un lado.",
                        "La llanta pierde aire seguido.",
                        "Ves una grieta, una fisura o el borde partido.",
                        "El golpe fue fuerte, por ejemplo un hueco a buena velocidad.",
                    ),
                ),
                Pieza.Cuidado(cuidado),
            ),
            acciones = listOf(
                AccionRespuesta("Agendar revisión", Destino.Agendar(ultimo?.id)),
                AccionRespuesta("Analizar un rin", Destino.Analizar),
            ),
            sugerencias = listOf("¿Dónde queda el taller?", "¿Cuánto cuesta enderezar un rin?"),
        )
    }

    private fun pagos(misOrdenes: List<Orden>): Respuesta {
        val pendiente = misOrdenes.firstOrNull { it.pagoPendiente }
        val aviso = "Por ahora los pagos en RinFix son de demostración: no se cobra dinero real."
        if (pendiente == null) {
            return Respuesta(
                texto = "No tienes pagos pendientes. Cuando el taller confirme el total de una reparación, puedes " +
                    "pagar con tarjeta, PSE, billetera digital o al recoger el rin. $aviso",
                sugerencias = SugerenciasGenerales,
            )
        }
        val total = Formato.pesos(pendiente.total ?: 0)
        return Respuesta(
            texto = "La orden ${pendiente.codigo} tiene un pago pendiente de $total. Puedes pagarla desde la app " +
                "o al recoger el rin en el taller. $aviso",
            acciones = listOf(
                AccionRespuesta("Pagar $total", Destino.Pagar(pendiente.id)),
                AccionRespuesta("Ver seguimiento", Destino.Seguimiento(pendiente.id)),
            ),
            sugerencias = listOf("¿Cuánto tarda la reparación?", "¿Dónde queda el taller?"),
        )
    }

    private fun tiempos(misOrdenes: List<Orden>, dano: TipoDano?): Respuesta {
        if (dano != null) {
            val servicio = Catalogo.servicios.getValue(dano)
            return Respuesta(
                texto = "${servicio.nombre}: suele tomar ${dias(servicio.diasMinimos, servicio.diasMaximos)} " +
                    "desde que el taller recibe el rin.",
                acciones = listOf(AccionRespuesta("Agendar reparación", Destino.Agendar())),
                sugerencias = listOf("¿Qué precios tiene el taller?", "¿Dónde queda el taller?"),
            )
        }
        val activa = misOrdenes.firstOrNull { it.activa }
        if (activa != null) {
            val entrega = activa.entregaEstimada
            val texto = when {
                activa.etapa == EtapaOrden.Lista ->
                    "Tu ${activa.rin} del ${activa.vehiculo} ya está listo. Puedes recogerlo en el horario del taller."
                entrega != null ->
                    "Entrega estimada de la orden ${activa.codigo}: ${minuscula(Formato.diaYHora(entrega))}. " +
                        "Si algo cambia, el taller lo actualiza en el seguimiento."
                else ->
                    "La fecha de entrega de la orden ${activa.codigo} se define después de la inspección en el taller. " +
                        "Te aviso cuando la registren."
            }
            return Respuesta(
                texto = texto,
                piezas = listOf(Pieza.OrdenRelacionada(activa.id)),
                acciones = listOf(AccionRespuesta("Ver seguimiento", Destino.Seguimiento(activa.id))),
                sugerencias = listOf("¿Cómo pago mi reparación?", "¿Dónde queda el taller?"),
            )
        }
        return Respuesta(
            texto = "Depende del daño. Estos son los tiempos del taller desde que recibe el rin:",
            piezas = listOf(
                Pieza.Tabla(
                    titulo = "Tiempos del taller",
                    filas = Catalogo.servicios.filterKeys { it != TipoDano.Otro }.values.map {
                        FilaTabla(nombre = it.nombre, valor = dias(it.diasMinimos, it.diasMaximos))
                    },
                    nota = "Si son varios daños, se suma un día por cada servicio adicional.",
                ),
            ),
            acciones = listOf(AccionRespuesta("Agendar reparación", Destino.Agendar())),
            sugerencias = listOf("¿Qué precios tiene el taller?", "¿Dónde queda el taller?"),
        )
    }

    private fun precios(dano: TipoDano?, vehiculo: Vehiculo?): Respuesta {
        val conMedidas = vehiculo?.takeIf { it.pulgadasRin != null }
        val material = conMedidas?.materialRin ?: MaterialRin.Aleacion
        val pulgadas = conMedidas?.pulgadasRin ?: 17
        val para = conMedidas?.let { " para tu ${it.nombre} (${minuscula(it.descripcionRin)})" }
            ?: " para un rin de 17\" de aleación"
        val incluido = Catalogo.incluidos.joinToString(" y ").lowercase()

        fun lineaDe(tipo: TipoDano): LineaCotizacion =
            Catalogo.cotizar(listOf(danoDeReferencia(tipo)), material, pulgadas).first { !it.incluido }

        if (dano != null) {
            val linea = lineaDe(dano)
            val rango = Catalogo.dias(listOf(danoDeReferencia(dano)))
            return Respuesta(
                texto = "Esta es la tarifa$para. Suele tomar ${dias(rango.first, rango.last)} y el $incluido va incluido.",
                piezas = listOfNotNull(
                    Pieza.Tabla(
                        titulo = "Tarifa del taller",
                        filas = listOf(FilaTabla(linea.servicio, Formato.rangoPesos(linea.minimo, linea.maximo), linea.detalle)),
                        nota = "El valor final lo confirma el taller cuando revisa el rin.",
                        dePrecios = true,
                    ),
                    if (dano == TipoDano.Fisura) {
                        Pieza.Cuidado("Una fisura puede afectar la seguridad. No uses ese rin en carretera hasta que el taller lo revise.")
                    } else {
                        null
                    },
                ),
                acciones = listOf(
                    AccionRespuesta("Analizar mi rin", Destino.Analizar),
                    AccionRespuesta("Agendar reparación", Destino.Agendar()),
                ),
                sugerencias = listOf("¿Qué precios tiene el taller?", "¿Puedo seguir manejando así?", "¿Dónde queda el taller?"),
            )
        }
        return Respuesta(
            texto = "Estas son las tarifas del taller$para. Para saber cuál aplica a tu caso, analiza el rin con " +
                "una foto y te armo la cotización.",
            piezas = listOf(
                Pieza.Tabla(
                    titulo = "Tarifas del taller",
                    filas = Catalogo.servicios.keys.filter { it != TipoDano.Otro }.map { tipo ->
                        val linea = lineaDe(tipo)
                        FilaTabla(linea.servicio, Formato.rangoPesos(linea.minimo, linea.maximo, corto = true))
                    },
                    nota = "El ${incluido} va incluido. El valor final lo confirma el taller cuando revisa el rin.",
                    dePrecios = true,
                ),
            ),
            acciones = listOf(AccionRespuesta("Analizar mi rin", Destino.Analizar)),
            sugerencias = listOf("¿Cuánto tarda la reparación?", "¿Dónde queda el taller?"),
        )
    }

    private fun taller() = Respuesta(
        texto = "${Catalogo.Taller} queda en ${Catalogo.DireccionTaller}. Si agendas una cita, el taller te " +
            "espera a la hora que elijas.",
        piezas = listOf(
            Pieza.Tabla(
                titulo = "Horario",
                filas = listOf(
                    FilaTabla("Lunes a viernes", "8:00 a. m. a 6:00 p. m.", "Cerrado de 12:00 m. a 2:00 p. m."),
                    FilaTabla("Sábados", "8:00 a. m. a 12:00 m."),
                    FilaTabla("Domingos", "Cerrado"),
                ),
            ),
        ),
        acciones = listOf(
            AccionRespuesta("Cómo llegar", Destino.ComoLlegar),
            AccionRespuesta("Agendar cita", Destino.Agendar()),
        ),
        sugerencias = listOf("¿Qué precios tiene el taller?", "¿Cómo va mi orden?"),
    )

    private fun fotos() = Respuesta(
        texto = "Así salen mejor las fotos para el análisis:",
        piezas = listOf(
            Pieza.Puntos(
                listOf(
                    "Busca buena luz, sin sombras fuertes ni el sol de frente.",
                    "Que el rin se vea completo y centrado, a un paso de distancia.",
                    "Toma hasta 3 fotos: de frente, del borde y de cerca al daño.",
                    "Si tiene mucho polvo o barro, límpialo antes.",
                ),
            ),
        ),
        acciones = listOf(AccionRespuesta("Analizar un rin", Destino.Analizar)),
        sugerencias = listOf("¿Qué precios tiene el taller?", "¿Dónde queda el taller?"),
    )

    private fun noSe() = Respuesta(
        texto = "En este modo de ejemplo todavía no sé responder eso. Puedo ayudarte con tus órdenes, tus " +
            "diagnósticos, precios, tiempos, pagos, fotos y el taller.",
        sugerencias = SugerenciasGenerales,
    )

    private fun vehiculoPara(t: String, vehiculos: List<Vehiculo>): Vehiculo? =
        vehiculos.firstOrNull { t.contains(normalizar(it.nombre)) } ?: vehiculos.firstOrNull { it.pulgadasRin != null }

    private fun danoEn(t: String): TipoDano? = when {
        t.tiene("fisura", "grieta", "partid", "rajad", "soldad") -> TipoDano.Fisura
        t.tiene("enderez", "doblad", "torcid", "deform", "chueco", "golpe", "hueco") -> TipoDano.Deformacion
        t.tiene("raspad", "anden", "sardinel", "raspon") -> TipoDano.Raspadura
        t.tiene("rayon", "rayad", "raya") -> TipoDano.Rayon
        t.tiene("corros", "oxid", "sulfat") -> TipoDano.Corrosion
        t.tiene("pintura", "pintar") -> TipoDano.Pintura
        else -> null
    }

    /** Lo que arma cada regla antes de convertirse en mensaje. */
    protected data class Respuesta(
        val texto: String,
        val piezas: List<Pieza> = emptyList(),
        val acciones: List<AccionRespuesta> = emptyList(),
        val sugerencias: List<String> = emptyList(),
        /** `true` si salió de las reglas de ejemplo; `false` si la escribió Claude. */
        val deEjemplo: Boolean = true,
    )

    private companion object {
        const val PausaMinima = 700L
        const val PausaExtraMaxima = 1_100L

        val CodigoOrden = Regex("rf ?-? ?(\\d{3,6})")
        val Saludo = Regex("^(hola|holi|buenas|buenos dias|buenas tardes|buenas noches|hey|que mas|quiubo)( [a-z]+)?$")
        val Agradecimiento = Regex("^(muchas )?(gracias|listo|perfecto|ok|vale|genial|chevere|super)( [a-z]+)?$")
        val SugerenciasGenerales = listOf("¿Cómo va mi orden?", "¿Qué precios tiene el taller?", "¿Dónde queda el taller?")

        private val Acentos = Regex("\\p{Mn}+")
        private val Signos = Regex("[¿?¡!.,;:()\"]")
        private val Espacios = Regex("\\s+")

        /** Sin tildes, en minúscula y sin signos, para comparar palabras sin importar cómo se escriban. */
        fun normalizar(texto: String): String = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Acentos, "")
            .lowercase()
            .replace(Signos, " ")
            .replace(Espacios, " ")
            .trim()

        /** Un daño moderado del tipo pedido, solo para consultar la tarifa del catálogo. */
        fun danoDeReferencia(tipo: TipoDano) =
            Dano(tipo = tipo, descripcion = tipo.nombre, gravedad = Gravedad.Moderada, confianza = 1f)

        fun dias(minimo: Int, maximo: Int): String = when {
            minimo == maximo && minimo == 1 -> "1 día hábil"
            minimo == maximo -> "$minimo días hábiles"
            else -> "$minimo a $maximo días hábiles"
        }

        fun minuscula(texto: String): String = texto.replaceFirstChar { it.lowercase() }
    }
}

private fun String.tiene(vararg raices: String): Boolean = raices.any { contains(it) }
