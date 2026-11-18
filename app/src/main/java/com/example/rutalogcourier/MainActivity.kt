package com.example.rutalogcourier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rutalogcourier.ui.navigation.AppNavigation
import com.example.rutalogcourier.ui.theme.RutaLogTheme
import com.example.rutalogcourier.viewmodel.AuthViewModel
import com.example.rutalogcourier.viewmodel.RutaViewModel
import com.example.rutalogcourier.viewmodel.SyncViewModel

/**
 * RutaLog Perú · App Courier.
 * Toma las dependencias del contenedor (Room, API, GPS y repositorios) e inicializa los ViewModel.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La barra superior siempre es azul oscuro: íconos del sistema en blanco.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))

        val contenedor = (application as RutaLogCourierApplication).contenedor

        setContent {
            RutaLogTheme {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.factory(contenedor.authRepository)
                )
                val rutaViewModel: RutaViewModel = viewModel(
                    factory = RutaViewModel.factory(
                        repositorio = contenedor.rutaRepository,
                        ubicacion = contenedor.ubicacion,
                        programarSincronizacion = contenedor::programarSincronizacion,
                        verificarEnBanco = contenedor.syncRepository::verificarCobro
                    )
                )
                val syncViewModel: SyncViewModel = viewModel(
                    factory = SyncViewModel.factory(
                        repositorio = contenedor.syncRepository,
                        conectividad = contenedor.conectividad,
                        simularError = contenedor.simularErrorServidor,
                        obtenerUbicacion = contenedor.ubicacion::actual
                    )
                )
                AppNavigation(
                    authViewModel = authViewModel,
                    rutaViewModel = rutaViewModel,
                    syncViewModel = syncViewModel
                )
            }
        }
    }
}
