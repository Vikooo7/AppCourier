package com.example.rutalogcourier.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Si el menú lateral está a la vista. Se guarda en el celular para que la app recuerde la elección. */
object PreferenciaMenu {
    private const val ARCHIVO = "preferencias_ui"
    private const val CLAVE = "menu_lateral_visible"

    var visible by mutableStateOf(true)
        private set

    fun cargar(context: Context) {
        visible = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getBoolean(CLAVE, true)
    }

    fun cambiar(context: Context, nuevo: Boolean) {
        visible = nuevo
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().putBoolean(CLAVE, nuevo).apply()
    }
}

/** Botón de la barra superior que muestra u oculta el menú lateral. */
@Composable
fun BotonMenuLateral() {
    val context = LocalContext.current
    IconButton(onClick = { PreferenciaMenu.cambiar(context, !PreferenciaMenu.visible) }) {
        Icon(
            if (PreferenciaMenu.visible) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
            contentDescription = if (PreferenciaMenu.visible) "Ocultar menú" else "Mostrar menú"
        )
    }
}

/**
 * Menú vertical a la derecha de la pantalla (reemplaza la barra inferior). Las opciones quedan centradas
 * en altura, al alcance del pulgar derecho, y el menú se puede ocultar para ganar espacio.
 */
@Composable
fun <T> MenuLateral(
    visible: Boolean,
    opciones: List<T>,
    seleccionada: T,
    etiqueta: (T) -> String,
    icono: (T) -> ImageVector,
    insignia: (T) -> Int,
    onSeleccion: (T) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally() + fadeIn(),
        exit = shrinkHorizontally() + fadeOut()
    ) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            // Los márgenes del sistema ya los aplica la pantalla.
            windowInsets = WindowInsets(0),
            modifier = Modifier.fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            opciones.forEach { opcion ->
                NavigationRailItem(
                    selected = opcion == seleccionada,
                    onClick = { onSeleccion(opcion) },
                    icon = {
                        val cantidad = insignia(opcion)
                        if (cantidad > 0) {
                            BadgedBox(badge = { Badge { Text(cantidad.toString()) } }) {
                                Icon(icono(opcion), contentDescription = etiqueta(opcion))
                            }
                        } else {
                            Icon(icono(opcion), contentDescription = etiqueta(opcion))
                        }
                    },
                    label = {
                        Text(
                            etiqueta(opcion),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
