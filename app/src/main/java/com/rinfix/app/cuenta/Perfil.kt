package com.rinfix.app.cuenta

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.Avatar
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.Campo
import com.rinfix.app.componentes.EncabezadoSeccion
import com.rinfix.app.componentes.Enlace
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.FotoRin
import com.rinfix.app.componentes.Hoja
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.MiniaturaFoto
import com.rinfix.app.componentes.TamanoBoton
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.componentes.TipoEtiqueta
import com.rinfix.app.componentes.TonoFoto
import com.rinfix.app.componentes.abrirEnMapas
import com.rinfix.app.componentes.lineaPunteadaArriba
import com.rinfix.app.datos.Catalogo
import com.rinfix.app.datos.Comprobante
import com.rinfix.app.datos.Diagnostico
import com.rinfix.app.datos.EstadoPago
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.Rol
import com.rinfix.app.datos.Servidor
import com.rinfix.app.datos.Usuario
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import com.rinfix.app.diseno.sombra
import com.rinfix.app.seguridad.Huella

/**
 * Lo que Perfil necesita de la sesión. Lo arma la Puerta, que es la que maneja la huella
 * y el cierre de sesión.
 */
data class ControlSesion(
    val huellaActiva: Boolean,
    val activandoHuella: Boolean,
    val errorHuella: String?,
    val activarHuella: () -> Unit,
    val desactivarHuella: () -> Unit,
    val limpiarErrorHuella: () -> Unit,
    val cerrarSesion: () -> Unit,
)

/** Hojas que se abren desde Perfil. */
private enum class HojaPerfil { Datos, Analisis, Pagos, ComoFunciona, Privacidad, CerrarSesion }

/**
 * Perfil, compartido por cliente y taller: datos de la cuenta, accesos a lo suyo,
 * desbloqueo con huella, ayuda y cierre de sesión.
 */
@Composable
fun PantallaPerfil(
    sesion: ControlSesion?,
    alVerGaraje: () -> Unit,
    alVerOrdenes: () -> Unit,
    alAnalizar: () -> Unit,
    alVerDiagnostico: (idDiagnostico: String) -> Unit,
    alVerComprobante: (idComprobante: String) -> Unit,
    modifier: Modifier = Modifier,
    logica: PerfilLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    val edicion by logica.edicion.collectAsState()
    val contexto = LocalContext.current
    var hoja by rememberSaveable { mutableStateOf<HojaPerfil?>(null) }

    // Si la persona registra una huella en los ajustes del teléfono y vuelve, el interruptor se habilita.
    var huellaDisponible by remember { mutableStateOf(Huella.disponible(contexto)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { huellaDisponible = Huella.disponible(contexto) }

    val usuario = estado.usuario ?: return Box(modifier = modifier.fillMaxSize())
    val visible = recordarEntrada()
    val esCliente = usuario.rol == Rol.Cliente

    Column(
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
    ) {
        Text(
            text = "Perfil",
            style = MaterialTheme.typography.headlineMedium,
            color = Colores.Tinta,
            modifier = Modifier
                .aparecer(visible, orden = 0)
                .semantics { heading() },
        )

        TarjetaCuenta(
            usuario = usuario,
            alEditar = {
                logica.limpiarEdicion()
                hoja = HojaPerfil.Datos
            },
            modifier = Modifier.aparecer(visible, orden = 1),
        )

        if (esCliente) {
            Seccion(titulo = "Lo tuyo", modifier = Modifier.aparecer(visible, orden = 2)) {
                FilaPerfil(
                    icono = Iconos.Carro,
                    fondoIcono = Colores.Mantequilla,
                    colorIcono = Colores.MantequillaTexto,
                    titulo = "Mi garaje",
                    detalle = cantidad(estado.vehiculos, "Sin vehículos todavía", "1 vehículo", "vehículos"),
                    alPulsar = alVerGaraje,
                )
                FilaPerfil(
                    icono = Iconos.Ia,
                    fondoIcono = Colores.Lavanda,
                    colorIcono = Colores.LavandaTexto,
                    titulo = "Mis análisis",
                    detalle = cantidad(estado.diagnosticos.size, "Sin análisis todavía", "1 diagnóstico", "diagnósticos"),
                    alPulsar = { hoja = HojaPerfil.Analisis },
                    conLinea = true,
                )
                FilaPerfil(
                    icono = Iconos.Lista,
                    fondoIcono = Colores.Crema2,
                    colorIcono = Colores.Tinta,
                    titulo = "Mis reparaciones",
                    detalle = when {
                        estado.enCurso > 0 -> cantidad(estado.enCurso, "", "1 en curso", "en curso")
                        estado.entregadas > 0 -> cantidad(estado.entregadas, "", "1 terminada", "terminadas")
                        else -> "Sin reparaciones todavía"
                    },
                    alPulsar = alVerOrdenes,
                    conLinea = true,
                )
                FilaPerfil(
                    icono = Iconos.Recibo,
                    fondoIcono = Colores.Menta,
                    colorIcono = Colores.MentaTexto,
                    titulo = "Pagos y comprobantes",
                    detalle = cantidad(estado.comprobantes.size, "Sin comprobantes todavía", "1 comprobante", "comprobantes"),
                    alPulsar = { hoja = HojaPerfil.Pagos },
                    conLinea = true,
                )
            }
        } else {
            Seccion(titulo = "Tu trabajo", modifier = Modifier.aparecer(visible, orden = 2)) {
                FilaPerfil(
                    icono = Iconos.Taller,
                    fondoIcono = Colores.Mantequilla,
                    colorIcono = Colores.MantequillaTexto,
                    titulo = usuario.taller ?: Catalogo.Taller,
                    detalle = "${Catalogo.DireccionTaller}. Toca para ver cómo llegar.",
                    alPulsar = { abrirEnMapas(contexto, Catalogo.DireccionTaller) },
                )
                FilaPerfil(
                    icono = Iconos.Herramienta,
                    fondoIcono = Colores.Crema2,
                    colorIcono = Colores.Tinta,
                    titulo = "Trabajos",
                    detalle = buildList {
                        add(cantidad(estado.enCurso, "Sin trabajos en curso", "1 en curso", "en curso"))
                        if (estado.listas > 0) add(cantidad(estado.listas, "", "1 listo para entregar", "listos para entregar"))
                    }.joinToString(", "),
                    alPulsar = alVerOrdenes,
                    conLinea = true,
                )
            }
        }

        if (sesion != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.aparecer(visible, orden = 3),
            ) {
                Seccion(titulo = "Seguridad") {
                    FilaHuella(sesion = sesion, disponible = huellaDisponible)
                    if (!huellaDisponible && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        Enlace(
                            texto = "Abrir ajustes del teléfono",
                            alPulsar = { abrirAjustesDeHuella(contexto) },
                            color = Colores.LavandaTexto,
                            modifier = Modifier.padding(start = 70.dp, bottom = 14.dp),
                        )
                    }
                }
                AnimatedVisibility(visible = sesion.errorHuella != null) {
                    Aviso(
                        titulo = "No se activó.",
                        mensaje = sesion.errorHuella.orEmpty(),
                        tipo = TipoAviso.Error,
                        textoAccion = "Entendido",
                        alPulsarAccion = sesion.limpiarErrorHuella,
                    )
                }
            }
        }

        Seccion(titulo = "Ayuda", modifier = Modifier.aparecer(visible, orden = 4)) {
            FilaPerfil(
                icono = Iconos.Informacion,
                fondoIcono = Colores.LavandaSuave,
                colorIcono = Colores.LavandaTexto,
                titulo = "Cómo funciona el diagnóstico",
                detalle = "Qué ve la IA y de dónde salen los precios",
                alPulsar = { hoja = HojaPerfil.ComoFunciona },
            )
            FilaPerfil(
                icono = Iconos.Escudo,
                fondoIcono = Colores.MentaSuave,
                colorIcono = Colores.MentaTexto,
                titulo = "Privacidad y datos",
                detalle = "Qué guarda RinFix y qué no",
                alPulsar = { hoja = HojaPerfil.Privacidad },
                conLinea = true,
            )
            FilaPerfil(
                icono = Iconos.Celular,
                fondoIcono = Colores.Niebla,
                colorIcono = Colores.Tinta2,
                titulo = "Versión de la app",
                detalle = remember { versionDeLaApp(contexto) },
                conLinea = true,
            )
        }

        if (sesion != null) {
            Boton(
                texto = "Cerrar sesión",
                alPulsar = { hoja = HojaPerfil.CerrarSesion },
                estilo = EstiloBoton.Blanco,
                icono = Iconos.Salir,
                modifier = Modifier
                    .fillMaxWidth()
                    .aparecer(visible, orden = 5),
            )
        }
    }

    when (hoja) {
        HojaPerfil.Datos -> HojaDatos(
            usuario = usuario,
            edicion = edicion,
            alGuardar = { nombre, telefono, cerrar ->
                logica.guardarDatos(nombre, telefono) {
                    cerrar()
                    Toast.makeText(contexto, "Datos guardados", Toast.LENGTH_SHORT).show()
                }
            },
            alCerrar = { hoja = null },
        )
        HojaPerfil.Analisis -> HojaAnalisis(
            diagnosticos = estado.diagnosticos,
            alElegir = alVerDiagnostico,
            alAnalizar = alAnalizar,
            alCerrar = { hoja = null },
        )
        HojaPerfil.Pagos -> HojaPagos(
            comprobantes = estado.comprobantes,
            alElegir = alVerComprobante,
            alCerrar = { hoja = null },
        )
        HojaPerfil.ComoFunciona -> HojaComoFunciona(alCerrar = { hoja = null })
        HojaPerfil.Privacidad -> HojaPrivacidad(alCerrar = { hoja = null })
        HojaPerfil.CerrarSesion -> if (sesion != null) {
            HojaCerrarSesion(
                huellaActiva = sesion.huellaActiva,
                alConfirmar = sesion.cerrarSesion,
                alCerrar = { hoja = null },
            )
        }
        null -> Unit
    }
}

/** «Sin vehículos todavía», «1 vehículo» o «3 vehículos». */
private fun cantidad(numero: Int, ninguno: String, uno: String, varios: String): String = when (numero) {
    0 -> ninguno
    1 -> uno
    else -> "$numero $varios"
}

/** 3001234567 → 300 123 4567 */
private fun celularLegible(celular: String): String =
    if (celular.length == 10) "${celular.take(3)} ${celular.substring(3, 6)} ${celular.takeLast(4)}" else celular

private fun versionDeLaApp(contexto: Context): String = try {
    val paquete = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        contexto.packageManager.getPackageInfo(contexto.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        contexto.packageManager.getPackageInfo(contexto.packageName, 0)
    }
    "RinFix ${paquete.versionName ?: "en desarrollo"}"
} catch (e: PackageManager.NameNotFoundException) {
    "RinFix"
}

/** Lleva a registrar una huella en el teléfono; si esa pantalla no existe, a seguridad o a ajustes. */
private fun abrirAjustesDeHuella(contexto: Context) {
    val intentos = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(
                Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                    .putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG),
            )
        }
        add(Intent(Settings.ACTION_SECURITY_SETTINGS))
        add(Intent(Settings.ACTION_SETTINGS))
    }
    for (intento in intentos) {
        try {
            contexto.startActivity(intento)
            return
        } catch (e: ActivityNotFoundException) {
            // Se prueba la siguiente.
        }
    }
}

// ---------- Piezas de la pantalla ----------

@Composable
private fun TarjetaCuenta(usuario: Usuario, alEditar: () -> Unit, modifier: Modifier = Modifier) {
    Tarjeta(espacio = 0, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Avatar(nombre = usuario.nombre, tamano = 64.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = usuario.nombre, style = MaterialTheme.typography.titleLarge, color = Colores.Tinta)
                Text(text = usuario.correo, style = MaterialTheme.typography.bodySmall, color = Colores.Tinta3)
                Text(
                    text = usuario.telefono?.let(::celularLegible) ?: "Sin celular",
                    style = MaterialTheme.typography.bodySmall,
                    color = Colores.Tinta3,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .lineaPunteadaArriba()
                .padding(top = 14.dp),
        ) {
            Etiqueta(
                texto = if (usuario.rol == Rol.Cliente) "Cliente" else "Taller",
                tipo = if (usuario.rol == Rol.Cliente) TipoEtiqueta.Neutra else TipoEtiqueta.EnProceso,
                icono = if (usuario.rol == Rol.Cliente) Iconos.Usuario else Iconos.Herramienta,
            )
            Box(modifier = Modifier.weight(1f))
            Boton(
                texto = "Editar datos",
                alPulsar = alEditar,
                estilo = EstiloBoton.Blanco,
                tamano = TamanoBoton.Pequeno,
            )
        }
    }
}

/** Título de sección y una tarjeta blanca con sus filas. */
@Composable
private fun Seccion(titulo: String, modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        EncabezadoSeccion(titulo = titulo)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sombra(Sombra.Baja, Formas.Tarjeta)
                .clip(Formas.Tarjeta)
                .background(Colores.Papel),
            content = contenido,
        )
    }
}

@Composable
private fun FilaPerfil(
    icono: ImageVector,
    fondoIcono: Color,
    colorIcono: Color,
    titulo: String,
    detalle: String?,
    alPulsar: (() -> Unit)? = null,
    conLinea: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (conLinea) Modifier.lineaPunteadaArriba(Colores.Linea) else Modifier)
            .then(
                if (alPulsar != null) Modifier.clickable(role = Role.Button, onClickLabel = titulo, onClick = alPulsar) else Modifier,
            )
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        IconoFila(icono = icono, fondo = fondoIcono, color = colorIcono)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
            detalle?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = Colores.Tinta3) }
        }
        if (alPulsar != null) {
            Icon(Iconos.Siguiente, contentDescription = null, tint = Colores.Tinta3, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun IconoFila(icono: ImageVector, fondo: Color, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(fondo, RoundedCornerShape(12.dp)),
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun FilaHuella(sesion: ControlSesion, disponible: Boolean) {
    val habilitada = disponible && !sesion.activandoHuella
    val activa = sesion.huellaActiva && disponible
    val fondoIcono by animateColorAsState(if (activa) Colores.Menta else Colores.Crema2, label = "ícono de huella")
    val detalle = when {
        !disponible && Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> "Necesita Android 9 o más reciente."
        !disponible -> "Registra una huella en los ajustes del teléfono para usarla aquí."
        sesion.activandoHuella -> "Esperando tu huella…"
        activa -> "Se pide al abrir RinFix y si sales por más de 5 minutos."
        else -> "Entra con tu huella en vez de escribir la contraseña."
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = activa,
                enabled = habilitada,
                role = Role.Switch,
                onValueChange = { encender -> if (encender) sesion.activarHuella() else sesion.desactivarHuella() },
            )
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        IconoFila(
            icono = Iconos.Huella,
            fondo = fondoIcono,
            color = if (activa) Colores.MentaTexto else Colores.Tinta2,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Desbloqueo con huella", style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
            Text(text = detalle, style = MaterialTheme.typography.bodySmall, color = Colores.Tinta3)
        }
        Switch(
            checked = activa,
            onCheckedChange = null,
            enabled = habilitada,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Colores.Papel,
                checkedTrackColor = Colores.MentaTexto,
                checkedBorderColor = Colores.MentaTexto,
                uncheckedThumbColor = Colores.Tinta3,
                uncheckedTrackColor = Colores.Niebla,
                uncheckedBorderColor = Colores.Linea2,
            ),
        )
    }
}

// ---------- Hojas ----------

@Composable
private fun HojaDatos(
    usuario: Usuario,
    edicion: PerfilLogica.Edicion,
    alGuardar: (nombre: String, telefono: String, cerrar: () -> Unit) -> Unit,
    alCerrar: () -> Unit,
) {
    var nombre by rememberSaveable { mutableStateOf(usuario.nombre) }
    var telefono by rememberSaveable { mutableStateOf(usuario.telefono?.let(::celularLegible).orEmpty()) }
    Hoja(alCerrar = alCerrar) { cerrar ->
        Text(
            text = "Tus datos",
            style = MaterialTheme.typography.headlineSmall,
            color = Colores.Tinta,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Tu celular es opcional y solo se usa para avisos de RinFix.",
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.Tinta3,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Campo(
                valor = nombre,
                alCambiar = { nombre = it.take(PerfilLogica.MaximoNombre) },
                etiqueta = "Nombre completo",
                icono = Iconos.Usuario,
                error = edicion.errorNombre,
                tipoContenido = ContentType.PersonFullName,
                opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                habilitado = !edicion.guardando,
            )
            Campo(
                valor = telefono,
                alCambiar = { nuevo -> telefono = nuevo.filter { it.isDigit() || it == ' ' }.take(13) },
                etiqueta = "Celular",
                icono = Iconos.Telefono,
                error = edicion.errorTelefono,
                ayuda = "Opcional",
                marcador = "300 123 4567",
                tipoTeclado = KeyboardType.Phone,
                tipoContenido = ContentType.PhoneNumber,
                opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                accionesTeclado = KeyboardActions(onDone = { alGuardar(nombre, telefono, cerrar) }),
                habilitado = !edicion.guardando,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Colores.Niebla, Formas.Campo)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(Iconos.Correo, contentDescription = null, tint = Colores.Tinta3, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = usuario.correo, style = MaterialTheme.typography.bodyMedium, color = Colores.Tinta2)
                    Text(
                        text = "El correo no se puede cambiar por ahora.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.Tinta3,
                    )
                }
            }
            AnimatedVisibility(visible = edicion.error != null) {
                Aviso(titulo = "No se guardó.", mensaje = edicion.error.orEmpty(), tipo = TipoAviso.Error)
            }
            Boton(
                texto = "Guardar cambios",
                alPulsar = { alGuardar(nombre, telefono, cerrar) },
                ocupado = edicion.guardando,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun HojaAnalisis(
    diagnosticos: List<Diagnostico>,
    alElegir: (String) -> Unit,
    alAnalizar: () -> Unit,
    alCerrar: () -> Unit,
) {
    Hoja(alCerrar = alCerrar) { cerrar ->
        TituloHoja(titulo = "Mis análisis", detalle = "Diagnósticos preliminares de tus fotos, del más reciente al más antiguo.")
        if (diagnosticos.isEmpty()) {
            HojaVacia(
                texto = "Todavía no has analizado ningún rin.",
                textoAccion = "Analizar un rin",
                iconoAccion = Iconos.Camara,
                alPulsar = {
                    cerrar()
                    alAnalizar()
                },
            )
        } else {
            ListaDeHoja {
                diagnosticos.forEach { diagnostico ->
                    FilaDiagnostico(
                        diagnostico = diagnostico,
                        alPulsar = {
                            cerrar()
                            alElegir(diagnostico.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaDiagnostico(diagnostico: Diagnostico, alPulsar: () -> Unit) {
    val fecha = Formato.fechaCorta(diagnostico.fecha)
    Tarjeta(
        fondo = Colores.LavandaSuave,
        alPulsar = alPulsar,
        descripcionAccion = "Ver el diagnóstico del $fecha",
        relleno = PaddingValues(14.dp),
        espacio = 0,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val foto = diagnostico.fotos.firstOrNull()
            if (foto != null) {
                MiniaturaFoto(ruta = foto, tamano = 52.dp)
            } else {
                FotoRin(ancho = 52.dp, alto = 52.dp, tamanoRin = 46.dp, tono = TonoFoto.Clara, conDano = true, radio = 14.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(fecha, diagnostico.vehiculo).joinToString(", "),
                    style = MaterialTheme.typography.titleSmall,
                    color = Colores.Tinta,
                )
                Text(
                    text = diagnostico.resumen.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = Colores.Tinta3,
                    maxLines = 1,
                )
                Text(
                    text = Formato.rangoPesos(diagnostico.precioMinimo, diagnostico.precioMaximo),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = Colores.Tinta,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (diagnostico.deEjemplo) {
                Etiqueta(texto = "Ejemplo", tipo = TipoEtiqueta.Ia, pequena = true)
            }
        }
    }
}

@Composable
private fun HojaPagos(comprobantes: List<Comprobante>, alElegir: (String) -> Unit, alCerrar: () -> Unit) {
    Hoja(alCerrar = alCerrar) { cerrar ->
        TituloHoja(
            titulo = "Pagos y comprobantes",
            detalle = "Pagos simulados que hiciste en la app. Ninguno cobró dinero real.",
        )
        if (comprobantes.isEmpty()) {
            HojaVacia(texto = "Cuando pagues una reparación desde la app, el comprobante queda aquí.")
        } else {
            ListaDeHoja {
                comprobantes.forEach { comprobante ->
                    Tarjeta(
                        alPulsar = {
                            cerrar()
                            alElegir(comprobante.id)
                        },
                        descripcionAccion = "Ver el comprobante de la orden ${comprobante.codigoOrden}",
                        relleno = PaddingValues(14.dp),
                        espacio = 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconoFila(icono = Iconos.Recibo, fondo = Colores.Menta, color = Colores.MentaTexto)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Orden ${comprobante.codigoOrden}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Colores.Tinta,
                                )
                                Text(
                                    text = "${Formato.fechaCorta(comprobante.fecha)}, ${comprobante.metodo.nombre.lowercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Colores.Tinta3,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Formato.pesos(comprobante.monto),
                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                    color = Colores.Tinta,
                                )
                                Text(
                                    text = if (comprobante.estado == EstadoPago.EnTaller) "En el taller" else "Aprobado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (comprobante.estado == EstadoPago.EnTaller) Colores.MantequillaTexto else Colores.MentaTexto,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HojaComoFunciona(alCerrar: () -> Unit) {
    Hoja(alCerrar = alCerrar) {
        TituloHoja(titulo = "Cómo funciona el diagnóstico", detalle = "Del rin en la foto a la cotización, en cuatro pasos.")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            (
                if (Servidor.configurado) {
                    listOf(
                        "Tomas hasta 3 fotos del rin, con buena luz y el rin completo.",
                        "Claude revisa las fotos en un servidor seguro y marca los daños que ve.",
                        "Te explica en palabras sencillas cada daño y qué tan grave parece.",
                        "El precio sale de las tarifas del taller, nunca de la IA.",
                    )
                } else {
                    listOf(
                        "Tomas hasta 3 fotos del rin, con buena luz y el rin completo.",
                        "Un modelo de visión marca las zonas donde ve posibles daños.",
                        "Claude explica en palabras sencillas lo que se ve y qué tan grave parece.",
                        "El precio sale de las tarifas del taller, nunca de la IA.",
                    )
                }
            ).forEachIndexed { indice, paso ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .background(Colores.Lavanda, RoundedCornerShape(10.dp)),
                    ) {
                        Text(
                            text = "${indice + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Colores.LavandaTexto,
                        )
                    }
                    Text(
                        text = paso,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Colores.Tinta2,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Aviso(
                titulo = "Es un diagnóstico preliminar.",
                mensaje = "Solo ve lo que sale en la foto: una fisura o una deformación pequeña puede no notarse. " +
                    "La revisión del taller es la que confirma que el rin es seguro.",
                tipo = TipoAviso.Informacion,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = if (Servidor.configurado) {
                    "Hoy Claude hace la detección y la explicación; el modelo del teléfono se suma más adelante."
                } else {
                    "En esta versión los análisis son de ejemplo: los resultados no vienen de fotos analizadas de verdad."
                },
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Tinta3,
            )
        }
    }
}

@Composable
private fun HojaPrivacidad(alCerrar: () -> Unit) {
    Hoja(alCerrar = alCerrar) {
        TituloHoja(titulo = "Privacidad y datos", detalle = "Lo que RinFix guarda y lo que nunca recibe.")
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PuntoPrivacidad(
                icono = Iconos.Huella,
                texto = "Tu huella nunca sale del teléfono. Android la revisa y a RinFix solo le dice si coincidió.",
            )
            PuntoPrivacidad(
                icono = Iconos.Camara,
                texto = "Las fotos de tus rines solo se usan para el diagnóstico y el seguimiento de tu reparación.",
            )
            PuntoPrivacidad(
                icono = Iconos.TarjetaPago,
                texto = "Los pagos son simulados: no se cobra dinero ni se guardan datos de tarjetas o cuentas.",
            )
            PuntoPrivacidad(
                icono = Iconos.Usuario,
                texto = "Tu nombre, correo y celular se usan para tu cuenta y para los avisos de tus reparaciones.",
            )
        }
    }
}

@Composable
private fun PuntoPrivacidad(icono: ImageVector, texto: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        IconoFila(icono = icono, fondo = Colores.MentaSuave, color = Colores.MentaTexto)
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.Tinta2,
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
        )
    }
}

@Composable
private fun HojaCerrarSesion(huellaActiva: Boolean, alConfirmar: () -> Unit, alCerrar: () -> Unit) {
    var saliendo by remember { mutableStateOf(false) }
    Hoja(
        alCerrar = {
            alCerrar()
            if (saliendo) alConfirmar()
        },
    ) { cerrar ->
        TituloHoja(
            titulo = "¿Cerrar sesión?",
            detalle = "Para volver a entrar necesitarás tu correo y tu contraseña." +
                if (huellaActiva) " El desbloqueo con huella se desactiva en este teléfono." else "",
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Boton(
                texto = "Cerrar sesión",
                alPulsar = {
                    saliendo = true
                    cerrar()
                },
                icono = Iconos.Salir,
                modifier = Modifier.fillMaxWidth(),
            )
            Boton(
                texto = "Cancelar",
                alPulsar = cerrar,
                estilo = EstiloBoton.Transparente,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TituloHoja(titulo: String, detalle: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.headlineSmall,
        color = Colores.Tinta,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        text = detalle,
        style = MaterialTheme.typography.bodyMedium,
        color = Colores.Tinta3,
        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
    )
}

/** Lista con desplazamiento propio, para que una lista larga no tape toda la pantalla. */
@Composable
private fun ListaDeHoja(contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState()),
        content = contenido,
    )
}

@Composable
private fun HojaVacia(
    texto: String,
    textoAccion: String? = null,
    iconoAccion: ImageVector? = null,
    alPulsar: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Colores.Crema, Formas.Tarjeta)
            .padding(20.dp),
    ) {
        Text(text = texto, style = MaterialTheme.typography.bodyMedium, color = Colores.Tinta2)
        if (textoAccion != null && alPulsar != null) {
            Boton(texto = textoAccion, alPulsar = alPulsar, estilo = EstiloBoton.Lavanda, tamano = TamanoBoton.Pequeno, icono = iconoAccion)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 1100)
@Composable
private fun VistaPreviaPerfil() {
    val usuario = Usuario(id = "cliente-1", nombre = "Laura Méndez", correo = "laura@correo.com", rol = Rol.Cliente)
    TemaRinFix {
        Column(verticalArrangement = Arrangement.spacedBy(22.dp), modifier = Modifier.padding(20.dp)) {
            TarjetaCuenta(usuario = usuario, alEditar = {})
            Seccion(titulo = "Lo tuyo") {
                FilaPerfil(Iconos.Carro, Colores.Mantequilla, Colores.MantequillaTexto, "Mi garaje", "2 vehículos", alPulsar = {})
                FilaPerfil(Iconos.Ia, Colores.Lavanda, Colores.LavandaTexto, "Mis análisis", "1 diagnóstico", alPulsar = {}, conLinea = true)
            }
            Seccion(titulo = "Seguridad") {
                FilaHuella(
                    sesion = ControlSesion(
                        huellaActiva = true,
                        activandoHuella = false,
                        errorHuella = null,
                        activarHuella = {},
                        desactivarHuella = {},
                        limpiarErrorHuella = {},
                        cerrarSesion = {},
                    ),
                    disponible = true,
                )
            }
        }
    }
}
