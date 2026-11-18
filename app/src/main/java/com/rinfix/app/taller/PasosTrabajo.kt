package com.rinfix.app.taller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.Campo
import com.rinfix.app.componentes.Casilla
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.Hoja
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.SelectorFichas
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.componentes.lineaPunteadaArriba
import com.rinfix.app.componentes.tipoEtiqueta
import com.rinfix.app.datos.Catalogo
import com.rinfix.app.datos.EtapaOrden
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.LineaCotizacion
import com.rinfix.app.datos.Orden
import com.rinfix.app.diseno.Colores
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/** Lo que el taller hace para pasar una orden a la etapa siguiente. */
internal data class PasoTrabajo(
    val titulo: String,
    val descripcion: String,
    val boton: String,
    val icono: ImageVector,
    val pregunta: String,
    val ejemploNota: String,
)

/** El paso que sigue desde esta etapa, o `null` si el trabajo ya se entregó. */
internal fun pasoDesde(etapa: EtapaOrden): PasoTrabajo? = when (etapa) {
    EtapaOrden.Solicitud -> PasoTrabajo(
        titulo = "Recibir el rin",
        descripcion = "Cuando el cliente llegue, regístralo para empezar la inspección.",
        boton = "Recibir el rin",
        icono = Iconos.Herramienta,
        pregunta = "¿Recibiste el rin?",
        ejemploNota = "Ej.: Llegó con la llanta montada.",
    )
    EtapaOrden.Inspeccion -> PasoTrabajo(
        titulo = "Confirmar la cotización",
        descripcion = "Después de revisar el rin, confirma los servicios y el valor exacto con el cliente.",
        boton = "Armar cotización",
        icono = Iconos.Recibo,
        pregunta = "Cotización",
        ejemploNota = "Ej.: Se confirmó la deformación del borde.",
    )
    EtapaOrden.Cotizacion -> PasoTrabajo(
        titulo = "Empezar la reparación",
        descripcion = "El cliente ya conoce el valor. Avísale cuando empieces.",
        boton = "Empezar reparación",
        icono = Iconos.Herramienta,
        pregunta = "¿Empezar la reparación?",
        ejemploNota = "Ej.: Empezamos con el enderezado.",
    )
    EtapaOrden.Reparacion -> PasoTrabajo(
        titulo = "Pasar a control de calidad",
        descripcion = "Cuando termines, revisa balanceo y acabado antes de entregar.",
        boton = "Pasar a calidad",
        icono = Iconos.Escudo,
        pregunta = "¿Pasar a control de calidad?",
        ejemploNota = "Ej.: Enderezado y pintura terminados.",
    )
    EtapaOrden.Calidad -> PasoTrabajo(
        titulo = "Marcar como listo",
        descripcion = "El cliente recibe un aviso para venir por su rin.",
        boton = "Marcar como listo",
        icono = Iconos.Hecho,
        pregunta = "¿El rin está listo?",
        ejemploNota = "Ej.: Quedó balanceado y sin vibración.",
    )
    EtapaOrden.Lista -> PasoTrabajo(
        titulo = "Entregar al cliente",
        descripcion = "Registra la entrega cuando el cliente se lleve su rin.",
        boton = "Entregar",
        icono = Iconos.Carro,
        pregunta = "¿Entregar el rin?",
        ejemploNota = "Ej.: Se entregó con la llanta a 32 PSI.",
    )
    EtapaOrden.Entregada -> null
}

/** Suma días hábiles (sin domingos) y deja la hora en que el taller entrega ese día. */
internal fun entregaEn(diasHabiles: Int, desde: LocalDate = LocalDate.now()): LocalDateTime {
    var dia = desde
    var faltan = diasHabiles
    while (faltan > 0) {
        dia = dia.plusDays(1)
        if (dia.dayOfWeek != DayOfWeek.SUNDAY) faltan--
    }
    return dia.atTime(if (dia.dayOfWeek == DayOfWeek.SATURDAY) 11 else 17, 0)
}

/** «Recepción» → «Inspección», con una flecha en medio. */
@Composable
private fun DeEtapaAEtapa(actual: EtapaOrden, siguiente: EtapaOrden) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp),
    ) {
        Etiqueta(texto = actual.estado, tipo = tipoEtiqueta(actual), pequena = true)
        Icon(Iconos.Siguiente, contentDescription = "pasa a", tint = Colores.Tinta3, modifier = Modifier.size(16.dp))
        Etiqueta(texto = siguiente.estado, tipo = tipoEtiqueta(siguiente), conPunto = true, pequena = true)
    }
}

/**
 * Confirmación para pasar a la etapa siguiente, con nota opcional para el cliente.
 * Al entregar, pide registrar el cobro si todavía no está pagado.
 */
@Composable
internal fun HojaCambiarEtapa(
    orden: Orden,
    actual: EtapaOrden,
    paso: PasoTrabajo,
    siguiente: EtapaOrden,
    guardando: Boolean,
    error: String?,
    alConfirmar: (nota: String, cobrado: Boolean, cerrar: () -> Unit) -> Unit,
    alCerrar: () -> Unit,
) {
    var nota by rememberSaveable { mutableStateOf("") }
    var cobrado by rememberSaveable { mutableStateOf(false) }
    val debeCobrar = siguiente == EtapaOrden.Entregada && !orden.pagada
    Hoja(alCerrar = alCerrar) { cerrar ->
        Text(
            text = paso.pregunta,
            style = MaterialTheme.typography.headlineSmall,
            color = Colores.Tinta,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Orden ${orden.codigo}, ${orden.vehiculo}. El cliente ve el cambio en su seguimiento y recibe un aviso.",
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.Tinta3,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        DeEtapaAEtapa(actual = actual, siguiente = siguiente)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Campo(
                valor = nota,
                alCambiar = { nota = it.take(MaximoNota) },
                etiqueta = "Nota para el cliente",
                ayuda = "Opcional",
                marcador = paso.ejemploNota,
                lineas = 3,
                opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                habilitado = !guardando,
            )
            if (siguiente == EtapaOrden.Entregada) {
                val total = orden.total
                if (debeCobrar && total != null) {
                    Casilla(
                        marcada = cobrado,
                        alCambiar = { cobrado = it },
                        texto = "Cobré ${Formato.pesos(total)} en el taller",
                        habilitada = !guardando,
                    )
                } else {
                    Text(
                        text = "El pago ya está registrado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Colores.MentaTexto,
                    )
                }
            }
            AnimatedVisibility(visible = error != null) {
                Aviso(titulo = "No se guardó.", mensaje = error.orEmpty(), tipo = TipoAviso.Error)
            }
            Boton(
                texto = paso.boton,
                alPulsar = { alConfirmar(nota, cobrado, cerrar) },
                icono = paso.icono,
                ocupado = guardando,
                habilitado = !debeCobrar || cobrado,
                modifier = Modifier.fillMaxWidth(),
            )
            Boton(
                texto = "Cancelar",
                alPulsar = cerrar,
                estilo = EstiloBoton.Transparente,
                habilitado = !guardando,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Cotización que el taller confirma después de inspeccionar: servicios del catálogo con su
 * valor exacto, el balanceo incluido y la fecha de entrega en días hábiles.
 */
@Composable
internal fun HojaCotizar(
    orden: Orden,
    guardando: Boolean,
    error: String?,
    alConfirmar: (lineas: List<LineaCotizacion>, entrega: LocalDateTime, nota: String, cerrar: () -> Unit) -> Unit,
    alCerrar: () -> Unit,
) {
    val servicios = Catalogo.servicios.values.toList()
    // Valor escrito por servicio elegido, en pesos y solo con números.
    var valores by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    var dias by rememberSaveable { mutableIntStateOf(2) }
    var nota by rememberSaveable { mutableStateOf("") }

    val lineas = servicios.mapNotNull { servicio ->
        val valor = valores[servicio.nombre]?.toLongOrNull() ?: return@mapNotNull null
        LineaCotizacion(servicio = servicio.nombre, minimo = valor, maximo = valor)
    }
    val total = lineas.sumOf { it.minimo }
    val entrega = entregaEn(dias)
    val completa = valores.isNotEmpty() && valores.values.all { (it.toLongOrNull() ?: 0L) in 1L..MaximoValor }

    Hoja(alCerrar = alCerrar) { cerrar ->
        Text(
            text = "Cotización de ${orden.codigo}",
            style = MaterialTheme.typography.headlineSmall,
            color = Colores.Tinta,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Marca los servicios y escribe el valor exacto. El cliente la ve en su seguimiento y puede pagar desde la app.",
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.Tinta3,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            servicios.forEach { servicio ->
                val elegido = servicio.nombre in valores
                Casilla(
                    marcada = elegido,
                    alCambiar = { marcar ->
                        valores = if (marcar) {
                            valores + (servicio.nombre to servicio.minimo.toString())
                        } else {
                            valores - servicio.nombre
                        }
                        // La entrega sugerida sigue al servicio que más tarda.
                        val elegidos = servicios.filter { it.nombre in valores }
                        if (elegidos.isNotEmpty()) {
                            val sugeridos = elegidos.maxOf { it.diasMaximos } + elegidos.size - 1
                            dias = OpcionesDias.firstOrNull { it >= sugeridos } ?: OpcionesDias.last()
                        }
                    },
                    texto = servicio.nombre,
                    habilitada = !guardando,
                )
                AnimatedVisibility(visible = elegido) {
                    val texto = valores[servicio.nombre].orEmpty()
                    val numero = texto.toLongOrNull()
                    Campo(
                        valor = texto,
                        alCambiar = { nuevo -> valores = valores + (servicio.nombre to nuevo.filter { it.isDigit() }.take(7)) },
                        etiqueta = "Valor en pesos",
                        icono = Iconos.TarjetaPago,
                        error = if (numero == null || numero !in 1L..MaximoValor) "Escribe un valor entre \$1 y ${Formato.pesos(MaximoValor)}." else null,
                        ayuda = "Catálogo: ${Formato.rangoPesos(servicio.minimo, servicio.maximo)}" +
                            (numero?.let { ". Quedaría en ${Formato.pesos(it)}" } ?: ""),
                        tipoTeclado = KeyboardType.Number,
                        opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        habilitado = !guardando,
                        modifier = Modifier.padding(start = 36.dp),
                    )
                }
            }

            Text(
                text = "${Catalogo.incluidos.joinToString(" y ")}: incluido",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.Tinta3,
                modifier = Modifier.padding(start = 4.dp),
            )

            Text(
                text = "Entrega estimada",
                style = MaterialTheme.typography.titleSmall,
                color = Colores.Tinta,
                modifier = Modifier.padding(top = 6.dp),
            )
            SelectorFichas(
                opciones = OpcionesDias,
                seleccionada = dias,
                alElegir = { dias = it },
                texto = { if (it == 1) "1 día" else "$it días" },
                descripcion = { if (it == 1) "Entrega en 1 día hábil" else "Entrega en $it días hábiles" },
                habilitado = !guardando,
            )
            Text(
                text = "${Formato.fechaLarga(entrega.toLocalDate())}, ${Formato.hora(entrega)}",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.Tinta2,
            )

            Campo(
                valor = nota,
                alCambiar = { nota = it.take(MaximoNota) },
                etiqueta = "Nota para el cliente",
                ayuda = "Opcional",
                marcador = "Ej.: Se confirmó la deformación del borde.",
                lineas = 2,
                opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                habilitado = !guardando,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .lineaPunteadaArriba()
                .padding(top = 12.dp),
        ) {
            Text(
                text = "Total, IVA incluido",
                style = MaterialTheme.typography.titleSmall,
                color = Colores.Tinta,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Formato.pesos(total),
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                color = Colores.Tinta,
            )
        }
        AnimatedVisibility(visible = error != null) {
            Aviso(
                titulo = "No se guardó.",
                mensaje = error.orEmpty(),
                tipo = TipoAviso.Error,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Boton(
            texto = "Confirmar cotización",
            alPulsar = {
                val conIncluidos = lineas + Catalogo.incluidos.map { LineaCotizacion(servicio = it, minimo = 0, maximo = 0) }
                alConfirmar(conIncluidos, entrega, nota, cerrar)
            },
            icono = Iconos.Recibo,
            ocupado = guardando,
            habilitado = completa,
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth(),
        )
        if (!completa) {
            Text(
                text = "Marca al menos un servicio y revisa sus valores.",
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Tinta3,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private val OpcionesDias = listOf(1, 2, 3, 5)
private const val MaximoNota = 300
private const val MaximoValor = 5_000_000L
