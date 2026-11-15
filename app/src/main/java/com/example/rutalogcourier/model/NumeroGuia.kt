package com.example.rutalogcourier.model

/** Número de guía con el formato RLP-AA-NNNNNN-D (lo genera la App Cliente al registrar). */
object NumeroGuia {

    /** Quita guiones y espacios para comparar la guía escaneada o escrita con la de la parada. */
    fun normalizar(texto: String): String = texto.uppercase().filter { it.isLetterOrDigit() }

    fun coinciden(escaneada: String, esperada: String): Boolean =
        normalizar(escaneada).isNotEmpty() && normalizar(escaneada) == normalizar(esperada)
}
