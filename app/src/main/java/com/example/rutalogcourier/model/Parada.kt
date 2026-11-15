package com.example.rutalogcourier.model

/** Qué tiene que hacer el courier con el envío. */
enum class TipoParada(val codigo: String, val etiqueta: String) {
    RECOJO("recojo", "Recojo"),
    ENTREGA("entrega", "Entrega"),
    /** Recogido o en tránsito: va camino a la ciudad de destino, todavía no es parada. */
    EN_CAMINO("en_camino", "En camino"),
    /** Entregado o fallido en este teléfono, a la espera de sincronizar. */
    COMPLETADA("completada", "Completada");

    companion object {
        fun desde(codigo: String): TipoParada = entries.firstOrNull { it.codigo == codigo } ?: EN_CAMINO

        /** Pendiente → recojo; en reparto o reprogramado → entrega; recogido o en tránsito → en camino. */
        fun segunEstado(estado: EstadoEnvio): TipoParada = when (estado) {
            EstadoEnvio.PENDIENTE -> RECOJO
            EstadoEnvio.EN_REPARTO, EstadoEnvio.REPROGRAMADO -> ENTREGA
            EstadoEnvio.RECOGIDO, EstadoEnvio.EN_TRANSITO -> EN_CAMINO
            EstadoEnvio.ENTREGADO, EstadoEnvio.FALLIDO, EstadoEnvio.DEVUELTO -> COMPLETADA
        }
    }
}

/**
 * Una parada de la ruta del día: el envío con lo que el courier necesita para recogerlo o entregarlo.
 * El courier solo ve nombre, dirección y teléfono del destinatario (ética del caso).
 */
data class Parada(
    /** Id del envío en el servidor. */
    val idRemoto: Int,
    val tipo: TipoParada,
    /** Posición en la ruta (1, 2, 3…); las que no son paradas van al final. */
    val orden: Int,
    val numeroGuia: String,
    val ruta: String,
    val estado: EstadoEnvio,
    val version: Int,
    val destinatarioNombre: String?,
    val destinatarioTelefono: String?,
    val destinatarioDireccion: String?,
    /** Monto que se cobra al destinatario (RN-02: sin cobro confirmado no se entrega). */
    val montoContraentrega: Double,
    val intentos: Int,
    val latitud: Double?,
    val longitud: Double?,
    val kmDesdeAnterior: Double?,
    val estadoSync: EstadoSincronizacion,
    val mensajeError: String?
) {
    val esParada: Boolean get() = tipo == TipoParada.RECOJO || tipo == TipoParada.ENTREGA
    val cobraContraentrega: Boolean get() = montoContraentrega > 0.0
    val tieneUbicacion: Boolean get() = latitud != null && longitud != null
}

/** Resumen de la ruta del día: km con el orden de ML-04 frente al orden de registro (métrica de la ficha). */
data class ResumenRuta(
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

/** Motivos de una entrega fallida (los mismos de la API). */
enum class MotivoFalla(val codigo: String, val etiqueta: String) {
    AUSENTE("ausente", "Destinatario ausente"),
    DIRECCION_ERRADA("direccion_errada", "Dirección errada"),
    RECHAZADO("rechazado", "Rechazado por el destinatario"),
    ZONA_INACCESIBLE("zona_inaccesible", "Zona inaccesible")
}
