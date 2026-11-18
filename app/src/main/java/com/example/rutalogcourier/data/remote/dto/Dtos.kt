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

/** Ingreso con Google: ID token de Firebase y la app desde la que se entra. */
data class SocialRequest(
    val idToken: String,
    val app: String
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
    val envio: EnvioDto,
    /** RF-C05: en las entregas contraentrega, el cobro QR ya creado en Banco Andino (sirve sin señal). */
    val cobro: CobroDto? = null,
    /** ML-01 "prioridad en la ruta": riesgo de que la entrega falle (null en los recojos). */
    val riesgo: RiesgoParadaDto? = null
)

data class RiesgoParadaDto(val probabilidad: Double, val riesgoAlto: Boolean, val factores: List<String>?)

/** Cobro contraentrega. Estados: POR_COBRAR, PENDIENTE_CONFIRMACION, CONFIRMADO, RECHAZADO, LIQUIDADO. */
data class CobroDto(
    val id: String,
    val monto: Double,
    val estado: String,
    /** Texto del QR (BANCOANDINO:QR-...), solo mientras está por cobrar. */
    val codigoQr: String?,
    val nroOperacion: String?,
    val motivoRechazo: String?
)

/** RF-C06: cobro sin señal con el n.º de operación que muestra la app del banco y la foto del comprobante. */
data class CobroOfflineRequest(
    val uuidOperacion: String,
    val nroOperacion: String,
    val comprobante: String,
    val fechaHora: String
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
