package com.rinfix.app.datos

import android.content.Context
import android.content.SharedPreferences

/**
 * Punto único de donde la app saca sus servicios.
 * Hoy usa datos de ejemplo; al conectar el backend, solo se cambian estas líneas.
 */
object Servicios {

    private var preferencias: SharedPreferences? = null

    /** Se llama una vez al abrir la app (en MainActivity), antes de usar cualquier servicio. */
    fun iniciar(contexto: Context) {
        if (preferencias == null) {
            preferencias = contexto.applicationContext
                .getSharedPreferences("rinfix_ejemplo", Context.MODE_PRIVATE)
        }
    }

    val cuentas: ServicioCuentas by lazy {
        CuentasDeEjemplo(checkNotNull(preferencias) { "Falta llamar a Servicios.iniciar() en MainActivity." })
    }

    val ordenes: ServicioOrdenes by lazy { OrdenesDeEjemplo() }

    val garaje: ServicioGaraje by lazy { GarajeDeEjemplo() }

    /** Con servidor configurado, Claude analiza las fotos de verdad; si no, se usa el ejemplo. */
    val analisis: ServicioAnalisis by lazy {
        if (Servidor.configurado) AnalisisConClaude() else AnalisisDeEjemplo()
    }

    val pagos: ServicioPagos by lazy { PagosDeEjemplo(ordenes) }

    /** Con servidor configurado, responde Claude; si no, las reglas de ejemplo. */
    val asistente: ServicioAsistente by lazy {
        if (Servidor.configurado) AsistenteConClaude(ordenes, analisis, garaje) else AsistenteDeEjemplo(ordenes, analisis, garaje)
    }
}
