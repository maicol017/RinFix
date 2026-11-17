package com.rinfix.app.cliente

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.Aviso
import com.rinfix.app.componentes.BarraSuperior
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.BotonIcono
import com.rinfix.app.componentes.Campo
import com.rinfix.app.componentes.Enlace
import com.rinfix.app.componentes.EstadoVacio
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.Hoja
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.SelectorFichas
import com.rinfix.app.componentes.SelectorSegmentos
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TipoAviso
import com.rinfix.app.componentes.TipoEtiqueta
import com.rinfix.app.componentes.lineaPunteadaArriba
import com.rinfix.app.datos.GarajeDeEjemplo
import com.rinfix.app.datos.MaterialRin
import com.rinfix.app.datos.TamanosRin
import com.rinfix.app.datos.Vehiculo
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import java.time.LocalDate

/**
 * Mi garaje: los vehículos del cliente con el tamaño y material de sus rines.
 * Con `abrirFormulario` la pantalla llega con el formulario de vehículo nuevo abierto
 * (desde «Agregar vehículo» en Inicio).
 */
@Composable
fun PantallaGaraje(
    alVolver: () -> Unit,
    alAnalizar: (idVehiculo: String) -> Unit,
    modifier: Modifier = Modifier,
    abrirFormulario: Boolean = false,
    logica: GarajeLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    ContenidoGaraje(
        estado = estado,
        alVolver = alVolver,
        alAnalizar = alAnalizar,
        abrirFormulario = abrirFormulario,
        alGuardar = logica::guardar,
        alQuitar = logica::quitar,
        alLimpiarError = logica::limpiarError,
        modifier = modifier,
    )
}

@Composable
private fun ContenidoGaraje(
    estado: GarajeLogica.Estado,
    alVolver: () -> Unit,
    alAnalizar: (String) -> Unit,
    abrirFormulario: Boolean,
    alGuardar: (Vehiculo, () -> Unit) -> Unit,
    alQuitar: (String) -> Unit,
    alLimpiarError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = recordarEntrada()
    val contexto = LocalContext.current

    // null: cerrado. "": vehículo nuevo. Un id: editar ese vehículo.
    var formulario by rememberSaveable { mutableStateOf(if (abrirFormulario) "" else null) }
    var porQuitar by rememberSaveable { mutableStateOf<String?>(null) }

    fun abrirNuevo() {
        alLimpiarError()
        formulario = ""
    }

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperior(
            titulo = "Mi garaje",
            alVolver = alVolver,
            accion = {
                BotonIcono(icono = Iconos.Agregar, descripcion = "Agregar vehículo", alPulsar = ::abrirNuevo)
            },
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        ) {
            AnimatedVisibility(visible = estado.error != null && formulario == null) {
                Aviso(titulo = "No se pudo completar.", mensaje = estado.error.orEmpty(), tipo = TipoAviso.Error)
            }

            if (!estado.cargando && estado.vehiculos.isEmpty()) {
                EstadoVacio(
                    icono = Iconos.Carro,
                    titulo = "Aún no tienes vehículos",
                    mensaje = "Agrega tu carro para que el análisis y la cotización tengan en cuenta " +
                        "el tamaño y el material del rin.",
                    textoAccion = "Agregar vehículo",
                    iconoAccion = Iconos.Agregar,
                    alPulsarAccion = ::abrirNuevo,
                    modifier = Modifier.aparecer(visible, orden = 0),
                )
            } else {
                Text(
                    text = "Guarda tus vehículos para analizar y cotizar más rápido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Colores.Tinta2,
                    modifier = Modifier.aparecer(visible, orden = 0),
                )
                estado.vehiculos.forEachIndexed { indice, vehiculo ->
                    key(vehiculo.id) {
                        TarjetaVehiculo(
                            vehiculo = vehiculo,
                            alAnalizar = { alAnalizar(vehiculo.id) },
                            alEditar = {
                                alLimpiarError()
                                formulario = vehiculo.id
                            },
                            alQuitar = { porQuitar = vehiculo.id },
                            modifier = Modifier.aparecer(visible, orden = indice + 1),
                        )
                    }
                }
            }
        }
    }

    formulario?.let { idFormulario ->
        val existente = estado.vehiculos.firstOrNull { it.id == idFormulario }
        Hoja(alCerrar = { formulario = null }) { cerrar ->
            FormularioVehiculo(
                inicial = existente,
                guardando = estado.guardando,
                error = estado.error,
                alGuardar = { vehiculo ->
                    alGuardar(vehiculo) {
                        cerrar()
                        val texto = if (existente == null) "Agregaste ${vehiculo.titulo}." else "Guardaste los cambios."
                        Toast.makeText(contexto, texto, Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
    }

    porQuitar?.let { idQuitar ->
        val vehiculo = estado.vehiculos.firstOrNull { it.id == idQuitar }
        if (vehiculo != null) {
            Hoja(alCerrar = { porQuitar = null }) { cerrar ->
                ConfirmarQuitar(
                    vehiculo = vehiculo,
                    alQuitar = {
                        alQuitar(vehiculo.id)
                        cerrar()
                        Toast.makeText(contexto, "Quitaste ${vehiculo.titulo} de tu garaje.", Toast.LENGTH_SHORT).show()
                    },
                    alCancelar = cerrar,
                )
            }
        }
    }
}

@Composable
private fun TarjetaVehiculo(
    vehiculo: Vehiculo,
    alAnalizar: () -> Unit,
    alEditar: () -> Unit,
    alQuitar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Tarjeta(
        relleno = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        espacio = 0,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .background(Colores.Mantequilla, RoundedCornerShape(16.dp)),
            ) {
                Icon(
                    imageVector = Iconos.Carro,
                    contentDescription = null,
                    tint = Colores.MantequillaTexto,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehiculo.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = Colores.Tinta,
                    modifier = Modifier.semantics { heading() },
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    val pulgadas = vehiculo.pulgadasRin
                    val conMaterial = vehiculo.materialRin != MaterialRin.Desconocido
                    if (pulgadas != null) {
                        Etiqueta(texto = "Rin $pulgadas\"", tipo = TipoEtiqueta.Neutra, pequena = true)
                    }
                    if (conMaterial) {
                        Etiqueta(texto = vehiculo.materialRin.nombre, tipo = TipoEtiqueta.Neutra, pequena = true)
                    }
                    if (pulgadas == null && !conMaterial) {
                        Etiqueta(texto = "Rin sin datos", tipo = TipoEtiqueta.Neutra, pequena = true)
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .lineaPunteadaArriba(),
        ) {
            Enlace(texto = "Analizar este rin", alPulsar = alAnalizar)
            Spacer(modifier = Modifier.weight(1f))
            Enlace(texto = "Editar", alPulsar = alEditar, color = Colores.Tinta2)
            Enlace(texto = "Quitar", alPulsar = alQuitar, color = Colores.Tinta2)
        }
    }
}

/** Tamaños de rin del formulario; `null` es «No sé». */
private val OpcionesTamano: List<Int?> = TamanosRin + listOf(null)

@Composable
private fun FormularioVehiculo(
    inicial: Vehiculo?,
    guardando: Boolean,
    error: String?,
    alGuardar: (Vehiculo) -> Unit,
) {
    val foco = LocalFocusManager.current
    val anioMaximo = LocalDate.now().year + 1

    var nombre by rememberSaveable { mutableStateOf(inicial?.nombre.orEmpty()) }
    var anio by rememberSaveable { mutableStateOf(inicial?.anio?.toString().orEmpty()) }
    var pulgadas by rememberSaveable { mutableStateOf(inicial?.pulgadasRin) }
    var material by rememberSaveable { mutableStateOf(inicial?.materialRin ?: MaterialRin.Desconocido) }
    var errorNombre by rememberSaveable { mutableStateOf<String?>(null) }
    var errorAnio by rememberSaveable { mutableStateOf<String?>(null) }
    var sacudidas by rememberSaveable { mutableIntStateOf(0) }

    fun enviar() {
        val numero = anio.toIntOrNull()
        errorNombre = if (nombre.trim().length < 2) "Escribe la marca y el modelo, por ejemplo Renault Logan." else null
        errorAnio = if (anio.isNotEmpty() && (numero == null || numero !in 1970..anioMaximo)) {
            "Escribe un año entre 1970 y $anioMaximo, o déjalo vacío."
        } else {
            null
        }
        if (errorNombre != null || errorAnio != null) {
            sacudidas++
            return
        }
        foco.clearFocus()
        alGuardar(
            Vehiculo(
                id = inicial?.id.orEmpty(),
                nombre = nombre.trim(),
                anio = numero,
                pulgadasRin = pulgadas,
                materialRin = material,
            ),
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(Colores.Mantequilla, RoundedCornerShape(14.dp)),
            ) {
                Icon(
                    imageVector = Iconos.Carro,
                    contentDescription = null,
                    tint = Colores.MantequillaTexto,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = if (inicial == null) "Agregar vehículo" else "Editar vehículo",
                style = MaterialTheme.typography.headlineSmall,
                color = Colores.Tinta,
                modifier = Modifier.semantics { heading() },
            )
        }

        AnimatedVisibility(visible = error != null) {
            Aviso(titulo = "No se pudo guardar.", mensaje = error.orEmpty(), tipo = TipoAviso.Error)
        }

        Campo(
            valor = nombre,
            alCambiar = {
                nombre = it
                errorNombre = null
            },
            etiqueta = "Marca y modelo",
            icono = Iconos.Carro,
            error = errorNombre,
            marcador = "Ej.: Renault Logan",
            sacudir = sacudidas,
            opcionesTeclado = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            habilitado = !guardando,
        )
        Campo(
            valor = anio,
            alCambiar = { nuevo ->
                anio = nuevo.filter(Char::isDigit).take(4)
                errorAnio = null
            },
            etiqueta = "Año (opcional)",
            icono = Iconos.Calendario,
            error = errorAnio,
            marcador = "Ej.: 2019",
            sacudir = sacudidas,
            tipoTeclado = KeyboardType.Number,
            opcionesTeclado = KeyboardOptions(imeAction = ImeAction.Done),
            accionesTeclado = KeyboardActions(onDone = { foco.clearFocus() }),
            habilitado = !guardando,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Tamaño del rin", style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
            SelectorFichas(
                opciones = OpcionesTamano,
                seleccionada = pulgadas,
                alElegir = { pulgadas = it },
                texto = { tamano -> tamano?.let { "$it\"" } ?: "No sé" },
                descripcion = { tamano -> tamano?.let { "$it pulgadas" } ?: "No sé el tamaño" },
                habilitado = !guardando,
            )
            Text(
                text = "Está escrito en la llanta: en 205/55 R16, el rin es de 16.",
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Tinta3,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Material del rin", style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
            SelectorSegmentos(
                opciones = MaterialRin.entries,
                seleccionada = material,
                alElegir = { material = it },
                texto = { it.nombre },
                habilitado = !guardando,
            )
        }

        Boton(
            texto = if (inicial == null) "Guardar vehículo" else "Guardar cambios",
            alPulsar = ::enviar,
            ocupado = guardando,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

@Composable
private fun ConfirmarQuitar(vehiculo: Vehiculo, alQuitar: () -> Unit, alCancelar: () -> Unit) {
    Text(
        text = "¿Quitar ${vehiculo.titulo}?",
        style = MaterialTheme.typography.headlineSmall,
        color = Colores.Tinta,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        text = "Se quita de tu garaje. Tus diagnósticos y órdenes anteriores se conservan.",
        style = MaterialTheme.typography.bodyMedium,
        color = Colores.Tinta2,
        modifier = Modifier.padding(top = 6.dp),
    )
    Boton(
        texto = "Quitar vehículo",
        alPulsar = alQuitar,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
    )
    Boton(
        texto = "Cancelar",
        alPulsar = alCancelar,
        estilo = EstiloBoton.Transparente,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 700)
@Composable
private fun VistaPreviaGaraje() {
    TemaRinFix {
        ContenidoGaraje(
            estado = GarajeLogica.Estado(
                cargando = false,
                vehiculos = GarajeDeEjemplo.vehiculosIniciales().getValue("cliente-1"),
            ),
            alVolver = {},
            alAnalizar = {},
            abrirFormulario = false,
            alGuardar = { _, _ -> },
            alQuitar = {},
            alLimpiarError = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 700)
@Composable
private fun VistaPreviaGarajeVacio() {
    TemaRinFix {
        ContenidoGaraje(
            estado = GarajeLogica.Estado(cargando = false),
            alVolver = {},
            alAnalizar = {},
            abrirFormulario = false,
            alGuardar = { _, _ -> },
            alQuitar = {},
            alLimpiarError = {},
        )
    }
}
