package com.example.rutalogcourier.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rutalogcourier.data.repository.RutaRepository
import com.example.rutalogcourier.model.EstadoEnvio
import com.example.rutalogcourier.model.MotivoFalla
import com.example.rutalogcourier.model.NumeroGuia
import com.example.rutalogcourier.model.Parada
import com.example.rutalogcourier.model.ReglasCourier
import com.example.rutalogcourier.model.ResumenRuta
import com.example.rutalogcourier.util.ProveedorUbicacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Ruta del día del courier (RF-C01 a RF-C08). Valida con [ReglasCourier] y guarda en Room
 * con la hora y el GPS del momento; funciona igual con o sin señal. No conoce Room ni la API.
 */
class RutaViewModel(
    private val repositorio: RutaRepository,
    private val ubicacion: ProveedorUbicacion,
    private val programarSincronizacion: () -> Unit
) : ViewModel() {

    val paradas: StateFlow<List<Parada>> = repositorio.observarParadas()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val resumen: StateFlow<ResumenRuta?> = repositorio.observarResumen()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun observarParada(idRemoto: Int): Flow<Parada?> = repositorio.observarParada(idRemoto)

    /** RF-C03: al escanear la guía, el envío pasa a RECOGIDO solo si es la de esta parada. */
    fun registrarRecojo(parada: Parada, guiaLeida: String, onError: (String) -> Unit, onHecho: () -> Unit) {
        if (parada.estado != EstadoEnvio.PENDIENTE) {
            onError("Este envío ya fue recogido.")
            return
        }
        if (!NumeroGuia.coinciden(guiaLeida, parada.numeroGuia)) {
            onError("La guía escaneada (${guiaLeida.trim().uppercase()}) no corresponde a esta parada (${parada.numeroGuia}).")
            return
        }
        viewModelScope.launch {
            repositorio.registrarAvance(parada.idRemoto, EstadoEnvio.RECOGIDO, guiaLeida.trim().uppercase(), ubicacion.actual())
            programarSincronizacion()
            onHecho()
        }
    }

    /** Recogido → en tránsito; en tránsito o reprogramado → en reparto. */
    fun avanzar(parada: Parada, onError: (String) -> Unit, onHecho: () -> Unit) {
        val siguiente = ReglasCourier.siguienteEstado(parada.estado)
        if (siguiente == null || siguiente == EstadoEnvio.RECOGIDO) {
            onError("Este envío no tiene un avance pendiente.")
            return
        }
        viewModelScope.launch {
            repositorio.registrarAvance(parada.idRemoto, siguiente, null, ubicacion.actual())
            programarSincronizacion()
            onHecho()
        }
    }

    /** RF-C04: foto, firma, nombre y DNI. RN-02: un contraentrega necesita el cobro confirmado. */
    fun entregar(
        parada: Parada,
        foto: ByteArray?,
        firma: ByteArray?,
        recibeNombre: String,
        recibeDni: String,
        onError: (String) -> Unit,
        onHecho: () -> Unit
    ) {
        val error = ReglasCourier.motivoNoEntregar(parada)
            ?: ReglasCourier.validarEntrega(recibeNombre, recibeDni, foto != null, firma != null)
        if (error != null || foto == null || firma == null) {
            onError(error ?: "Faltan datos de la entrega.")
            return
        }
        if (parada.estado != EstadoEnvio.EN_REPARTO) {
            onError("Solo se entrega un envío en reparto.")
            return
        }
        viewModelScope.launch {
            repositorio.registrarEntrega(parada.idRemoto, foto, firma, recibeNombre, recibeDni, ubicacion.actual())
            programarSincronizacion()
            onHecho()
        }
    }

    /** RF-C07: motivo y foto de la fachada; al tercer intento el envío vuelve al remitente (RN-05). */
    fun registrarFallida(
        parada: Parada,
        motivo: MotivoFalla?,
        foto: ByteArray?,
        onError: (String) -> Unit,
        onHecho: (EstadoEnvio) -> Unit
    ) {
        val error = ReglasCourier.validarFallida(motivo, foto != null)
        if (error != null || motivo == null || foto == null) {
            onError(error ?: "Faltan datos de la entrega fallida.")
            return
        }
        if (parada.estado != EstadoEnvio.EN_REPARTO) {
            onError("Solo se registra una entrega fallida si el envío está en reparto.")
            return
        }
        viewModelScope.launch {
            repositorio.registrarFallida(parada.idRemoto, motivo, foto, ubicacion.actual())
            programarSincronizacion()
            onHecho(ReglasCourier.estadoTrasFallida(parada.intentos))
        }
    }

    /** RF-C02: sube o baja una parada en la ruta. El cambio queda registrado y se envía al servidor. */
    fun mover(parada: Parada, haciaArriba: Boolean) {
        val orden = paradas.value.filter { it.esParada }.toMutableList()
        val indice = orden.indexOfFirst { it.idRemoto == parada.idRemoto }
        val destino = if (haciaArriba) indice - 1 else indice + 1
        if (indice < 0 || destino !in orden.indices) return
        orden.add(destino, orden.removeAt(indice))
        viewModelScope.launch {
            repositorio.reordenar(orden.map { it.idRemoto })
            programarSincronizacion()
        }
    }

    companion object {
        fun factory(
            repositorio: RutaRepository,
            ubicacion: ProveedorUbicacion,
            programarSincronizacion: () -> Unit
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { RutaViewModel(repositorio, ubicacion, programarSincronizacion) }
        }
    }
}
