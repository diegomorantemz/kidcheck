package pe.edu.kidcheck.porteria

import android.app.Application
import android.util.Log
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

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("sqlcipher")
        } catch (ex: Throwable) {
            Log.e(TAG, "No se pudo cargar SQLCipher", ex)
        }
        graph = AppGraph(this)
        SyncScheduler.programar(this)
    }

    private companion object {
        const val TAG = "KidCheckApp"
    }
}
