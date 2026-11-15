package com.example.rutalogcourier.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Coordenadas GPS del momento en que el courier registra algo (RN-06). */
data class Ubicacion(val latitud: Double, val longitud: Double)

/** Espera el resultado de una tarea de Google Play services; null si falla o se cancela. */
private suspend fun <T> Task<T>.esperar(): T? = suspendCancellableCoroutine { continuacion ->
    addOnSuccessListener { continuacion.resume(it) }
    addOnFailureListener { continuacion.resume(null) }
    addOnCanceledListener { continuacion.resume(null) }
}

/** GPS del teléfono. Si no hay permiso o tarda demasiado, el registro se hace igual, sin ubicación. */
class ProveedorUbicacion(private val context: Context) {

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    suspend fun actual(): Ubicacion? {
        if (!tienePermiso()) return null
        val cliente = LocationServices.getFusedLocationProviderClient(context)
        val cancelacion = CancellationTokenSource()
        val ubicacion = withTimeoutOrNull(8_000) {
            cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelacion.token).esperar()
        } ?: run {
            cancelacion.cancel()
            cliente.lastLocation.esperar()
        }
        return ubicacion?.let { Ubicacion(it.latitude, it.longitude) }
    }
}

/**
 * RF-C03: escanea la guía con el escáner de Google Play services (trae su propia pantalla de cámara,
 * así la app no pide permiso de cámara). Devuelve el texto leído o null si el courier cancela.
 */
object EscanerGuia {

    private val opciones = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39)
        .enableAutoZoom()
        .build()

    suspend fun escanear(context: Context): String? =
        GmsBarcodeScanning.getClient(context, opciones).startScan().esperar()?.rawValue

    /** Descarga el módulo del escáner con señal, para que luego funcione aunque no la haya. */
    fun prepararModulo(context: Context) {
        val pedido = ModuleInstallRequest.newBuilder()
            .addApi(GmsBarcodeScanning.getClient(context))
            .build()
        ModuleInstall.getClient(context).installModules(pedido)
    }
}

/** Fotos y firma comprimidas antes de guardarlas en la cola (RNF-07: se suben aunque la señal sea débil). */
object Imagenes {

    private const val LADO_MAXIMO = 1280
    private const val MAXIMO_BYTES = 350 * 1024

    /** Foto tomada con la cámara → JPEG de hasta 1280 px y ~350 KB. */
    fun comprimirFoto(context: Context, uri: Uri): ByteArray? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        var escala = 1
        while (maxOf(limites.outWidth, limites.outHeight) / (escala * 2) >= LADO_MAXIMO) escala *= 2
        val original = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = escala })
        } ?: return null

        val factor = LADO_MAXIMO.toFloat() / maxOf(original.width, original.height)
        val foto = if (factor < 1f) {
            Bitmap.createScaledBitmap(original, (original.width * factor).toInt(), (original.height * factor).toInt(), true)
        } else {
            original
        }
        var calidad = 80
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().use { salida ->
                foto.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
                salida.toByteArray()
            }
            calidad -= 10
        } while (bytes.size > MAXIMO_BYTES && calidad >= 30)
        return bytes
    }

    fun firmaPng(firma: Bitmap): ByteArray = ByteArrayOutputStream().use { salida ->
        firma.compress(Bitmap.CompressFormat.PNG, 100, salida)
        salida.toByteArray()
    }
}

/** Aviso de nuevas paradas asignadas por el operador. */
object Notificaciones {

    private const val CANAL = "paradas"

    fun crearCanal(context: Context) {
        val canal = NotificationChannel(CANAL, "Nuevas paradas", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Avisa cuando el operador te asigna recojos o entregas."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun avisarNuevasParadas(context: Context, cantidad: Int) {
        if (cantidad <= 0) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val aviso = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Nueva parada en tu ruta")
            .setContentText(if (cantidad == 1) "Tienes 1 parada nueva." else "Tienes $cantidad paradas nuevas.")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1, aviso)
    }
}
