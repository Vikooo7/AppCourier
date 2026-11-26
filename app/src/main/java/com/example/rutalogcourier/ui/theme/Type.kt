package com.example.rutalogcourier.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.example.rutalogcourier.R

// Letras incluidas en la app (licencia OFL, ver licencias/): se ven igual en cualquier celular,
// sin depender de la fuente que el usuario tenga elegida en su teléfono.
@OptIn(ExperimentalTextApi::class)
private fun variable(archivo: Int, peso: Int) =
    Font(archivo, FontWeight(peso), variationSettings = FontVariation.Settings(FontVariation.weight(peso)))

private val pesos = listOf(400, 500, 600, 700, 800)

/** Inter: textos de toda la app. */
val FuenteTexto = FontFamily(pesos.map { variable(R.font.inter, it) })

/** JetBrains Mono: números de guía, de operación y montos que se comparan dígito a dígito. */
val FuenteGuia = FontFamily(pesos.map { variable(R.font.jetbrains_mono, it) })

private val base = Typography()

val TipografiaRutaLog = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = FuenteTexto),
    displayMedium = base.displayMedium.copy(fontFamily = FuenteTexto),
    displaySmall = base.displaySmall.copy(fontFamily = FuenteTexto),
    headlineLarge = base.headlineLarge.copy(fontFamily = FuenteTexto),
    headlineMedium = base.headlineMedium.copy(fontFamily = FuenteTexto),
    headlineSmall = base.headlineSmall.copy(fontFamily = FuenteTexto),
    titleLarge = base.titleLarge.copy(fontFamily = FuenteTexto),
    titleMedium = base.titleMedium.copy(fontFamily = FuenteTexto),
    titleSmall = base.titleSmall.copy(fontFamily = FuenteTexto),
    bodyLarge = base.bodyLarge.copy(fontFamily = FuenteTexto),
    bodyMedium = base.bodyMedium.copy(fontFamily = FuenteTexto),
    bodySmall = base.bodySmall.copy(fontFamily = FuenteTexto),
    labelLarge = base.labelLarge.copy(fontFamily = FuenteTexto),
    labelMedium = base.labelMedium.copy(fontFamily = FuenteTexto),
    labelSmall = base.labelSmall.copy(fontFamily = FuenteTexto)
)
