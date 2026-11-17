package com.rinfix.app.cliente

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinfix.app.componentes.Avatar
import com.rinfix.app.componentes.Boton
import com.rinfix.app.componentes.BotonIcono
import com.rinfix.app.componentes.EncabezadoSeccion
import com.rinfix.app.componentes.Enlace
import com.rinfix.app.componentes.EstiloBoton
import com.rinfix.app.componentes.Etiqueta
import com.rinfix.app.componentes.FotoRin
import com.rinfix.app.componentes.Hoja
import com.rinfix.app.componentes.Iconos
import com.rinfix.app.componentes.LineasTecnicas
import com.rinfix.app.componentes.RinIlustrado
import com.rinfix.app.componentes.TamanoBoton
import com.rinfix.app.componentes.Tarjeta
import com.rinfix.app.componentes.TarjetaOrden
import com.rinfix.app.componentes.TipoEtiqueta
import com.rinfix.app.componentes.TonoFoto
import com.rinfix.app.datos.AnalisisDeEjemplo
import com.rinfix.app.datos.Formato
import com.rinfix.app.datos.GarajeDeEjemplo
import com.rinfix.app.datos.Notificacion
import com.rinfix.app.datos.OrdenesDeEjemplo
import com.rinfix.app.datos.TipoNotificacion
import com.rinfix.app.datos.Vehiculo
import com.rinfix.app.diseno.Colores
import com.rinfix.app.diseno.Formas
import com.rinfix.app.diseno.Movimiento
import com.rinfix.app.diseno.Sombra
import com.rinfix.app.diseno.TemaRinFix
import com.rinfix.app.diseno.aparecer
import com.rinfix.app.diseno.recordarEntrada
import com.rinfix.app.diseno.sombra
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Inicio del cliente: saludo y avisos, la portada para analizar un rin, accesos rápidos,
 * la reparación en curso, Mi garaje y lo más reciente.
 *
 * Quien la usa decide a dónde lleva cada acción (ver [com.rinfix.app.navegacion.NavegacionRinFix]).
 */
@Composable
fun PantallaInicio(
    alAnalizar: () -> Unit,
    alVerOrden: (idOrden: String) -> Unit,
    alVerOrdenes: () -> Unit,
    alAgendar: () -> Unit,
    alPagar: (idOrden: String) -> Unit,
    alAbrirAsistente: () -> Unit,
    alVerGaraje: () -> Unit,
    alAgregarVehiculo: () -> Unit,
    alVerDiagnostico: (idDiagnostico: String) -> Unit,
    modifier: Modifier = Modifier,
    logica: InicioLogica = viewModel(),
) {
    val estado by logica.estado.collectAsState()
    ContenidoInicio(
        estado = estado,
        alAnalizar = alAnalizar,
        alVerOrden = alVerOrden,
        alVerOrdenes = alVerOrdenes,
        alAgendar = alAgendar,
        alPagar = alPagar,
        alAbrirAsistente = alAbrirAsistente,
        alVerGaraje = alVerGaraje,
        alAgregarVehiculo = alAgregarVehiculo,
        alVerDiagnostico = alVerDiagnostico,
        alMarcarLeidas = logica::marcarLeidas,
        modifier = modifier,
    )
}

@Composable
private fun ContenidoInicio(
    estado: InicioLogica.Estado,
    alAnalizar: () -> Unit,
    alVerOrden: (String) -> Unit,
    alVerOrdenes: () -> Unit,
    alAgendar: () -> Unit,
    alPagar: (String) -> Unit,
    alAbrirAsistente: () -> Unit,
    alVerGaraje: () -> Unit,
    alAgregarVehiculo: () -> Unit,
    alVerDiagnostico: (String) -> Unit,
    alMarcarLeidas: () -> Unit,
    modifier: Modifier = Modifier,
    hoy: LocalDate = LocalDate.now(),
) {
    val visible = recordarEntrada()
    val contexto = LocalContext.current
    var verAvisos by rememberSaveable { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
    ) {
        Encabezado(
            nombre = estado.nombre,
            fecha = Formato.fechaLarga(hoy),
            sinLeer = estado.sinLeer,
            alAbrirAvisos = { verAvisos = true },
            modifier = Modifier.aparecer(visible, orden = 0),
        )

        Portada(alAnalizar = alAnalizar, modifier = Modifier.aparecer(visible, orden = 1))

        AccesosRapidos(
            alVerOrdenes = alVerOrdenes,
            alAgendar = alAgendar,
            alPagos = {
                val pendiente = estado.pagoPendiente
                if (pendiente != null) {
                    alPagar(pendiente.id)
                } else {
                    Toast.makeText(contexto, "No tienes pagos pendientes.", Toast.LENGTH_SHORT).show()
                }
            },
            alAbrirAsistente = alAbrirAsistente,
            modifier = Modifier.aparecer(visible, orden = 2),
        )

        if (!estado.cargando) {
            Column(modifier = Modifier.aparecer(visible, orden = 3)) {
                EncabezadoSeccion(
                    titulo = "Reparación en curso",
                    accion = "Ver todas",
                    alPulsarAccion = alVerOrdenes,
                )
                val orden = estado.ordenActiva
                if (orden != null) {
                    TarjetaOrden(
                        orden = orden,
                        alPulsar = { alVerOrden(orden.id) },
                        modifier = Modifier.padding(top = 6.dp),
                    )
                } else {
                    SinReparacion(alAnalizar = alAnalizar, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Column(modifier = Modifier.aparecer(visible, orden = 4)) {
                EncabezadoSeccion(titulo = "Mi garaje", accion = "Ver todo", alPulsarAccion = alVerGaraje)
                FilaGaraje(
                    vehiculos = estado.vehiculos,
                    alVerGaraje = alVerGaraje,
                    alAgregarVehiculo = alAgregarVehiculo,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            if (estado.actividad.isNotEmpty()) {
                Column(modifier = Modifier.aparecer(visible, orden = 5)) {
                    EncabezadoSeccion(titulo = "Historial reciente")
                    Historial(
                        actividad = estado.actividad,
                        alVerDiagnostico = alVerDiagnostico,
                        alVerOrden = alVerOrden,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }

    if (verAvisos) {
        Hoja(alCerrar = { verAvisos = false }) { cerrar ->
            ListaAvisos(
                avisos = estado.notificaciones,
                alMarcarLeidas = alMarcarLeidas,
                alAbrirOrden = { idOrden ->
                    cerrar()
                    alVerOrden(idOrden)
                },
            )
        }
    }
}

// ---------- Encabezado ----------

@Composable
private fun Encabezado(
    nombre: String,
    fecha: String,
    sinLeer: Int,
    alAbrirAvisos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primerNombre = nombre.trim().substringBefore(' ')
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
    ) {
        Avatar(nombre = nombre, tamano = 46.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fecha,
                style = MaterialTheme.typography.bodySmall,
                color = Colores.Tinta3,
            )
            Text(
                text = if (primerNombre.isEmpty()) "Hola" else "Hola, $primerNombre",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 28.sp),
                color = Colores.Tinta,
                modifier = Modifier.semantics { heading() },
            )
        }
        Box {
            BotonIcono(
                icono = Iconos.Campana,
                descripcion = if (sinLeer > 0) "Notificaciones, $sinLeer sin leer" else "Notificaciones",
                alPulsar = alAbrirAvisos,
            )
            if (sinLeer > 0) {
                Insignia(
                    cantidad = sinLeer,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp),
                )
            }
        }
    }
}

/** Número rojo sobre la campana. Lo anuncia el botón, así que el lector de pantalla lo salta. */
@Composable
private fun Insignia(cantidad: Int, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clearAndSetSemantics { }
            .background(Colores.Crema, Formas.Pildora)
            .padding(2.dp)
            .defaultMinSize(minWidth = 14.dp, minHeight = 14.dp)
            .background(Colores.Freno, Formas.Pildora)
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = if (cantidad > 9) "9+" else cantidad.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

// ---------- Portada ----------

private val FormaPortada = RoundedCornerShape(30.dp)

private val SombraRinPortada = Shadow(
    radius = 20.dp,
    color = Color.Black.copy(alpha = 0.6f),
    offset = DpOffset(x = 0.dp, y = 18.dp),
)

/**
 * Tarjeta oscura para analizar un rin. El rin entra girando y se detiene:
 * un solo movimiento al abrir, sin animación continua que gaste batería.
 */
@Composable
private fun Portada(alAnalizar: () -> Unit, modifier: Modifier = Modifier) {
    val interaccion = remember { MutableInteractionSource() }
    val presionada by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionada) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "escala de la portada",
    )
    val enVistaPrevia = LocalInspectionMode.current
    val giro = remember { Animatable(if (enVistaPrevia) 0f else -140f) }
    LaunchedEffect(Unit) {
        giro.animateTo(0f, tween(durationMillis = 1600, easing = Movimiento.Suave))
    }
    val vista = LocalView.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 212.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .sombra(Sombra.Alta, FormaPortada)
            .clip(FormaPortada)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        0f to Color(0xFF3A352F),
                        0.45f to Color(0xFF141210),
                        1f to Colores.Tinta,
                        center = Offset(size.width * 0.9f, size.height * 0.1f),
                        radius = size.width * 1.2f,
                    ),
                )
            }
            .clickable(
                interactionSource = interaccion,
                indication = null,
                role = Role.Button,
                onClickLabel = "Analizar un rin",
                onClick = {
                    vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    alAnalizar()
                },
            ),
    ) {
        LineasTecnicas(
            tamano = 260.dp,
            color = Colores.Menta.copy(alpha = 0.22f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-26).dp),
        )
        RinIlustrado(
            tamano = 214.dp,
            inclinacion = 8f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 46.dp, y = (-4).dp)
                .clearAndSetSemantics { }
                .dropShadow(CircleShape, SombraRinPortada)
                .graphicsLayer { rotationZ = giro.value },
        )

        Column(modifier = Modifier.padding(18.dp)) {
            Etiqueta(texto = "Con IA", tipo = TipoEtiqueta.Vidrio, icono = Iconos.Ia, pequena = true)
            Text(
                text = "Analiza\ntu rin",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 25.sp, lineHeight = 27.sp),
                color = Colores.Crema,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = "Posibles daños y precio estimado en minutos",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.Crema.copy(alpha = 0.78f),
                modifier = Modifier
                    .widthIn(max = 170.dp)
                    .padding(top = 4.dp),
            )
            Boton(
                texto = "Analizar un rin",
                alPulsar = alAnalizar,
                estilo = EstiloBoton.Menta,
                tamano = TamanoBoton.Pequeno,
                icono = Iconos.Camara,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

// ---------- Accesos rápidos ----------

private val FormaAcceso = RoundedCornerShape(20.dp)

@Composable
private fun AccesosRapidos(
    alVerOrdenes: () -> Unit,
    alAgendar: () -> Unit,
    alPagos: () -> Unit,
    alAbrirAsistente: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        val celda = Modifier
            .weight(1f)
            .fillMaxHeight()
        AccesoRapido("Mis órdenes", Iconos.Lista, Colores.Mantequilla, Colores.MantequillaTexto, alVerOrdenes, celda)
        AccesoRapido("Agendar", Iconos.Calendario, Colores.Lavanda, Colores.LavandaTexto, alAgendar, celda)
        AccesoRapido("Pagos", Iconos.TarjetaPago, Colores.Menta, Colores.MentaTexto, alPagos, celda)
        AccesoRapido("Asistente", Iconos.Ia, Colores.Tinta, Colores.Crema, alAbrirAsistente, celda)
    }
}

@Composable
private fun AccesoRapido(
    texto: String,
    icono: ImageVector,
    fondo: Color,
    colorIcono: Color,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "escala de $texto",
    )
    val vista = LocalView.current
    // Sobre tinta el círculo del ícono es crema translúcida; sobre pastel, blanco translúcido.
    val fondoIcono = if (fondo == Colores.Tinta) Colores.Crema.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.55f)

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .heightIn(min = 92.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .sombra(Sombra.Baja, FormaAcceso)
            .clip(FormaAcceso)
            .background(fondo)
            .clickable(
                interactionSource = interaccion,
                indication = null,
                role = Role.Button,
                onClick = {
                    vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    alPulsar()
                },
            )
            .padding(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(fondoIcono, RoundedCornerShape(11.dp)),
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = colorIcono,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = colorIcono,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

// ---------- Reparación en curso ----------

private val SombraRinVacio = Shadow(
    radius = 14.dp,
    color = Colores.Tinta.copy(alpha = 0.25f),
    offset = DpOffset(x = 0.dp, y = 12.dp),
)

/** Sin reparaciones: invita al primer análisis, en lavanda porque es el color de la IA. */
@Composable
private fun SinReparacion(alAnalizar: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 168.dp)
            .clip(Formas.Tarjeta)
            .background(Colores.LavandaSuave)
            .border(1.dp, Colores.LavandaTexto.copy(alpha = 0.08f), Formas.Tarjeta),
    ) {
        LineasTecnicas(
            tamano = 210.dp,
            color = Colores.LavandaTexto.copy(alpha = 0.18f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 56.dp, y = (-44).dp),
        )
        RinIlustrado(
            tamano = 128.dp,
            conDano = true,
            inclinacion = 20f,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 24.dp, y = 30.dp)
                .clearAndSetSemantics { }
                .dropShadow(CircleShape, SombraRinVacio),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(18.dp),
        ) {
            Etiqueta(texto = "Primer paso", tipo = TipoEtiqueta.Ia, icono = Iconos.Ia, pequena = true)
            Text(
                text = "Aún no tienes reparaciones",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Colores.LavandaTexto,
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .padding(top = 6.dp),
            )
            Text(
                text = "Toma una foto del rin y en minutos ves posibles daños y un precio.",
                style = MaterialTheme.typography.bodySmall,
                color = Colores.LavandaTexto,
                modifier = Modifier.widthIn(max = 196.dp),
            )
            Boton(
                texto = "Analizar mi primer rin",
                alPulsar = alAnalizar,
                estilo = EstiloBoton.Lavanda,
                tamano = TamanoBoton.Pequeno,
                icono = Iconos.Camara,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

// ---------- Mi garaje ----------

@Composable
private fun FilaGaraje(
    vehiculos: List<Vehiculo>,
    alVerGaraje: () -> Unit,
    alAgregarVehiculo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (vehiculos.isEmpty()) {
        AgregarPrimerVehiculo(alPulsar = alAgregarVehiculo, modifier = modifier)
    } else {
        // La fila llega hasta los bordes de la pantalla y se desliza de lado.
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier
                .anchoCompleto(margen = 20.dp)
                .horizontalScroll(rememberScrollState())
                .height(IntrinsicSize.Min)
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            vehiculos.forEach { vehiculo ->
                TarjetaVehiculo(vehiculo = vehiculo, alPulsar = alVerGaraje, modifier = Modifier.fillMaxHeight())
            }
            AgregarVehiculo(alPulsar = alAgregarVehiculo, modifier = Modifier.fillMaxHeight())
        }
    }
}

/** Garaje vacío: una tarjeta mantequilla que explica para qué sirve agregar el carro. */
@Composable
private fun AgregarPrimerVehiculo(alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(Formas.Tarjeta)
            .background(Colores.MantequillaSuave)
            .border(1.dp, Colores.MantequillaTexto.copy(alpha = 0.1f), Formas.Tarjeta)
            .clickable(role = Role.Button, onClickLabel = "Agregar vehículo", onClick = alPulsar)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(Colores.Mantequilla, RoundedCornerShape(16.dp)),
        ) {
            Icon(
                imageVector = Iconos.Carro,
                contentDescription = null,
                tint = Colores.MantequillaTexto,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "Agrega tu carro",
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = Colores.Tinta,
            )
            Text(
                text = "El análisis usa el tamaño y el material de su rin.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
                color = Colores.MantequillaTexto,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(Colores.Tinta, CircleShape),
        ) {
            Icon(
                imageVector = Iconos.Agregar,
                contentDescription = null,
                tint = Colores.Crema,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun TarjetaVehiculo(vehiculo: Vehiculo, alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    Tarjeta(
        alPulsar = alPulsar,
        descripcionAccion = "Ver ${vehiculo.titulo} en Mi garaje",
        relleno = PaddingValues(14.dp),
        espacio = 3,
        modifier = modifier.widthIn(min = 150.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(bottom = 6.dp)
                .size(40.dp)
                .background(Colores.Mantequilla, RoundedCornerShape(14.dp)),
        ) {
            Icon(
                imageVector = Iconos.Carro,
                contentDescription = null,
                tint = Colores.MantequillaTexto,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(text = vehiculo.titulo, style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
        Text(text = vehiculo.descripcionRin, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
    }
}

@Composable
private fun AgregarVehiculo(alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        modifier = modifier
            .widthIn(min = 120.dp)
            .clip(Formas.Tarjeta)
            .background(Colores.MantequillaSuave)
            .drawBehind {
                val trazo = 1.5.dp.toPx()
                drawRoundRect(
                    color = Colores.MantequillaTexto.copy(alpha = 0.3f),
                    topLeft = Offset(trazo / 2, trazo / 2),
                    size = Size(size.width - trazo, size.height - trazo),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(
                        width = trazo,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                    ),
                )
            }
            .clickable(role = Role.Button, onClick = alPulsar)
            .padding(14.dp),
    ) {
        Icon(
            imageVector = Iconos.Agregar,
            contentDescription = null,
            tint = Colores.MantequillaTexto,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "Agregar vehículo",
            style = MaterialTheme.typography.labelMedium,
            color = Colores.MantequillaTexto,
            textAlign = TextAlign.Center,
        )
    }
}

/** Deja que el contenido ocupe también los márgenes laterales de la pantalla. */
private fun Modifier.anchoCompleto(margen: Dp): Modifier = layout { medible, restricciones ->
    val lado = margen.roundToPx()
    val ancho = restricciones.maxWidth + lado * 2
    val colocable = medible.measure(restricciones.copy(minWidth = ancho, maxWidth = ancho))
    layout(restricciones.maxWidth, colocable.height) {
        colocable.place(-lado, 0)
    }
}

// ---------- Historial reciente ----------

@Composable
private fun Historial(
    actividad: List<InicioLogica.Actividad>,
    alVerDiagnostico: (String) -> Unit,
    alVerOrden: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Tarjeta(
        relleno = PaddingValues(vertical = 4.dp),
        espacio = 0,
        modifier = modifier.fillMaxWidth(),
    ) {
        actividad.forEachIndexed { indice, item ->
            if (indice > 0) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Colores.Linea,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
            when (item) {
                is InicioLogica.Actividad.Analisis -> {
                    val diagnostico = item.diagnostico
                    FilaHistorial(
                        titulo = "Diagnóstico preliminar",
                        detalle = buildString {
                            append(Formato.fechaCorta(diagnostico.fecha))
                            append(", ")
                            append(diagnostico.resumen)
                            if (diagnostico.deEjemplo) append(" (ejemplo)")
                        },
                        valor = Formato.rangoPesos(diagnostico.precioMinimo, diagnostico.precioMaximo, corto = true),
                        alPulsar = { alVerDiagnostico(diagnostico.id) },
                    ) {
                        FotoRin(
                            ancho = 44.dp,
                            alto = 44.dp,
                            tamanoRin = 40.dp,
                            tono = TonoFoto.Taller,
                            conDano = true,
                            inclinacion = 20f,
                            radio = 12.dp,
                        )
                    }
                }

                is InicioLogica.Actividad.Entrega -> {
                    val orden = item.orden
                    FilaHistorial(
                        titulo = "${orden.servicio} entregado",
                        detalle = "${Formato.fechaCorta(orden.ultimaActualizacion)}, ${orden.vehiculo}",
                        valor = orden.total?.let(Formato::pesos).orEmpty(),
                        alPulsar = { alVerOrden(orden.id) },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Colores.MentaSuave, RoundedCornerShape(14.dp)),
                        ) {
                            Icon(
                                imageVector = Iconos.Hecho,
                                contentDescription = null,
                                tint = Colores.MentaTexto,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaHistorial(
    titulo: String,
    detalle: String,
    valor: String,
    alPulsar: () -> Unit,
    imagen: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = alPulsar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        imagen()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
            Text(text = detalle, style = MaterialTheme.typography.labelSmall, color = Colores.Tinta3)
        }
        Text(
            text = valor,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = Colores.Tinta,
        )
    }
}

// ---------- Avisos ----------

@Composable
private fun ListaAvisos(
    avisos: List<Notificacion>,
    alMarcarLeidas: () -> Unit,
    alAbrirOrden: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Notificaciones",
            style = MaterialTheme.typography.headlineSmall,
            color = Colores.Tinta,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (avisos.any { !it.leida }) {
            Enlace(
                texto = "Marcar como leídas",
                alPulsar = alMarcarLeidas,
                estilo = MaterialTheme.typography.bodyMedium,
                color = Colores.Tinta2,
            )
        }
    }
    if (avisos.isEmpty()) {
        Text(
            text = "No tienes notificaciones por ahora.",
            style = MaterialTheme.typography.bodyMedium,
            color = Colores.Tinta2,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    avisos.forEach { aviso ->
        val idOrden = aviso.ordenId
        FilaAviso(
            aviso = aviso,
            alPulsar = if (idOrden != null) ({ alAbrirOrden(idOrden) }) else null,
        )
    }
}

@Composable
private fun FilaAviso(aviso: Notificacion, alPulsar: (() -> Unit)?) {
    val (icono, fondo, color) = when (aviso.tipo) {
        TipoNotificacion.Avance -> Triple(Iconos.Herramienta, Colores.MentaSuave, Colores.MentaTexto)
        TipoNotificacion.Fotos -> Triple(Iconos.Imagen, Colores.LavandaSuave, Colores.LavandaTexto)
        TipoNotificacion.Cotizacion -> Triple(Iconos.Recibo, Colores.Mantequilla, Colores.MantequillaTexto)
        TipoNotificacion.Pago -> Triple(Iconos.TarjetaPago, Colores.MentaSuave, Colores.MentaTexto)
        TipoNotificacion.Entrega -> Triple(Iconos.Hecho, Colores.MentaSuave, Colores.MentaTexto)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(Formas.Campo)
            .then(if (alPulsar != null) Modifier.clickable(role = Role.Button, onClick = alPulsar) else Modifier)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(fondo, CircleShape),
        ) {
            Icon(imageVector = icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = aviso.titulo, style = MaterialTheme.typography.titleSmall, color = Colores.Tinta)
            Text(
                text = "${aviso.detalle}, ${Formato.haceCuanto(aviso.fecha)}",
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Tinta3,
            )
        }
        if (!aviso.leida) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Colores.Freno, CircleShape)
                    .semantics { contentDescription = "Sin leer" },
            )
        }
    }
}

// ---------- Vistas previas ----------

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 1500)
@Composable
private fun VistaPreviaInicio() {
    val ahora = LocalDateTime.now()
    val ordenes = OrdenesDeEjemplo.ordenesIniciales(ahora).filter { it.clienteId == "cliente-1" }
    val diagnosticos = AnalisisDeEjemplo.diagnosticosIniciales(ahora).getValue("cliente-1")
    TemaRinFix {
        ContenidoInicio(
            estado = InicioLogica.Estado(
                cargando = false,
                nombre = "Laura Méndez",
                ordenActiva = ordenes.first { it.activa },
                pagoPendiente = ordenes.first { it.activa },
                vehiculos = GarajeDeEjemplo.vehiculosIniciales().getValue("cliente-1"),
                actividad = listOf(
                    InicioLogica.Actividad.Analisis(diagnosticos.first()),
                    InicioLogica.Actividad.Entrega(ordenes.first { !it.activa }),
                ),
                notificaciones = OrdenesDeEjemplo.notificacionesIniciales(ahora),
            ),
            alAnalizar = {},
            alVerOrden = {},
            alVerOrdenes = {},
            alAgendar = {},
            alPagar = {},
            alAbrirAsistente = {},
            alVerGaraje = {},
            alAgregarVehiculo = {},
            alVerDiagnostico = {},
            alMarcarLeidas = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF3E7, heightDp = 900)
@Composable
private fun VistaPreviaInicioCuentaNueva() {
    TemaRinFix {
        ContenidoInicio(
            estado = InicioLogica.Estado(cargando = false, nombre = "Andrea Gómez"),
            alAnalizar = {},
            alVerOrden = {},
            alVerOrdenes = {},
            alAgendar = {},
            alPagar = {},
            alAbrirAsistente = {},
            alVerGaraje = {},
            alAgregarVehiculo = {},
            alVerDiagnostico = {},
            alMarcarLeidas = {},
        )
    }
}
