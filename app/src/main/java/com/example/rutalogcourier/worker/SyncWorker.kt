package com.example.rutalogcourier.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.OutOfQuotaPolicy
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

    /**
     * En Android 10 y 11 un trabajo urgente corre como servicio en primer plano y necesita esta notificación
     * (discreta, sin sonido). En Android 12 o más no se muestra.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val gestor = applicationContext.getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannel(
            NotificationChannel(CANAL_ENVIO, "Envío de registros", NotificationManager.IMPORTANCE_MIN)
        )
        val aviso = NotificationCompat.Builder(applicationContext, CANAL_ENVIO)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Enviando tus registros a RutaLog")
            .setSilent(true)
            .build()
        return ForegroundInfo(ID_AVISO_ENVIO, aviso)
    }

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
        private const val CANAL_ENVIO = "envio_registros"
        private const val ID_AVISO_ENVIO = 4401

        private val conRed = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /**
         * Programa una sincronización para cuando haya señal. Es urgente (expedited): si hay señal, el recojo,
         * la entrega o la fallida salen en segundos y la API avisa al remitente en tiempo real, en vez de esperar
         * a que Android decida correr el trabajo. Si se acaba la cuota de trabajos urgentes, corre como uno normal.
         */
        fun programar(context: Context) {
            val trabajo = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(conRed)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
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
