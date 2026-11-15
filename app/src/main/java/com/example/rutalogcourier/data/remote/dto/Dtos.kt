package com.example.rutalogcourier.data.remote.dto

/** Formato JSON de la API REST. Los nombres coinciden con los que envía y recibe el servidor. */

data class LoginRequest(
    val correo: String,
    val clave: String
)

data class UsuarioDto(
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String
)

data class LoginResponse(
    val token: String,
    val usuario: UsuarioDto
)

data class EnvioDto(
    val id: Int,
    val uuid: String,
    val numeroGuia: String,
    val ruta: String,
    val estado: String,
    val version: Int,
    val destinatarioNombre: String?,
    val destinatarioTelefono: String?,
    val destinatarioDireccion: String?,
    val montoContraentrega: Double?,
    val intentos: Int?,
    val origenLatitud: Double?,
    val origenLongitud: Double?,
    val destinoLatitud: Double?,
    val destinoLongitud: Double?
)

data class PuntoDto(
    val latitud: Double,
    val longitud: Double
)

data class ParadaDto(
    val orden: Int,
    /** "recojo" o "entrega". */
    val tipo: String,
    val latitud: Double?,
    val longitud: Double?,
    val kmDesdeAnterior: Double?,
    val envio: EnvioDto
)

/** GET /api/courier/ruta: paradas ordenadas con ML-04 y lo que va en camino. */
data class RutaDto(
    val fecha: String,
    val inicio: PuntoDto,
    val ordenManual: Boolean,
    val kmRecorrido: Double,
    val kmOptimizado: Double,
    val kmOrdenRegistro: Double,
    val ahorroPorcentaje: Int,
    val paradas: List<ParadaDto>,
    val enCamino: List<EnvioDto>
)

/** POST /api/envios/{id}/eventos: recogido (con la guía escaneada), en tránsito o en reparto. */
data class EventoRequest(
    val uuidOperacion: String,
    val estado: String,
    val guia: String?,
    /** Hora real del evento en ISO-8601 (RN-06). */
    val fechaHora: String,
    val latitud: Double?,
    val longitud: Double?
)

/** POST /api/envios/{id}/evidencia: foto (JPEG) y firma (PNG) en base64, nombre y DNI de quien recibe. */
data class EvidenciaRequest(
    val uuidOperacion: String,
    val foto: String,
    val firma: String,
    val recibeNombre: String,
    val recibeDni: String,
    val fechaHora: String,
    val latitud: Double?,
    val longitud: Double?
)

/** POST /api/envios/{id}/fallido: motivo y foto de la fachada. */
data class FallidoRequest(
    val uuidOperacion: String,
    val motivo: String,
    val foto: String,
    val fechaHora: String,
    val latitud: Double?,
    val longitud: Double?
)

data class ParadaOrdenDto(
    val envioId: Int,
    val tipo: String
)

/** PUT /api/courier/ruta/orden: el orden que eligió el courier (RF-C02). */
data class ReordenarRequest(
    val uuidOperacion: String,
    val paradas: List<ParadaOrdenDto>
)

/** Cuerpo de las respuestas de error. En un conflicto (409) incluye el envío real del servidor. */
data class ErrorDto(
    val codigo: String?,
    val mensaje: String?,
    val envio: EnvioDto?
)
