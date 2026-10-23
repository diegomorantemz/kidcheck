package pe.edu.kidcheck.porteria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.kidcheck.porteria.AppGraph
import pe.edu.kidcheck.porteria.net.ApiException
import pe.edu.kidcheck.porteria.worker.SyncScheduler
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(graph: AppGraph, onEntrar: () -> Unit) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    var servidor by remember { mutableStateOf(graph.prefs.servidor) }
    var usuario by remember { mutableStateOf("vigilante1") }
    var password by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun entrar() {
        if (guardando || usuario.isBlank() || password.isBlank()) return
        alcance.launch {
            guardando = true
            error = null
            try {
                val url = graph.api.normalizarServidor(servidor)
                graph.prefs.servidor = url
                servidor = url

                val respuesta = graph.api.iniciarSesion(usuario.trim(), password)
                graph.prefs.guardarSesion(respuesta)

                // Primera descarga del roster; si falla, la pantalla de portería reintenta.
                runCatching { graph.repository.sincronizarRoster() }
                SyncScheduler.sincronizarAhora(contexto)
                onEntrar()
            } catch (ex: ApiException) {
                error = ex.message
            } catch (ex: Exception) {
                error = "No se pudo conectar con el servidor. Revisa la URL y la red Wi-Fi."
            } finally {
                guardando = false
            }
        }
    }

    Surface(color = Fondo, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Verde, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "KC",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "KidCheck Portería",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                "Ingreso escolar con credencial QR",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoSuave
                            )
                        }
                    }

                    Text(
                        "Inicia sesión en la tablet de portería. La sesión queda guardada de forma cifrada.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSuave
                    )

                    OutlinedTextField(
                        value = servidor,
                        onValueChange = { servidor = it },
                        label = { Text("Servidor de la API") },
                        supportingText = {
                            Text("Emulador: http://10.0.2.2:8080 · Tablet: http://IP-DEL-PC:8080")
                        },
                        singleLine = true,
                        enabled = !guardando,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = usuario,
                        onValueChange = { usuario = it },
                        label = { Text("Usuario") },
                        singleLine = true,
                        enabled = !guardando,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        enabled = !guardando,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { entrar() })
                    )

                    if (error != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RojoClaro, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                error ?: "",
                                color = Rojo,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Button(
                        onClick = { entrar() },
                        enabled = !guardando && usuario.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Verde)
                    ) {
                        if (guardando) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Conectando…")
                        } else {
                            Text("Entrar a portería", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Text(
                        "Demo académica: vigilante1 · KidCheck2026! · Datos ficticios.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSuave
                    )
                }
            }
        }
    }
}
