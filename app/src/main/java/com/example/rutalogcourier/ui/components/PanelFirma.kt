package com.example.rutalogcourier.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Trazos de la firma. Se guardan aparte para poder convertirlos en imagen al cerrar la entrega. */
class EstadoFirma {
    val trazos = mutableStateListOf<List<Offset>>()
    internal val actual = mutableStateListOf<Offset>()
    internal var ancho = 0
    internal var alto = 0

    val vacia: Boolean get() = trazos.isEmpty() && actual.isEmpty()

    fun limpiar() {
        trazos.clear()
        actual.clear()
    }

    /** RN-03: la firma se guarda como PNG en blanco y negro, del tamaño del panel. */
    fun aBitmap(): Bitmap? {
        if (vacia || ancho <= 0 || alto <= 0) return null
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = android.graphics.Canvas(bitmap)
        lienzo.drawColor(android.graphics.Color.WHITE)
        val pintura = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        (trazos + listOf(actual.toList())).filter { it.size > 1 }.forEach { trazo ->
            val camino = android.graphics.Path()
            camino.moveTo(trazo.first().x, trazo.first().y)
            trazo.drop(1).forEach { camino.lineTo(it.x, it.y) }
            lienzo.drawPath(camino, pintura)
        }
        return bitmap
    }
}

@Composable
fun rememberEstadoFirma(): EstadoFirma = remember { EstadoFirma() }

/** HU-C03: el destinatario firma con el dedo en la pantalla. */
@Composable
fun PanelFirma(estado: EstadoFirma, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (estado.vacia) {
            Text("Firma aquí", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(estado) {
                    estado.ancho = size.width
                    estado.alto = size.height
                    detectDragGestures(
                        onDragStart = { inicio -> estado.actual.add(inicio) },
                        onDragEnd = {
                            estado.trazos.add(estado.actual.toList())
                            estado.actual.clear()
                        },
                        onDrag = { cambio, _ -> estado.actual.add(cambio.position) }
                    )
                }
        ) {
            val pintar: (List<Offset>) -> Unit = { trazo ->
                if (trazo.size > 1) {
                    val camino = Path().apply {
                        moveTo(trazo.first().x, trazo.first().y)
                        trazo.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(camino, Color.Black, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            estado.trazos.forEach(pintar)
            pintar(estado.actual)
        }
    }
}
