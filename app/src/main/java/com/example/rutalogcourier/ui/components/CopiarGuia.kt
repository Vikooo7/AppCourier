package com.example.rutalogcourier.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Copia el número de guía al portapapeles, para pegarlo en un mensaje o en otra app. */
@Composable
fun BotonCopiarGuia(guia: String, tinte: Color = Color.White) {
    val context = LocalContext.current
    IconButton(onClick = {
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("Número de guía", guia))
        Toast.makeText(context, "Guía " + guia + " copiada", Toast.LENGTH_SHORT).show()
    }) {
        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar número de guía", tint = tinte.copy(alpha = 0.85f))
    }
}
