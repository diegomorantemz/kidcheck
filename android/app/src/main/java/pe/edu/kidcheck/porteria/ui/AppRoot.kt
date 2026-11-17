package pe.edu.kidcheck.porteria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.edu.kidcheck.porteria.AppGraph
import pe.edu.kidcheck.porteria.EstadoArranque
import pe.edu.kidcheck.porteria.KidCheckApp

@Composable
fun AppRoot() {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as KidCheckApp
    val estado by app.estado.collectAsState()

    when (val actual = estado) {
        EstadoArranque.Cargando -> CargandoDatos()
        is EstadoArranque.Error -> ErrorDatos(actual.mensaje, app::reintentarArranque)
        is EstadoArranque.Listo -> Contenido(actual.graph)
    }
}

@Composable
private fun Contenido(grafo: AppGraph) {
    var autenticado by remember(grafo) { mutableStateOf(grafo.prefs.estaAutenticado) }

    if (autenticado) {
        PorteriaScreen(
            graph = grafo,
            onSalir = {
                grafo.prefs.limpiarSesion()
                autenticado = false
            }
        )
    } else {
        LoginScreen(
            graph = grafo,
            onEntrar = { autenticado = true }
        )
    }
}

@Composable
private fun CargandoDatos() {
    Column(
        modifier = Modifier.fillMaxSize().background(Fondo),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Verde)
        Text(
            "Preparando la portería…",
            style = MaterialTheme.typography.titleMedium,
            color = TextoSuave
        )
    }
}

@Composable
private fun ErrorDatos(mensaje: String, onReintentar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "No se pudo preparar la portería",
            style = MaterialTheme.typography.titleLarge,
            color = Texto
        )
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSuave,
            textAlign = TextAlign.Center
        )
        Button(onClick = onReintentar) {
            Text("Reintentar")
        }
    }
}
