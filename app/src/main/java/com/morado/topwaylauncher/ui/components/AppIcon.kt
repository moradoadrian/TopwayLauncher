package com.morado.topwaylauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.morado.topwaylauncher.data.LauncherApp

/** Escala al pulsar un icono: respuesta táctil visible sin cambiar el diseño. */
const val ICON_PRESSED_SCALE = 0.9f

/** Escala al pulsar una tarjeta: en superficies grandes basta un cambio pequeño. */
const val CARD_PRESSED_SCALE = 0.98f

/**
 * Contenedor pulsable que se encoge ligeramente al tocarlo. Sin ripple: en un coche la
 * respuesta debe leerse de reojo, y el cambio de escala se percibe mejor que un destello.
 */
@Composable
fun Pressable(
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    pressedScale: Float = ICON_PRESSED_SCALE,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, label = "pressScale")
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = onClickLabel,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Icono de la app, o su inicial sobre un paso de tono si el icono no está disponible. */
@Composable
fun AppIcon(app: LauncherApp, size: Dp, modifier: Modifier = Modifier) {
    val icon = app.icon
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(percent = 24))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.info.label.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}
