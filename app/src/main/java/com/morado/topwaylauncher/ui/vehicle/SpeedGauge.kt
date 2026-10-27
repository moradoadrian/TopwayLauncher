package com.morado.topwaylauncher.ui.vehicle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.ui.theme.TopwayPalette
import kotlin.math.cos
import kotlin.math.sin

/** Fondo de escala del arco. Valores mayores se muestran en cifras y la aguja queda al tope. */
const val GAUGE_MAX_KMH = 200
private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f
private const val MAJOR_STEP_KMH = 20
private const val MINOR_STEP_KMH = 10
private const val NEEDLE_MS = 350

/** Por debajo de este tamaño el arco no se lee bien: solo cifras. */
private val MinArcSize = 150.dp

/**
 * Velocímetro: un solo Canvas con la pista, 21 marcas, el arco recorrido y la aguja; encima,
 * la cifra grande. La geometría se calcula una vez por tamaño (drawWithCache) y la aguja solo
 * se anima cuando cambia el valor, así que en reposo no consume CPU.
 *
 * Con [speedKmh] null no hay arco ni aguja: se muestra "--", nunca un 0.
 */
@Composable
fun SpeedGauge(speedKmh: Int?, modifier: Modifier = Modifier) {
    val description = if (speedKmh != null) {
        pluralStringResource(R.plurals.speed_desc_known, speedKmh, speedKmh)
    } else {
        stringResource(R.string.speed_desc_unknown)
    }
    BoxWithConstraints(
        modifier.clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val diameter = minOf(maxWidth, maxHeight)
        if (diameter >= MinArcSize) {
            GaugeArc(speedKmh, diameter)
        }
        SpeedReadout(speedKmh, numberHeight = if (diameter >= MinArcSize) diameter * 0.26f else diameter * 0.5f)
    }
}

@Composable
private fun GaugeArc(speedKmh: Int?, diameter: Dp) {
    val target = speedKmh?.coerceIn(0, GAUGE_MAX_KMH)?.toFloat()
    val needle = remember { Animatable(target ?: 0f) }
    LaunchedEffect(target) {
        if (target == null) needle.snapTo(0f) else needle.animateTo(target, tween(NEEDLE_MS))
    }
    val known = target != null
    Spacer(
        Modifier
            .size(diameter)
            .drawWithCache {
                val stroke = size.minDimension * 0.045f
                val tickLength = stroke * 1.4f
                val radius = size.minDimension / 2f - stroke
                val center = Offset(size.width / 2f, size.height / 2f)
                val arcTopLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)
                val trackStroke = Stroke(width = stroke, cap = StrokeCap.Round)
                val tickRadius = radius - stroke * 1.4f

                fun point(angleDeg: Float, r: Float): Offset {
                    val rad = Math.toRadians(angleDeg.toDouble())
                    return Offset(center.x + r * cos(rad).toFloat(), center.y + r * sin(rad).toFloat())
                }

                // Marcas: mayores cada 20 km/h, menores cada 10. Se calculan una sola vez.
                val ticks = (0..GAUGE_MAX_KMH step MINOR_STEP_KMH).map { kmh ->
                    val angle = START_ANGLE + SWEEP_ANGLE * kmh / GAUGE_MAX_KMH
                    val major = kmh % MAJOR_STEP_KMH == 0
                    val inner = tickRadius - if (major) tickLength else tickLength * 0.55f
                    Triple(point(angle, tickRadius), point(angle, inner), major)
                }

                onDrawBehind {
                    drawArc(TopwayPalette.GaugeTrack, START_ANGLE, SWEEP_ANGLE, false, arcTopLeft, arcSize, style = trackStroke)
                    ticks.forEach { (outer, inner, major) ->
                        drawLine(
                            color = if (major) TopwayPalette.TextSecondary else TopwayPalette.GaugeTick,
                            start = outer,
                            end = inner,
                            strokeWidth = if (major) stroke * 0.32f else stroke * 0.2f,
                            cap = StrokeCap.Round,
                        )
                    }
                    if (known) {
                        // needle.value se lee aquí, en la fase de dibujo: animar no recompone.
                        val sweep = SWEEP_ANGLE * needle.value / GAUGE_MAX_KMH
                        drawArc(TopwayPalette.Accent, START_ANGLE, sweep, false, arcTopLeft, arcSize, style = trackStroke)
                        val angle = START_ANGLE + sweep
                        drawLine(
                            color = TopwayPalette.Accent,
                            start = point(angle, radius + stroke * 0.9f),
                            end = point(angle, tickRadius - tickLength * 1.6f),
                            strokeWidth = stroke * 0.5f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            },
    )
}

@Composable
private fun SpeedReadout(speedKmh: Int?, numberHeight: Dp) {
    val numberSize = with(LocalDensity.current) { numberHeight.toSp() }
    Box(contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speedKmh?.toString() ?: stringResource(R.string.speed_unknown),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = numberSize, lineHeight = numberSize),
                color = if (speedKmh != null) TopwayPalette.TextPrimary else TopwayPalette.TextSecondary,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.speed_unit),
                style = MaterialTheme.typography.titleMedium,
                color = TopwayPalette.TextSecondary,
            )
        }
    }
}
