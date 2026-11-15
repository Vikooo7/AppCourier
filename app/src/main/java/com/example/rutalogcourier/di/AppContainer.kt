package com.example.rutalogcourier.di

import android.content.Context
import com.example.rutalogcourier.data.local.AppDatabase
import com.example.rutalogcourier.data.local.SesionSegura
import com.example.rutalogcourier.data.remote.RetrofitClient
import com.example.rutalogcourier.data.remote.api.ApiService
import com.example.rutalogcourier.data.repository.AuthRepository
import com.example.rutalogcourier.data.repository.RutaRepository
import com.example.rutalogcourier.data.repository.SyncRepository
import com.example.rutalogcourier.util.ConnectivityObserver
import com.example.rutalogcourier.util.ProveedorUbicacion
import com.example.rutalogcourier.worker.SyncWorker
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Crea una sola vez las piezas que comparten los ViewModel y el SyncWorker:
 * la base de datos Room, el cliente de la API, el GPS y los repositorios.
 */
class AppContainer(context: Context) {

    private val contextoApp = context.applicationContext

    val db: AppDatabase = AppDatabase.getDB(contextoApp)
    val sesion = SesionSegura(contextoApp)
    val conectividad = ConnectivityObserver(contextoApp)
    val ubicacion = ProveedorUbicacion(contextoApp)

    /** Prueba de la exposición: mientras esté en true, la API responde con error 503. */
    val simularErrorServidor = AtomicBoolean(false)

    val api: ApiService = RetrofitClient.crear(
        token = { sesion.token },
        simularError = { simularErrorServidor.get() }
    )

    val authRepository = AuthRepository(api, db, sesion)
    val rutaRepository = RutaRepository(db, contextoApp)
    val syncRepository = SyncRepository(db, api, contextoApp)

    /** RF-C08: deja programado el envío de la cola para cuando haya señal, aunque la app se cierre. */
    fun programarSincronizacion() {
        SyncWorker.programar(contextoApp)
    }
}
