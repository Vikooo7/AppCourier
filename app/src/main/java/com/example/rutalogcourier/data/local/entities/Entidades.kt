package com.example.rutalogcourier.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Perfil del courier autorizado por la API. No se guarda la contraseña: la sesión es un token cifrado. */
@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String
)

/**
 * Tabla "paradas": la ruta del día descargada de la API, con lo que el courier necesita sin señal.
 * Guarda las coordenadas de recojo y de entrega: al cambiar de estado, la parada pasa de una a otra.
 */
@Entity(tableName = "paradas")
data class ParadaEntity(
    /** Id del envío en el servidor. */
    @PrimaryKey val idRemoto: Int,
    /** Código de TipoParada. */
    val tipo: String,
    val orden: Int,
    val numeroGuia: String,
    val ruta: String,
    /** Código de EstadoEnvio. */
    val estado: String,
    val version: Int,
    val destinatarioNombre: String?,
    val destinatarioTelefono: String?,
    val destinatarioDireccion: String?,
    val montoContraentrega: Double,
    val intentos: Int,
    val origenLatitud: Double?,
    val origenLongitud: Double?,
    val destinoLatitud: Double?,
    val destinoLongitud: Double?,
    val kmDesdeAnterior: Double?,
    /** Nombre de EstadoSincronizacion. */
    val estadoSync: String,
    val mensajeError: String? = null,
    /** RF-C05 / RF-C06: cobro contraentrega de esta entrega (null si no es contraentrega o no llegó). */
    val cobroId: String? = null,
    val cobroEstado: String? = null,
    val cobroQr: String? = null,
    val cobroNro: String? = null,
    /** ML-01: probabilidad de falla, si es riesgo alto y sus razones separadas por " · " (null en recojos). */
    val riesgoProbabilidad: Double? = null,
    val riesgoAlto: Boolean? = null,
    val riesgoFactores: String? = null
)

/** Resumen de la ruta del día (una sola fila, id = 1). */
@Entity(tableName = "resumen_ruta")
data class ResumenRutaEntity(
    @PrimaryKey val id: Int = 1,
    val fecha: String,
    val kmRecorrido: Double,
    val kmOptimizado: Double,
    val kmOrdenRegistro: Double,
    val ahorroPorcentaje: Int,
    val ordenManual: Boolean,
    val inicioLatitud: Double,
    val inicioLongitud: Double,
    val actualizadoEn: Long
)

/**
 * Cola de lo que el courier registró y falta enviar (RF-C08): avances, entregas, fallidas y orden manual.
 * Cada operación lleva su UUID (el servidor no la aplica dos veces) y la hora real en que ocurrió (RN-06).
 * Las fotos y la firma se guardan como archivos del teléfono; el payload lleva su ruta.
 */
@Entity(tableName = "operaciones_pendientes")
data class OperacionPendienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuidOperacion: String,
    /** Código de TipoOperacion. */
    val tipoOperacion: String,
    /** Id remoto del envío; 0 para el orden manual de la ruta. */
    val idRemoto: Int,
    /** Datos de la operación en JSON. */
    val payload: String,
    /** Hora real en que el courier lo registró (milisegundos). */
    val fechaRegistro: Long,
    /** Nombre de EstadoSincronizacion. */
    val estado: String,
    val intentos: Int = 0,
    val mensajeError: String? = null
)

object TipoOperacion {
    const val EVENTO = "EVENTO"
    const val EVIDENCIA = "EVIDENCIA"
    const val FALLIDO = "FALLIDO"
    const val REORDENAR = "REORDENAR"
    const val COBRO_OFFLINE = "COBRO_OFFLINE"
}

/** Última sincronización exitosa. */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val recurso: String,
    val ultimaSincronizacionExitosa: Long
)
