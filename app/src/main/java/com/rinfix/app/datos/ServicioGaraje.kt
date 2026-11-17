package com.rinfix.app.datos

import kotlinx.coroutines.flow.Flow

/** Vehículos que la persona guarda en «Mi garaje». */
interface ServicioGaraje {

    fun vehiculos(usuario: Usuario): Flow<List<Vehiculo>>

    /** Guarda el vehículo y lo devuelve con su id definitivo. */
    suspend fun agregar(usuario: Usuario, vehiculo: Vehiculo): Vehiculo

    suspend fun quitar(usuario: Usuario, idVehiculo: String)
}
