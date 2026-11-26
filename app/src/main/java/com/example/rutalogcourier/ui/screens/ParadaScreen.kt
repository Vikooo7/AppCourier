package com.example.rutalogcourier.ui.screens

import com.example.rutalogcourier.ui.components.MensajeAdvertencia
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.rutalogcourier.ui.components.BotonCopiarGuia
import com.example.rutalogcourier.ui.theme.FuenteGuia
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rutalogcourier.model.AccionCourier
import com.example.rutalogcourier.model.EstadoEnvio
import com.example.rutalogcourier.model.EstadoSincronizacion
import com.example.rutalogcourier.model.Parada
import com.example.rutalogcourier.model.ReglasCourier
import com.example.rutalogcourier.model.TipoParada
import com.example.rutalogcourier.ui.components.AppScaffold
import com.example.rutalogcourier.ui.components.BannerDegradado
import com.example.rutalogcourier.ui.components.EstadoBadge
import com.example.rutalogcourier.ui.components.MensajeError
import com.example.rutalogcourier.ui.components.MensajeInfo
import com.example.rutalogcourier.ui.components.SyncStatusIndicator
import com.example.rutalogcourier.ui.components.formatoSoles
import com.example.rutalogcourier.util.EscanerGuia
import kotlinx.coroutines.launch

/**
 * Detalle de una parada con lo que corresponde hacer según su estado: escanear la guía para recoger
 * (RF-C03), avanzar a tránsito o reparto, entregar con foto y firma (RF-C04) o registrar una fallida (RF-C07).
 */
@Composable
fun ParadaScreen(
    parada: Parada?,
    onVolver: () -> Unit,
    onRecoger: (Parada, guia: String, onError: (String) -> Unit) -> Unit,
    onAvanzar: (Parada, onError: (String) -> Unit) -> Unit,
    onEntregar: (Parada) -> Unit,
    onCobrar: (Parada) -> Unit,
    onFallida: (Parada) -> Unit
) {
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    val vibracion = LocalHapticFeedback.current
    var error by remember { mutableStateOf<String?>(null) }
    var escaneando by remember { mutableStateOf(false) }
    var escribirGuia by remember { mutableStateOf(false) }

    AppScaffold(titulo = "Parada", subtitulo = parada?.numeroGuia, onVolver = onVolver) {
        if (parada == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Esta parada ya no está en tu ruta.", style = MaterialTheme.typography.bodyMedium)
            }
            return@AppScaffold
        }

        val acciones = ReglasCourier.acciones(parada)

        // Datos arriba (con scroll) y acciones fijas abajo, al alcance del pulgar.
        Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BannerDegradado {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(parada.tipo.etiqueta.uppercase(), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                parada.numeroGuia,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FuenteGuia,
                                fontWeight = FontWeight.Bold
                            )
                            BotonCopiarGuia(guia = parada.numeroGuia)
                        }
                    }
                    EstadoBadge(estado = parada.estado, sobreFondoOscuro = true)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(parada.ruta, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }

            // Ética del caso: el courier solo ve nombre, dirección y teléfono, y solo durante la entrega.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (parada.tipo == TipoParada.ENTREGA) "Destinatario" else "Datos del recojo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (parada.tipo == TipoParada.ENTREGA) {
                        Fila(Icons.Default.Person, "Nombre", parada.destinatarioNombre ?: "No registrado")
                        parada.destinatarioTelefono?.let { Fila(Icons.Default.Phone, "Celular", it) }
                        Fila(Icons.Default.Home, "Dirección", parada.destinatarioDireccion ?: "No registrada")
                    } else {
                        // La API no los envía fuera del reparto: el courier no necesita esos datos para recoger.
                        Fila(Icons.Default.Lock, "Datos de contacto", "Se muestran en la parada de entrega")
                    }
                    Fila(Icons.Default.Map, "Ruta", parada.ruta)
                    Fila(
                        Icons.Default.QrCode,
                        "Cobro contraentrega",
                        if (parada.cobraContraentrega) formatoSoles(parada.montoContraentrega) else "No (prepagado)"
                    )
                    if (parada.intentos > 0) {
                        Fila(Icons.Default.ReportProblem, "Intentos fallidos", "${parada.intentos} de ${EstadoEnvio.MAX_INTENTOS}")
                    }
                }
            }

            // ML-01 "prioridad en la ruta": se recomienda confirmar antes de ir.
            parada.textoRiesgo?.let { pct ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Riesgo alto de entrega fallida ($pct)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (parada.riesgoFactores.isNotEmpty()) {
                            Text(
                                "Por qué: ${parada.riesgoFactores.joinToString(" · ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            "Llama al destinatario antes de ir para confirmar que habrá alguien.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Solo se avisa cuando hay algo por enviar o un rechazo; "sincronizado" es lo normal.
            if (parada.estadoSync != EstadoSincronizacion.SINCRONIZADO) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sincronización: ", style = MaterialTheme.typography.bodySmall)
                    SyncStatusIndicator(estado = parada.estadoSync)
                }
            }
            parada.mensajeError?.let { MensajeError(texto = it) }
            if (parada.estadoSync == EstadoSincronizacion.ERROR) {
                MensajeAdvertencia("El servidor rechazó un registro de esta parada. Ve a Sincronizar para reintentar o descartar.")
            }
            error?.let { MensajeError(texto = it) }

            // RF-C03 / RN-06: cada avance lleva hora y GPS. Con la ubicación apagada se registra igual, pero sin GPS.
            if (acciones.isNotEmpty() && !ubicacionActiva(context)) {
                MensajeAdvertencia("La ubicación del teléfono está apagada: los registros se guardarán sin GPS. Actívala en los ajustes rápidos.")
            }
            if (acciones.isEmpty()) {
                MensajeInfo(
                    when (parada.estado) {
                        EstadoEnvio.ENTREGADO -> "Entrega registrada. Se enviará al servidor con la foto y la firma."
                        EstadoEnvio.FALLIDO -> "Entrega fallida registrada. El operador la reprogramará."
                        EstadoEnvio.DEVUELTO -> "Tercer intento fallido: el envío vuelve al remitente."
                        else -> "No hay acciones pendientes para esta parada."
                    }
                )
            }
        }

        if (acciones.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            acciones.forEach { accion ->
                when (accion) {
                    AccionCourier.ESCANEAR_RECOJO -> {
                        Button(
                            onClick = {
                                error = null
                                escaneando = true
                                alcance.launch {
                                    val leida = EscanerGuia.escanear(context)
                                    escaneando = false
                                    if (leida != null) {
                                        // Se leyó la guía: vibración corta para no tener que mirar la pantalla.
                                        vibracion.performHapticFeedback(HapticFeedbackType.Confirm)
                                        onRecoger(parada, leida) { error = it }
                                    }
                                }
                            },
                            enabled = !escaneando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            if (escaneando) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                Text("  ${accion.etiqueta}")
                            }
                        }
                        OutlinedButton(onClick = { escribirGuia = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Keyboard, contentDescription = null)
                            Text("  Escribir la guía a mano")
                        }
                    }

                    AccionCourier.MARCAR_EN_TRANSITO, AccionCourier.SALIR_A_REPARTO -> Button(
                        onClick = {
                            error = null
                            onAvanzar(parada) { error = it }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null)
                        Text("  ${accion.etiqueta}")
                    }

                    AccionCourier.ENTREGAR -> {
                        // RN-02: el contraentrega se cobra con QR antes de entregar.
                        if (parada.cobraContraentrega && !parada.cobroListo) {
                            Button(
                                onClick = { onCobrar(parada) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null)
                                Text("  Cobrar ${formatoSoles(parada.montoContraentrega)} con QR")
                            }
                        }
                        ReglasCourier.motivoNoEntregar(parada)?.let { MensajeInfo(it) }
                        Button(
                            onClick = { onEntregar(parada) },
                            enabled = ReglasCourier.motivoNoEntregar(parada) == null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Text("  ${accion.etiqueta}")
                        }
                    }

                    AccionCourier.REGISTRAR_FALLIDA -> Button(
                        onClick = { onFallida(parada) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null)
                        Text("  ${accion.etiqueta}")
                    }
                }
            }
            }
            }
        }
        }

        if (escribirGuia) {
            var texto by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { escribirGuia = false },
                title = { Text("Escribe la guía") },
                text = {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it.uppercase().take(20) },
                        label = { Text("Por ejemplo RLP-26-300025-0") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        escribirGuia = false
                        error = null
                        onRecoger(parada, texto) { error = it }
                    }) { Text("Recoger") }
                },
                dismissButton = { TextButton(onClick = { escribirGuia = false }) { Text("Cancelar") } }
            )
        }
    }
}

@Composable
private fun Fila(icono: ImageVector, etiqueta: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** true si el GPS / la ubicación del sistema está encendida (el permiso se pide aparte). */
private fun ubicacionActiva(context: android.content.Context): Boolean =
    androidx.core.location.LocationManagerCompat.isLocationEnabled(
        context.getSystemService(android.location.LocationManager::class.java)
    )
