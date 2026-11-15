package com.example.rutalogcourier

import com.example.rutalogcourier.model.AccionCourier
import com.example.rutalogcourier.model.EstadoEnvio
import com.example.rutalogcourier.model.EstadoSincronizacion
import com.example.rutalogcourier.model.MotivoFalla
import com.example.rutalogcourier.model.NumeroGuia
import com.example.rutalogcourier.model.Parada
import com.example.rutalogcourier.model.ReglasCourier
import com.example.rutalogcourier.model.TipoParada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reglas de la App Courier (las mismas que valida la API). */
class ReglasCourierTest {

    private fun parada(estado: EstadoEnvio, monto: Double = 0.0, intentos: Int = 0) = Parada(
        idRemoto = 1,
        tipo = TipoParada.segunEstado(estado),
        orden = 1,
        numeroGuia = "RLP-26-300025-0",
        ruta = "Lima → Huancayo",
        estado = estado,
        version = 1,
        destinatarioNombre = "Carlos Mendoza",
        destinatarioTelefono = "987654321",
        destinatarioDireccion = "Av. Huancavelica 512",
        montoContraentrega = monto,
        intentos = intentos,
        latitud = -12.06,
        longitud = -75.2,
        kmDesdeAnterior = 1.2,
        estadoSync = EstadoSincronizacion.SINCRONIZADO,
        mensajeError = null
    )

    @Test
    fun laGuiaEscaneadaDebeSerLaDeLaParada() {
        assertTrue(NumeroGuia.coinciden("rlp-26-300025-0", "RLP-26-300025-0"))
        assertTrue(NumeroGuia.coinciden("RLP 26 300025 0", "RLP-26-300025-0"))
        assertFalse(NumeroGuia.coinciden("RLP-26-300026-1", "RLP-26-300025-0"))
        assertFalse(NumeroGuia.coinciden("", "RLP-26-300025-0"))
    }

    @Test
    fun elAvanceSigueElFlujo_yLaEntregaVaAparte() {
        assertEquals(EstadoEnvio.RECOGIDO, ReglasCourier.siguienteEstado(EstadoEnvio.PENDIENTE))
        assertEquals(EstadoEnvio.EN_TRANSITO, ReglasCourier.siguienteEstado(EstadoEnvio.RECOGIDO))
        assertEquals(EstadoEnvio.EN_REPARTO, ReglasCourier.siguienteEstado(EstadoEnvio.EN_TRANSITO))
        assertEquals(EstadoEnvio.EN_REPARTO, ReglasCourier.siguienteEstado(EstadoEnvio.REPROGRAMADO))
        assertNull(ReglasCourier.siguienteEstado(EstadoEnvio.EN_REPARTO))
        assertNull(ReglasCourier.siguienteEstado(EstadoEnvio.ENTREGADO))
    }

    @Test
    fun accionesSegunElEstado() {
        assertEquals(listOf(AccionCourier.ESCANEAR_RECOJO), ReglasCourier.acciones(parada(EstadoEnvio.PENDIENTE)))
        assertEquals(
            listOf(AccionCourier.ENTREGAR, AccionCourier.REGISTRAR_FALLIDA),
            ReglasCourier.acciones(parada(EstadoEnvio.EN_REPARTO))
        )
        assertTrue(ReglasCourier.acciones(parada(EstadoEnvio.DEVUELTO)).isEmpty())
    }

    @Test
    fun sinFotoYFirmaNoSeCierraLaEntrega() {
        assertNotNull(ReglasCourier.validarEntrega("Carlos", "45124512", tieneFoto = false, tieneFirma = true))
        assertNotNull(ReglasCourier.validarEntrega("Carlos", "45124512", tieneFoto = true, tieneFirma = false))
        assertNotNull(ReglasCourier.validarEntrega("", "45124512", tieneFoto = true, tieneFirma = true))
        assertNotNull(ReglasCourier.validarEntrega("Carlos", "4512", tieneFoto = true, tieneFirma = true))
        assertNull(ReglasCourier.validarEntrega("Carlos", "45124512", tieneFoto = true, tieneFirma = true))
    }

    @Test
    fun unContraentregaNoSeEntregaSinCobro() {
        assertNotNull(ReglasCourier.motivoNoEntregar(parada(EstadoEnvio.EN_REPARTO, monto = 189.0)))
        assertNull(ReglasCourier.motivoNoEntregar(parada(EstadoEnvio.EN_REPARTO)))
    }

    @Test
    fun laFallidaNecesitaMotivoYFoto_yAlTercerIntentoSeDevuelve() {
        assertNotNull(ReglasCourier.validarFallida(null, tieneFoto = true))
        assertNotNull(ReglasCourier.validarFallida(MotivoFalla.AUSENTE, tieneFoto = false))
        assertNull(ReglasCourier.validarFallida(MotivoFalla.AUSENTE, tieneFoto = true))
        assertEquals(EstadoEnvio.FALLIDO, ReglasCourier.estadoTrasFallida(0))
        assertEquals(EstadoEnvio.FALLIDO, ReglasCourier.estadoTrasFallida(1))
        assertEquals(EstadoEnvio.DEVUELTO, ReglasCourier.estadoTrasFallida(2))
    }

    @Test
    fun tipoDeParadaSegunElEstado() {
        assertEquals(TipoParada.RECOJO, TipoParada.segunEstado(EstadoEnvio.PENDIENTE))
        assertEquals(TipoParada.EN_CAMINO, TipoParada.segunEstado(EstadoEnvio.EN_TRANSITO))
        assertEquals(TipoParada.ENTREGA, TipoParada.segunEstado(EstadoEnvio.REPROGRAMADO))
        assertEquals(TipoParada.COMPLETADA, TipoParada.segunEstado(EstadoEnvio.ENTREGADO))
    }
}
