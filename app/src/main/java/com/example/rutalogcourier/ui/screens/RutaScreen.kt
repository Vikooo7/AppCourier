package com.example.rutalogcourier.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rutalogcourier.model.Parada
import com.example.rutalogcourier.model.ResumenRuta
import com.example.rutalogcourier.model.TipoParada
import com.example.rutalogcourier.ui.components.AppScaffold
import com.example.rutalogcourier.ui.components.BannerDegradado
import com.example.rutalogcourier.ui.components.EstadoBadge
import com.example.rutalogcourier.ui.components.EstadoVacio
import com.example.rutalogcourier.ui.components.MapaRuta
import com.example.rutalogcourier.ui.components.PestanaCourier
import com.example.rutalogcourier.ui.components.SyncStatusIndicator
import com.example.rutalogcourier.ui.components.formatoSoles
import org.osmdroid.util.GeoPoint

/**
 * RF-C01: ruta del día. Resumen con los km de ML-04 frente al orden de registro, mapa y lista de
 * paradas en orden (RF-C02: se puede subir o bajar cada una). Al final, lo que va en camino.
 */
@Composable
fun RutaScreen(
    nombreUsuario: String,
    paradas: List<Parada>,
    resumen: ResumenRuta?,
    sincronizando: Boolean,
    onActualizar: () -> Unit,
    onParada: (Parada) -> Unit,
    onMover: (Parada, haciaArriba: Boolean) -> Unit,
    onPestana: (PestanaCourier) -> Unit,
    onCerrarSesion: () -> Unit
) {
    // GPS para cada registro y avisos de paradas nuevas: se piden una vez al abrir la ruta.
    val permisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    LaunchedEffect(Unit) {
        val pedidos = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permisos.launch(pedidos.toTypedArray())
    }

    val enRuta = paradas.filter { it.esParada }.sortedBy { it.orden }
    val enCamino = paradas.filter { it.tipo == TipoParada.EN_CAMINO }
    val completadas = paradas.filter { it.tipo == TipoParada.COMPLETADA }

    AppScaffold(
        titulo = "Ruta del día",
        subtitulo = "Courier · $nombreUsuario",
        pestana = PestanaCourier.RUTA,
        onPestana = onPestana,
        onCerrarSesion = onCerrarSesion
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ResumenDelDia(
                    enRuta = enRuta,
                    resumen = resumen,
                    sincronizando = sincronizando,
                    onActualizar = onActualizar
                )
            }

            if (enRuta.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(20.dp)) {
                        MapaRuta(
                            paradas = enRuta,
                            inicio = resumen?.let { GeoPoint(it.inicioLatitud, it.inicioLongitud) },
                            onParada = onParada,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                    }
                }
            }

            if (enRuta.isEmpty() && enCamino.isEmpty()) {
                item {
                    EstadoVacio(
                        icono = Icons.Default.Route,
                        titulo = "Sin paradas por ahora",
                        mensaje = "Cuando el operador te asigne recojos o entregas aparecerán aquí. Toca Actualizar."
                    )
                }
            }

            items(enRuta, key = { "ruta-${it.idRemoto}" }) { parada ->
                TarjetaParada(
                    parada = parada,
                    numero = enRuta.indexOf(parada) + 1,
                    puedeSubir = enRuta.first() != parada,
                    puedeBajar = enRuta.last() != parada,
                    onClick = { onParada(parada) },
                    onMover = { arriba -> onMover(parada, arriba) }
                )
            }

            if (enCamino.isNotEmpty()) {
                item { Titulo("En camino a la ciudad de destino (${enCamino.size})") }
                items(enCamino, key = { "camino-${it.idRemoto}" }) { parada ->
                    TarjetaParada(parada = parada, numero = null, onClick = { onParada(parada) })
                }
            }

            if (completadas.isNotEmpty()) {
                item { Titulo("Registradas hoy, por sincronizar (${completadas.size})") }
                items(completadas, key = { "hecha-${it.idRemoto}" }) { parada ->
                    TarjetaParada(parada = parada, numero = null, onClick = { onParada(parada) })
                }
            }
        }
    }
}

@Composable
private fun ResumenDelDia(
    enRuta: List<Parada>,
    resumen: ResumenRuta?,
    sincronizando: Boolean,
    onActualizar: () -> Unit
) {
    BannerDegradado {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${enRuta.size} paradas",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${enRuta.count { it.tipo == TipoParada.RECOJO }} recojos · " +
                        "${enRuta.count { it.tipo == TipoParada.ENTREGA }} entregas · " +
                        "${enRuta.count { it.cobraContraentrega }} contraentrega",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (sincronizando) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onActualizar) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Text(" Actualizar", color = Color.White)
                }
            }
        }
        if (resumen != null && enRuta.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (resumen.ordenManual) {
                    "Orden elegido por ti · ${"%.1f".format(resumen.kmRecorrido)} km"
                } else {
                    "Orden más corto (ML-04): ${"%.1f".format(resumen.kmOptimizado)} km · " +
                        "${resumen.ahorroPorcentaje} % menos que el orden de registro (${"%.1f".format(resumen.kmOrdenRegistro)} km)"
                },
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun TarjetaParada(
    parada: Parada,
    numero: Int?,
    onClick: () -> Unit,
    puedeSubir: Boolean = false,
    puedeBajar: Boolean = false,
    onMover: ((Boolean) -> Unit)? = null
) {
    val colorTipo = when (parada.tipo) {
        TipoParada.RECOJO -> Color(0xFFEF6C00)
        TipoParada.ENTREGA -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.outline
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colorTipo),
                contentAlignment = Alignment.Center
            ) {
                if (numero != null) {
                    Text("$numero", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = parada.tipo.etiqueta.uppercase(),
                        color = colorTipo,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = parada.numeroGuia,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = when (parada.tipo) {
                        TipoParada.RECOJO -> "Recoger en el almacén de origen · ${parada.ruta}"
                        else -> listOfNotNull(parada.destinatarioNombre, parada.destinatarioDireccion)
                            .joinToString(" · ")
                            .ifEmpty { parada.ruta }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadoBadge(estado = parada.estado)
                    if (parada.cobraContraentrega) {
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(12.dp))
                                Text(" ${formatoSoles(parada.montoContraentrega)}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    parada.kmDesdeAnterior?.let {
                        Text("${"%.1f".format(it)} km", style = MaterialTheme.typography.labelSmall)
                    }
                }
                // ML-01 "prioridad en la ruta": entrega con riesgo alto de fallar.
                parada.textoRiesgo?.let { pct ->
                    Text(
                        text = "⚠ Riesgo alto de falla $pct · llama antes",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                SyncStatusIndicator(estado = parada.estadoSync)
            }
            if (onMover != null) {
                Column {
                    IconButton(onClick = { onMover(true) }, enabled = puedeSubir, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir parada")
                    }
                    IconButton(onClick = { onMover(false) }, enabled = puedeBajar, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar parada")
                    }
                }
            }
        }
    }
}
