package com.rinfix.app.componentes

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.sombra
import kotlinx.coroutines.delay

/** `true` si la red actual tiene internet comprobado por Android. */
fun hayConexion(contexto: Context): Boolean {
    val gestor = contexto.getSystemService(ConnectivityManager::class.java) ?: return true
    val capacidades = gestor.getNetworkCapabilities(gestor.activeNetwork) ?: return false
    return capacidades.tieneInternet()
}

private fun NetworkCapabilities.tieneInternet(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

/**
 * `true` mientras el teléfono tiene internet. Escucha los cambios de red del sistema
 * (wifi, datos, modo avión) y se actualiza solo.
 */
@Composable
fun recordarConexion(): Boolean {
    if (LocalInspectionMode.current) return true
    val contexto = LocalContext.current
    var conectado by remember { mutableStateOf(hayConexion(contexto)) }
    DisposableEffect(contexto) {
        val gestor = contexto.getSystemService(ConnectivityManager::class.java)
        if (gestor == null) {
            onDispose { }
        } else {
            val escucha = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(red: Network, capacidades: NetworkCapabilities) {
                    conectado = capacidades.tieneInternet()
                }

                override fun onLost(red: Network) {
                    conectado = false
                }
            }
            gestor.registerDefaultNetworkCallback(escucha)
            onDispose { gestor.unregisterNetworkCallback(escucha) }
        }
    }
    return conectado
}

/**
 * Aviso que baja desde arriba cuando no hay internet. Espera un momento antes de aparecer
 * para no parpadear al pasar del wifi a los datos.
 */
@Composable
fun AvisoSinConexion(modifier: Modifier = Modifier) {
    val conectado = recordarConexion()
    var mostrar by remember { mutableStateOf(false) }
    LaunchedEffect(conectado) {
        if (conectado) {
            mostrar = false
        } else {
            delay(1_500)
            mostrar = true
        }
    }
    AnimatedVisibility(
        visible = mostrar,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        Aviso(
            titulo = "Sin conexión.",
            mensaje = "Revisa el wifi o los datos. Lo que ya cargó lo puedes seguir viendo.",
            tipo = TipoAviso.SinConexion,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .sombra(Sombra.Media, Formas.Campo),
        )
    }
}
