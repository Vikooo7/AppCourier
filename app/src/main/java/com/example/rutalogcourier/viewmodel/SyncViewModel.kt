package com.example.rutalogcourier.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rutalogcourier.data.repository.ResultadoSync
import com.example.rutalogcourier.data.local.entities.TipoOperacion
import com.example.rutalogcourier.data.repository.SyncRepository
import com.example.rutalogcourier.model.EstadoSincronizacion
import com.example.rutalogcourier.util.ConnectivityObserver
import com.example.rutalogcourier.util.Ubicacion
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Una operación de la cola, lista para mostrarse en la pantalla de sincronización. */
data class OperacionUi(
    val id: Int,
    val descripcion: String,
    val numeroGuia: String,
    val estado: EstadoSincronizacion,
    val intentos: Int,
    val mensajeError: String?,
    val fechaRegistro: Long
)

data class SyncUiState(
    val enLinea: Boolean = true,
    val sincronizando: Boolean = false,
    val operaciones: List<OperacionUi> = emptyList(),
    /** Fecha de la última sincronización exitosa (milisegundos), o null si nunca hubo. */
    val ultimaSincronizacion: Long? = null,
    val mensaje: String? = null,
    val mensajeEsError: Boolean = false,
    val simularErrorServidor: Boolean = false
) {
    val pendientes: Int get() = operaciones.count { it.estado != EstadoSincronizacion.ERROR }
    val errores: Int get() = operaciones.count { it.estado == EstadoSincronizacion.ERROR }
}

/** RF-C08: sincronización manual y estado de la cola (pendientes, errores, última fecha). */
class SyncViewModel(
    private val repositorio: SyncRepository,
    conectividad: ConnectivityObserver,
    private val simularError: AtomicBoolean,
    /** GPS actual: la ruta del día se ordena desde donde está el courier. */
    private val obtenerUbicacion: suspend () -> Ubicacion?
) : ViewModel() {

    private val _estado = MutableStateFlow(
        SyncUiState(enLinea = conectividad.hayConexion(), simularErrorServidor = simularError.get())
    )
    val estado: StateFlow<SyncUiState> = _estado.asStateFlow()

    /** La app indica si hay sesión; sin sesión no se sincroniza. */
    var haySesion: () -> Boolean = { false }

    init {
        viewModelScope.launch {
            repositorio.observarOperaciones().collect { lista ->
                _estado.update { actual ->
                    actual.copy(
                        operaciones = lista.map { fila ->
                            val op = fila.operacion
                            OperacionUi(
                                id = op.id,
                                descripcion = describirOperacion(op.tipoOperacion),
                                numeroGuia = fila.numeroGuia ?: "—",
                                estado = EstadoSincronizacion.desde(op.estado),
                                intentos = op.intentos,
                                mensajeError = op.mensajeError,
                                fechaRegistro = op.fechaRegistro
                            )
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            repositorio.observarUltimaSincronizacion().collect { fecha ->
                _estado.update { it.copy(ultimaSincronizacion = fecha) }
            }
        }
        viewModelScope.launch {
            conectividad.enLinea.collect { enLinea ->
                _estado.update { it.copy(enLinea = enLinea) }
            }
        }
        // Al recuperar la conexión se sincroniza sola.
        viewModelScope.launch {
            conectividad.enLinea.drop(1).collect { enLinea ->
                if (enLinea && haySesion()) sincronizar(silencioso = true)
            }
        }
    }

    /**
     * Sincroniza ahora: envía la cola y descarga la ruta del día.
     * Con [silencioso] = true solo avisa si algo falló (se usa tras el login y al guardar un cambio).
     */
    fun sincronizar(silencioso: Boolean = false) {
        if (_estado.value.sincronizando) return
        if (!_estado.value.enLinea) {
            if (!silencioso) {
                mostrar("Sin conexión. Tus cambios están guardados y se enviarán al recuperarla.", esError = true)
            }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(sincronizando = true, mensaje = null) }
            val resultado = repositorio.sincronizar(obtenerUbicacion())
            _estado.update { it.copy(sincronizando = false) }
            if (!silencioso || resultado.error != null || resultado.rechazadas > 0) {
                mostrar(describirSincronizacion(resultado), esError = resultado.error != null || resultado.rechazadas > 0)
            }
        }
    }

    fun reintentar(idOperacion: Int) {
        viewModelScope.launch {
            repositorio.reintentar(idOperacion)
            sincronizar()
        }
    }

    /** Descarta el cambio local rechazado y vuelve a traer los datos del servidor. */
    fun descartar(idOperacion: Int) {
        viewModelScope.launch {
            repositorio.descartar(idOperacion)
            sincronizar(silencioso = true)
        }
    }

    /** Prueba de la exposición: hace que la API responda con error 503. */
    fun cambiarSimulacionError(activa: Boolean) {
        simularError.set(activa)
        _estado.update { it.copy(simularErrorServidor = activa) }
    }

    fun limpiarMensaje() {
        _estado.update { it.copy(mensaje = null) }
    }

    private fun mostrar(texto: String, esError: Boolean) {
        _estado.update { it.copy(mensaje = texto, mensajeEsError = esError) }
    }

    companion object {
        fun factory(
            repositorio: SyncRepository,
            conectividad: ConnectivityObserver,
            simularError: AtomicBoolean,
            obtenerUbicacion: suspend () -> Ubicacion?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SyncViewModel(repositorio, conectividad, simularError, obtenerUbicacion) }
        }
    }
}

/** Qué registró el courier, para la lista de la cola. */
fun describirOperacion(tipo: String): String = when (tipo) {
    TipoOperacion.EVENTO -> "Avance (recojo, tránsito o reparto)"
    TipoOperacion.EVIDENCIA -> "Entrega con foto y firma"
    TipoOperacion.FALLIDO -> "Entrega fallida con foto"
    TipoOperacion.REORDENAR -> "Orden de la ruta"
    else -> tipo
}

/** Texto que resume una sincronización para el courier. */
fun describirSincronizacion(resultado: ResultadoSync): String {
    val enviadas = if (resultado.enviadas > 0) "${resultado.enviadas} cambio(s) enviado(s)" else null
    val rechazadas =
        if (resultado.rechazadas > 0) "${resultado.rechazadas} cambio(s) rechazado(s) por el servidor" else null
    val descarga = "${resultado.descargados} paradas en tu ruta."
    return when {
        resultado.error != null -> {
            val hecho = listOfNotNull(enviadas, rechazadas)
            "No se completó: ${resultado.error}" + if (hecho.isEmpty()) "" else " (${hecho.joinToString(", ")})"
        }

        // Un rechazo no es un éxito: no se dice "Sincronizado".
        rechazadas != null ->
            "Sincronización terminada con $rechazadas." + (enviadas?.let { " $it." } ?: "") + " $descarga"

        enviadas != null -> "Sincronizado: $enviadas. $descarga"
        else -> "Todo al día. $descarga"
    }
}
