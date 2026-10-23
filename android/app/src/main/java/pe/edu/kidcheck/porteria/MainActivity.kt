package pe.edu.kidcheck.porteria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import pe.edu.kidcheck.porteria.ui.AppRoot
import pe.edu.kidcheck.porteria.ui.KidCheckTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KidCheckTheme {
                AppRoot()
            }
        }
    }
}
