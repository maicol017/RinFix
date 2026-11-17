package com.rinfix.app.datos

/** Los dos tipos de usuario de RinFix. Cada uno tiene su propia barra inferior. */
enum class Rol { Cliente, Taller }

/**
 * Persona con sesión iniciada.
 * `taller` solo lo tienen los trabajadores: es el nombre del taller al que pertenecen.
 */
data class Usuario(
    val id: String,
    val nombre: String,
    val correo: String,
    val rol: Rol,
    val taller: String? = null,
)
