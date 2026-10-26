package com.morado.topwaylauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val TopwayColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00212E),
    secondary = Color(0xFF80CBC4),
    background = Color(0xFF0B0E13),
    onBackground = Color(0xFFE6EAF0),
    surface = Color(0xFF151A22),
    onSurface = Color(0xFFE6EAF0),
    surfaceVariant = Color(0xFF1E252F),
    onSurfaceVariant = Color(0xFFA9B3C1),
    error = Color(0xFFFF8A80),
)

// Tamaños generosos: la pantalla se lee a distancia y con el vehículo en marcha.
private val TopwayTypography = Typography(
    titleLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 18.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
)

/** Tema siempre oscuro, independiente del modo del sistema. */
@Composable
fun TopwayLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TopwayColors,
        typography = TopwayTypography,
        content = content,
    )
}
