package com.example.rutalogcourier.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rutalogcourier.model.Parada
import com.example.rutalogcourier.model.TipoParada
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private const val COLOR_RECOJO = 0xFFEF6C00.toInt()
private const val COLOR_ENTREGA = 0xFF2E7D32.toInt()
private const val COLOR_RUTA = 0xFF0B2F5B.toInt()
private const val COLOR_INICIO = 0xFF1E88E5.toInt()

/**
 * RF-C01: la ruta del día en el mapa (OpenStreetMap). Cada parada lleva su número de orden;
 * naranja = recojo, verde = entrega. La línea sale desde donde está el courier.
 */
@Composable
fun MapaRuta(
    paradas: List<Parada>,
    inicio: GeoPoint?,
    onParada: (Parada) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapa = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(-12.0464, -77.0428))
        }
    }
    DisposableEffect(mapa) {
        mapa.onResume()
        onDispose {
            mapa.onPause()
            mapa.onDetach()
        }
    }

    AndroidView(factory = { mapa }, modifier = modifier) { vista ->
        vista.overlays.clear()
        val ordenadas = paradas.filter { it.esParada && it.tieneUbicacion }.sortedBy { it.orden }
        val puntos = ordenadas.map { GeoPoint(it.latitud!!, it.longitud!!) }

        Polyline(vista).apply {
            setPoints(listOfNotNull(inicio) + puntos)
            outlinePaint.color = COLOR_RUTA
            outlinePaint.strokeWidth = 7f
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

        val todos = listOfNotNull(inicio) + puntos
        if (todos.size >= 2) {
            vista.post { vista.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(todos).increaseByScale(1.4f), false) }
        } else if (todos.size == 1) {
            vista.controller.setCenter(todos.first())
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
