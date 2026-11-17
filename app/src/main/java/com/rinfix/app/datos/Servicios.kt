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
}
