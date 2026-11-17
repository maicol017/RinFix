package com.rinfix.app.datos

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Garaje de ejemplo en memoria. Laura tiene un Mazda 3 y un Kia Picanto. */
class GarajeDeEjemplo : ServicioGaraje {

    /** Vehículos por id de usuario. */
    private val porUsuario = MutableStateFlow(vehiculosIniciales())
    private var siguiente = 100

    override fun vehiculos(usuario: Usuario): Flow<List<Vehiculo>> =
        porUsuario.map { it[usuario.id].orEmpty() }

    override suspend fun agregar(usuario: Usuario, vehiculo: Vehiculo): Vehiculo {
        delay(500)
        val guardado = vehiculo.copy(id = "vehiculo-${siguiente++}")
        porUsuario.update { mapa -> mapa + (usuario.id to mapa[usuario.id].orEmpty() + guardado) }
        return guardado
    }

    override suspend fun quitar(usuario: Usuario, idVehiculo: String) {
        delay(300)
        porUsuario.update { mapa ->
            mapa + (usuario.id to mapa[usuario.id].orEmpty().filterNot { it.id == idVehiculo })
        }
    }

    internal companion object {
        fun vehiculosIniciales(): Map<String, List<Vehiculo>> = mapOf(
            "cliente-1" to listOf(
                Vehiculo("vehiculo-1", "Mazda 3", 2021, 17, MaterialRin.Aleacion),
                Vehiculo("vehiculo-2", "Kia Picanto", 2016, 14, MaterialRin.Acero),
            ),
        )
    }
}
