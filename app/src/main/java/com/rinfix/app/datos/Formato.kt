package com.rinfix.app.datos

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Cómo se muestran el dinero y las fechas en RinFix, con las costumbres de Colombia.
 * Pesos sin decimales y con punto de miles: $210.000.
 */
object Formato {

    val Colombia: Locale = Locale.forLanguageTag("es-CO")

    private val formatoDiaLargo = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Colombia)
    private val formatoDiaCorto = DateTimeFormatter.ofPattern("d MMM", Colombia)
    private val formatoDiaSemana = DateTimeFormatter.ofPattern("EEE d", Colombia)
    private val formatoHora = DateTimeFormatter.ofPattern("h:mm a", Colombia)

    /** $210.000 */
    fun pesos(valor: Long): String = "$" + "%,d".format(Colombia, valor)

    /** $180.000 – $260.000, o $180–260 mil si `corto` es `true`. */
    fun rangoPesos(minimo: Long, maximo: Long, corto: Boolean = false): String = when {
        minimo == maximo -> pesos(minimo)
        corto && minimo % 1000 == 0L && maximo % 1000 == 0L && maximo < 1_000_000 ->
            "\$${minimo / 1000}–${maximo / 1000} mil"
        else -> "${pesos(minimo)} – ${pesos(maximo)}"
    }

    /** Martes 17 de noviembre */
    fun fechaLarga(fecha: LocalDate): String = formatoDiaLargo.format(fecha).conMayuscula()

    /** 12 nov */
    fun fechaCorta(fecha: LocalDateTime): String = formatoDiaCorto.format(fecha).sinPuntos()

    /** 4:00 p. m. */
    fun hora(fecha: LocalDateTime): String = formatoHora.format(fecha)

    /** Hoy, 4:00 p. m. · Mañana, 9:00 a. m. · Jue 19, 4:00 p. m. */
    fun diaYHora(fecha: LocalDateTime, hoy: LocalDate = LocalDate.now()): String {
        val dia = when (fecha.toLocalDate()) {
            hoy -> "Hoy"
            hoy.plusDays(1) -> "Mañana"
            else -> formatoDiaSemana.format(fecha).sinPuntos().conMayuscula()
        }
        return "$dia, ${hora(fecha)}"
    }

    /** hace un momento · hace 25 min · hace 3 h · ayer · 12 nov */
    fun haceCuanto(fecha: LocalDateTime, ahora: LocalDateTime = LocalDateTime.now()): String {
        val minutos = Duration.between(fecha, ahora).toMinutes()
        return when {
            minutos < 1 -> "hace un momento"
            minutos < 60 -> "hace $minutos min"
            fecha.toLocalDate() == ahora.toLocalDate() -> "hace ${minutos / 60} h"
            fecha.toLocalDate() == ahora.toLocalDate().minusDays(1) -> "ayer"
            else -> fechaCorta(fecha)
        }
    }

    private fun String.conMayuscula(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(Colombia) else it.toString() }

    /** Android escribe «nov.» y «jue.»; en la app se ven sin punto. */
    private fun String.sinPuntos(): String = replace(".", "")
}
