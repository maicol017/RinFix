package com.rinfix.app.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix

/**
 * Íconos de RinFix: trazo de 1,8 sobre una cuadrícula de 24, con puntas y uniones redondeadas.
 *
 * La mayoría están basados en el set libre Lucide (licencia ISC, ver assets/licencias/Lucide-ISC.txt).
 * El ícono del rin es propio.
 *
 * Se usan con `Icon(Iconos.Inicio, contentDescription = ...)`. El color lo pone `tint`.
 * Cada ícono se construye la primera vez que se usa, así no se gasta memoria en los que no aparecen.
 */
object Iconos {

    /** Inicio. */
    val Inicio: ImageVector by lazy {
        icono(
            "Inicio",
            "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z",
        )
    }

    /** Rin, botón central de analizar. */
    val Rin: ImageVector by lazy {
        icono(
            "Rin",
            "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0",
            "M9.4 12a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0 -5.2 0",
            "M12 9.4V4.2M14.5 11.2l4.9-1.6M13.5 14.1l3 4.2M10.5 14.1l-3 4.2M9.5 11.2 4.6 9.6",
        )
    }

    /** Inteligencia artificial y asistente. */
    val Ia: ImageVector by lazy {
        icono(
            "Ia",
            "M11 3l1.9 5.6L18.5 10.5l-5.6 1.9L11 18l-1.9-5.6L3.5 10.5l5.6-1.9z",
            "M19 15l.8 2.2 2.2.8-2.2.8L19 21l-.8-2.2-2.2-.8 2.2-.8z",
        )
    }

    /** Órdenes y trabajos. */
    val Lista: ImageVector by lazy {
        icono(
            "Lista",
            "M9 2h6a1 1 0 0 1 1 1v2a1 1 0 0 1 -1 1h-6a1 1 0 0 1 -1 -1v-2a1 1 0 0 1 1 -1z",
            "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2M12 11h4M12 16h4M8 11h.01M8 16h.01",
        )
    }

    /** Perfil. */
    val Usuario: ImageVector by lazy {
        icono(
            "Usuario",
            "M8 8a4 4 0 1 0 8 0a4 4 0 1 0 -8 0",
            "M20 21a8 8 0 0 0-16 0",
        )
    }

    /** Cuenta. */
    val UsuarioCirculo: ImageVector by lazy {
        icono(
            "UsuarioCirculo",
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0",
            "M9 10a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
            "M7 20.66V19a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v1.66",
        )
    }

    /** Panel del taller. */
    val Panel: ImageVector by lazy {
        icono(
            "Panel",
            "M4.5 3h4a1.5 1.5 0 0 1 1.5 1.5v6a1.5 1.5 0 0 1 -1.5 1.5h-4a1.5 1.5 0 0 1 -1.5 -1.5v-6a1.5 1.5 0 0 1 1.5 -1.5z",
            "M15.5 3h4a1.5 1.5 0 0 1 1.5 1.5v2a1.5 1.5 0 0 1 -1.5 1.5h-4a1.5 1.5 0 0 1 -1.5 -1.5v-2a1.5 1.5 0 0 1 1.5 -1.5z",
            "M15.5 12h4a1.5 1.5 0 0 1 1.5 1.5v6a1.5 1.5 0 0 1 -1.5 1.5h-4a1.5 1.5 0 0 1 -1.5 -1.5v-6a1.5 1.5 0 0 1 1.5 -1.5z",
            "M4.5 16h4a1.5 1.5 0 0 1 1.5 1.5v2a1.5 1.5 0 0 1 -1.5 1.5h-4a1.5 1.5 0 0 1 -1.5 -1.5v-2a1.5 1.5 0 0 1 1.5 -1.5z",
        )
    }

    /** Agenda. */
    val Calendario: ImageVector by lazy {
        icono(
            "Calendario",
            "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2z",
            "M16 2v4M8 2v4M3 10h18",
        )
    }

    /** Reparación. */
    val Herramienta: ImageVector by lazy {
        icono(
            "Herramienta",
            "M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z",
        )
    }

    /** Tomar foto. */
    val Camara: ImageVector by lazy {
        icono(
            "Camara",
            "M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z",
            "M8.8 13a3.2 3.2 0 1 0 6.4 0a3.2 3.2 0 1 0 -6.4 0",
        )
    }

    /** Elegir de la galería. */
    val Galeria: ImageVector by lazy {
        icono(
            "Galeria",
            "M5 3h10a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2z",
            "M7 21h12a2 2 0 0 0 2-2V7M7 13l3-3 4 4",
        )
    }

    /** Foto. */
    val Imagen: ImageVector by lazy {
        icono(
            "Imagen",
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2z",
            "M7 9a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "m21 15-3.09-3.09a2 2 0 0 0-2.82 0L6 21",
        )
    }

    /** Ampliar foto. */
    val Ampliar: ImageVector by lazy {
        icono(
            "Ampliar",
            "M4 11a7 7 0 1 0 14 0a7 7 0 1 0 -14 0",
            "m21 21-4.3-4.3M11 8v6M8 11h6",
        )
    }

    /** Flash de la cámara. */
    val Flash: ImageVector by lazy {
        icono(
            "Flash",
            "M13 2 3 14h9l-1 8 10-12h-9l1-8z",
        )
    }

    /** Capas o etapas. */
    val Capas: ImageVector by lazy {
        icono(
            "Capas",
            "m12 2 10 5-10 5L2 7zM2 17l10 5 10-5M2 12l10 5 10-5",
        )
    }

    /** Conversación. */
    val Chat: ImageVector by lazy {
        icono(
            "Chat",
            "M7.9 20A9 9 0 1 0 4 16.1L2 22Z",
        )
    }

    /** Enviar mensaje. */
    val Enviar: ImageVector by lazy {
        icono(
            "Enviar",
            "M22 2 11 13M22 2l-7 20-4-9-9-4z",
        )
    }

    /** Notificaciones. */
    val Campana: ImageVector by lazy {
        icono(
            "Campana",
            "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.94 1.94 0 0 0 3.4 0",
        )
    }

    /** Completado. */
    val Hecho: ImageVector by lazy {
        icono(
            "Hecho",
            "M20 6 9 17l-5-5",
        )
    }

    /** Tiempo o pendiente. */
    val Reloj: ImageVector by lazy {
        icono(
            "Reloj",
            "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0",
            "M12 7v5l3 2",
        )
    }

    /** Información. */
    val Informacion: ImageVector by lazy {
        icono(
            "Informacion",
            "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0",
            "M12 16v-4M12 8h.01",
        )
    }

    /** Atención. */
    val Alerta: ImageVector by lazy {
        icono(
            "Alerta",
            "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3ZM12 9v4M12 17h.01",
        )
    }

    /** Seguridad o garantía. */
    val Escudo: ImageVector by lazy {
        icono(
            "Escudo",
            "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
            "m9 12 2 2 4-4",
        )
    }

    /** Atrás. */
    val Atras: ImageVector by lazy {
        icono(
            "Atras",
            "m15 18-6-6 6-6",
        )
    }

    /** Siguiente o ver más. */
    val Siguiente: ImageVector by lazy {
        icono(
            "Siguiente",
            "m9 18 6-6-6-6",
        )
    }

    /** Desplegar. */
    val Abajo: ImageVector by lazy {
        icono(
            "Abajo",
            "m6 9 6 6 6-6",
        )
    }

    /** Plegar. */
    val Arriba: ImageVector by lazy {
        icono(
            "Arriba",
            "m18 15-6-6-6 6",
        )
    }

    /** Continuar. */
    val Flecha: ImageVector by lazy {
        icono(
            "Flecha",
            "M5 12h14M13 5l7 7-7 7",
        )
    }

    /** Agregar. */
    val Agregar: ImageVector by lazy {
        icono(
            "Agregar",
            "M5 12h14M12 5v14",
        )
    }

    /** Cerrar. */
    val Cerrar: ImageVector by lazy {
        icono(
            "Cerrar",
            "M18 6 6 18M6 6l12 12",
        )
    }

    /** Más opciones. */
    val Opciones: ImageVector by lazy {
        icono(
            "Opciones",
            "M11 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
            "M11 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
            "M11 19a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
        )
    }

    /** Reintentar. */
    val Reintentar: ImageVector by lazy {
        icono(
            "Reintentar",
            "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8M21 3v5h-5M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16M8 16H3v5",
        )
    }

    /** Descargar. */
    val Descargar: ImageVector by lazy {
        icono(
            "Descargar",
            "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3",
        )
    }

    /** Compartir. */
    val Compartir: ImageVector by lazy {
        icono(
            "Compartir",
            "M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8M16 6l-4-4-4 4M12 2v13",
        )
    }

    /** Vehículo. */
    val Carro: ImageVector by lazy {
        icono(
            "Carro",
            "M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2M9 17h6",
            "M5 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M15 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        )
    }

    /** Ubicación del taller. */
    val Ubicacion: ImageVector by lazy {
        icono(
            "Ubicacion",
            "M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z",
            "M9 10a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        )
    }

    /** Llamar. */
    val Telefono: ImageVector by lazy {
        icono(
            "Telefono",
            "M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1.9.4 1.8.7 2.7a2 2 0 0 1-.5 2.1L8 9.8a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.7.7a2 2 0 0 1 1.7 2z",
        )
    }

    /** Taller. */
    val Taller: ImageVector by lazy {
        icono(
            "Taller",
            "M3 9l1.5-5h15L21 9M3 9h18v1.5a3 3 0 0 1-6 0 3 3 0 0 1-6 0 3 3 0 0 1-6 0zM5 13v8h14v-8M10 21v-4h4v4",
        )
    }

    /** Pago con tarjeta. */
    val TarjetaPago: ImageVector by lazy {
        icono(
            "TarjetaPago",
            "M4 5h16a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2z",
            "M2 10h20M6 15h4",
        )
    }

    /** Billetera. */
    val Billetera: ImageVector by lazy {
        icono(
            "Billetera",
            "M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4",
        )
    }

    /** Transferencia. */
    val Banco: ImageVector by lazy {
        icono(
            "Banco",
            "M3 21h18M5 18v-7M9.5 18v-7M14.5 18v-7M19 18v-7M12 3l8.5 5h-17z",
        )
    }

    /** Comprobante. */
    val Recibo: ImageVector by lazy {
        icono(
            "Recibo",
            "M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z",
            "M8 8h8M8 12h8M8 16h5",
        )
    }

    /** Pago con celular. */
    val Celular: ImageVector by lazy {
        icono(
            "Celular",
            "M8.5 2h7a2.5 2.5 0 0 1 2.5 2.5v15a2.5 2.5 0 0 1 -2.5 2.5h-7a2.5 2.5 0 0 1 -2.5 -2.5v-15a2.5 2.5 0 0 1 2.5 -2.5z",
            "M11 18h2",
        )
    }

    /** Correo. */
    val Correo: ImageVector by lazy {
        icono(
            "Correo",
            "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-12a2 2 0 0 1 2 -2z",
            "m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7",
        )
    }

    /** Contraseña. */
    val Candado: ImageVector by lazy {
        icono(
            "Candado",
            "M6 11h12a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2z",
            "M8 11V7a4 4 0 0 1 8 0v4",
        )
    }

    /** Mostrar contraseña. */
    val Ojo: ImageVector by lazy {
        icono(
            "Ojo",
            "M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z",
            "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        )
    }

    /** Ocultar contraseña. */
    val OjoTachado: ImageVector by lazy {
        icono(
            "OjoTachado",
            "M10.7 5.1A10.7 10.7 0 0 1 21.9 11.7a1 1 0 0 1 0 .7 10.7 10.7 0 0 1-1.4 2.5M14.1 14.2a3 3 0 0 1-4.2-4.2M17.5 17.5A10.8 10.8 0 0 1 2.1 12.3a1 1 0 0 1 0-.7A10.8 10.8 0 0 1 6.5 6.5M2 2l20 20",
        )
    }

    /** Huella digital. */
    val Huella: ImageVector by lazy {
        icono(
            "Huella",
            "M2 12a10 10 0 0 1 18-6M5 19.5C5.5 18 6 15 6 12a6 6 0 0 1 .34-2M17.29 21.02c.12-.6.43-2.3.5-3.02M12 10a2 2 0 0 0-2 2c0 1.02-.1 2.51-.26 4M8.65 22c.21-.66.45-1.32.57-2M14 13.12c0 2.38 0 6.38-1 8.88M2 16h.01M21.8 16c.2-2 .13-5.35 0-6M9 6.8a6 6 0 0 1 9 5.2c0 .47 0 1.17-.02 2",
        )
    }

    /** Reconocimiento facial. */
    val Rostro: ImageVector by lazy {
        icono(
            "Rostro",
            "M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2M8 14s1.5 2 4 2 4-2 4-2M9 9h.01M15 9h.01",
        )
    }

    /** Código de invitación. */
    val Llave: ImageVector by lazy {
        icono(
            "Llave",
            "M3 15.5a4.5 4.5 0 1 0 9 0a4.5 4.5 0 1 0 -9 0",
            "m10.7 12.3 9.8-9.8M17 6l3 3M14 9l2 2",
        )
    }

    /** Cerrar sesión. */
    val Salir: ImageVector by lazy {
        icono(
            "Salir",
            "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9",
        )
    }

    /** Ajustes. */
    val Ajustes: ImageVector by lazy {
        icono(
            "Ajustes",
            "M21 4h-7M10 4H3M21 12h-9M8 12H3M21 20h-5M12 20H3M14 2v4M8 10v4M16 18v4",
        )
    }

    /** Sincronizado. */
    val Nube: ImageVector by lazy {
        icono(
            "Nube",
            "M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z",
        )
    }

    /** Sin conexión. */
    val SinConexion: ImageVector by lazy {
        icono(
            "SinConexion",
            "M12 20h.01M8.5 16.43a5 5 0 0 1 7 0M2 8.82a15 15 0 0 1 4.17-2.65M10.66 5c4.01-.36 8.14.9 11.34 3.76M16.85 11.25a10 10 0 0 1 2.22 1.68M5 13a10 10 0 0 1 5.24-2.76M2 2l20 20",
        )
    }
}

/** Construye un ícono de trazo a partir de los caminos SVG de una cuadrícula de 24 × 24. */
private fun icono(nombre: String, vararg trazos: String): ImageVector =
    ImageVector.Builder(
        name = nombre,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        trazos.forEach { trazo ->
            addPath(
                pathData = addPathNodes(trazo),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, widthDp = 360)
@Composable
private fun VistaPreviaIconos() {
    val todos = listOf(
        Iconos.Inicio,
        Iconos.Rin,
        Iconos.Ia,
        Iconos.Lista,
        Iconos.Usuario,
        Iconos.UsuarioCirculo,
        Iconos.Panel,
        Iconos.Calendario,
        Iconos.Herramienta,
        Iconos.Camara,
        Iconos.Galeria,
        Iconos.Imagen,
        Iconos.Ampliar,
        Iconos.Flash,
        Iconos.Capas,
        Iconos.Chat,
        Iconos.Enviar,
        Iconos.Campana,
        Iconos.Hecho,
        Iconos.Reloj,
        Iconos.Informacion,
        Iconos.Alerta,
        Iconos.Escudo,
        Iconos.Atras,
        Iconos.Siguiente,
        Iconos.Abajo,
        Iconos.Arriba,
        Iconos.Flecha,
        Iconos.Agregar,
        Iconos.Cerrar,
        Iconos.Opciones,
        Iconos.Reintentar,
        Iconos.Descargar,
        Iconos.Compartir,
        Iconos.Carro,
        Iconos.Ubicacion,
        Iconos.Telefono,
        Iconos.Taller,
        Iconos.TarjetaPago,
        Iconos.Billetera,
        Iconos.Banco,
        Iconos.Recibo,
        Iconos.Celular,
        Iconos.Correo,
        Iconos.Candado,
        Iconos.Ojo,
        Iconos.OjoTachado,
        Iconos.Huella,
        Iconos.Rostro,
        Iconos.Llave,
        Iconos.Salir,
        Iconos.Ajustes,
        Iconos.Nube,
        Iconos.SinConexion,
    )
    TemaRinFix {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            todos.chunked(8).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    fila.forEach { icono ->
                        Icon(
                            imageVector = icono,
                            contentDescription = icono.name,
                            tint = Colores.Tinta,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}
