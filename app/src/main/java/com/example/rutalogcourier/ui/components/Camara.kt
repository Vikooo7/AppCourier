package com.example.rutalogcourier.ui.components

import android.content.ActivityNotFoundException
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.rutalogcourier.util.Imagenes
import java.io.File

/**
 * Abre la app de cámara del teléfono y devuelve la foto ya comprimida (JPEG de ~350 KB como máximo).
 * La foto se toma a resolución completa en un archivo temporal y luego se reduce.
 */
@Composable
fun rememberCamara(onFoto: (ByteArray?) -> Unit, onError: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val archivo = remember {
        File(context.cacheDir, "fotos").apply { mkdirs() }.let { File(it, "captura.jpg") }
    }
    val uri = remember { FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo) }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tomada ->
        onFoto(if (tomada) Imagenes.comprimirFoto(context, uri) else null)
    }
    return {
        try {
            lanzador.launch(uri)
        } catch (e: ActivityNotFoundException) {
            onError("Este teléfono no tiene una app de cámara disponible.")
        }
    }
}

/** Vista previa de la foto tomada. */
@Composable
fun VistaFoto(foto: ByteArray, modifier: Modifier = Modifier) {
    val imagen = remember(foto) { BitmapFactory.decodeByteArray(foto, 0, foto.size)?.asImageBitmap() }
    if (imagen != null) {
        Image(
            bitmap = imagen,
            contentDescription = "Foto tomada",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(14.dp))
        )
    }
}
