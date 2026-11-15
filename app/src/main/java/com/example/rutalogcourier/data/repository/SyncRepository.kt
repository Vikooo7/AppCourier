package com.example.rutalogcourier.data.repository

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import com.example.rutalogcourier.data.local.AppDatabase
import com.example.rutalogcourier.data.local.dao.OperacionConGuia
import com.example.rutalogcourier.data.local.entities.OperacionPendienteEntity
import com.example.rutalogcourier.data.local.entities.ParadaEntity
import com.example.rutalogcourier.data.local.entities.ResumenRutaEntity
import com.example.rutalogcourier.data.local.entities.SyncMetadataEntity
import com.example.rutalogcourier.data.local.entities.TipoOperacion
import com.example.rutalogcourier.data.remote.ResultadoApi
import com.example.rutalogcourier.data.remote.api.ApiService
import com.example.rutalogcourier.data.remote.dto.EnvioDto
import com.example.rutalogcourier.data.remote.dto.EventoRequest
import com.example.rutalogcourier.data.remote.dto.EvidenciaRequest
import com.example.rutalogcourier.data.remote.dto.FallidoRequest
import com.example.rutalogcourier.data.remote.dto.ReordenarRequest
import com.example.rutalogcourier.data.remote.dto.RutaDto
import com.example.rutalogcourier.data.remote.llamarApi
import com.example.rutalogcourier.model.EstadoEnvio
import com.example.rutalogcourier.model.EstadoSincronizacion
import com.example.rutalogcourier.model.TipoParada
import com.example.rutalogcourier.util.Ubicacion
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Resumen de una sincronización, para mostrarlo en pantalla. */
data class ResultadoSync(
    val enviadas: Int = 0,
    val rechazadas: Int = 0,
    /** Paradas de la ruta del día después de descargarla. */
    val descargados: Int = 0,
    /** Paradas que no estaban antes (para avisar al courier). */
    val nuevas: Int = 0,
    /** null si todo salió bien; si no, el motivo por el que se detuvo. */
    val error: String? = null
)

/**
 * Sincronización con la API REST (RF-C08):
 * 1. Envía, en el orden en que ocurrieron, los registros de la cola. Cada uno lleva su UUID y su
 *    hora real (RN-06): reenviarlo no lo duplica y el servidor lo aplica con la hora en que pasó.
 * 2. Descarga la ruta del día (ordenada con ML-04) sin pisar las paradas con registros por enviar.
 *
 * Si el servidor rechaza un registro (por ejemplo, la guía no coincide o falta el cobro), queda en
 * ERROR con el motivo y sus fotos se conservan, para que el courier reintente o descarte.
 */
class SyncRepository(
    private val db: AppDatabase,
    private val api: ApiService,
    private val context: Context
) {

    private val paradaDao = db.paradaDao()
    private val operacionDao = db.operacionPendienteDao()
    private val gson = Gson()

    /** Evita que el botón manual y el SyncWorker sincronicen al mismo tiempo. */
    private val candado = Mutex()

    fun observarOperaciones(): Flow<List<OperacionConGuia>> = operacionDao.observarConGuia()

    fun observarUltimaSincronizacion(): Flow<Long?> =
        operacionDao.observarMetadata(RECURSO_RUTA).map { it?.ultimaSincronizacionExitosa }

    suspend fun sincronizar(ubicacion: Ubicacion? = null): ResultadoSync = candado.withLock {
        operacionDao.recuperarInterrumpidas()
        paradaDao.recuperarInterrumpidas()

        var enviadas = 0
        var rechazadas = 0

        for (operacion in operacionDao.porEnviar()) {
            marcar(operacion, EstadoSincronizacion.ENVIANDO)
            when (val resultado = enviar(operacion)) {
                is ResultadoApi.Exito -> {
                    aplicarExito(operacion, resultado.valor)
                    enviadas++
                }

                is ResultadoApi.Rechazo -> {
                    if (resultado.http == 401) {
                        // Sesión vencida: no es un rechazo del registro, que sigue en la cola.
                        marcar(operacion, EstadoSincronizacion.PENDIENTE)
                        return@withLock ResultadoSync(enviadas, rechazadas, error = resultado.mensaje)
                    }
                    aplicarRechazo(operacion, resultado)
                    rechazadas++
                }

                is ResultadoApi.Fallo -> {
                    // Sin señal o servidor caído: el registro sigue en la cola para otro intento.
                    db.withTransaction {
                        operacionDao.actualizar(
                            operacion.copy(
                                estado = EstadoSincronizacion.PENDIENTE.name,
                                intentos = operacion.intentos + 1,
                                mensajeError = resultado.mensaje
                            )
                        )
                        marcarParada(operacion.idRemoto, EstadoSincronizacion.PENDIENTE)
                    }
                    return@withLock ResultadoSync(enviadas, rechazadas, error = resultado.mensaje)
                }
            }
        }

        descargar(enviadas, rechazadas, ubicacion)
    }

    /** El courier quiere volver a intentarlo: el registro regresa a la cola. */
    suspend fun reintentar(idOperacion: Int) {
        db.withTransaction {
            val operacion = operacionDao.porId(idOperacion) ?: return@withTransaction
            operacionDao.actualizar(operacion.copy(estado = EstadoSincronizacion.PENDIENTE.name, mensajeError = null))
            marcarParada(operacion.idRemoto, EstadoSincronizacion.PENDIENTE)
        }
    }

    /**
     * El courier renuncia a un registro rechazado: se borra con sus fotos y la parada queda lista
     * para que la próxima descarga la deje igual que en el servidor.
     */
    suspend fun descartar(idOperacion: Int) {
        db.withTransaction {
            val operacion = operacionDao.porId(idOperacion) ?: return@withTransaction
            borrarArchivos(operacion)
            operacionDao.eliminar(operacion)
            if (operacionDao.deEnvio(operacion.idRemoto).isEmpty()) {
                paradaDao.porId(operacion.idRemoto)?.let {
                    paradaDao.actualizar(it.copy(estadoSync = EstadoSincronizacion.SINCRONIZADO.name, mensajeError = null))
                }
            }
        }
    }

    private suspend fun marcar(operacion: OperacionPendienteEntity, estado: EstadoSincronizacion) {
        db.withTransaction {
            operacionDao.actualizar(operacion.copy(estado = estado.name))
            marcarParada(operacion.idRemoto, estado)
        }
    }

    private suspend fun marcarParada(idRemoto: Int, estado: EstadoSincronizacion, mensaje: String? = null) {
        paradaDao.porId(idRemoto)?.let { paradaDao.actualizar(it.copy(estadoSync = estado.name, mensajeError = mensaje)) }
    }

    /** Envía un registro. Devuelve el envío como quedó en el servidor (null en el orden manual). */
    private suspend fun enviar(operacion: OperacionPendienteEntity): ResultadoApi<EnvioDto?> {
        val id = operacion.idRemoto
        val uuid = operacion.uuidOperacion
        return when (operacion.tipoOperacion) {
            TipoOperacion.EVENTO -> {
                val d = gson.fromJson(operacion.payload, PayloadEvento::class.java)
                llamarApi { api.registrarEvento(id, EventoRequest(uuid, d.estado, d.guia, d.fechaHora, d.latitud, d.longitud)) }
            }

            TipoOperacion.EVIDENCIA -> {
                val d = gson.fromJson(operacion.payload, PayloadEvidencia::class.java)
                val foto = base64(d.archivoFoto) ?: return faltaArchivo()
                val firma = base64(d.archivoFirma) ?: return faltaArchivo()
                llamarApi {
                    api.registrarEvidencia(
                        id,
                        EvidenciaRequest(uuid, foto, firma, d.recibeNombre, d.recibeDni, d.fechaHora, d.latitud, d.longitud)
                    )
                }
            }

            TipoOperacion.FALLIDO -> {
                val d = gson.fromJson(operacion.payload, PayloadFallido::class.java)
                val foto = base64(d.archivoFoto) ?: return faltaArchivo()
                llamarApi { api.registrarFallido(id, FallidoRequest(uuid, d.motivo, foto, d.fechaHora, d.latitud, d.longitud)) }
            }

            else -> {
                val d = gson.fromJson(operacion.payload, PayloadReorden::class.java)
                when (val r = llamarApi { api.reordenar(ReordenarRequest(uuid, d.paradas)) }) {
                    is ResultadoApi.Exito -> ResultadoApi.Exito(null)
                    is ResultadoApi.Rechazo -> r
                    is ResultadoApi.Fallo -> r
                }
            }
        }
    }

    private fun faltaArchivo(): ResultadoApi<EnvioDto?> =
        ResultadoApi.Rechazo(0, "SIN_ARCHIVO", "La foto o la firma ya no está en el teléfono: descarta y vuelve a registrar.")

    private fun base64(nombre: String): String? {
        val archivo = File(File(context.filesDir, RutaRepository.CARPETA), nombre)
        return if (archivo.exists()) Base64.encodeToString(archivo.readBytes(), Base64.NO_WRAP) else null
    }

    private fun borrarArchivos(operacion: OperacionPendienteEntity) {
        val nombres = when (operacion.tipoOperacion) {
            TipoOperacion.EVIDENCIA -> gson.fromJson(operacion.payload, PayloadEvidencia::class.java)
                .let { listOf(it.archivoFoto, it.archivoFirma) }
            TipoOperacion.FALLIDO -> listOf(gson.fromJson(operacion.payload, PayloadFallido::class.java).archivoFoto)
            else -> emptyList()
        }
        nombres.forEach { File(File(context.filesDir, RutaRepository.CARPETA), it).delete() }
    }

    private suspend fun aplicarExito(operacion: OperacionPendienteEntity, remoto: EnvioDto?) {
        db.withTransaction {
            borrarArchivos(operacion)
            operacionDao.eliminar(operacion)
            if (remoto == null) return@withTransaction
            val actual = paradaDao.porId(remoto.id) ?: return@withTransaction
            val quedanPendientes = operacionDao.deEnvio(remoto.id).isNotEmpty()
            paradaDao.actualizar(
                if (quedanPendientes) {
                    // Hay otro registro del mismo envío esperando: se guarda la versión, sin perder lo local.
                    actual.copy(version = remoto.version, estadoSync = EstadoSincronizacion.PENDIENTE.name)
                } else {
                    actual.copy(
                        estado = remoto.estado,
                        version = remoto.version,
                        intentos = remoto.intentos ?: actual.intentos,
                        estadoSync = EstadoSincronizacion.SINCRONIZADO.name,
                        mensajeError = null
                    )
                }
            )
        }
    }

    /** El servidor rechazó el registro. No se pierde: queda en ERROR con el motivo y sus fotos. */
    private suspend fun aplicarRechazo(operacion: OperacionPendienteEntity, rechazo: ResultadoApi.Rechazo) {
        val motivo = rechazo.envio?.let {
            "${rechazo.mensaje} En el servidor: ${EstadoEnvio.desde(it.estado).etiqueta.lowercase()}."
        } ?: rechazo.mensaje
        db.withTransaction {
            operacionDao.actualizar(
                operacion.copy(
                    estado = EstadoSincronizacion.ERROR.name,
                    intentos = operacion.intentos + 1,
                    mensajeError = motivo
                )
            )
            marcarParada(operacion.idRemoto, EstadoSincronizacion.ERROR, motivo)
        }
    }

    /** RF-C01: descarga la ruta del día. Si algo falla, lo guardado en Room no se toca. */
    private suspend fun descargar(enviadas: Int, rechazadas: Int, ubicacion: Ubicacion?): ResultadoSync =
        when (val respuesta = llamarApi { api.obtenerRuta(ubicacion?.latitud, ubicacion?.longitud) }) {
            is ResultadoApi.Exito -> {
                val nuevas = guardarDescarga(respuesta.valor)
                ResultadoSync(enviadas, rechazadas, descargados = respuesta.valor.paradas.size, nuevas = nuevas)
            }

            is ResultadoApi.Rechazo -> ResultadoSync(enviadas, rechazadas, error = respuesta.mensaje)
            is ResultadoApi.Fallo -> ResultadoSync(enviadas, rechazadas, error = respuesta.mensaje)
        }

    /** Guarda la ruta descargada sin pisar las paradas que tienen registros por enviar. Devuelve cuántas son nuevas. */
    private suspend fun guardarDescarga(ruta: RutaDto): Int = db.withTransaction {
        val locales = paradaDao.todas().associateBy { it.idRemoto }
        val conCambios = operacionDao.todas().map { it.idRemoto }.toSet()

        val remotas = ruta.paradas.map { p -> p.envio.aEntidad(TipoParada.desde(p.tipo), p.orden, p.kmDesdeAnterior) } +
            ruta.enCamino.map { it.aEntidad(TipoParada.EN_CAMINO, ORDEN_EN_CAMINO, null) }

        remotas.forEach { remota ->
            val local = locales[remota.idRemoto]
            if (local == null || (remota.idRemoto !in conCambios && local.estadoSync != EstadoSincronizacion.ERROR.name)) {
                paradaDao.guardar(remota)
            }
        }
        // Lo que ya no está en la ruta (entregado, fallido o reasignado) se quita si no espera envío.
        val idsRemotos = remotas.map { it.idRemoto }.toSet()
        locales.values
            .filter { it.idRemoto !in idsRemotos && it.idRemoto !in conCambios }
            .forEach { paradaDao.eliminar(it) }

        val ahora = System.currentTimeMillis()
        paradaDao.guardarResumen(
            ResumenRutaEntity(
                fecha = ruta.fecha,
                kmRecorrido = ruta.kmRecorrido,
                kmOptimizado = ruta.kmOptimizado,
                kmOrdenRegistro = ruta.kmOrdenRegistro,
                ahorroPorcentaje = ruta.ahorroPorcentaje,
                ordenManual = ruta.ordenManual,
                inicioLatitud = ruta.inicio.latitud,
                inicioLongitud = ruta.inicio.longitud,
                actualizadoEn = ahora
            )
        )
        operacionDao.guardarMetadata(SyncMetadataEntity(RECURSO_RUTA, ahora))

        // En la primera descarga todo es "nuevo": solo se avisa de lo que llega después.
        if (locales.isEmpty()) 0 else ruta.paradas.count { it.envio.id !in locales }
    }

    private fun EnvioDto.aEntidad(tipo: TipoParada, orden: Int, km: Double?) = ParadaEntity(
        idRemoto = id,
        tipo = tipo.codigo,
        orden = orden,
        numeroGuia = numeroGuia,
        ruta = ruta,
        estado = estado,
        version = version,
        destinatarioNombre = destinatarioNombre,
        destinatarioTelefono = destinatarioTelefono,
        destinatarioDireccion = destinatarioDireccion,
        montoContraentrega = montoContraentrega ?: 0.0,
        intentos = intentos ?: 0,
        origenLatitud = origenLatitud,
        origenLongitud = origenLongitud,
        destinoLatitud = destinoLatitud,
        destinoLongitud = destinoLongitud,
        kmDesdeAnterior = km,
        estadoSync = EstadoSincronizacion.SINCRONIZADO.name
    )

    private companion object {
        const val RECURSO_RUTA = "ruta"
    }
}
