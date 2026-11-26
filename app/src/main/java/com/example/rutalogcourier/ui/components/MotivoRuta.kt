package com.example.rutalogcourier.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Motivo visual de RutaLog: "la ruta". Una línea punteada que va de un punto de origen a un pin de destino.

/** Pin de destino con la punta en [punta]. */
private fun DrawScope.pinDestino(punta: Offset, radio: Float, color: Color) {
    val centro = Offset(punta.x, punta.y - radio * 2.1f)
    val cola = Path().apply {
        moveTo(centro.x - radio * 0.78f, centro.y + radio * 0.6f)
        lineTo(punta.x, punta.y)
        lineTo(centro.x + radio * 0.78f, centro.y + radio * 0.6f)
        close()
    }
    drawPath(cola, color)
    drawCircle(color, radius = radio, center = centro)
}

/**
 * Fondo de los encabezados azules: una ruta punteada que avanza (animada) desde el origen hasta el pin
 * de destino, en la mitad derecha y con poca opacidad para no competir con el texto.
 */
@Composable
fun FondoRuta(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "Ruta")
    val avance by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "AvanceRuta"
    )
    Canvas(modifier = modifier) {
        val color = Color.White.copy(alpha = 0.16f)
        val guion = 10.dp.toPx()
        val hueco = 8.dp.toPx()
        val radioPin = 8.dp.toPx()
        // Ruta baja y a la derecha: no choca con el estado del envío, que va arriba a la derecha.
        val origen = Offset(size.width * 0.52f, size.height * 0.90f)
        val destino = Offset(size.width * 0.91f, size.height * 0.80f)
        val ruta = Path().apply {
            moveTo(origen.x, origen.y)
            cubicTo(
                size.width * 0.66f, size.height * 1.02f,
                size.width * 0.74f, size.height * 0.56f,
                destino.x, destino.y
            )
        }
        drawPath(
            ruta,
            color,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(guion, hueco), phase = -avance * (guion + hueco))
            )
        )
        drawCircle(color, radius = 6.dp.toPx(), center = origen)
        pinDestino(destino, radioPin, color)
    }
}

/** Tramo vertical de la línea de tiempo: lleno si ya se recorrió, punteado si todavía falta. */
@Composable
fun TramoRuta(recorrido: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val x = size.width / 2
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = size.width,
            cap = StrokeCap.Round,
            pathEffect = if (recorrido) null else PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))
        )
    }
}

/** Ilustración de las pantallas vacías: la ruta punteada pasa por encima del ícono, del origen al destino. */
@Composable
fun IlustracionRuta(icono: ImageVector, modifier: Modifier = Modifier) {
    val primario = MaterialTheme.colorScheme.primary
    val destino = MaterialTheme.colorScheme.secondary
    Box(modifier = modifier.size(width = 180.dp, height = 112.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val y = size.height * 0.80f
            val ruta = Path().apply {
                moveTo(size.width * 0.08f, y)
                cubicTo(size.width * 0.28f, size.height * 0.02f, size.width * 0.72f, size.height * 0.02f, size.width * 0.92f, y)
            }
            drawPath(
                ruta,
                primario.copy(alpha = 0.55f),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 7.dp.toPx())))
            )
            drawCircle(primario, radius = 6.dp.toPx(), center = Offset(size.width * 0.08f, y))
            pinDestino(Offset(size.width * 0.92f, y + 2.dp.toPx()), 8.dp.toPx(), destino)
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
        }
    }
}

/** Número que "cuenta" desde 0 hasta [valor] al aparecer (y entre valores cuando cambia). */
@Composable
fun conteoAnimado(valor: Int): Int {
    val animable = remember { Animatable(0f) }
    LaunchedEffect(valor) { animable.animateTo(valor.toFloat(), tween(900, easing = FastOutSlowInEasing)) }
    return animable.value.roundToInt()
}
