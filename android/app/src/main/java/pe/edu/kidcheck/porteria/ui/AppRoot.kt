package pe.edu.kidcheck.porteria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import pe.edu.kidcheck.porteria.KidCheckApp

@Composable
fun AppRoot() {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as KidCheckApp
    val graph by app.graph.collectAsState()

    val grafo = graph ?: run {
        CargandoDatos()
        return
    }

    var autenticado by remember { mutableStateOf(grafo.prefs.estaAutenticado) }

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
