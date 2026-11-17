package com.rinfix.app.componentes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.rinfix.app.datos.Caja
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

/**
 * Foto guardada en el teléfono, recortada para llenar su espacio.
 * `alCargar` recibe el tamaño real de la foto ya enderezada, para ubicar marcas encima
 * con [zonaEnPantalla].
 */
@Composable
fun FotoGuardada(
    ruta: String,
    modifier: Modifier = Modifier,
    descripcion: String? = null,
    alCargar: (IntSize) -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.background(Colores.Tinta2)) {
        val lado = with(LocalDensity.current) {
            (if (constraints.hasBoundedHeight) maxOf(maxWidth, maxHeight) else maxWidth).roundToPx()
        }
        val imagen by produceState<ImageBitmap?>(initialValue = null, ruta, lado) {
            value = withContext(Dispatchers.IO) { cargarMiniatura(ruta, lado) }
        }
        val avisar by rememberUpdatedState(alCargar)
        LaunchedEffect(imagen) {
            imagen?.let { avisar(IntSize(it.width, it.height)) }
        }
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

/**
 * Dónde queda una zona de la foto (en proporciones de 0 a 1) dentro de un recuadro que muestra
 * la foto recortada para llenarlo. Sin `imagen`, la zona se toma sobre el recuadro completo.
 */
fun zonaEnPantalla(caja: Caja, imagen: IntSize?, contenedor: Size): Rect {
    var ancho = contenedor.width
    var alto = contenedor.height
    var izquierda = 0f
    var arriba = 0f
    if (imagen != null && imagen.width > 0 && imagen.height > 0) {
        val escala = maxOf(contenedor.width / imagen.width, contenedor.height / imagen.height)
        ancho = imagen.width * escala
        alto = imagen.height * escala
        izquierda = (contenedor.width - ancho) / 2
        arriba = (contenedor.height - alto) / 2
    }
    return Rect(
        left = izquierda + caja.izquierda * ancho,
        top = arriba + caja.arriba * alto,
        right = izquierda + (caja.izquierda + caja.ancho) * ancho,
        bottom = arriba + (caja.arriba + caja.alto) * alto,
    )
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
