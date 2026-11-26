package com.example.rutalogcourier.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = AzulRuta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = RojoRuta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410003),
    tertiary = VerdeEntrega,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB8F0B9),
    onTertiaryContainer = Color(0xFF002106),
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF191C1E),
    surface = Color.White,
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41474D),
    outline = Color(0xFF71787E),
    // Superficies con un leve tono azul en vez del gris neutro de Material.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9FC),
    surfaceContainer = Color(0xFFF1F4F9),
    surfaceContainerHigh = Color(0xFFEBEFF5),
    surfaceContainerHighest = Color(0xFFE4E9F0)
)

private val DarkColors = darkColorScheme(
    // Modo oscuro con la marca: fondos azul noche (no gris) y un azul RutaLog vivo para botones y enlaces.
    primary = Color(0xFF5EA0FF),
    onPrimary = Color(0xFF00214D),
    primaryContainer = Color(0xFF16437F),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFFF8A80),
    onSecondary = Color(0xFF5C0008),
    secondaryContainer = Color(0xFF8C1420),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFF9CD49E),
    onTertiary = Color(0xFF00390F),
    tertiaryContainer = Color(0xFF16521F),
    onTertiaryContainer = Color(0xFFB8F0B9),
    background = Color(0xFF0A1626),
    onBackground = Color(0xFFE3E9F2),
    surface = Color(0xFF0F1F33),
    onSurface = Color(0xFFE3E9F2),
    surfaceVariant = Color(0xFF26374D),
    onSurfaceVariant = Color(0xFFB9C5D6),
    outline = Color(0xFF71839A),
    outlineVariant = Color(0xFF2E4058),
    surfaceDim = Color(0xFF0A1626),
    surfaceBright = Color(0xFF22385A),
    surfaceContainerLowest = Color(0xFF07111E),
    surfaceContainerLow = Color(0xFF0D1B2D),
    surfaceContainer = Color(0xFF112237),
    surfaceContainerHigh = Color(0xFF162A42),
    surfaceContainerHighest = Color(0xFF1C324D)
)

@Composable
fun RutaLogTheme(
    darkTheme: Boolean = PreferenciaTema.esOscuro(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = TipografiaRutaLog,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(6.dp),
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}
