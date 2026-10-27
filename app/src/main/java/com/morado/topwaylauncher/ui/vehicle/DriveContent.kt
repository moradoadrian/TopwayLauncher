package com.morado.topwaylauncher.ui.vehicle

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.data.vehicle.GpsStatus
import com.morado.topwaylauncher.data.vehicle.ObdStatus
import com.morado.topwaylauncher.data.vehicle.VehicleState
import com.morado.topwaylauncher.ui.components.CARD_PRESSED_SCALE
import com.morado.topwaylauncher.ui.components.Pressable
import com.morado.topwaylauncher.ui.theme.TopwayDimens
import com.morado.topwaylauncher.ui.theme.TopwayPalette

private const val GAUGE_WEIGHT = 0.62f
private const val SIDE_WEIGHT = 0.38f

/**
 * Modo conducción: velocidad grande, estado de las fuentes y dos accesos grandes (navegación
 * y música). Sin gráficas, sin controles de desarrollo y sin avisos sonoros. Si el adaptador
 * se desconecta, la velocidad pasa a "--" y el estado lo explica; la vista sigue funcionando.
 */
@Composable
fun DriveContent(
    vehicle: State<VehicleState>,
    onNavigation: () -> Unit,
    onMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        if (maxWidth >= maxHeight) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(TopwayDimens.Gap)) {
                SpeedCard(vehicle, onClick = null, Modifier.weight(GAUGE_WEIGHT).fillMaxHeight(), showDisclaimer = true)
                DriveSide(vehicle, onNavigation, onMusic, Modifier.weight(SIDE_WEIGHT).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(TopwayDimens.Gap)) {
                SpeedCard(vehicle, onClick = null, Modifier.weight(1f).fillMaxWidth(), showDisclaimer = true)
                DriveSide(vehicle, onNavigation, onMusic, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DriveSide(
    vehicle: State<VehicleState>,
    onNavigation: () -> Unit,
    onMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(TopwayDimens.Gap)) {
        SourcesPanel(vehicle, Modifier.fillMaxWidth())
        DriveButton(R.drawable.ic_nav_navigation, R.string.drive_navigation, onNavigation, Modifier.fillMaxWidth().weight(1f))
        DriveButton(R.drawable.ic_nav_music, R.string.drive_music, onMusic, Modifier.fillMaxWidth().weight(1f))
    }
}

/** Estado real de cada fuente. El GPS solo aparece si el usuario lo activó. */
@Composable
private fun SourcesPanel(vehicle: State<VehicleState>, modifier: Modifier = Modifier) {
    val state = vehicle.value
    val shape = RoundedCornerShape(TopwayDimens.CardRadius)
    Column(
        modifier
            .clip(shape)
            .background(TopwayPalette.Card)
            .border(1.dp, TopwayPalette.CardBorder, shape)
            .padding(horizontal = TopwayDimens.CardPadding, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val protocol = (state.obd as? ObdStatus.Connected)?.protocol
        SourceLine(stringResource(R.string.obd_label), obdLabel(state.obd), protocol)
        if (state.gps != GpsStatus.Disabled) {
            SourceLine(stringResource(R.string.gps_label), gpsLabel(state.gps, state.gpsHasFix), detail = null)
        }
    }
}

@Composable
private fun SourceLine(name: String, label: StatusLabel, detail: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(label.color)
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.labelLarge, color = TopwayPalette.TextPrimary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(label.text),
                style = MaterialTheme.typography.bodyMedium,
                color = TopwayPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = TopwayPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DriveButton(@DrawableRes icon: Int, label: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val text = stringResource(label)
    val shape = RoundedCornerShape(TopwayDimens.CardRadius)
    Pressable(onClick = onClick, onClickLabel = text, modifier = modifier, pressedScale = CARD_PRESSED_SCALE) {
        Row(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(TopwayPalette.Card)
                .border(1.dp, TopwayPalette.CardBorder, shape)
                .padding(horizontal = TopwayDimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = TopwayPalette.TextPrimary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, color = TopwayPalette.TextPrimary, maxLines = 1)
        }
    }
}
