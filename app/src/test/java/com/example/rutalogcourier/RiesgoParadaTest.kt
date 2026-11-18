package com.example.rutalogcourier

import com.example.rutalogcourier.data.local.entities.ParadaEntity
import com.example.rutalogcourier.data.repository.aModelo
import com.example.rutalogcourier.model.TipoParada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RiesgoParadaTest {

    private fun entidad(tipo: TipoParada, prob: Double?, alto: Boolean?, factores: String?) = ParadaEntity(
        idRemoto = 1, tipo = tipo.codigo, orden = 1, numeroGuia = "RLP-1", ruta = "Lima → Callao", estado = "en_reparto",
        version = 1, destinatarioNombre = null, destinatarioTelefono = null, destinatarioDireccion = null,
        montoContraentrega = 0.0, intentos = 0, origenLatitud = null, origenLongitud = null, destinoLatitud = null,
        destinoLongitud = null, kmDesdeAnterior = null, estadoSync = "SINCRONIZADO",
        riesgoProbabilidad = prob, riesgoAlto = alto, riesgoFactores = factores
    )

    @Test
    fun `una entrega de riesgo alto muestra el porcentaje y sus razones`() {
        val p = entidad(TipoParada.ENTREGA, 0.874, true, "Franja de 8 h · Zona Oriente").aModelo()
        assertEquals("87 %", p.textoRiesgo)
        assertEquals(listOf("Franja de 8 h", "Zona Oriente"), p.riesgoFactores)
    }

    @Test
    fun `sin riesgo alto, en recojos o sin datos no se marca`() {
        assertNull(entidad(TipoParada.ENTREGA, 0.05, false, "").aModelo().textoRiesgo)
        assertNull(entidad(TipoParada.RECOJO, 0.9, true, null).aModelo().textoRiesgo)
        val viejo = entidad(TipoParada.ENTREGA, null, null, null).aModelo() // fila de antes de la migración 2 → 3
        assertNull(viejo.textoRiesgo)
        assertEquals(emptyList<String>(), viejo.riesgoFactores)
    }
}
