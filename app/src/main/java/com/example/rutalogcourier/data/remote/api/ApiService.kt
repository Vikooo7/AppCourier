package com.example.rutalogcourier.data.remote.api

import com.example.rutalogcourier.data.remote.dto.EnvioDto
import com.example.rutalogcourier.data.remote.dto.CobroDto
import com.example.rutalogcourier.data.remote.dto.CobroOfflineRequest
import com.example.rutalogcourier.data.remote.dto.EventoRequest
import com.example.rutalogcourier.data.remote.dto.EvidenciaRequest
import com.example.rutalogcourier.data.remote.dto.FallidoRequest
import com.example.rutalogcourier.data.remote.dto.LoginRequest
import com.example.rutalogcourier.data.remote.dto.LoginResponse
import com.example.rutalogcourier.data.remote.dto.SocialRequest
import com.example.rutalogcourier.data.remote.dto.ReordenarRequest
import com.example.rutalogcourier.data.remote.dto.RutaDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints de la API REST de RutaLog que usa el courier. La app nunca habla con el banco. */
interface ApiService {

    /** RF-C05: estado del cobro; la API pregunta en ese momento a Banco Andino. */
    @GET("api/pagos/{id}")
    suspend fun consultarCobro(@Path("id") id: String): Response<CobroDto>

    /** RF-C06: cobro registrado sin señal; la API lo verifica con el banco. */
    @POST("api/envios/{id}/cobro-offline")
    suspend fun registrarCobroOffline(@Path("id") id: Int, @Body cuerpo: CobroOfflineRequest): Response<CobroDto>

    @POST("api/auth/login")
    suspend fun login(@Body cuerpo: LoginRequest): Response<LoginResponse>

    /** La API valida el token de Firebase (Google) y entrega su propia sesión. */
    @POST("api/auth/social")
    suspend fun loginSocial(@Body cuerpo: SocialRequest): Response<LoginResponse>

    /** RF-C01: ruta del día ordenada con ML-04, desde la ubicación actual si se conoce. */
    @GET("api/courier/ruta")
    suspend fun obtenerRuta(
        @Query("latitud") latitud: Double?,
        @Query("longitud") longitud: Double?
    ): Response<RutaDto>

    /** RF-C02: orden elegido por el courier. */
    @PUT("api/courier/ruta/orden")
    suspend fun reordenar(@Body cuerpo: ReordenarRequest): Response<RutaDto>

    /** RF-C03: recogido (con la guía escaneada), en tránsito o en reparto. */
    @POST("api/envios/{id}/eventos")
    suspend fun registrarEvento(@Path("id") id: Int, @Body cuerpo: EventoRequest): Response<EnvioDto>

    /** RF-C04: entrega con foto, firma, nombre y DNI. */
    @POST("api/envios/{id}/evidencia")
    suspend fun registrarEvidencia(@Path("id") id: Int, @Body cuerpo: EvidenciaRequest): Response<EnvioDto>

    /** RF-C07: entrega fallida con motivo y foto de la fachada. */
    @POST("api/envios/{id}/fallido")
    suspend fun registrarFallido(@Path("id") id: Int, @Body cuerpo: FallidoRequest): Response<EnvioDto>
}
