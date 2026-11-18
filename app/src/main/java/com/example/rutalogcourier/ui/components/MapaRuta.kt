package com.example.rutalogcourier.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rutalogcourier.model.Parada
import com.example.rutalogcourier.model.TipoParada
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private const val COLOR_RECOJO = 0xFFEF6C00.toInt()
private const val COLOR_ENTREGA = 0xFF2E7D32.toInt()
private const val COLOR_RUTA = 0xFF0B2F5B.toInt()
private const val COLOR_INICIO = 0xFF1E88E5.toInt()

/**
 * Mosaicos de OpenStreetMap desde su servidor único (el que recomienda su política de uso).
 * Tiene nombre propio para no reutilizar la caché de osmdroid, que podía guardar la imagen "Access blocked".
 */
private val MAPA_OSM = XYTileSource(
    "RutaLogOSM",
    0,
    19,
    256,
    ".png",
    arrayOf("https://tile.openstreetmap.org/"),
    "© OpenStreetMap contributors"
)

/** Lo que el mapa ya dibujó, para no rehacerlo (ni moverle la vista al courier) en cada recomposición. */
private class EstadoMapa {
    var claveDibujo: Any? = null
    var claveEncuadre: Any? = null
    var puntos: List<GeoPoint> = emptyList()
}

/**
 * RF-C01: la ruta del día en el mapa (OpenStreetMap). Cada parada lleva su número de orden;
 * naranja = recojo, verde = entrega. La línea sale desde donde está el courier.
 *
 * Gestos: un dedo desplaza el mapa (sin mover la lista de la pantalla), dos dedos acercan o alejan,
 * doble toque acerca. El botón de la esquina vuelve a mostrar toda la ruta.
 */
@Composable
fun MapaRuta(
    paradas: List<Parada>,
    inicio: GeoPoint?,
    onParada: (Parada) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val estado = remember { EstadoMapa() }
    val mapa = remember { crearMapa(context) }
    DisposableEffect(mapa) {
        mapa.onResume()
        onDispose {
            mapa.onPause()
            mapa.onDetach()
        }
    }

    val ordenadas = paradas.filter { it.esParada && it.tieneUbicacion }.sortedBy { it.orden }

    Box(modifier = modifier) {
        AndroidView(factory = { mapa }, modifier = Modifier.fillMaxSize()) { vista ->
            // Solo se redibuja si cambian las paradas, su orden o el punto de inicio.
            val claveDibujo = ordenadas.map { listOf(it.idRemoto, it.orden, it.tipo, it.latitud, it.longitud) } +
                listOf(inicio?.latitude, inicio?.longitude)
            if (claveDibujo != estado.claveDibujo) {
                estado.claveDibujo = claveDibujo
                estado.puntos = dibujar(vista, ordenadas, inicio, onParada)
            }
            // Solo se encuadra la ruta cuando llega una ruta distinta; si no, se respeta donde dejó el mapa el courier.
            val claveEncuadre = ordenadas.map { it.idRemoto }.toSet()
            if (claveEncuadre != estado.claveEncuadre && estado.puntos.isNotEmpty()) {
                estado.claveEncuadre = claveEncuadre
                vista.post { encuadrar(vista, estado.puntos, animado = false) }
            }
        }

        if (estado.puntos.isNotEmpty() || ordenadas.isNotEmpty()) {
            SmallFloatingActionButton(
                onClick = { encuadrar(mapa, estado.puntos, animado = true) },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            ) {
                Icon(Icons.Default.ZoomOutMap, contentDescription = "Ver toda la ruta")
            }
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
private fun crearMapa(context: Context): MapView = MapView(context).apply {
    setTileSource(MAPA_OSM)
    setMultiTouchControls(true) // dos dedos: acercar, alejar
    isFlingEnabled = true // al soltar el dedo el mapa sigue deslizándose
    isTilesScaledToDpi = true // calles y nombres legibles en pantallas de alta densidad
    isVerticalMapRepetitionEnabled = false
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER) // sin botones + y −
    minZoomLevel = 5.0
    maxZoomLevel = 19.0
    controller.setZoom(12.0)
    controller.setCenter(GeoPoint(-12.0464, -77.0428))
    // El mapa está dentro de una lista: mientras el dedo está sobre él, la lista no se desplaza.
    setOnTouchListener { vista, evento ->
        when (evento.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE ->
                vista.parent?.requestDisallowInterceptTouchEvent(true)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                vista.parent?.requestDisallowInterceptTouchEvent(false)
        }
        false // el mapa sigue procesando el gesto
    }
}

/** Dibuja la línea, el punto de inicio y las paradas numeradas. Devuelve todos los puntos para encuadrar. */
private fun dibujar(vista: MapView, ordenadas: List<Parada>, inicio: GeoPoint?, onParada: (Parada) -> Unit): List<GeoPoint> {
    vista.overlays.clear()
    val puntos = ordenadas.map { GeoPoint(it.latitud!!, it.longitud!!) }

    Polyline(vista).apply {
        setPoints(listOfNotNull(inicio) + puntos)
        outlinePaint.color = COLOR_RUTA
        outlinePaint.strokeWidth = 7f
        outlinePaint.strokeCap = Paint.Cap.ROUND
        infoWindow = null // tocar la línea no abre una ventana
        vista.overlays.add(this)
    }
    inicio?.let { punto ->
        Marker(vista).apply {
            position = punto
            icon = BitmapDrawable(vista.resources, circulo(vista.context, "Tú", COLOR_INICIO))
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "Tu ubicación"
            vista.overlays.add(this)
        }
    }
    ordenadas.forEachIndexed { indice, parada ->
        Marker(vista).apply {
            position = puntos[indice]
            val color = if (parada.tipo == TipoParada.RECOJO) COLOR_RECOJO else COLOR_ENTREGA
            icon = BitmapDrawable(vista.resources, circulo(vista.context, "${indice + 1}", color))
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "${indice + 1}. ${parada.tipo.etiqueta} · ${parada.numeroGuia}"
            setOnMarkerClickListener { _, _ ->
                onParada(parada)
                true
            }
            vista.overlays.add(this)
        }
    }
    vista.invalidate()
    return listOfNotNull(inicio) + puntos
}

/** Muestra toda la ruta (o centra el único punto). */
private fun encuadrar(vista: MapView, puntos: List<GeoPoint>, animado: Boolean) {
    when {
        puntos.size >= 2 ->
            vista.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(puntos).increaseByScale(1.4f), animado)

        puntos.size == 1 -> if (animado) {
            vista.controller.animateTo(puntos.first(), 16.0, 400L)
        } else {
            vista.controller.setZoom(16.0)
            vista.controller.setCenter(puntos.first())
        }
    }
}

/** Círculo de color con un texto blanco (el número de la parada). */
private fun circulo(context: Context, texto: String, color: Int): Bitmap {
    val densidad = context.resources.displayMetrics.density
    val lado = (34 * densidad).toInt()
    val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
    val lienzo = Canvas(bitmap)
    val pintura = Paint(Paint.ANTI_ALIAS_FLAG)
    pintura.color = android.graphics.Color.WHITE
    lienzo.drawCircle(lado / 2f, lado / 2f, lado / 2f, pintura)
    pintura.color = color
    lienzo.drawCircle(lado / 2f, lado / 2f, lado / 2f - 2.5f * densidad, pintura)
    pintura.color = android.graphics.Color.WHITE
    pintura.textAlign = Paint.Align.CENTER
    pintura.textSize = (if (texto.length > 2) 10 else 14) * densidad
    pintura.isFakeBoldText = true
    val y = lado / 2f - (pintura.descent() + pintura.ascent()) / 2
    lienzo.drawText(texto, lado / 2f, y, pintura)
    return bitmap
}
