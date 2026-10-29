package pe.edu.kidcheck.porteria.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import pe.edu.kidcheck.porteria.KidCheckApp
import pe.edu.kidcheck.porteria.net.SinSesionException
import java.io.IOException
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as KidCheckApp
        // El grafo se inicializa en segundo plano; el worker espera a que esté listo.
        val graph = app.graph.filterNotNull().first()
        if (!graph.prefs.estaAutenticado) return Result.success()

        return try {
            graph.repository.sincronizarRoster()
            graph.repository.sincronizarCola()
            Result.success()
        } catch (ex: SinSesionException) {
            Result.success()
        } catch (ex: IOException) {
            Log.w(TAG, "Sin red para sincronizar", ex)
            if (runAttemptCount < 4) Result.retry() else Result.success()
        } catch (ex: Exception) {
            Log.e(TAG, "Error al sincronizar", ex)
            if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }

    private companion object {
        const val TAG = "KidCheckSync"
    }
}

object SyncScheduler {

    private const val PERIODICO = "kidcheck-sync-periodico"
    private const val AHORA = "kidcheck-sync-ahora"

    private val conexion = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun programar(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(conexion)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODICO,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun sincronizarAhora(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(conexion)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            AHORA,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
