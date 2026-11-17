package com.rinfix.app.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas

/**
 * Enlace de texto subrayado, para acciones menores como «Inicia sesión».
 * Aunque el texto sea pequeño, el área que responde al toque mide al menos 48 dp de alto.
 */
@Composable
fun Enlace(
    texto: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    estilo: TextStyle = MaterialTheme.typography.labelLarge,
    color: Color = Colores.Tinta,
) {
    Text(
        text = texto,
        style = estilo.copy(
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
        ),
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(Formas.Pequena)
            .clickable(role = Role.Button, onClick = alPulsar)
            .heightIn(min = 48.dp)
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = 6.dp),
    )
}
