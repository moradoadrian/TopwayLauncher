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

/** Intensidad del tinte de la carátula sobre el fondo: apenas perceptible. */
private const val TINT_ALPHA = 0.14f
private const val TINT_FADE_MS = 900

/**
 * Fondo de la pantalla: grafito liso. Si suena algo con carátula, un tinte muy suave de su
 * color del lado de la música, con un fundido lento que solo ocurre al cambiar de pista.
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
            drawRect(TopwayPalette.Background)
            if (animatedTint.alpha > 0f) {
                drawRect(
                    Brush.radialGradient(
                        0f to animatedTint,
                        1f to Color.Transparent,
                        center = Offset(size.width * 0.8f, size.height * 0.45f),
                        radius = maxOf(size.width, size.height) * 0.5f,
                    ),
                )
            }
        },
    ) {
        content()
    }
}
