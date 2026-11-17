package pe.edu.kidcheck.porteria

import android.app.Application
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

sealed interface EstadoArranque {
    data object Cargando : EstadoArranque
    data class Listo(val graph: AppGraph) : EstadoArranque
    data class Error(val mensaje: String) : EstadoArranque
}

class KidCheckApp : Application() {

    /**
     * El grafo se construye fuera del hilo principal: abrir el Keystore
     * (EncryptedSharedPreferences) y la base SQLCipher puede tardar varios
     * segundos en un emulador y provocaba el diálogo "La app no responde".
     */
    private val _estado = MutableStateFlow<EstadoArranque>(EstadoArranque.Cargando)
    val estado: StateFlow<EstadoArranque> = _estado.asStateFlow()

    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var trabajo: Job? = null

    override fun onCreate() {
        super.onCreate()
        iniciarDatos()
    }

    /**
     * Vuelve a intentar el arranque si Keystore, SQLCipher o la base fallaron.
     * No lanza un segundo arranque mientras el actual siga activo.
     */
    @Synchronized
    fun reintentarArranque() {
        if (trabajo?.isActive == true) return
        _estado.value = EstadoArranque.Cargando
        iniciarDatos()
    }

    private fun iniciarDatos() {
        trabajo = initScope.launch {
            val inicio = SystemClock.elapsedRealtime()
            try {
                try {
                    System.loadLibrary("sqlcipher")
                } catch (ex: Throwable) {
                    Log.e(TAG, "No se pudo cargar SQLCipher", ex)
                }

                val grafo = AppGraph(this@KidCheckApp)
                _estado.value = EstadoArranque.Listo(grafo)
                Log.d(TAG, "Arranque de datos en ${SystemClock.elapsedRealtime() - inicio} ms")

                // Un fallo al programar la sincronización no invalida los datos
                // ya cargados: se registra y la app sigue disponible.
                try {
                    SyncScheduler.programar(this@KidCheckApp)
                } catch (ex: Throwable) {
                    Log.e(TAG, "No se pudo programar la sincronización", ex)
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Throwable) {
                Log.e(TAG, "No se pudo inicializar la app", ex)
                _estado.value = EstadoArranque.Error(
                    "No se pudieron preparar los datos de la portería. " +
                        "Verifica el almacenamiento e inténtalo de nuevo."
                )
            }
        }
    }

    private companion object {
        const val TAG = "KidCheckApp"
    }
}
