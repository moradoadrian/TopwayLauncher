package com.morado.topwaylauncher.ui.vehicle

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.data.vehicle.GpsStatus
import com.morado.topwaylauncher.data.vehicle.ObdError
import com.morado.topwaylauncher.data.vehicle.ObdStatus
import com.morado.topwaylauncher.data.vehicle.SpeedSource
import com.morado.topwaylauncher.data.vehicle.VehicleState
import com.morado.topwaylauncher.ui.components.CARD_PRESSED_SCALE
import com.morado.topwaylauncher.ui.components.Pressable
import com.morado.topwaylauncher.ui.theme.TopwayDimens
import com.morado.topwaylauncher.ui.theme.TopwayPalette

/** Texto y color de un estado de conexión. */
data class StatusLabel(@StringRes val text: Int, val color: Color)

fun obdLabel(status: ObdStatus): StatusLabel = when (status) {
    ObdStatus.Disabled -> StatusLabel(R.string.obd_disabled, TopwayPalette.Disconnected)
    ObdStatus.NotConfigured -> StatusLabel(R.string.obd_not_configured, TopwayPalette.Disconnected)
    ObdStatus.NeedsPermission -> StatusLabel(R.string.obd_needs_permission, TopwayPalette.Warning)
    ObdStatus.BluetoothUnavailable -> StatusLabel(R.string.obd_bt_unavailable, TopwayPalette.Disconnected)
    ObdStatus.BluetoothOff -> StatusLabel(R.string.obd_bt_off, TopwayPalette.Warning)
    ObdStatus.Connecting -> StatusLabel(R.string.obd_connecting, TopwayPalette.Warning)
    is ObdStatus.Connected -> StatusLabel(R.string.obd_connected, TopwayPalette.Connected)
    ObdStatus.SpeedUnsupported -> StatusLabel(R.string.obd_unsupported, TopwayPalette.Warning)
    is ObdStatus.Error -> StatusLabel(
        when (status.reason) {
            ObdError.ConnectFailed -> R.string.obd_err_connect
            ObdError.AdapterNotResponding -> R.string.obd_err_adapter
            ObdError.NoVehicleResponse -> R.string.obd_err_vehicle
            ObdError.ConnectionLost -> R.string.obd_err_lost
        },
        TopwayPalette.Warning,
    )
}

fun gpsLabel(status: GpsStatus, hasFix: Boolean): StatusLabel = when (status) {
    GpsStatus.Disabled -> StatusLabel(R.string.gps_disabled, TopwayPalette.Disconnected)
    GpsStatus.NeedsPermission -> StatusLabel(R.string.gps_needs_permission, TopwayPalette.Warning)
    GpsStatus.Unavailable -> StatusLabel(R.string.gps_unavailable, TopwayPalette.Disconnected)
    GpsStatus.LocationOff -> StatusLabel(R.string.gps_off, TopwayPalette.Warning)
    GpsStatus.On -> if (hasFix) {
        StatusLabel(R.string.gps_active, TopwayPalette.Connected)
    } else {
        StatusLabel(R.string.gps_searching, TopwayPalette.Warning)
    }
}

/** El GPS se indica solo cuando hay permiso y el usuario lo activó: su estado es real. */
fun showGpsIndicator(status: GpsStatus): Boolean = status == GpsStatus.On || status == GpsStatus.LocationOff

@StringRes
fun sourceName(source: SpeedSource): Int = when (source) {
    SpeedSource.Obd -> R.string.source_obd
    SpeedSource.Gps -> R.string.source_gps
    SpeedSource.Demo -> R.string.source_demo
}

/**
 * Línea de estado bajo la velocidad: si hay lectura, que es en vivo; si dejó de haberla, que
 * no hay datos recientes; si no hay ninguna fuente, la causa (la conexión OBD, el GPS o que
 * no se ha configurado nada).
 */
fun speedStatus(state: VehicleState): StatusLabel = when {
    state.source == SpeedSource.Demo -> StatusLabel(R.string.source_demo, TopwayPalette.Warning)
    state.source != null -> StatusLabel(R.string.speed_live, TopwayPalette.Connected)
    state.stale -> StatusLabel(R.string.speed_stale, TopwayPalette.Warning)
    state.obd != ObdStatus.Disabled -> obdLabel(state.obd)
    state.gps != GpsStatus.Disabled -> gpsLabel(state.gps, state.gpsHasFix)
    else -> StatusLabel(R.string.speed_configure, TopwayPalette.Disconnected)
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(8.dp)
            .background(color, CircleShape),
    )
}

/** Indicador compacto para la cabecera: punto de color, nombre y estado. */
@Composable
fun StatusChip(name: String, label: StatusLabel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(TopwayPalette.BackgroundSecondary)
            .border(1.dp, TopwayPalette.Border, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(label.color)
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.labelMedium, color = TopwayPalette.TextPrimary, maxLines = 1)
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(label.text),
            style = MaterialTheme.typography.labelMedium,
            color = TopwayPalette.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Estado OBD-II (siempre) y GPS (solo si está activo) para la cabecera. */
@Composable
fun VehicleStatusChips(vehicle: State<VehicleState>, modifier: Modifier = Modifier) {
    val state = vehicle.value
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showGpsIndicator(state.gps)) {
            StatusChip(stringResource(R.string.gps_label), gpsLabel(state.gps, state.gpsHasFix))
        }
        StatusChip(stringResource(R.string.obd_label), obdLabel(state.obd))
    }
}

/** Etiqueta de la fuente que se está mostrando. La demo va en ámbar para no confundirla. */
@Composable
fun SourceBadge(source: SpeedSource?, modifier: Modifier = Modifier) {
    if (source == null) return
    val color = if (source == SpeedSource.Demo) TopwayPalette.Warning else TopwayPalette.TextPrimary
    val shape = RoundedCornerShape(8.dp)
    Text(
        text = stringResource(sourceName(source)),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.6f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * Tarjeta protagonista del inicio. Lee el estado del vehículo aquí dentro: cuando cambia la
 * velocidad solo se recompone esta tarjeta, no toda la pantalla.
 */
@Composable
fun SpeedCard(
    vehicle: State<VehicleState>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showDisclaimer: Boolean = false,
) {
    if (onClick == null) {
        Box(modifier) { SpeedCardContent(vehicle.value, showDisclaimer) }
    } else {
        Pressable(
            onClick = onClick,
            onClickLabel = stringResource(R.string.drive_title),
            modifier = modifier,
            pressedScale = CARD_PRESSED_SCALE,
        ) {
            SpeedCardContent(vehicle.value, showDisclaimer)
        }
    }
}

@Composable
private fun SpeedCardContent(state: VehicleState, showDisclaimer: Boolean) {
    val status = speedStatus(state)
    val shape = RoundedCornerShape(TopwayDimens.CardRadius)
    Column(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(TopwayPalette.Card)
            .border(1.dp, TopwayPalette.CardBorder, shape)
            .padding(TopwayDimens.CardPadding),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.speed_title).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TopwayPalette.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            SourceBadge(state.source)
        }
        SpeedGauge(
            speedKmh = state.speedKmh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 6.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(status.color)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(status.text),
                style = MaterialTheme.typography.bodyMedium,
                color = TopwayPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showDisclaimer) {
                Text(
                    text = stringResource(R.string.speed_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = TopwayPalette.TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}
