package com.morado.topwaylauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.morado.topwaylauncher.ui.theme.TopwayPalette

/** Intensidad del halo ciruela y del tinte de la carátula sobre el fondo. */
private const val HALO_ALPHA = 0.45f
private const val TINT_ALPHA = 0.28f
private const val TINT_FADE_MS = 900

/**
 * Fondo de la pantalla: degradado grafito con un toque morado, un halo ciruela detrás de las
 * tarjetas y, si suena algo con carátula, un tinte suave de su color del lado de la música.
 * El tinte entra y sale con un fundido lento para que no llame la atención al conducir.
 */
@Composable
fun AmbientBackground(tint: Color?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val animatedTint by animateColorAsState(
        targetValue = tint?.copy(alpha = TINT_ALPHA) ?: Color.Transparent,
        animationSpec = tween(TINT_FADE_MS),
        label = "ambientTint",
    )
    Box(
        modifier = modifier.drawBehind {
            drawRect(
                Brush.linearGradient(
                    0f to TopwayPalette.BackgroundTop,
                    0.55f to TopwayPalette.Background,
                    1f to TopwayPalette.BackgroundDeep,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
            )
            val radius = maxOf(size.width, size.height) * 0.55f
            drawRect(
                Brush.radialGradient(
                    0f to TopwayPalette.Accent.copy(alpha = HALO_ALPHA),
                    1f to Color.Transparent,
                    center = Offset(size.width * 0.3f, size.height * 0.35f),
                    radius = radius,
                ),
            )
            if (animatedTint.alpha > 0f) {
                drawRect(
                    Brush.radialGradient(
                        0f to animatedTint,
                        1f to Color.Transparent,
                        center = Offset(size.width * 0.82f, size.height * 0.4f),
                        radius = radius,
                    ),
                )
            }
        },
    ) {
        content()
    }
}
