package com.rinfix.app.navegacion

import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.Pestana

/** Nombre de cada pantalla dentro de la navegación. */
object Rutas {

    // Cliente
    const val Inicio = "inicio"
    const val Analizar = "analizar?vehiculo={vehiculo}"
    const val Asistente = "asistente"
    const val Ordenes = "ordenes"

    // Cliente: pantallas de detalle (sin barra inferior)
    const val Garaje = "garaje?agregar={agregar}"
    const val Analizando = "analizando/{analisis}"
    const val Agendar = "agendar?diagnostico={diagnostico}"
    const val Resultado = "resultado/{diagnostico}"
    const val Seguimiento = "seguimiento/{orden}"
    const val Pago = "pago/{orden}"

    fun analizar(idVehiculo: String? = null) = if (idVehiculo == null) "analizar" else "analizar?vehiculo=$idVehiculo"
    fun analizando(idAnalisis: String) = "analizando/$idAnalisis"
    fun garaje(agregar: Boolean = false) = if (agregar) "garaje?agregar=true" else "garaje"
    fun agendar(idDiagnostico: String? = null) =
        if (idDiagnostico == null) "agendar" else "agendar?diagnostico=$idDiagnostico"
    fun resultado(idDiagnostico: String) = "resultado/$idDiagnostico"
    fun seguimiento(idOrden: String) = "seguimiento/$idOrden"
    fun pago(idOrden: String) = "pago/$idOrden"

    // Taller
    const val Panel = "panel"
    const val Agenda = "agenda"
    const val Trabajos = "trabajos"

    // Ambos roles
    const val Perfil = "perfil"
}

/** Pestañas del cliente. El botón central del rin (Analizar) va aparte, en la mitad. */
val PestanasCliente = listOf(
    Pestana(Rutas.Inicio, "Inicio", Iconos.Inicio),
    Pestana(Rutas.Ordenes, "Órdenes", Iconos.Lista),
    Pestana(Rutas.Asistente, "Asistente", Iconos.Ia),
    Pestana(Rutas.Perfil, "Perfil", Iconos.Usuario),
)

/** Pestañas del trabajador del taller. */
val PestanasTaller = listOf(
    Pestana(Rutas.Panel, "Panel", Iconos.Panel),
    Pestana(Rutas.Agenda, "Agenda", Iconos.Calendario),
    Pestana(Rutas.Trabajos, "Trabajos", Iconos.Lista),
    Pestana(Rutas.Perfil, "Perfil", Iconos.Usuario),
)

/** Rutas que son pestañas: entre ellas la transición es solo un fundido. */
internal val RutasPestanas: Set<String> =
    (PestanasCliente + PestanasTaller).map { it.ruta }.toSet()
