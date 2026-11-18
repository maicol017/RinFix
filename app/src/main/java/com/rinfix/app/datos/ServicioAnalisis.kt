package com.rinfix.app.datos

import kotlinx.coroutines.flow.Flow

/** Fotos y datos del rin que la persona envía a analizar. */
data class SolicitudAnalisis(
    /** Rutas de las fotos en el teléfono (de 1 a 3). */
    val fotos: List<String>,
    val material: MaterialRin,
    val pulgadas: Int?,
    /** Vehículo del garaje, si lo eligió: «Mazda 3 2021». */
    val vehiculo: String?,
)

/** Pasos del análisis, en orden, con dónde ocurre cada uno. */
enum class PasoAnalisis(val titulo: String, val lugar: String) {
    Foto("Revisión de la foto", "En tu teléfono"),
    Deteccion("Detección con el modelo RinFix", "En tu teléfono, sin internet"),
    Interpretacion("Interpretación con Claude", "Servidor seguro, requiere conexión"),
    Cotizacion("Cotización con el catálogo del taller", "Reglas y tarifas en pesos"),
}

enum class EstadoPaso { Pendiente, EnCurso, Listo, Fallo }

data class PasoEnCurso(
    val paso: PasoAnalisis,
    val estado: EstadoPaso = EstadoPaso.Pendiente,
    /** Qué está pasando ahora, o el resultado del paso: «2 zonas posibles». */
    val detalle: String = paso.lugar,
)

/** Cómo va un análisis. Se actualiza paso a paso hasta tener `idDiagnostico`. */
data class AvanceAnalisis(
    val pasos: List<PasoEnCurso> = PasoAnalisis.entries.map { PasoEnCurso(it) },
    /** Foto principal enviada, para mostrarla mientras se analiza. */
    val foto: String? = null,
    /** Zonas que marcó la detección, para dibujarlas sobre la foto. */
    val zonas: List<Pair<String, Caja>> = emptyList(),
    val fotoRevisada: Boolean = false,
    /** Mientras se interpreta la zona con la IA. */
    val interpretando: Boolean = false,
    val idDiagnostico: String? = null,
    val error: String? = null,
    val deEjemplo: Boolean = false,
) {
    /** El estado de un paso. Si este análisis no usa ese paso, cuenta como listo. */
    fun estadoDe(paso: PasoAnalisis): PasoEnCurso =
        pasos.firstOrNull { it.paso == paso } ?: PasoEnCurso(paso, EstadoPaso.Listo)
}

/**
 * Análisis de fotos de rines.
 * Con el backend, el análisis lo hace un servidor en Vercel que llama a Claude;
 * la clave de la IA nunca está en el teléfono.
 */
interface ServicioAnalisis {

    /** Diagnósticos anteriores de la persona, el más reciente primero. */
    fun historial(usuario: Usuario): Flow<List<Diagnostico>>

    /** Un diagnóstico de la persona, o `null` si no existe. */
    fun diagnostico(usuario: Usuario, idDiagnostico: String): Flow<Diagnostico?>

    /**
     * Recibe las fotos y empieza el análisis. Devuelve el id del análisis en curso,
     * para seguir su avance en la pantalla «Analizando».
     */
    suspend fun iniciar(usuario: Usuario, solicitud: SolicitudAnalisis): String

    /** Avance de un análisis iniciado, paso a paso. */
    fun avance(idAnalisis: String): Flow<AvanceAnalisis>
}
