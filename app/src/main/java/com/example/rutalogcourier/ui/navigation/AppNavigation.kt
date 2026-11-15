package com.example.rutalogcourier.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rutalogcourier.ui.components.LocalEnLinea
import com.example.rutalogcourier.ui.components.LocalOperacionesEnCola
import com.example.rutalogcourier.ui.components.PestanaCourier
import com.example.rutalogcourier.ui.screens.EntregaScreen
import com.example.rutalogcourier.ui.screens.FallidaScreen
import com.example.rutalogcourier.ui.screens.LoginScreen
import com.example.rutalogcourier.ui.screens.ParadaScreen
import com.example.rutalogcourier.ui.screens.RutaScreen
import com.example.rutalogcourier.ui.screens.SplashScreen
import com.example.rutalogcourier.ui.screens.SyncScreen
import com.example.rutalogcourier.viewmodel.AuthViewModel
import com.example.rutalogcourier.viewmodel.RutaViewModel
import com.example.rutalogcourier.viewmodel.SyncViewModel

object Rutas {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val RUTA = "ruta"
    const val SINCRONIZAR = "sincronizar"
    const val PARADA = "parada/{id}"
    const val ENTREGA = "entrega/{id}"
    const val FALLIDA = "fallida/{id}"

    fun parada(id: Int) = "parada/$id"
    fun entrega(id: Int) = "entrega/$id"
    fun fallida(id: Int) = "fallida/$id"
}

/** Splash → Login (o directo a la ruta si ya hay sesión) → Ruta del día / Sincronizar → Parada → Entrega o Fallida. */
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    rutaViewModel: RutaViewModel,
    syncViewModel: SyncViewModel,
    navController: NavHostController = rememberNavController()
) {
    val auth by authViewModel.estado.collectAsState()
    val sync by syncViewModel.estado.collectAsState()
    val nombreUsuario = auth.usuario?.nombre.orEmpty()
    // Mientras se comprueba la sesión guardada no se expulsa al courier al Login.
    val haySesion = auth.usuario != null || auth.comprobandoSesion

    // El SyncViewModel solo sincroniza solo (al volver la señal) si hay sesión.
    syncViewModel.haySesion = { authViewModel.estado.value.usuario != null }

    val irAPestana: (PestanaCourier) -> Unit = { pestana ->
        val destino = when (pestana) {
            PestanaCourier.RUTA -> Rutas.RUTA
            PestanaCourier.SINCRONIZAR -> Rutas.SINCRONIZAR
        }
        navController.navigate(destino) {
            popUpTo(Rutas.RUTA)
            launchSingleTop = true
        }
    }

    val cerrarSesion: () -> Unit = {
        authViewModel.cerrarSesion()
        navController.navigate(Rutas.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    /** Lo registrado ya está en Room y en la cola; si hay señal se envía de inmediato. */
    val trasRegistrar: () -> Unit = { syncViewModel.sincronizar(silencioso = true) }

    val argumentoId = listOf(navArgument("id") { type = NavType.IntType })

    CompositionLocalProvider(
        LocalEnLinea provides sync.enLinea,
        LocalOperacionesEnCola provides sync.operaciones.size
    ) {
        NavHost(
            navController = navController,
            startDestination = Rutas.SPLASH,
            enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 10 } },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(150)) }
        ) {
            composable(Rutas.SPLASH) {
                SplashScreen(
                    onFinish = {
                        // Con sesión guardada se entra directo, aunque no haya señal.
                        val conSesion = authViewModel.estado.value.usuario != null
                        if (conSesion) syncViewModel.sincronizar(silencioso = true)
                        navController.navigate(if (conSesion) Rutas.RUTA else Rutas.LOGIN) {
                            popUpTo(Rutas.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Rutas.LOGIN) {
                LoginScreen(
                    estado = auth,
                    onLogin = { correo, clave ->
                        authViewModel.login(correo, clave) {
                            // RF-C01: tras el login se descarga la ruta del día.
                            syncViewModel.sincronizar(silencioso = true)
                            navController.navigate(Rutas.RUTA) {
                                popUpTo(Rutas.LOGIN) { inclusive = true }
                            }
                        }
                    },
                    onLimpiarMensajes = authViewModel::limpiarMensajes
                )
            }

            composable(Rutas.RUTA) {
                SesionRequerida(hayUsuario = haySesion, onSinSesion = cerrarSesion)
                val paradas by rutaViewModel.paradas.collectAsState()
                val resumen by rutaViewModel.resumen.collectAsState()
                RutaScreen(
                    nombreUsuario = nombreUsuario,
                    paradas = paradas,
                    resumen = resumen,
                    sincronizando = sync.sincronizando,
                    onActualizar = { syncViewModel.sincronizar() },
                    onParada = { navController.navigate(Rutas.parada(it.idRemoto)) },
                    onMover = rutaViewModel::mover,
                    onPestana = irAPestana,
                    onCerrarSesion = cerrarSesion
                )
            }

            composable(Rutas.SINCRONIZAR) {
                SesionRequerida(hayUsuario = haySesion, onSinSesion = cerrarSesion)
                SyncScreen(
                    estado = sync,
                    nombreUsuario = nombreUsuario,
                    onSincronizar = { syncViewModel.sincronizar() },
                    onReintentar = syncViewModel::reintentar,
                    onDescartar = syncViewModel::descartar,
                    onSimularError = syncViewModel::cambiarSimulacionError,
                    onPestana = irAPestana,
                    onCerrarSesion = cerrarSesion
                )
            }

            composable(Rutas.PARADA, arguments = argumentoId) { entrada ->
                SesionRequerida(hayUsuario = haySesion, onSinSesion = cerrarSesion)
                val id = entrada.arguments?.getInt("id") ?: 0
                val flujo = remember(id) { rutaViewModel.observarParada(id) }
                val parada by flujo.collectAsState(initial = null)
                ParadaScreen(
                    parada = parada,
                    onVolver = { navController.popBackStack() },
                    onRecoger = { p, guia, onError -> rutaViewModel.registrarRecojo(p, guia, onError, trasRegistrar) },
                    onAvanzar = { p, onError -> rutaViewModel.avanzar(p, onError, trasRegistrar) },
                    onEntregar = { navController.navigate(Rutas.entrega(it.idRemoto)) },
                    onFallida = { navController.navigate(Rutas.fallida(it.idRemoto)) }
                )
            }

            composable(Rutas.ENTREGA, arguments = argumentoId) { entrada ->
                SesionRequerida(hayUsuario = haySesion, onSinSesion = cerrarSesion)
                val id = entrada.arguments?.getInt("id") ?: 0
                val flujo = remember(id) { rutaViewModel.observarParada(id) }
                val parada by flujo.collectAsState(initial = null)
                EntregaScreen(
                    parada = parada,
                    onVolver = { navController.popBackStack(Rutas.RUTA, inclusive = false) },
                    onGuardar = { p, foto, firma, nombre, dni, onError, onHecho ->
                        rutaViewModel.entregar(p, foto, firma, nombre, dni, onError) {
                            trasRegistrar()
                            onHecho()
                        }
                    }
                )
            }

            composable(Rutas.FALLIDA, arguments = argumentoId) { entrada ->
                SesionRequerida(hayUsuario = haySesion, onSinSesion = cerrarSesion)
                val id = entrada.arguments?.getInt("id") ?: 0
                val flujo = remember(id) { rutaViewModel.observarParada(id) }
                val parada by flujo.collectAsState(initial = null)
                FallidaScreen(
                    parada = parada,
                    onVolver = { navController.popBackStack(Rutas.RUTA, inclusive = false) },
                    onGuardar = { p, motivo, foto, onError, onHecho ->
                        rutaViewModel.registrarFallida(p, motivo, foto, onError) { estado ->
                            trasRegistrar()
                            onHecho(estado)
                        }
                    }
                )
            }
        }
    }
}

/** Si Android cerró la app y se perdió la sesión, vuelve al Login en vez de mostrar datos sin usuario. */
@Composable
private fun SesionRequerida(hayUsuario: Boolean, onSinSesion: () -> Unit) {
    LaunchedEffect(hayUsuario) {
        if (!hayUsuario) onSinSesion()
    }
}
