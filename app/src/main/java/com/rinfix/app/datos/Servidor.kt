package com.rinfix.app.datos

import com.rinfix.app.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * Conexión con el servidor de RinFix en Vercel (carpeta `servidor/` del proyecto).
 *
 * La dirección y la clave de la app salen de `local.properties` al compilar. La llave de Claude
 * nunca pasa por el teléfono: vive en el servidor.
 */
object Servidor {

    private val direccion: String = BuildConfig.RINFIX_SERVIDOR.trimEnd('/')
    private val clave: String = BuildConfig.RINFIX_CLAVE_APP

    /** `true` si el APK se compiló con la dirección y la clave del servidor. */
    val configurado: Boolean = direccion.startsWith("https://") && clave.isNotBlank()

    /** Error con un mensaje listo para mostrarse. */
    class Error(mensaje: String, val codigo: String? = null) : Exception(mensaje)

    /**
     * Envía `cuerpo` como JSON a `ruta` (por ejemplo «/api/analizar») y devuelve la respuesta.
     * Lanza [Error] con un mensaje claro si no hay internet, si tarda o si el servidor responde mal.
     */
    suspend fun enviar(ruta: String, cuerpo: JSONObject, esperaMaximaMs: Int = 70_000): JSONObject =
        withContext(Dispatchers.IO) {
            if (!configurado) throw Error("Esta versión de la app no tiene servidor configurado.")
            val conexion = (URL(direccion + ruta).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = esperaMaximaMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("x-rinfix-clave", clave)
            }
            try {
                conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
                val codigo = conexion.responseCode
                val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
                val texto = flujo?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = try {
                    JSONObject(texto)
                } catch (e: JSONException) {
                    null
                }
                if (codigo !in 200..299) {
                    val error = json?.optJSONObject("error")
                    throw Error(
                        mensaje = error?.optString("mensaje")?.takeIf { it.isNotBlank() } ?: mensajePorCodigo(codigo),
                        codigo = error?.optString("codigo"),
                    )
                }
                json ?: throw Error("La respuesta del servidor llegó dañada. Inténtalo de nuevo.")
            } catch (e: Error) {
                throw e
            } catch (e: SocketTimeoutException) {
                throw Error("El servidor tardó demasiado en responder. Revisa tu conexión e inténtalo de nuevo.")
            } catch (e: UnknownHostException) {
                throw Error("Sin conexión a internet. Revisa el wifi o los datos.")
            } catch (e: IOException) {
                throw Error("No pudimos conectarnos con el servidor. Revisa tu conexión e inténtalo de nuevo.")
            } finally {
                conexion.disconnect()
            }
        }

    private fun mensajePorCodigo(codigo: Int): String = when (codigo) {
        401, 403 -> "Esta versión de la app no está autorizada. Actualízala."
        413 -> "Las fotos pesan demasiado. Toma fotos con menos resolución."
        429 -> "Hiciste muchas solicitudes seguidas. Espera unos minutos."
        in 500..599 -> "El servidor tuvo un problema. Inténtalo de nuevo en un momento."
        else -> "No pudimos completar la solicitud (código $codigo)."
    }
}
