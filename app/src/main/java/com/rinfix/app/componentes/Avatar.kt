package com.rinfix.app.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix

/**
 * Círculo con las iniciales de la persona («Laura Méndez» → «LM»).
 * Si no hay nombre, muestra el ícono de usuario.
 *
 * Es decorativo: el lector de pantalla lo salta, porque el nombre siempre aparece al lado.
 * Las letras no crecen con el tamaño de letra del teléfono, para que no se salgan del círculo.
 */
@Composable
fun Avatar(
    nombre: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 44.dp,
    fondo: Color = Colores.Lavanda,
    colorTexto: Color = Colores.LavandaTexto,
) {
    val letras = iniciales(nombre)
    val tamanoLetra = with(LocalDensity.current) { (tamano * 0.36f).toSp() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamano)
            .background(fondo, CircleShape)
            .clearAndSetSemantics { },
    ) {
        if (letras.isEmpty()) {
            Icon(
                imageVector = Iconos.Usuario,
                contentDescription = null,
                tint = colorTexto,
                modifier = Modifier.size(tamano * 0.46f),
            )
        } else {
            Text(
                text = letras,
                color = colorTexto,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = tamanoLetra,
                    lineHeight = tamanoLetra,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}

/** Primera letra de las dos primeras palabras del nombre, en mayúscula. */
internal fun iniciales(nombre: String): String =
    nombre.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercase() }

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7)
@Composable
private fun VistaPreviaAvatar() {
    TemaRinFix {
        Row(modifier = Modifier.padding(16.dp)) {
            Avatar(nombre = "Laura Méndez", tamano = 88.dp)
            Avatar(
                nombre = "Carlos Rueda",
                fondo = Colores.Menta,
                colorTexto = Colores.MentaTexto,
                modifier = Modifier.padding(start = 12.dp),
            )
            Avatar(nombre = "", modifier = Modifier.padding(start = 12.dp))
        }
    }
}
