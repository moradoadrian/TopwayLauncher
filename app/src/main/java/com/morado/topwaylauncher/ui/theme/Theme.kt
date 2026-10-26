package com.morado.topwaylauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Paleta monocromática de grafito. La interfaz no usa colores saturados: el único color
 * en pantalla es el de los iconos de las apps. Las superficies se separan por pasos de
 * tono; el borde se reserva para divisores. Ambos grises de texto superan 4.5:1 sobre
 * el fondo y sobre los paneles.
 */
object TopwayPalette {
    val Background = Color(0xFF101113)
    val Panel = Color(0xFF1B1D20)
    val PanelRaised = Color(0xFF25282C)
    val Border = Color(0xFF36393E)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA8ADB4)
    val Selected = Color(0xFFE5E7EB)
    val OnSelected = Color(0xFF101113)
}

/** Medidas compartidas. Las proporciones de la pantalla se calculan a partir de estas. */
object TopwayDimens {
    val ScreenPaddingHorizontal = 24.dp
    val ScreenPaddingVertical = 16.dp
    val Gap = 16.dp
    val CardRadius = 24.dp
    val CardPadding = 20.dp
    val ControlRadius = 16.dp

    /** La barra lateral ocupa ~10 % del ancho, acotado para pantallas pequeñas y grandes. */
    const val RAIL_WIDTH_FRACTION = 0.10f
    val RailMinWidth = 80.dp
    val RailMaxWidth = 112.dp
    val RailMinItemHeight = 52.dp
    val RailMaxItemHeight = 76.dp
    val RailIcon = 26.dp

    val QuickIcon = 52.dp
    val QuickSlotWidth = 92.dp
    val MinTouchTarget = 48.dp
}

// Una sola familia (la del sistema). Cifras tabulares en todo lo que muestra la hora.
private const val TABULAR = "tnum"

private val TopwayTypography = Typography(
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR),
    titleLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

private val TopwayColors = darkColorScheme(
    primary = TopwayPalette.Selected,
    onPrimary = TopwayPalette.OnSelected,
    background = TopwayPalette.Background,
    onBackground = TopwayPalette.TextPrimary,
    surface = TopwayPalette.Panel,
    onSurface = TopwayPalette.TextPrimary,
    surfaceVariant = TopwayPalette.PanelRaised,
    onSurfaceVariant = TopwayPalette.TextSecondary,
    surfaceContainerHigh = TopwayPalette.PanelRaised,
    outline = TopwayPalette.Border,
    inverseSurface = TopwayPalette.Selected,
    inverseOnSurface = TopwayPalette.OnSelected,
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
