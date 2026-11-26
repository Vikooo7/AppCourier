package com.example.rutalogcourier.ui.screens

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.rutalogcourier.model.EstadoEnvio
import com.example.rutalogcourier.model.MotivoFalla
import com.example.rutalogcourier.model.Parada
import com.example.rutalogcourier.ui.components.AppScaffold
import com.example.rutalogcourier.ui.components.InputField
import com.example.rutalogcourier.ui.components.MensajeError
import com.example.rutalogcourier.ui.components.MensajeInfo
import com.example.rutalogcourier.ui.components.PanelFirma
import com.example.rutalogcourier.ui.components.VistaFoto
import com.example.rutalogcourier.ui.components.rememberCamara
import com.example.rutalogcourier.ui.components.rememberEstadoFirma
import com.example.rutalogcourier.util.Imagenes

/**
 * RF-C04 / HU-C03: entrega con foto, firma en pantalla, nombre y DNI de quien recibe.
 * Sin foto y firma no se cierra (RN-03). Se guarda sin señal y se envía al sincronizar.
 */
@Composable
fun EntregaScreen(
    parada: Parada?,
    onVolver: () -> Unit,
    onGuardar: (
        parada: Parada,
        foto: ByteArray?,
        firma: ByteArray?,
        nombre: String,
        dni: String,
        onError: (String) -> Unit,
        onHecho: () -> Unit
    ) -> Unit
) {
    var foto by remember { mutableStateOf<ByteArray?>(null) }
    var nombre by remember(parada?.idRemoto) { mutableStateOf(parada?.destinatarioNombre.orEmpty()) }
    var dni by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    val vibracion = LocalHapticFeedback.current
    val firma = rememberEstadoFirma()
    val tomarFoto = rememberCamara(onFoto = { if (it != null) foto = it }, onError = { error = it })

    AppScaffold(titulo = "Entregar", subtitulo = parada?.numeroGuia, onVolver = onVolver) {
        if (parada == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("La parada ya no está en tu ruta.") }
            return@AppScaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Seccion("1. Foto de la entrega") {
                foto?.let { VistaFoto(it) }
                OutlinedButton(onClick = tomarFoto, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Text(if (foto == null) "  Tomar foto" else "  Tomar otra foto")
                }
            }

            Seccion("2. Firma del destinatario") {
                PanelFirma(estado = firma)
                TextButton(onClick = { firma.limpiar() }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(" Borrar firma")
                }
            }

            Seccion("3. Quién recibe") {
                InputField(
                    valor = nombre,
                    onValorChange = {
                        nombre = it.take(80)
                        error = null
                    },
                    etiqueta = "Nombre",
                    icono = Icons.Default.Person,
                    mayusculas = KeyboardCapitalization.Words
                )
                InputField(
                    valor = dni,
                    onValorChange = { valor ->
                        dni = valor.filter { it.isDigit() }.take(8)
                        error = null
                    },
                    etiqueta = "DNI",
                    icono = Icons.Default.Badge,
                    textoAyuda = "${dni.length}/8 · Se guarda solo como prueba de entrega",
                    tipoTeclado = KeyboardType.Number,
                    accionIme = ImeAction.Done
                )
            }

            AnimatedVisibility(visible = error != null) { MensajeError(texto = error.orEmpty()) }

            Button(
                onClick = {
                    error = null
                    guardando = true
                    onGuardar(
                        parada,
                        foto,
                        firma.aBitmap()?.let(Imagenes::firmaPng),
                        nombre,
                        dni,
                        {
                            error = it
                            guardando = false
                        },
                        {
                            guardando = false
                            // Confirma con una vibración que la entrega quedó registrada (entrega o fallida), sin mirar la pantalla.
                            vibracion.performHapticFeedback(HapticFeedbackType.Confirm)
                            onVolver()
                        }
                    )
                },
                enabled = !guardando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Text("  Cerrar entrega")
            }
        }
    }
}

/**
 * RF-C07 / HU-C05: entrega fallida con motivo y foto de la fachada. El operador la reprograma;
 * al tercer intento el envío vuelve al remitente (RN-05).
 */
@Composable
fun FallidaScreen(
    parada: Parada?,
    onVolver: () -> Unit,
    onGuardar: (
        parada: Parada,
        motivo: MotivoFalla?,
        foto: ByteArray?,
        onError: (String) -> Unit,
        onHecho: (EstadoEnvio) -> Unit
    ) -> Unit
) {
    var motivo by remember { mutableStateOf<MotivoFalla?>(null) }
    var foto by remember { mutableStateOf<ByteArray?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    val vibracion = LocalHapticFeedback.current
    val tomarFoto = rememberCamara(onFoto = { if (it != null) foto = it }, onError = { error = it })

    AppScaffold(titulo = "Entrega fallida", subtitulo = parada?.numeroGuia, onVolver = onVolver) {
        if (parada == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("La parada ya no está en tu ruta.") }
            return@AppScaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (parada.intentos + 1 >= EstadoEnvio.MAX_INTENTOS) {
                MensajeInfo("Es el intento ${parada.intentos + 1} de ${EstadoEnvio.MAX_INTENTOS}: el envío volverá al remitente.")
            } else {
                MensajeInfo("Intento ${parada.intentos + 1} de ${EstadoEnvio.MAX_INTENTOS}. El operador la reprogramará.")
            }

            Seccion("1. Motivo") {
                MotivoFalla.entries.forEach { opcion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = motivo == opcion, onClick = {
                                motivo = opcion
                                error = null
                            }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = motivo == opcion, onClick = null)
                        Text(opcion.etiqueta, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Seccion("2. Foto de la fachada") {
                foto?.let { VistaFoto(it) }
                OutlinedButton(onClick = tomarFoto, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Text(if (foto == null) "  Tomar foto" else "  Tomar otra foto")
                }
            }

            AnimatedVisibility(visible = error != null) { MensajeError(texto = error.orEmpty()) }

            Button(
                onClick = {
                    error = null
                    guardando = true
                    onGuardar(
                        parada,
                        motivo,
                        foto,
                        {
                            error = it
                            guardando = false
                        },
                        {
                            guardando = false
                            // Confirma con una vibración que la entrega quedó registrada (entrega o fallida), sin mirar la pantalla.
                            vibracion.performHapticFeedback(HapticFeedbackType.Confirm)
                            onVolver()
                        }
                    )
                },
                enabled = !guardando,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.ReportProblem, contentDescription = null)
                Text("  Registrar entrega fallida")
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
