package com.rinfix.app.componentes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Abre la app de mapas con una dirección; si no hay, abre Google Maps en el navegador. */
fun abrirEnMapas(contexto: Context, direccion: String) {
    val consulta = Uri.encode(direccion)
    try {
        contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$consulta")))
    } catch (e: ActivityNotFoundException) {
        try {
            contexto.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$consulta")),
            )
        } catch (otro: ActivityNotFoundException) {
            // Sin mapas ni navegador: no hay nada que abrir.
        }
    }
}
