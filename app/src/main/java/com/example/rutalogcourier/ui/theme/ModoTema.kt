package com.example.rutalogcourier.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

enum class ModoTema(val etiqueta: String, val icono: ImageVector) {
    SISTEMA("Según el teléfono", Icons.Default.BrightnessAuto),
    CLARO("Claro", Icons.Default.LightMode),
    OSCURO("Oscuro", Icons.Default.DarkMode)
}

/** Tema que eligió el usuario en la app. Se guarda en el celular; por defecto sigue el modo del teléfono. */
object PreferenciaTema {
    private const val ARCHIVO = "preferencias_ui"
    private const val CLAVE = "modo_tema"

    var modo by mutableStateOf(ModoTema.SISTEMA)
        private set

    fun cargar(context: Context) {
        val guardado = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getString(CLAVE, null)
        modo = ModoTema.entries.firstOrNull { it.name == guardado } ?: ModoTema.SISTEMA
    }

    fun cambiar(context: Context, nuevo: ModoTema) {
        modo = nuevo
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().putString(CLAVE, nuevo.name).apply()
    }

    @Composable
    fun esOscuro(): Boolean = when (modo) {
        ModoTema.SISTEMA -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }
}

/** Botón de la barra superior para elegir el tema: según el teléfono, claro u oscuro. */
@Composable
fun BotonTema() {
    val context = LocalContext.current
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(PreferenciaTema.modo.icono, contentDescription = "Tema: " + PreferenciaTema.modo.etiqueta)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            ModoTema.entries.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.etiqueta) },
                    leadingIcon = { Icon(opcion.icono, contentDescription = null) },
                    trailingIcon = {
                        if (opcion == PreferenciaTema.modo) Icon(Icons.Default.Check, contentDescription = "Elegido")
                    },
                    onClick = {
                        PreferenciaTema.cambiar(context, opcion)
                        abierto = false
                    }
                )
            }
        }
    }
}
