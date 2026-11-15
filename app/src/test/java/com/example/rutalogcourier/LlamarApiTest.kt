package com.example.rutalogcourier

import com.example.rutalogcourier.data.remote.ResultadoApi
import com.example.rutalogcourier.data.remote.dto.EnvioDto
import com.example.rutalogcourier.data.remote.dto.PuntoDto
import com.example.rutalogcourier.data.remote.dto.RutaDto
import com.example.rutalogcourier.data.remote.llamarApi
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** Cómo clasifica la app las respuestas de la API: éxito, rechazo del servidor o fallo reintentable. */
class LlamarApiTest {

    private fun cuerpoError(json: String) = ResponseBody.create(MediaType.parse("application/json"), json)

    @Test
    fun respuestaCorrecta_esExito() = runBlocking<Unit> {
        val ruta = RutaDto("2026-11-14", PuntoDto(-12.0, -77.0), false, 10.0, 10.0, 12.5, 20, emptyList(), emptyList())
        val resultado = llamarApi { Response.success(ruta) }

        assertTrue(resultado is ResultadoApi.Exito)
        assertEquals(20, (resultado as ResultadoApi.Exito).valor.ahorroPorcentaje)
    }

    @Test
    fun guiaQueNoCoincide_esRechazo422() = runBlocking<Unit> {
        val json = """{"codigo":"GUIA_NO_COINCIDE","mensaje":"La guía escaneada no corresponde a esta parada."}"""

        val resultado = llamarApi<EnvioDto> { Response.error(422, cuerpoError(json)) }

        assertTrue(resultado is ResultadoApi.Rechazo)
        assertEquals("GUIA_NO_COINCIDE", (resultado as ResultadoApi.Rechazo).codigo)
    }

    @Test
    fun cobroPendiente409_traeElEnvioDelServidor() = runBlocking<Unit> {
        val json = """
            {"codigo":"COBRO_PENDIENTE","mensaje":"Primero cobra S/ 50.00 al destinatario.",
             "envio":{"id":7,"uuid":"00000000-0000-4000-8000-000000000007","numeroGuia":"RLP-26-100007-8",
                      "ruta":"Lima → Ica","estado":"en_reparto","version":3,"montoContraentrega":50}}
        """.trimIndent()

        val resultado = llamarApi<EnvioDto> { Response.error(409, cuerpoError(json)) }

        assertTrue(resultado is ResultadoApi.Rechazo)
        val rechazo = resultado as ResultadoApi.Rechazo
        assertEquals(409, rechazo.http)
        assertEquals("en_reparto", rechazo.envio?.estado)
        assertEquals(50.0, rechazo.envio?.montoContraentrega ?: 0.0, 0.0)
    }

    @Test
    fun error503_esFalloQueSePuedeReintentar() = runBlocking<Unit> {
        val json = """{"codigo":"SERVIDOR_NO_DISPONIBLE","mensaje":"Error del servidor simulado para la prueba."}"""

        val resultado = llamarApi<EnvioDto> { Response.error(503, cuerpoError(json)) }

        assertTrue(resultado is ResultadoApi.Fallo)
        assertFalse((resultado as ResultadoApi.Fallo).sinConexion)
    }

    @Test
    fun sinSenal_esFalloSinConexion() = runBlocking<Unit> {
        val resultado = llamarApi<EnvioDto> { throw IOException("sin red") }

        assertTrue(resultado is ResultadoApi.Fallo)
        assertTrue((resultado as ResultadoApi.Fallo).sinConexion)
    }
}
