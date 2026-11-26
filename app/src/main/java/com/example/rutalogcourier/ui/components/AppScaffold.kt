@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.rutalogcourier.ui.components

import androidx.compose.foundation.layout.Row
import com.example.rutalogcourier.ui.theme.BotonTema
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rutalogcourier.ui.theme.AzulRuta

enum class PestanaCourier(val etiqueta: String, val icono: ImageVector) {
    RUTA("Ruta del día", Icons.Default.Map),
    SINCRONIZAR("Sincronizar", Icons.Default.Sync)
}

/**
 * Estructura común de las pantallas: barra superior azul, aviso de "sin conexión" y,
 * si [pestana] no es null, la barra inferior con Ruta del día / Sincronizar.
 */
@Composable
fun AppScaffold(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    pestana: PestanaCourier? = null,
    onPestana: (PestanaCourier) -> Unit = {},
    onVolver: (() -> Unit)? = null,
    onCerrarSesion: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    var confirmarSalida by remember { mutableStateOf(false) }
    val enLinea = LocalEnLinea.current
    val enCola = LocalOperacionesEnCola.current

    Scaffold(
        modifier = modifier,
        snackbarHost = { if (snackbarHostState != null) SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = titulo,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitulo != null) {
                            Text(
                                text = subtitulo,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onVolver != null) {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    } else if (onCerrarSesion != null) {
                        // La cuenta de quien usa la app: sus datos y la opción de cerrar sesión.
                        AvatarUsuario(
                            onCerrarSesion = { confirmarSalida = true },
                            modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                        )
                    } else {
                        AppEmblema(modifier = Modifier.padding(start = 12.dp, end = 4.dp))
                    }
                },
                actions = {
                    if (onCerrarSesion != null) BotonTema()
                    if (pestana != null) BotonMenuLateral()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulRuta,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ConnectivityBanner(enLinea = enLinea)
                Box(modifier = Modifier.weight(1f)) {
                    content(PaddingValues(0.dp))
                }
            }
            // Menú vertical a la derecha (antes era la barra inferior); se oculta con el botón de la barra superior.
            if (pestana != null) {
                MenuLateral(
                    visible = PreferenciaMenu.visible,
                    opciones = PestanaCourier.entries,
                    seleccionada = pestana,
                    etiqueta = { it.etiqueta },
                    icono = { it.icono },
                    insignia = { if (it == PestanaCourier.SINCRONIZAR) enCola else 0 },
                    onSeleccion = { if (it != pestana) onPestana(it) }
                )
            }
        }
    }

    if (confirmarSalida && onCerrarSesion != null) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            title = { Text("¿Cerrar sesión?") },
            text = {
                Text(
                    if (enCola > 0) {
                        "Tienes $enCola cambio(s) sin enviar. Quedan guardados en el teléfono y se enviarán cuando vuelvas a entrar."
                    } else {
                        "Tu ruta del día queda guardada en el teléfono."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarSalida = false
                        onCerrarSesion()
                    }
                ) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") }
            }
        )
    }
}
