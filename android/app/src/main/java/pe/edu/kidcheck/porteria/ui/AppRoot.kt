package pe.edu.kidcheck.porteria.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import pe.edu.kidcheck.porteria.KidCheckApp

@Composable
fun AppRoot() {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as KidCheckApp
    val graph = app.graph

    var autenticado by remember { mutableStateOf(graph.prefs.estaAutenticado) }

    if (autenticado) {
        PorteriaScreen(
            graph = graph,
            onSalir = {
                graph.prefs.limpiarSesion()
                autenticado = false
            }
        )
    } else {
        LoginScreen(
            graph = graph,
            onEntrar = { autenticado = true }
        )
    }
}
