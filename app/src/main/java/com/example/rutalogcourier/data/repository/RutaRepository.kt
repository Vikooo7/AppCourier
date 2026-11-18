package com.example.rutalogcourier.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.rutalogcourier.data.local.AppDatabase
import com.example.rutalogcourier.data.local.entities.OperacionPendienteEntity
import com.example.rutalogcourier.data.local.entities.ParadaEntity
import com.example.rutalogcourier.data.local.entities.ResumenRutaEntity
import com.example.rutalogcourier.data.local.entities.TipoOperacion
import com.example.rutalogcourier.data.remote.dto.ParadaOrdenDto
import com.example.rutalogcourier.model.EstadoEnvio
import com.example.rutalogcourier.model.EstadoSincronizacion
import com.example.rutalogcourier.model.MotivoFalla
import com.example.rutalogcourier.model.Parada
import com.example.rutalogcourier.model.ReglasCourier
import com.example.rutalogcourier.model.ResumenRuta
import com.example.rutalogcourier.model.TipoParada
import com.example.rutalogcourier.util.Ubicacion
import com.google.gson.Gson
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Datos de cada operación de la cola, tal como se guardan en el campo payload (JSON). */
data class PayloadEvento(
    val estado: String,
    val guia: String?,
    val fechaHora: String,
    val latitud: Double?,
    val longitud: Double?
)

data class PayloadEvidencia(
    val archivoFoto: String,
    val archivoFirma: String,
    val recibeNombre: String,
    val recibeDni: String,
    val fechaHora: String,
    val latitud: Double?,
    val longitud: Double?
)

data class PayloadFallido(
    val motivo: String,
    val archivoFoto: String,
    val fechaHora: String,
    val latitud: Double?,
    val longitud: Double?
)

/** RF-C06: cobro sin señal. */
data class PayloadCobroOffline(
    val nroOperacion: String,
    val archivoComprobante: String,
    val fechaHora: String
)

data class PayloadReorden(
    val paradas: List<ParadaOrdenDto>
)

/** Orden de lo que no es parada: al final de la lista. */
internal const val ORDEN_EN_CAMINO = 900
internal const val ORDEN_COMPLETADA = 950

fun ParadaEntity.aModelo(): Parada {
    val tipoParada = TipoParada.desde(tipo)
    val (latitud, longitud) = when (tipoParada) {
        TipoParada.RECOJO -> origenLatitud to origenLongitud
        TipoParada.ENTREGA, TipoParada.COMPLETADA -> destinoLatitud to destinoLongitud
        TipoParada.EN_CAMINO -> null to null
    }
    return Parada(
        idRemoto = idRemoto,
        tipo = tipoParada,
        orden = orden,
        numeroGuia = numeroGuia,
        ruta = ruta,
        estado = EstadoEnvio.desde(estado),
        version = version,
        destinatarioNombre = destinatarioNombre,
        destinatarioTelefono = destinatarioTelefono,
        destinatarioDireccion = destinatarioDireccion,
        montoContraentrega = montoContraentrega,
        intentos = intentos,
        latitud = latitud,
        longitud = longitud,
        kmDesdeAnterior = kmDesdeAnterior,
        estadoSync = EstadoSincronizacion.desde(estadoSync),
        mensajeError = mensajeError,
        cobroId = cobroId,
        cobroEstado = cobroEstado,
        cobroQr = cobroQr,
        cobroNro = cobroNro,
        riesgoProbabilidad = riesgoProbabilidad,
        riesgoAlto = riesgoAlto == true,
        riesgoFactores = riesgoFactores?.split(" · ")?.filter { it.isNotBlank() }.orEmpty()
    )
}

fun ResumenRutaEntity.aModelo(): ResumenRuta = ResumenRuta(
    fecha, kmRecorrido, kmOptimizado, kmOrdenRegistro, ahorroPorcentaje, ordenManual, inicioLatitud, inicioLongitud, actualizadoEn
)

/**
 * Ruta del día del courier. Las pantallas leen siempre de Room. Lo que registra el courier
 * (avance, entrega, fallida, orden) se aplica en Room y queda en la cola en una sola transacción
 * (RF-C08), con la hora real y el GPS del momento (RN-06). SyncRepository lo envía cuando hay señal.
 */
class RutaRepository(
    private val db: AppDatabase,
    private val context: Context
) {

    private val paradaDao = db.paradaDao()
    private val operacionDao = db.operacionPendienteDao()
    private val gson = Gson()

    fun observarParadas(): Flow<List<Parada>> = paradaDao.observarTodas().map { lista -> lista.map { it.aModelo() } }

    fun observarParada(idRemoto: Int): Flow<Parada?> = paradaDao.observar(idRemoto).map { it?.aModelo() }

    fun observarResumen(): Flow<ResumenRuta?> = paradaDao.observarResumen().map { it?.aModelo() }

    /** RF-C03: recogido (con la guía escaneada), en tránsito o en reparto. */
    suspend fun registrarAvance(idRemoto: Int, nuevoEstado: EstadoEnvio, guia: String?, ubicacion: Ubicacion?) {
        db.withTransaction {
            val actual = paradaDao.porId(idRemoto) ?: return@withTransaction
            val tipo = TipoParada.segunEstado(nuevoEstado)
            val orden = when (tipo) {
                // Al salir a reparto pasa a ser una parada de entrega: va al final de la ruta del día.
                TipoParada.ENTREGA -> (paradaDao.todas().filter { it.orden < ORDEN_EN_CAMINO }.maxOfOrNull { it.orden } ?: 0) + 1
                TipoParada.RECOJO -> actual.orden
                TipoParada.EN_CAMINO -> ORDEN_EN_CAMINO
                TipoParada.COMPLETADA -> ORDEN_COMPLETADA
            }
            paradaDao.actualizar(actual.pendiente(estado = nuevoEstado.codigo, tipo = tipo.codigo, orden = orden))
            encolar(
                TipoOperacion.EVENTO,
                idRemoto,
                PayloadEvento(nuevoEstado.codigo, guia, ahora(), ubicacion?.latitud, ubicacion?.longitud)
            )
        }
    }

    /** RF-C04 / RN-03: entrega con foto, firma, nombre y DNI. Las imágenes se guardan como archivos. */
    suspend fun registrarEntrega(
        idRemoto: Int,
        foto: ByteArray,
        firma: ByteArray,
        recibeNombre: String,
        recibeDni: String,
        ubicacion: Ubicacion?
    ) {
        val archivoFoto = guardarArchivo(foto, "jpg")
        val archivoFirma = guardarArchivo(firma, "png")
        db.withTransaction {
            val actual = paradaDao.porId(idRemoto) ?: return@withTransaction
            paradaDao.actualizar(
                actual.pendiente(estado = EstadoEnvio.ENTREGADO.codigo, tipo = TipoParada.COMPLETADA.codigo, orden = ORDEN_COMPLETADA)
            )
            encolar(
                TipoOperacion.EVIDENCIA,
                idRemoto,
                PayloadEvidencia(
                    archivoFoto,
                    archivoFirma,
                    recibeNombre.trim(),
                    recibeDni.trim(),
                    ahora(),
                    ubicacion?.latitud,
                    ubicacion?.longitud
                )
            )
        }
    }

    /** RF-C07 / HU-C05: entrega fallida con motivo y foto de la fachada. Al tercer intento, DEVUELTO. */
    suspend fun registrarFallida(idRemoto: Int, motivo: MotivoFalla, foto: ByteArray, ubicacion: Ubicacion?) {
        val archivoFoto = guardarArchivo(foto, "jpg")
        db.withTransaction {
            val actual = paradaDao.porId(idRemoto) ?: return@withTransaction
            val estado = ReglasCourier.estadoTrasFallida(actual.intentos)
            paradaDao.actualizar(
                actual.pendiente(estado = estado.codigo, tipo = TipoParada.COMPLETADA.codigo, orden = ORDEN_COMPLETADA)
                    .copy(intentos = actual.intentos + 1)
            )
            encolar(
                TipoOperacion.FALLIDO,
                idRemoto,
                PayloadFallido(motivo.codigo, archivoFoto, ahora(), ubicacion?.latitud, ubicacion?.longitud)
            )
        }
    }

    /**
     * RF-C06: sin señal en la puerta. El destinatario pagó el QR con su app del banco y le muestra el número
     * de operación; el courier lo anota con una foto del comprobante. Queda PENDIENTE_CONFIRMACION (permite
     * entregar) y la API lo verifica con Banco Andino al sincronizar.
     */
    suspend fun registrarCobroOffline(idRemoto: Int, nroOperacion: String, comprobante: ByteArray) {
        val archivo = guardarArchivo(comprobante, "jpg")
        db.withTransaction {
            val actual = paradaDao.porId(idRemoto) ?: return@withTransaction
            paradaDao.actualizar(
                actual.copy(
                    cobroEstado = "PENDIENTE_CONFIRMACION",
                    cobroNro = nroOperacion,
                    estadoSync = EstadoSincronizacion.PENDIENTE.name,
                    mensajeError = null
                )
            )
            encolar(TipoOperacion.COBRO_OFFLINE, idRemoto, PayloadCobroOffline(nroOperacion, archivo, ahora()))
        }
    }

    /**
     * RF-C02: el courier cambia el orden de sus paradas. Se guarda en Room y en la cola; si ya
     * había un orden sin enviar, se reemplaza (al servidor solo le importa el último).
     */
    suspend fun reordenar(idsEnOrden: List<Int>) {
        db.withTransaction {
            val paradas = paradaDao.todas().associateBy { it.idRemoto }
            idsEnOrden.forEachIndexed { indice, id ->
                paradas[id]?.let { paradaDao.actualizar(it.copy(orden = indice + 1, kmDesdeAnterior = null)) }
            }
            val orden = PayloadReorden(idsEnOrden.mapNotNull { id -> paradas[id]?.let { ParadaOrdenDto(id, it.tipo) } })
            val pendiente = operacionDao.todas().lastOrNull {
                it.tipoOperacion == TipoOperacion.REORDENAR && it.estado != EstadoSincronizacion.ENVIANDO.name
            }
            if (pendiente != null) {
                operacionDao.actualizar(
                    pendiente.copy(payload = gson.toJson(orden), estado = EstadoSincronizacion.PENDIENTE.name, mensajeError = null)
                )
            } else {
                encolar(TipoOperacion.REORDENAR, 0, orden)
            }
        }
    }

    private fun ParadaEntity.pendiente(estado: String, tipo: String, orden: Int) = copy(
        estado = estado,
        tipo = tipo,
        orden = orden,
        estadoSync = EstadoSincronizacion.PENDIENTE.name,
        mensajeError = null
    )

    private suspend fun encolar(tipo: String, idRemoto: Int, payload: Any) {
        operacionDao.insertar(
            OperacionPendienteEntity(
                uuidOperacion = UUID.randomUUID().toString(),
                tipoOperacion = tipo,
                idRemoto = idRemoto,
                payload = gson.toJson(payload),
                fechaRegistro = System.currentTimeMillis(),
                estado = EstadoSincronizacion.PENDIENTE.name
            )
        )
    }

    /** Guarda la imagen en la carpeta privada de la app; la cola guarda solo el nombre del archivo. */
    private fun guardarArchivo(contenido: ByteArray, extension: String): String {
        val carpeta = File(context.filesDir, CARPETA).apply { mkdirs() }
        val archivo = File(carpeta, "${UUID.randomUUID()}.$extension")
        archivo.writeBytes(contenido)
        return archivo.name
    }

    /** Hora real en ISO-8601 con milisegundos, por ejemplo 2026-11-14T15:20:31.482Z. */
    private fun ahora(): String = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString()

    companion object {
        const val CARPETA = "evidencias"
    }
}
