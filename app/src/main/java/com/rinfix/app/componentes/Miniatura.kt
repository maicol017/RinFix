package com.rinfix.app.componentes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.rinfix.app.diseno.Colores
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Miniatura de una foto guardada en el teléfono.
 * La carga en segundo plano, reducida al tamaño que se ve y girada según la cámara.
 */
@Composable
fun MiniaturaFoto(
    ruta: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 56.dp,
    radio: Dp = 14.dp,
    descripcion: String? = null,
) {
    val lado = with(LocalDensity.current) { tamano.roundToPx() }
    val imagen by produceState<ImageBitmap?>(initialValue = null, ruta, lado) {
        value = withContext(Dispatchers.IO) { cargarMiniatura(ruta, lado) }
    }
    Box(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(radio))
            .background(Colores.Tinta2),
    ) {
        imagen?.let {
            Image(
                bitmap = it,
                contentDescription = descripcion,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** Lee la foto en un tamaño cercano a `lado` píxeles y la endereza según el EXIF. */
internal fun cargarMiniatura(ruta: String, lado: Int): ImageBitmap? = runCatching {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(ruta, medidas)
    var muestra = 1
    while (medidas.outWidth / (muestra * 2) >= lado && medidas.outHeight / (muestra * 2) >= lado) {
        muestra *= 2
    }
    val mapa = BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = muestra })
        ?: return null
    val giro = when (ExifInterface(ruta).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val derecha = if (giro == 0f) {
        mapa
    } else {
        Bitmap.createBitmap(mapa, 0, 0, mapa.width, mapa.height, Matrix().apply { postRotate(giro) }, true)
    }
    derecha.asImageBitmap()
}.getOrNull()
