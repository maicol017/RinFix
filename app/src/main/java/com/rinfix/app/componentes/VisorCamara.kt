package com.rinfix.app.componentes

import android.content.Context
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rinfix.app.diseno.Colores
import com.rinfix.app.ia.Encuadre
import com.rinfix.app.ia.RevisorEncuadre
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation

/**
 * Lo que la pantalla puede pedirle a la cámara: tomar la foto y saber si hay linterna.
 * Se crea con [recordarControlCamara] y se le pasa a [VisorCamara].
 */
class ControlCamara {

    internal var captura: ImageCapture? by mutableStateOf(null)
    internal var camara: Camera? by mutableStateOf(null)

    /** `true` cuando la cámara ya abrió y puede tomar fotos. */
    val lista: Boolean get() = captura != null

    val tieneLinterna: Boolean get() = camara?.cameraInfo?.hasFlashUnit() == true

    /** Toma la foto y la guarda en `destino`. `alTerminar` recibe el archivo, o `null` si falló. */
    fun tomarFoto(contexto: Context, destino: File, alTerminar: (File?) -> Unit) {
        val actual = captura
        if (actual == null) {
            alTerminar(null)
            return
        }
        val opciones = ImageCapture.OutputFileOptions.Builder(destino).build()
        actual.takePicture(
            opciones,
            ContextCompat.getMainExecutor(contexto),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(resultado: ImageCapture.OutputFileResults) {
                    alTerminar(destino)
                }

                override fun onError(error: ImageCaptureException) {
                    destino.delete()
                    alTerminar(null)
                }
            },
        )
    }
}

@Composable
fun recordarControlCamara(): ControlCamara = remember { ControlCamara() }

/**
 * Imagen en vivo de la cámara trasera, con CameraX.
 *
 * - Abre la cámara al aparecer y la suelta al salir de la pantalla o al pasar la app a segundo plano.
 * - `alRevisarEncuadre` recibe la revisión de luz unas dos veces por segundo, ya en el hilo principal.
 * - `alFallar` avisa si el teléfono no pudo abrir la cámara (por ejemplo, si otra app la está usando).
 * - Pide permiso de cámara antes de mostrarlo: este componente no lo pide.
 */
@Composable
fun VisorCamara(
    control: ControlCamara,
    modifier: Modifier = Modifier,
    linterna: Boolean = false,
    alRevisarEncuadre: (Encuadre) -> Unit = {},
    alFallar: () -> Unit = {},
) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    var solicitud by remember { mutableStateOf<SurfaceRequest?>(null) }
    val revisar by rememberUpdatedState(alRevisarEncuadre)
    val fallar by rememberUpdatedState(alFallar)

    LaunchedEffect(ciclo) {
        val proveedor = try {
            ProcessCameraProvider.awaitInstance(contexto)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallar()
            return@LaunchedEffect
        }

        val principal = ContextCompat.getMainExecutor(contexto)
        val hiloRevision = Executors.newSingleThreadExecutor()
        val vista = Preview.Builder().build().also { previa ->
            previa.setSurfaceProvider { pedido -> solicitud = pedido }
        }
        val captura = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        val analisis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(hiloRevision, RevisorEncuadre { encuadre -> principal.execute { revisar(encuadre) } })
            }

        try {
            proveedor.unbindAll()
            control.camara = proveedor.bindToLifecycle(
                ciclo,
                CameraSelector.DEFAULT_BACK_CAMERA,
                vista,
                captura,
                analisis,
            )
            control.captura = captura
            awaitCancellation()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallar()
        } finally {
            proveedor.unbindAll()
            hiloRevision.shutdown()
            control.camara = null
            control.captura = null
            solicitud = null
        }
    }

    // La linterna se prende o apaga cuando cambia el botón o cuando la cámara termina de abrir.
    LaunchedEffect(linterna, control.camara) {
        val camara = control.camara
        if (camara != null && camara.cameraInfo.hasFlashUnit()) {
            camara.cameraControl.enableTorch(linterna)
        }
    }

    Box(modifier = modifier.background(Colores.Tinta)) {
        solicitud?.let { pedido ->
            CameraXViewfinder(surfaceRequest = pedido, modifier = Modifier.fillMaxSize())
        }
    }
}
