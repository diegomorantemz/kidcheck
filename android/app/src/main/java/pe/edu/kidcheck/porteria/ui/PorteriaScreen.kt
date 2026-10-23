package pe.edu.kidcheck.porteria.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import pe.edu.kidcheck.porteria.AppGraph
import pe.edu.kidcheck.porteria.data.ColaEntity
import pe.edu.kidcheck.porteria.data.PorteriaRepository
import pe.edu.kidcheck.porteria.data.ResultadoRegistro
import pe.edu.kidcheck.porteria.data.RosterEntity
import pe.edu.kidcheck.porteria.net.SinSesionException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val zonaLima = ZoneId.of("America/Lima")
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm:ss")
private val formatoHoraCorta = DateTimeFormatter.ofPattern("HH:mm")

private val MOTIVOS = listOf(
    "enfermedad" to "Enfermedad",
    "cita_medica" to "Cita médica",
    "viaje" to "Viaje familiar",
    "emergencia_familiar" to "Emergencia familiar",
    "otro" to "Otro"
)

private sealed interface ResultadoUi {
    data class Registrado(
        val nombre: String,
        val seccion: String,
        val hora: String,
        val manual: Boolean
    ) : ResultadoUi

    data class Duplicado(val nombre: String, val hora: String) : ResultadoUi
    data class Invalido(val mensaje: String) : ResultadoUi
}

@Composable
fun PorteriaScreen(graph: AppGraph, onSalir: () -> Unit) {
    val contexto = LocalContext.current
    val vista = LocalView.current
    val actividad = remember(contexto) { contexto.findActivity() }
    val alcance = rememberCoroutineScope()

    var permisoCamara by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> permisoCamara = concedido }

    var roster by remember { mutableStateOf<List<RosterEntity>>(emptyList()) }
    var resultado by remember { mutableStateOf<ResultadoUi?>(null) }
    var procesando by remember { mutableStateOf(false) }
    var sincronizando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var ultimaSync by remember { mutableStateOf(graph.prefs.ultimaSyncCola) }
    var mostrarBusqueda by remember { mutableStateOf(false) }
    var mostrarHistorial by remember { mutableStateOf(false) }
    var mostrarSalida by remember { mutableStateOf(false) }
    var diaActual by remember { mutableStateOf(LocalDate.now(zonaLima)) }

    val flujoDia = remember(diaActual) { graph.repository.observarDelDia() }
    val delDia by flujoDia.collectAsState(initial = emptyList())
    val pendientes by graph.repository.observarPendientes().collectAsState(initial = 0)

    val tono = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 90) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose { tono?.release() }
    }

    fun sonido(exito: Boolean) {
        try {
            tono?.startTone(
                if (exito) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_NACK,
                170
            )
        } catch (_: Exception) {
            // Algunos emuladores no tienen audio; no debe impedir el registro.
        }
    }

    fun vibrar(exito: Boolean) {
        val vibrador = ContextCompat.getSystemService(contexto, Vibrator::class.java) ?: return
        vibrador.vibrate(
            VibrationEffect.createOneShot(
                if (exito) 90L else 220L,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }

    suspend fun sincronizarTodo(mostrarAviso: Boolean) {
        if (sincronizando) return
        sincronizando = true
        try {
            graph.repository.sincronizarRoster()
            roster = graph.repository.roster()
            val resumen = graph.repository.sincronizarCola()
            ultimaSync = graph.prefs.ultimaSyncCola
            if (mostrarAviso) {
                aviso = when {
                    resumen.total == 0 -> "Todo sincronizado. No había ingresos pendientes."
                    resumen.errores > 0 -> "Se enviaron ${resumen.total - resumen.errores} de ${resumen.total}. Revisa los errores."
                    else -> "Sincronización completa: ${resumen.total} ingreso(s)."
                }
            }
        } catch (ex: SinSesionException) {
            aviso = "La sesión expiró. Vuelve a iniciar sesión."
            onSalir()
        } catch (ex: Exception) {
            if (mostrarAviso) {
                aviso = "Sin conexión o sesión vencida. Los ingresos quedan en cola."
            }
        } finally {
            sincronizando = false
        }
    }

    fun procesarQr(contenido: String) {
        if (procesando) return
        procesando = true
        alcance.launch {
            try {
                val estudiante = graph.repository.estudiantePorQr(contenido)
                if (estudiante == null) {
                    sonido(false)
                    vibrar(false)
                    resultado = ResultadoUi.Invalido("Credencial no reconocida")
                } else {
                    when (val registro = graph.repository.registrarEscaneo(estudiante)) {
                        is ResultadoRegistro.Registrado -> {
                            sonido(true)
                            vibrar(true)
                            resultado = ResultadoUi.Registrado(
                                estudiante.nombreCompleto,
                                estudiante.seccion,
                                hora(registro.item.fechaHoraUtc),
                                manual = false
                            )
                        }
                        is ResultadoRegistro.Duplicado -> {
                            sonido(false)
                            vibrar(false)
                            resultado = ResultadoUi.Duplicado(
                                estudiante.nombreCompleto,
                                hora(registro.existente.fechaHoraUtc)
                            )
                        }
                    }
                }
            } catch (ex: Exception) {
                resultado = ResultadoUi.Invalido("No se pudo registrar. Intenta nuevamente.")
            } finally {
                procesando = false
            }
        }
    }

    fun alRegistrarManual(registro: ResultadoRegistro, estudiante: RosterEntity) {
        when (registro) {
            is ResultadoRegistro.Registrado -> {
                sonido(true)
                vibrar(true)
                resultado = ResultadoUi.Registrado(
                    estudiante.nombreCompleto,
                    estudiante.seccion,
                    hora(registro.item.fechaHoraUtc),
                    manual = true
                )
            }
            is ResultadoRegistro.Duplicado -> {
                sonido(false)
                vibrar(false)
                resultado = ResultadoUi.Duplicado(
                    estudiante.nombreCompleto,
                    hora(registro.existente.fechaHoraUtc)
                )
            }
        }
    }

    LaunchedEffect(resultado) {
        if (resultado != null) {
            delay(4500)
            resultado = null
        }
    }

    LaunchedEffect(Unit) {
        if (!permisoCamara) lanzadorPermiso.launch(Manifest.permission.CAMERA)
        roster = graph.repository.roster()
        sincronizarTodo(mostrarAviso = false)
    }

    LaunchedEffect(Unit) {
        // Un kiosco puede quedar encendido toda la noche: al cambiar el día se
        // vuelve a suscribir el historial y la cola local al nuevo rango.
        while (true) {
            delay(60_000)
            diaActual = LocalDate.now(zonaLima)
        }
    }

    // Modo kiosco: pantalla encendida, sin barras del sistema y sin botón atrás.
    DisposableEffect(actividad) {
        val ventana = actividad?.window
        if (ventana == null) {
            onDispose { }
        } else {
            ventana.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val controlador = WindowCompat.getInsetsController(ventana, vista)
            controlador.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controlador.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                ventana.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                controlador.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = true) {
        // El botón atrás está bloqueado mientras la tablet está en modo kiosco.
    }

    Column(modifier = Modifier.fillMaxSize().background(Fondo)) {
        BarraSuperior(
            colegio = graph.prefs.colegioNombre.ifBlank { "KidCheck" },
            usuario = graph.prefs.usuarioNombre,
            pendientes = pendientes,
            sincronizando = sincronizando,
            ultimaSync = ultimaSync,
            alSincronizar = {
                aviso = null
                alcance.launch { sincronizarTodo(mostrarAviso = true) }
            },
            alSalir = { mostrarSalida = true }
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(modifier = Modifier.weight(1.45f).fillMaxHeight()) {
                if (permisoCamara) {
                    CameraScanner(
                        activo = resultado == null && !procesando &&
                            !mostrarBusqueda && !mostrarHistorial && !mostrarSalida,
                        modifier = Modifier.fillMaxSize(),
                        onQrDetectado = { procesarQr(it) }
                    )
                } else {
                    SinCamara(
                        alReintentar = { lanzadorPermiso.launch(Manifest.permission.CAMERA) },
                        alBuscar = { mostrarBusqueda = true }
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TarjetaResultado(resultado)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Estadistica(
                        titulo = "Ingresos hoy",
                        valor = delDia.size.toString(),
                        color = VerdeOscuro,
                        modifier = Modifier.weight(1f)
                    )
                    Estadistica(
                        titulo = "Por enviar",
                        valor = pendientes.toString(),
                        color = if (pendientes > 0) Ambar else VerdeOscuro,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = { mostrarBusqueda = true },
                    modifier = Modifier.fillMaxWidth().height(62.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Verde)
                ) {
                    Text("Buscar alumno", style = MaterialTheme.typography.titleMedium)
                }

                OutlinedButton(
                    onClick = { mostrarHistorial = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) {
                    Text("Historial del día", style = MaterialTheme.typography.titleMedium)
                }

                if (aviso != null) {
                    Text(
                        aviso ?: "",
                        color = TextoSuave,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    "Modo kiosco activo · Pantalla fija · Salida con PIN",
                    color = TextoSuave,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Si se corta el internet, los ingresos se guardan y se envían solos al recuperar la conexión.",
                    color = TextoSuave,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (mostrarBusqueda) {
        BusquedaManualDialog(
            graph = graph,
            onCerrar = { mostrarBusqueda = false },
            onResultado = { registro, estudiante ->
                mostrarBusqueda = false
                alRegistrarManual(registro, estudiante)
            }
        )
    }

    if (mostrarHistorial) {
        HistorialDialog(
            items = delDia,
            onCerrar = { mostrarHistorial = false },
            onSincronizar = {
                aviso = null
                alcance.launch { sincronizarTodo(mostrarAviso = true) }
            }
        )
    }

    if (mostrarSalida) {
        SalidaDialog(
            pinValido = { graph.prefs.pinValido(it) },
            onCancelar = { mostrarSalida = false },
            onSalir = {
                mostrarSalida = false
                onSalir()
            }
        )
    }
}

@Composable
private fun BarraSuperior(
    colegio: String,
    usuario: String,
    pendientes: Int,
    sincronizando: Boolean,
    ultimaSync: Long,
    alSincronizar: () -> Unit,
    alSalir: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Verde, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("KC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Portería", style = MaterialTheme.typography.titleLarge)
                Text(colegio, color = TextoSuave, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (sincronizando) "Sincronizando…" else "Pendientes: $pendientes",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (pendientes > 0) Ambar else VerdeOscuro
                )
                Text(
                    "Última sincronización: ${horaSync(ultimaSync)} · ${usuario}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSuave
                )
            }
            Spacer(Modifier.width(14.dp))
            OutlinedButton(
                onClick = alSincronizar,
                enabled = !sincronizando,
                modifier = Modifier.heightIn(min = 52.dp)
            ) {
                if (sincronizando) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("Sincronizar")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = alSalir,
                modifier = Modifier.heightIn(min = 52.dp)
            ) {
                Text("Salir")
            }
        }
    }
}

@Composable
private fun SinCamara(alReintentar: () -> Unit, alBuscar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101613), RoundedCornerShape(18.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Cámara no disponible",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Concede el permiso de cámara para escanear credenciales QR.",
                color = Color(0xFFD7E0D9),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = alReintentar) { Text("Dar permiso") }
                OutlinedButton(onClick = alBuscar) { Text("Buscar manualmente") }
            }
        }
    }
}

@Composable
private fun TarjetaResultado(resultado: ResultadoUi?) {
    val fondo: Color
    val borde: Color
    val titulo: String
    val detalle: String
    val tercero: String

    when (resultado) {
        is ResultadoUi.Registrado -> {
            fondo = VerdeClaro
            borde = Verde
            titulo = if (resultado.manual) "Ingreso manual registrado" else "Ingreso registrado"
            detalle = resultado.nombre
            tercero = "${resultado.seccion} · ${resultado.hora}"
        }
        is ResultadoUi.Duplicado -> {
            fondo = AmbarClaro
            borde = Ambar
            titulo = "Ya registrado hoy"
            detalle = resultado.nombre
            tercero = "Ingreso anterior: ${resultado.hora}"
        }
        is ResultadoUi.Invalido -> {
            fondo = RojoClaro
            borde = Rojo
            titulo = "No se registró"
            detalle = resultado.mensaje
            tercero = "Verifica la credencial o usa la búsqueda manual."
        }
        null -> {
            fondo = Color.White
            borde = Borde
            titulo = "Listo para escanear"
            detalle = "Acerca la credencial QR a la cámara."
            tercero = "El registro funciona incluso sin internet."
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 168.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
        border = BorderStroke(if (resultado == null) 1.dp else 2.dp, borde)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleLarge,
                color = if (resultado is ResultadoUi.Invalido) Rojo else Texto
            )
            Spacer(Modifier.height(8.dp))
            Text(
                detalle,
                style = MaterialTheme.typography.headlineMedium,
                color = Texto
            )
            Spacer(Modifier.height(6.dp))
            Text(tercero, style = MaterialTheme.typography.bodyLarge, color = TextoSuave)
        }
    }
}

@Composable
private fun Estadistica(titulo: String, valor: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, color = TextoSuave, style = MaterialTheme.typography.bodyMedium)
            Text(valor, color = color, style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
private fun BusquedaManualDialog(
    graph: AppGraph,
    onCerrar: () -> Unit,
    onResultado: (ResultadoRegistro, RosterEntity) -> Unit
) {
    val alcance = rememberCoroutineScope()
    var texto by remember { mutableStateOf("") }
    var estudiantes by remember { mutableStateOf<List<RosterEntity>>(emptyList()) }
    var seleccionado by remember { mutableStateOf<RosterEntity?>(null) }
    var motivo by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(texto) {
        delay(180)
        estudiantes = graph.repository.buscarRoster(texto)
    }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Registro manual", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Busca al estudiante y registra un motivo obligatorio.",
                            color = TextoSuave,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    TextButton(onClick = onCerrar) { Text("Cerrar") }
                }
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(modifier = Modifier.weight(1.15f).fillMaxHeight()) {
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { texto = it },
                            label = { Text("Nombre, apellido o código") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(estudiantes, key = { it.idEstudiante }) { estudiante ->
                                val activo = seleccionado?.idEstudiante == estudiante.idEstudiante
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (activo) VerdeClaro else Color.Transparent)
                                        .clickable {
                                            seleccionado = estudiante
                                            motivo = null
                                            error = null
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            estudiante.nombreCompleto,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "${estudiante.seccion} · ${estudiante.codigoMatricula}",
                                            color = TextoSuave,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    if (activo) {
                                        Text("Elegido", color = VerdeOscuro)
                                    }
                                }
                                HorizontalDivider(color = Borde)
                            }
                            if (estudiantes.isEmpty()) {
                                item {
                                    Text(
                                        "No hay estudiantes en el roster local. Conéctate y sincroniza.",
                                        color = TextoSuave,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(0.85f).fillMaxHeight()) {
                        val estudiante = seleccionado
                        if (estudiante == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Fondo, RoundedCornerShape(14.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Selecciona un estudiante de la lista para continuar.",
                                    color = TextoSuave,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            Text(
                                "Motivo del registro manual",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                estudiante.nombreCompleto,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextoSuave
                            )
                            Spacer(Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                MOTIVOS.forEach { (valor, etiqueta) ->
                                    FilterChip(
                                        selected = motivo == valor,
                                        onClick = { motivo = valor },
                                        label = { Text(etiqueta) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            if (error != null) {
                                Text(error ?: "", color = Rojo, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = {
                                    val elegido = motivo ?: return@Button
                                    guardando = true
                                    error = null
                                    alcance.launch {
                                        try {
                                            val registro = graph.repository.registrarManual(estudiante, elegido)
                                            onResultado(registro, estudiante)
                                        } catch (ex: Exception) {
                                            error = "No se pudo registrar. Intenta nuevamente."
                                        } finally {
                                            guardando = false
                                        }
                                    }
                                },
                                enabled = motivo != null && !guardando,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text(if (guardando) "Registrando…" else "Confirmar ingreso")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorialDialog(
    items: List<ColaEntity>,
    onCerrar: () -> Unit,
    onSincronizar: () -> Unit
) {
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Historial del día", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${items.size} ingreso(s) registrados en esta tablet.",
                            color = TextoSuave,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedButton(onClick = onSincronizar) { Text("Sincronizar") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onCerrar) { Text("Cerrar") }
                }
                Spacer(Modifier.height(14.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items, key = { it.clientId }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Fondo, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                hora(item.fechaHoraUtc),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.width(92.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.nombre, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    buildString {
                                        append(item.seccion)
                                        if (item.canal == PorteriaRepository.CANAL_MANUAL) {
                                            append(" · manual")
                                            if (!item.motivo.isNullOrBlank()) {
                                                append(" · ${etiquetaMotivo(item.motivo)}")
                                            }
                                        }
                                        if (!item.error.isNullOrBlank()) append(" · ${item.error}")
                                    },
                                    color = TextoSuave,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            val (fondoEstado, colorEstado) = coloresEstado(item.estado)
                            Box(
                                modifier = Modifier
                                    .background(fondoEstado, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    item.estado,
                                    color = colorEstado,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    if (items.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Todavía no hay ingresos registrados hoy.",
                                    color = TextoSuave,
                                    style = MaterialTheme.typography.bodyLarge
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
private fun SalidaDialog(
    pinValido: (String) -> Boolean,
    onCancelar: () -> Unit,
    onSalir: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Salir del modo kiosco") },
        text = {
            Column {
                Text("Ingresa el PIN de la tablet para cerrar la sesión.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit).take(8)
                        error = false
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error
                )
                if (error) {
                    Spacer(Modifier.height(6.dp))
                    Text("PIN incorrecto.", color = Rojo)
                }
                Spacer(Modifier.height(8.dp))
                Text("PIN de demo: 1234", color = TextoSuave, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pinValido(pin)) onSalir() else error = true
                },
                enabled = pin.length >= 4
            ) {
                Text("Salir")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

private fun hora(epochMillis: Long): String =
    formatoHora.format(Instant.ofEpochMilli(epochMillis).atZone(zonaLima))

private fun horaSync(epochMillis: Long): String {
    if (epochMillis <= 0L) return "nunca"
    val fecha = Instant.ofEpochMilli(epochMillis).atZone(zonaLima)
    return "${fecha.toLocalDate()} ${formatoHoraCorta.format(fecha)}"
}

private fun coloresEstado(estado: String): Pair<Color, Color> = when (estado) {
    PorteriaRepository.ESTADO_ENVIADO -> VerdeClaro to VerdeOscuro
    PorteriaRepository.ESTADO_ERROR -> RojoClaro to Rojo
    else -> AmbarClaro to Ambar
}

private fun etiquetaMotivo(valor: String): String =
    MOTIVOS.firstOrNull { (codigo, _) -> codigo == valor }?.second ?: valor

private fun Context.findActivity(): Activity? {
    var actual: Context = this
    while (actual is ContextWrapper) {
        if (actual is Activity) return actual
        actual = actual.baseContext
    }
    return null
}
