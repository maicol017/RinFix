package com.rinfix.app.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix

/** Tipos de aviso. Cada uno tiene su color e ícono. */
enum class TipoAviso(
    internal val fondo: Color,
    internal val borde: Color,
    internal val colorIcono: Color,
    internal val colorTexto: Color,
    internal val icono: ImageVector,
) {
    SinConexion(Colores.Papel, Colores.Linea2, Colores.Tinta, Colores.Tinta2, Iconos.SinConexion),
    Error(Colores.FrenoSuave, Color.Transparent, Colores.FrenoTexto, Colores.FrenoTexto, Iconos.Alerta),
    Informacion(Colores.LavandaSuave, Color.Transparent, Colores.LavandaTexto, Colores.LavandaTexto, Iconos.Informacion),
    Exito(Colores.MentaSuave, Color.Transparent, Colores.MentaTexto, Colores.MentaTexto, Iconos.Hecho),
}

private val FormaAviso = RoundedCornerShape(18.dp)

/**
 * Aviso en bloque: título en negrita seguido del mensaje, y una acción opcional debajo.
 * El lector de pantalla lo anuncia en cuanto aparece.
 */
@Composable
fun Aviso(
    titulo: String,
    mensaje: String,
    tipo: TipoAviso,
    modifier: Modifier = Modifier,
    textoAccion: String? = null,
    alPulsarAccion: (() -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .background(tipo.fondo, FormaAviso)
            .border(1.dp, tipo.borde, FormaAviso)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Icon(
            imageVector = tipo.icono,
            contentDescription = null,
            tint = tipo.colorIcono,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Colores.Tinta)) {
                        append(titulo)
                    }
                    append(" ")
                    append(mensaje)
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = tipo.colorTexto),
            )
            if (textoAccion != null && alPulsarAccion != null) {
                Enlace(texto = textoAccion, alPulsar = alPulsarAccion, color = tipo.colorIcono)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, widthDp = 380)
@Composable
private fun VistaPreviaAvisos() {
    TemaRinFix {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Aviso("Sin conexión.", "Conéctate a internet para iniciar sesión.", TipoAviso.SinConexion)
            Aviso("No pudimos entrar.", "El correo o la contraseña no coinciden.", TipoAviso.Error)
            Aviso(
                "Revisión presencial.",
                "Las fotos no bastan para descartar una fisura.",
                TipoAviso.Informacion,
                textoAccion = "Ver talleres",
                alPulsarAccion = {},
            )
            Aviso("Listo.", "Tu cuenta quedó creada.", TipoAviso.Exito)
        }
    }
}
