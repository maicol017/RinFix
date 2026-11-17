package com.rinfix.app.datos

/**
 * Punto único de donde la app saca sus servicios.
 * Hoy usa datos de ejemplo; al conectar el backend, solo se cambian estas líneas.
 */
object Servicios {
    val cuentas: ServicioCuentas by lazy { CuentasDeEjemplo() }
}
