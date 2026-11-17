package com.rinfix.app.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.sombra

/**
 * Tipos de etiqueta. El color siempre significa lo mismo en toda la app.
 */
enum class TipoEtiqueta(internal val fondo: Color, internal val texto: Color) {

    /** Completado o confirmado. */
    Listo(Colores.Menta, Colores.MentaTexto),

    /** En proceso o pendiente. */
    EnProceso(Colores.Mantequilla, Colores.MantequillaTexto),

    /** Resultado o sugerencia de la inteligencia artificial. */
    Ia(Colores.Lavanda, Colores.LavandaTexto),

    /** Error o algo que necesita atención, como una posible fisura. */
    Atencion(Colores.FrenoSuave, Colores.FrenoTexto),

    /** Información sin estado, como el tamaño del rin. */
    Neutra(Colores.Arena, Colores.Tinta2),

    /** Destacada sobre fondos claros. */
    Oscura(Colores.Tinta, Colores.Crema),

    /** Sobre fotos o fondos de color. */
    Blanca(Colores.Papel, Colores.Tinta),
}

/**
 * Etiqueta tipo píldora para mostrar estados.
 *
 * - `conPunto` agrega un punto del mismo color del texto, útil para estados.
 * - `pequena` la hace más baja, para listas y miniaturas.
 * - Si cambia el tipo, el color pasa suavemente de uno a otro.
 */
@Composable
fun Etiqueta(
    texto: String,
    tipo: TipoEtiqueta,
    modifier: Modifier = Modifier,
    conPunto: Boolean = false,
    pequena: Boolean = false,
    icono: ImageVector? = null,
) {
    val fondo by animateColorAsState(
        targetValue = tipo.fondo,
        animationSpec = tween(durationMillis = 300),
        label = "fondo de la etiqueta",
    )
    val colorTexto by animateColorAsState(
        targetValue = tipo.texto,
        animationSpec = tween(durationMillis = 300),
        label = "texto de la etiqueta",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .then(if (tipo == TipoEtiqueta.Blanca) Modifier.sombra(Sombra.Baja, Formas.Pildora) else Modifier)
            .background(fondo, Formas.Pildora)
            .height(if (pequena) 24.dp else 28.dp)
            .padding(horizontal = if (pequena) 9.dp else 11.dp),
    ) {
        if (conPunto) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(colorTexto, CircleShape),
            )
        }
        if (icono != null) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = colorTexto,
                modifier = Modifier.size(if (pequena) 13.dp else 15.dp),
            )
        }
        Text(
            text = texto,
            color = colorTexto,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = if (pequena) 12.sp else 13.sp,
                lineHeight = if (pequena) 14.sp else 16.sp,
            ),
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaEtiquetas() {
    TemaRinFix {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Etiqueta(texto = "Listo", tipo = TipoEtiqueta.Listo, conPunto = true)
            Etiqueta(texto = "En pintura", tipo = TipoEtiqueta.EnProceso, conPunto = true)
            Etiqueta(texto = "Análisis con IA", tipo = TipoEtiqueta.Ia)
            Etiqueta(texto = "Revisión presencial", tipo = TipoEtiqueta.Atencion, conPunto = true)
            Etiqueta(texto = "Rin 17\"", tipo = TipoEtiqueta.Neutra, pequena = true)
            Etiqueta(texto = "Probable", tipo = TipoEtiqueta.Oscura, pequena = true)
            Etiqueta(texto = "Antes", tipo = TipoEtiqueta.Blanca, pequena = true)
        }
    }
}
