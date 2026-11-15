package com.example.rutalogcourier.worker

import android.content.Context
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
import com.example.rutalogcourier.RutaLogCourierApplication
import com.example.rutalogcourier.util.Notificaciones
import java.util.concurrent.TimeUnit

/**
 * RF-C08: sincroniza en segundo plano con WorkManager. Solo corre cuando hay señal, así lo registrado
 * sin cobertura se envía solo al recuperarla, aunque la app esté cerrada. Si llegan paradas nuevas, avisa.
 */
class SyncWorker(context: Context, parametros: WorkerParameters) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val contenedor = (applicationContext as RutaLogCourierApplication).contenedor
        if (contenedor.sesion.token == null) return Result.success()

        val resultado = contenedor.syncRepository.sincronizar()
        Notificaciones.avisarNuevasParadas(applicationContext, resultado.nuevas)
        return if (resultado.error != null && runAttemptCount < MAX_INTENTOS) Result.retry() else Result.success()
    }

    companion object {
        private const val NOMBRE = "sincronizacion_rutalog_courier"
        private const val NOMBRE_PERIODICA = "revision_rutalog_courier"
        private const val MAX_INTENTOS = 5

        private val conRed = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Programa una sincronización para cuando haya señal. */
        fun programar(context: Context) {
            val trabajo = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(conRed)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.REPLACE, trabajo)
        }

        /** Revisa cada 15 minutos (el mínimo de Android) si el operador asignó paradas nuevas. */
        fun programarPeriodica(context: Context) {
            val trabajo = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(conRed)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NOMBRE_PERIODICA, ExistingPeriodicWorkPolicy.KEEP, trabajo)
        }
    }
}
