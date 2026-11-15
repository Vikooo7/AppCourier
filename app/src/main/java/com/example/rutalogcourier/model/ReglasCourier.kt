package com.example.rutalogcourier.model

/** Lo que el courier puede registrar sobre una parada, según su estado. */
enum class AccionCourier(val etiqueta: String) {
    ESCANEAR_RECOJO("Escanear guía y recoger"),
    MARCAR_EN_TRANSITO("Salir a la ciudad de destino"),
    SALIR_A_REPARTO("Salir a reparto"),
    ENTREGAR("Entregar con foto y firma"),
    REGISTRAR_FALLIDA("Entrega fallida")
}

/**
 * Reglas de la App Courier. Son las mismas que valida la API, así lo registrado sin señal
 * no se rechaza después al sincronizar.
 */
object ReglasCourier {

    /** El avance que sigue al estado actual (RF-C03). La entrega se cierra aparte, con evidencia. */
    fun siguienteEstado(actual: EstadoEnvio): EstadoEnvio? = when (actual) {
        EstadoEnvio.PENDIENTE -> EstadoEnvio.RECOGIDO
        EstadoEnvio.RECOGIDO -> EstadoEnvio.EN_TRANSITO
        EstadoEnvio.EN_TRANSITO, EstadoEnvio.REPROGRAMADO -> EstadoEnvio.EN_REPARTO
        else -> null
    }

    fun acciones(parada: Parada): List<AccionCourier> = when (parada.estado) {
        EstadoEnvio.PENDIENTE -> listOf(AccionCourier.ESCANEAR_RECOJO)
        EstadoEnvio.RECOGIDO -> listOf(AccionCourier.MARCAR_EN_TRANSITO)
        EstadoEnvio.EN_TRANSITO, EstadoEnvio.REPROGRAMADO -> listOf(AccionCourier.SALIR_A_REPARTO)
        EstadoEnvio.EN_REPARTO -> listOf(AccionCourier.ENTREGAR, AccionCourier.REGISTRAR_FALLIDA)
        else -> emptyList()
    }

    /** RN-02: un contraentrega no se entrega sin cobro confirmado; el cobro con QR llega en la fase de pagos. */
    fun motivoNoEntregar(parada: Parada): String? =
        if (parada.cobraContraentrega) {
            "Primero hay que cobrar ${"S/ %.2f".format(parada.montoContraentrega)} con QR al destinatario. " +
                "El cobro contraentrega se activa en la fase de pagos con Banco Andino."
        } else {
            null
        }

    /** HU-C03: nombre y DNI de quien recibe; la foto y la firma son obligatorias (RN-03). */
    fun validarEntrega(recibeNombre: String, recibeDni: String, tieneFoto: Boolean, tieneFirma: Boolean): String? = when {
        !tieneFoto -> "Toma la foto de la entrega."
        !tieneFirma -> "Pide al destinatario que firme en la pantalla."
        recibeNombre.isBlank() -> "Escribe el nombre de quien recibe."
        recibeNombre.trim().length > 80 -> "El nombre admite hasta 80 caracteres."
        !Regex("^\\d{8}$").matches(recibeDni.trim()) -> "El DNI de quien recibe debe tener 8 dígitos."
        else -> null
    }

    /** HU-C05: motivo y foto de la fachada. */
    fun validarFallida(motivo: MotivoFalla?, tieneFoto: Boolean): String? = when {
        motivo == null -> "Elige el motivo."
        !tieneFoto -> "Toma una foto de la fachada como evidencia."
        else -> null
    }

    /** RN-05: si este es el tercer intento, el envío vuelve al remitente. */
    fun estadoTrasFallida(intentosPrevios: Int): EstadoEnvio =
        if (intentosPrevios + 1 >= EstadoEnvio.MAX_INTENTOS) EstadoEnvio.DEVUELTO else EstadoEnvio.FALLIDO
}
