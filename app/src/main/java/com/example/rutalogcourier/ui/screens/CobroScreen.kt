package com.example.rutalogcourier.ui.screens

import com.example.rutalogcourier.ui.theme.FuenteGuia
import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.rutalogcourier.model.Parada
import com.example.rutalogcourier.ui.components.AppScaffold
import com.example.rutalogcourier.ui.components.LocalEnLinea
import com.example.rutalogcourier.ui.components.MensajeError
import com.example.rutalogcourier.ui.components.MensajeInfo
import com.example.rutalogcourier.ui.components.VistaFoto
import com.example.rutalogcourier.ui.components.formatoSoles
import com.example.rutalogcourier.ui.components.rememberCamara
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

private val VerdeOk = Color(0xFF2E7D32)

/**
 * RF-C05 / RF-C06 / RN-02: cobro contraentrega en la puerta.
 * - Con señal: el destinatario escanea el QR con Banco Andino y el courier verifica el pago.
 * - Sin señal: el QR ya vino con la ruta; el destinatario paga con sus datos y le muestra el n.º de operación.
 *   El courier lo anota con una foto del comprobante: queda PENDIENTE_CONFIRMACION y se verifica al sincronizar.
 */
@Composable
fun CobroScreen(
    parada: Parada?,
    onVolver: () -> Unit,
    onVerificar: (Parada, onResultado: (String, Boolean) -> Unit) -> Unit,
    onRegistrarSinSenal: (Parada, nro: String, foto: ByteArray?, onError: (String) -> Unit, onHecho: () -> Unit) -> Unit,
    onContinuar: (Parada) -> Unit
) {
    val enLinea = LocalEnLinea.current
    var verificando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var nro by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<ByteArray?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val tomarFoto = rememberCamara(onFoto = { if (it != null) foto = it }, onError = { error = it })

    AppScaffold(titulo = "Cobrar contraentrega", subtitulo = parada?.numeroGuia, onVolver = onVolver) {
        if (parada == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("La parada ya no está en tu ruta.") }
            return@AppScaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Cobrar a ${parada.destinatarioNombre ?: "el destinatario"}", style = MaterialTheme.typography.bodyMedium)
            Text(formatoSoles(parada.montoContraentrega), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)

            if (parada.cobroListo) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerdeOk, modifier = Modifier.size(64.dp))
                Text(
                    if (parada.cobroEstado == "PENDIENTE_CONFIRMACION") "Cobro registrado · se verificará con el banco al sincronizar"
                    else "Cobro confirmado por Banco Andino",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                parada.cobroNro?.let { Text("Operación $it", fontFamily = FuenteGuia, color = MaterialTheme.colorScheme.primary) }
                Button(onClick = { onContinuar(parada) }, modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)) { Text("Continuar con la entrega") }
                return@Column
            }

            val qr = parada.cobroQr
            if (qr != null) {
                QrCobro(qr)
                Text(qr.removePrefix("BANCOANDINO:"), fontFamily = FuenteGuia, fontWeight = FontWeight.SemiBold)
                Text(
                    "El destinatario abre Banco Andino → Pagar con QR y escanea este código. El monto ya está fijado.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                MensajeInfo("Esta parada no trae el QR del cobro. Con señal, actualiza la ruta para descargarlo.")
            }

            Button(
                onClick = {
                    verificando = true
                    onVerificar(parada) { texto, _ ->
                        verificando = false
                        mensaje = texto
                    }
                },
                enabled = enLinea && !verificando && parada.cobroId != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (verificando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Verified, contentDescription = null)
                    Text("  Ya pagó: verificar con el banco")
                }
            }
            mensaje?.let { MensajeInfo(it) }

            HorizontalDivider()

            // RF-C06: sin señal en la puerta.
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("¿Sin señal?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Si el destinatario ya pagó desde su teléfono, anota el n.º de operación que le muestra Banco Andino " +
                            "y toma una foto del comprobante. Podrás entregar y RutaLog lo verificará con el banco al recuperar la señal.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = nro,
                        onValueChange = { nro = it.uppercase().trim() },
                        label = { Text("N.º de operación (BA-…)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth()
                    )
                    foto?.let { VistaFoto(it) }
                    OutlinedButton(onClick = tomarFoto, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Text(if (foto == null) "  Foto del comprobante" else "  Tomar otra foto")
                    }
                    error?.let { MensajeError(it) }
                    TextButton(
                        onClick = {
                            error = null
                            onRegistrarSinSenal(parada, nro, foto, { error = it }) { /* la parada se actualiza sola desde Room */ }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Registrar cobro pendiente de confirmación") }
                }
            }
        }
    }
}

@Composable
private fun QrCobro(texto: String) {
    val imagen = remember(texto) { generarQr(texto) }
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White) // siempre blanco y negro para que la cámara lo lea, también en tema oscuro
            .padding(10.dp)
    ) {
        Image(bitmap = imagen, contentDescription = "QR del cobro", filterQuality = FilterQuality.None, modifier = Modifier.size(220.dp))
    }
}

private fun generarQr(texto: String): ImageBitmap {
    val matriz = QRCodeWriter().encode(
        texto,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
    )
    val pixeles = IntArray(matriz.width * matriz.height) { i ->
        if (matriz.get(i % matriz.width, i / matriz.width)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(pixeles, matriz.width, matriz.height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
