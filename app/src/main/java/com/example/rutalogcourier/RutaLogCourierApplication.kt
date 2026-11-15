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
        // OpenStreetMap pide identificar la app que descarga los mapas.
        Configuration.getInstance().userAgentValue = packageName
        Notificaciones.crearCanal(this)
        EscanerGuia.prepararModulo(this)
        SyncWorker.programarPeriodica(this)
    }
}
