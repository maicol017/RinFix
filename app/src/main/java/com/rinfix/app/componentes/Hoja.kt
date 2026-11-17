package com.rinfix.app.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import kotlinx.coroutines.launch

/**
 * Hoja que sube desde abajo, con el estilo de RinFix: blanca para despegarse del fondo
 * crema, con un oscurecido suave detrás, esquinas de 28 dp y una manija para arrastrarla.
 * Dentro de la hoja, los campos de texto se pintan crema.
 *
 * El contenido recibe `cerrar`, que baja la hoja con su animación y luego llama a `alCerrar`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Hoja(
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.(cerrar: () -> Unit) -> Unit,
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val alcance = rememberCoroutineScope()
    val cerrar: () -> Unit = {
        alcance.launch { estado.hide() }.invokeOnCompletion {
            if (!estado.isVisible) alCerrar()
        }
    }

    ModalBottomSheet(
        onDismissRequest = alCerrar,
        sheetState = estado,
        shape = Formas.Hoja,
        containerColor = Colores.Papel,
        contentColor = Colores.Tinta,
        scrimColor = Colores.Tinta.copy(alpha = 0.18f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .background(Colores.Linea2, Formas.Pildora),
            )
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        ) {
            CompositionLocalProvider(LocalFondoCampo provides Colores.Crema) {
                contenido(cerrar)
            }
        }
    }
}
