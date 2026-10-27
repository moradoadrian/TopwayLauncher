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
 * Paleta "Altima Cockpit": grafito casi negro, tarjetas opacas con borde fino y un rojo de
 * instrumentación como acento. El rojo es para agujas, indicadores activos y botones
 * principales; nunca como fondo grande.
 *
 * Contrastes: texto principal sobre tarjeta 14:1; secundario sobre tarjeta 6,7:1; blanco
 * sobre el rojo 6:1 (vale para texto). El rojo sobre el fondo da 3,2:1: suficiente para
 * gráficos (arco, aguja, barra de selección), no para texto pequeño, que usa AccentLight.
 */
object TopwayPalette {
    val Background = Color(0xFF0D0F11)
    val BackgroundSecondary = Color(0xFF171A1D)
    val Card = Color(0xFF202428)
    val CardBorder = Color(0xFF34393F)

    /** Barra lateral: el fondo secundario, que la separa del contenido sin bordes llamativos. */
    val Rail = BackgroundSecondary

    /** Controles sobre una tarjeta (píldoras, marcadores, casillas de icono). */
    val Control = Color(0xFF2B3035)

    /** Superficies de diálogos y avisos. */
    val Panel = BackgroundSecondary
    val PanelRaised = Card

    val Border = CardBorder
    val TextPrimary = Color(0xFFF5F5F5)
    val TextSecondary = Color(0xFFA5ABB2)

    val Accent = Color(0xFFD83A3A)
    val OnAccent = Color(0xFFFFFFFF)

    /** Rojo claro para texto y enlaces finos sobre fondos oscuros (5,9:1 sobre la tarjeta). */
    val AccentLight = Color(0xFFF08A84)

    /** Estados de conexión: verde discreto, gris y ámbar. */
    val Connected = Color(0xFF4FB477)
    val Disconnected = Color(0xFF7A8088)
    val Warning = Color(0xFFE3A33B)

    /** Pista del velocímetro y marcas menores. */
    val GaugeTrack = Color(0xFF2E3338)
    val GaugeTick = Color(0xFF5B6168)

    /** Indicadores inactivos (puntos de página). */
    val Inactive = Color(0xFF4A5057)
}

/** Medidas compartidas. Las proporciones de la pantalla se calculan a partir de estas. */
object TopwayDimens {
    val ScreenPaddingHorizontal = 20.dp
    val ScreenPaddingVertical = 14.dp
    val Gap = 14.dp
    val CardRadius = 20.dp
    val CardPadding = 18.dp
    val ControlRadius = 14.dp

    /** La barra lateral ocupa ~10 % del ancho, acotado para pantallas pequeñas y grandes. */
    const val RAIL_WIDTH_FRACTION = 0.10f
    val RailMinWidth = 80.dp
    val RailMaxWidth = 112.dp
    val RailMaxItemHeight = 76.dp
    val RailIcon = 26.dp

    val QuickIcon = 52.dp
    val QuickSlotWidth = 92.dp
    val MinTouchTarget = 48.dp
}

// Una sola familia (la del sistema). Cifras tabulares en la hora y la velocidad, para que no
// bailen al cambiar de valor.
private const val TABULAR = "tnum"

private val TopwayTypography = Typography(
    // Velocidad: el tamaño real lo fija el velocímetro según su espacio; aquí peso y cifras.
    displayLarge = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR),
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    titleMedium = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Medium, lineHeight = 25.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
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
    inverseSurface = TopwayPalette.Card,
    inverseOnSurface = TopwayPalette.TextPrimary,
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
