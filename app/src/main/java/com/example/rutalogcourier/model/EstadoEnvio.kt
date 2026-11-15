package com.example.rutalogcourier.model

/**
 * Estados de un envío (los mismos de la API y de las otras dos apps). En Room se guarda el [codigo].
 * El courier registra: recogido (escaneando la guía), en tránsito, en reparto, entregado (con foto
 * y firma) o entrega fallida. DEVUELTO llega solo al tercer intento fallido (RN-05).
 */
enum class EstadoEnvio(
    val codigo: String,
    val etiqueta: String,
    val paso: Int,
    val descripcion: String
) {
    PENDIENTE("pendiente", "Por recoger", 0, "La tienda espera el recojo."),
    RECOGIDO("recogido", "Recogido", 1, "La carga fue recogida con la guía escaneada."),
    EN_TRANSITO("en_transito", "En tránsito", 2, "La carga va en camino a la ciudad de destino."),
    EN_REPARTO("en_reparto", "En reparto", 3, "La carga salió a reparto con el destinatario."),
    ENTREGADO("entregado", "Entregado", 4, "Entregado con foto y firma del destinatario."),
    FALLIDO("fallido", "Entrega fallida", 3, "No se pudo entregar. El operador la reprogramará."),
    REPROGRAMADO("reprogramado", "Reprogramado", 2, "La entrega fallida se reprogramó: vuelve a salir a reparto."),
    DEVUELTO("devuelto", "Devuelto", 4, "Tres intentos fallidos: la carga vuelve al remitente.");

    companion object {
        /** RN-05: al tercer intento fallido el envío vuelve al remitente. */
        const val MAX_INTENTOS = 3

        fun desde(codigo: String): EstadoEnvio = entries.firstOrNull { it.codigo == codigo } ?: PENDIENTE
    }
}
