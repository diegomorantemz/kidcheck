package pe.edu.kidcheck.porteria

import android.app.Application
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.kidcheck.porteria.data.AppDatabase
import pe.edu.kidcheck.porteria.data.PorteriaRepository
import pe.edu.kidcheck.porteria.data.SecurePrefs
import pe.edu.kidcheck.porteria.net.KidCheckApi
import pe.edu.kidcheck.porteria.worker.SyncScheduler

class AppGraph(context: Application) {
    val prefs = SecurePrefs(context)
    val db = AppDatabase.build(context, prefs.claveBaseDatos())
    val api = KidCheckApi(prefs)
    val repository = PorteriaRepository(db.dao(), api, prefs)
}

class KidCheckApp : Application() {

    /**
     * El grafo se construye fuera del hilo principal: abrir el Keystore
     * (EncryptedSharedPreferences) y la base SQLCipher puede tardar varios
     * segundos en un emulador y provocaba el diálogo "La app no responde".
     */
    private val _graph = MutableStateFlow<AppGraph?>(null)
    val graph: StateFlow<AppGraph?> = _graph.asStateFlow()

    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initScope.launch {
            val inicio = SystemClock.elapsedRealtime()
            try {
                System.loadLibrary("sqlcipher")
            } catch (ex: Throwable) {
                Log.e(TAG, "No se pudo cargar SQLCipher", ex)
            }

            val grafo = AppGraph(this@KidCheckApp)
            _graph.value = grafo
            Log.d(TAG, "Arranque de datos en ${SystemClock.elapsedRealtime() - inicio} ms")

            // WorkManager también inicializa componentes; fuera del hilo principal.
            SyncScheduler.programar(this@KidCheckApp)
        }
    }

    private companion object {
        const val TAG = "KidCheckApp"
    }
}
