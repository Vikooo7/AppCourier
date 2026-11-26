package com.example.rutalogcourier.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.rutalogcourier.model.Usuario
import com.example.rutalogcourier.ui.theme.RojoRuta

/** Usuario con sesión iniciada, para mostrarlo en la barra superior de cualquier pantalla. */
val LocalUsuarioActual = compositionLocalOf<Usuario?> { null }

/** "Distribuidora Andina SAC" → "DA". */
fun iniciales(nombre: String): String =
    nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** Nombre del rol tal como lo entiende el usuario. */
fun etiquetaRol(rol: String): String = when (rol.lowercase()) {
    "cliente" -> "Remitente"
    "courier" -> "Courier"
    "operador" -> "Operador"
    "administrador" -> "Administrador"
    else -> rol.replaceFirstChar { it.uppercase() }
}

@Composable
private fun CirculoIniciales(nombre: String, tamano: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(RojoRuta),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iniciales(nombre),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = if (tamano > 48.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall
        )
    }
}

/**
 * Ícono de la cuenta en la barra superior: las iniciales de quien usa la app. Al tocarlo muestra
 * su nombre, correo y rol, y la opción de cerrar sesión.
 */
@Composable
fun AvatarUsuario(onCerrarSesion: () -> Unit, modifier: Modifier = Modifier) {
    val usuario = LocalUsuarioActual.current
    val nombre = usuario?.nombre.orEmpty()
    var abierto by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        CirculoIniciales(
            nombre = nombre,
            tamano = 40.dp,
            modifier = Modifier
                .clickable { abierto = true }
                .semantics { contentDescription = "Cuenta de $nombre" }
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            Column(
                modifier = Modifier
                    .widthIn(min = 240.dp, max = 300.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CirculoIniciales(nombre = nombre, tamano = 56.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = nombre.ifEmpty { "Sin sesión" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                usuario?.correo?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                usuario?.rol?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                        Text(
                            text = etiquetaRol(it),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    abierto = false
                    onCerrarSesion()
                }
            )
        }
    }
}
