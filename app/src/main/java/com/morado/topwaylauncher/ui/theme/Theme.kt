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
 * Paleta "ciruela". El fondo es un grafito con un toque morado y un halo de color; las
 * tarjetas son vidrio (blanco translúcido) para que el ambiente se vea a través de ellas.
 *
 * El ciruela es oscuro: sobre el fondo no llega a 3:1, así que solo se usa como RELLENO con
 * texto o icono blanco encima (8.9:1). Los detalles finos (puntos de página, enlaces de
 * diálogos) usan su versión clara, que sí contrasta con el fondo. Los dos grises de texto
 * superan 4.5:1 sobre el fondo, el vidrio y el halo.
 */
object TopwayPalette {
    val Background = Color(0xFF110C10)
    val BackgroundTop = Color(0xFF1E1219)
    val BackgroundDeep = Color(0xFF0B090A)

    /** Barra lateral: algo más oscura que el fondo, translúcida para que se vea el halo. */
    val Rail = Color(0x990E080C)

    /** Vidrio de las tarjetas: degradado de arriba a abajo y un borde que lo recorta. */
    val GlassTop = Color(0x16FFFFFF)
    val GlassBottom = Color(0x09FFFFFF)
    val GlassBorder = Color(0x17FFFFFF)

    /** Superficie translúcida para controles sobre el vidrio (píldoras, marcadores). */
    val Control = Color(0x14FFFFFF)

    /** Superficies opacas: diálogos y avisos, que no deben dejar ver lo de detrás. */
    val Panel = Color(0xFF1C1519)
    val PanelRaised = Color(0xFF2A2026)

    val Border = Color(0x14FFFFFF)
    val TextPrimary = Color(0xFFF4F6F8)
    val TextSecondary = Color(0xFFA9B1BA)

    val Accent = Color(0xFF76305E)
    val OnAccent = Color(0xFFFFFFFF)
    val AccentLight = Color(0xFFD58AB9)

    /** Indicadores inactivos (puntos de página). */
    val Inactive = Color(0x38FFFFFF)
}

/** Medidas compartidas. Las proporciones de la pantalla se calculan a partir de estas. */
object TopwayDimens {
    val ScreenPaddingHorizontal = 24.dp
    val ScreenPaddingVertical = 16.dp
    val Gap = 16.dp
    val CardRadius = 28.dp
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
    primary = TopwayPalette.Accent,
    onPrimary = TopwayPalette.OnAccent,
    background = TopwayPalette.Background,
    onBackground = TopwayPalette.TextPrimary,
    surface = TopwayPalette.Panel,
    onSurface = TopwayPalette.TextPrimary,
    surfaceVariant = TopwayPalette.PanelRaised,
    onSurfaceVariant = TopwayPalette.TextSecondary,
    surfaceContainerHigh = TopwayPalette.PanelRaised,
    outline = TopwayPalette.Border,
    inverseSurface = TopwayPalette.Accent,
    inverseOnSurface = TopwayPalette.OnAccent,
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
