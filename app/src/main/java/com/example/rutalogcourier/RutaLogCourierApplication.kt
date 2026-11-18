package com.example.rutalogcourier

import android.app.Application
import com.example.rutalogcourier.di.AppContainer
import com.example.rutalogcourier.util.EscanerGuia
import com.example.rutalogcourier.util.Notificaciones
import com.example.rutalogcourier.worker.SyncWorker
import org.osmdroid.config.Configuration

/** Clase Application: crea el contenedor de dependencias y prepara mapa, escáner y avisos. */
class RutaLogCourierApplication : Application() {

    lateinit var contenedor: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = AppContainer(this)
        // OpenStreetMap bloquea las apps con un User-Agent genérico (por ejemplo "com.example..."):
        // pide un nombre propio, versión y un contacto.
        val version = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        Configuration.getInstance().userAgentValue =
            "RutaLogCourier/$version (Android; +https://github.com/Vikooo7/AppCourier)"
        Notificaciones.crearCanal(this)
        EscanerGuia.prepararModulo(this)
        SyncWorker.programarPeriodica(this)
    }
}
