package com.morado.topwaylauncher.ui.vehicle

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.data.vehicle.ObdStatus
import com.morado.topwaylauncher.data.vehicle.PairedDevice
import com.morado.topwaylauncher.data.vehicle.VehicleSettings
import com.morado.topwaylauncher.data.vehicle.VehicleState
import com.morado.topwaylauncher.ui.theme.TopwayDimens
import com.morado.topwaylauncher.ui.theme.TopwayPalette

private val TwoColumnMinWidth = 720.dp

/**
 * Ajustes del launcher. Los permisos se piden solo aquí y solo cuando el usuario activa la
 * función que los necesita, con una explicación previa en el caso de la ubicación.
 */
@Composable
fun SettingsRoute(
    viewModel: VehicleViewModel,
    vehicle: State<VehicleState>,
    onSystemSettings: () -> Unit,
    onBluetoothSettings: () -> Unit,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    var showDevices by rememberSaveable { mutableStateOf(false) }
    var showGpsRationale by rememberSaveable { mutableStateOf(false) }

    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionsChanged()
        if (granted) showDevices = true else showMessage(resources.getString(R.string.settings_bt_permission_denied))
    }
    // Android 12+ exige pedir precisa y aproximada juntas; el usuario puede dar solo la
    // aproximada, que no sirve para la velocidad (no usa el GPS).
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        viewModel.onPermissionsChanged()
        when {
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true -> viewModel.setGpsEnabled(true)
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ->
                showMessage(resources.getString(R.string.gps_permission_precise))
            else -> showMessage(resources.getString(R.string.gps_permission_denied))
        }
    }

    val chooseAdapter = {
        if (viewModel.needsBluetoothPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            showDevices = true
        }
    }

    SettingsContent(
        settings = settings,
        vehicle = vehicle,
        demoAvailable = viewModel.demoAvailable,
        onSystemSettings = onSystemSettings,
        onObdEnabled = { enabled ->
            viewModel.setObdEnabled(enabled)
            if (enabled && settings.obdAddress == null) chooseAdapter()
        },
        onChooseAdapter = chooseAdapter,
        onBluetoothSettings = onBluetoothSettings,
        onPollInterval = viewModel::setPollInterval,
        onGpsEnabled = { enabled ->
            when {
                !enabled -> viewModel.setGpsEnabled(false)
                viewModel.hasLocationPermission() -> viewModel.setGpsEnabled(true)
                else -> showGpsRationale = true
            }
        },
        onDemoEnabled = viewModel::setDemoEnabled,
        onBrand = viewModel::setBrand,
        onModel = viewModel::setModel,
        modifier = modifier,
    )

    if (showDevices) {
        val devices = remember { viewModel.pairedDevices() }
        DevicePickerDialog(
            devices = devices,
            onSelect = {
                showDevices = false
                viewModel.selectAdapter(it)
            },
            onBluetoothSettings = {
                showDevices = false
                onBluetoothSettings()
            },
            onDismiss = { showDevices = false },
        )
    }
    if (showGpsRationale) {
        AlertDialog(
            onDismissRequest = { showGpsRationale = false },
            confirmButton = {
                TextButton(onClick = {
                    showGpsRationale = false
                    locationPermission.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                }) { Text(stringResource(R.string.gps_permission_continue), color = TopwayPalette.AccentLight) }
            },
            dismissButton = {
                TextButton(onClick = { showGpsRationale = false }) {
                    Text(stringResource(R.string.media_access_cancel), color = TopwayPalette.TextSecondary)
                }
            },
            title = { Text(stringResource(R.string.gps_permission_title)) },
            text = { Text(stringResource(R.string.gps_permission_message), Modifier.verticalScroll(rememberScrollState())) },
            containerColor = TopwayPalette.PanelRaised,
        )
    }
}

@Composable
fun SettingsContent(
    settings: VehicleSettings,
    vehicle: State<VehicleState>,
    demoAvailable: Boolean,
    onSystemSettings: () -> Unit,
    onObdEnabled: (Boolean) -> Unit,
    onChooseAdapter: () -> Unit,
    onBluetoothSettings: () -> Unit,
    onPollInterval: (Long) -> Unit,
    onGpsEnabled: (Boolean) -> Unit,
    onDemoEnabled: (Boolean) -> Unit,
    onBrand: (String) -> Unit,
    onModel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = vehicle.value
    val identity = @Composable {
        SettingsCard(stringResource(R.string.settings_vehicle_title), stringResource(R.string.settings_vehicle_desc)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VehicleField(
                    label = stringResource(R.string.settings_vehicle_brand),
                    placeholder = stringResource(R.string.settings_vehicle_brand_hint),
                    value = settings.brand,
                    onChange = onBrand,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.weight(1f),
                )
                VehicleField(
                    label = stringResource(R.string.settings_vehicle_model),
                    placeholder = stringResource(R.string.settings_vehicle_model_hint),
                    value = settings.model,
                    onChange = onModel,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    val android = @Composable {
        SettingsCard(stringResource(R.string.settings_android), stringResource(R.string.settings_android_desc)) {
            PillButton(stringResource(R.string.settings_open), highlighted = true, onClick = onSystemSettings)
        }
    }
    val obd = @Composable {
        SettingsCard(stringResource(R.string.settings_obd_title), stringResource(R.string.settings_obd_desc)) {
            SwitchRow(stringResource(R.string.settings_obd_enable), settings.obdEnabled, onObdEnabled)
            ValueRow(
                label = stringResource(R.string.settings_obd_adapter),
                value = settings.obdName ?: stringResource(R.string.settings_obd_none),
            ) { PillButton(stringResource(R.string.settings_obd_choose), onClick = onChooseAdapter) }
            ValueRow(label = stringResource(R.string.settings_obd_interval), value = null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VehicleSettings.POLL_OPTIONS_MS.forEach { ms ->
                        PillButton(
                            text = stringResource(R.string.settings_interval_value, (ms / 1000).toInt()),
                            highlighted = ms == settings.pollIntervalMs,
                            onClick = { onPollInterval(ms) },
                        )
                    }
                }
            }
            StatusRow(obdLabel(state.obd), (state.obd as? ObdStatus.Connected)?.protocol)
            PillButton(stringResource(R.string.settings_bt_settings), onClick = onBluetoothSettings)
        }
    }
    val gps = @Composable {
        SettingsCard(stringResource(R.string.settings_gps_title), stringResource(R.string.settings_gps_desc)) {
            SwitchRow(stringResource(R.string.settings_gps_enable), settings.gpsEnabled, onGpsEnabled)
            if (settings.gpsEnabled) StatusRow(gpsLabel(state.gps, state.gpsHasFix), detail = null)
        }
    }
    val demo = @Composable {
        if (demoAvailable) {
            SettingsCard(stringResource(R.string.settings_demo_title), stringResource(R.string.settings_demo_desc)) {
                SwitchRow(stringResource(R.string.settings_demo_enable), settings.demoEnabled, onDemoEnabled)
            }
        }
    }

    BoxWithConstraints(modifier) {
        val scroll = Modifier.verticalScroll(rememberScrollState())
        if (maxWidth >= TwoColumnMinWidth) {
            Row(horizontalArrangement = Arrangement.spacedBy(TopwayDimens.Gap)) {
                Column(Modifier.weight(1f).then(scroll), verticalArrangement = Arrangement.spacedBy(TopwayDimens.Gap)) {
                    obd()
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(TopwayDimens.Gap),
                ) {
                    android()
                    identity()
                    gps()
                    demo()
                }
            }
        } else {
            Column(scroll, verticalArrangement = Arrangement.spacedBy(TopwayDimens.Gap)) {
                android()
                identity()
                obd()
                gps()
                demo()
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(TopwayDimens.CardRadius)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TopwayPalette.Card)
            .border(1.dp, TopwayPalette.CardBorder, shape)
            .padding(TopwayDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TopwayPalette.TextPrimary)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = TopwayPalette.TextSecondary)
        }
        content()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = TopwayDimens.MinTouchTarget)
            .clickable(role = Role.Switch, onClickLabel = label) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TopwayPalette.TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TopwayPalette.OnAccent,
                checkedTrackColor = TopwayPalette.Accent,
                uncheckedThumbColor = TopwayPalette.TextSecondary,
                uncheckedTrackColor = TopwayPalette.Control,
                uncheckedBorderColor = TopwayPalette.Border,
            ),
        )
    }
}

@Composable
private fun VehicleField(
    label: String,
    placeholder: String,
    value: String,
    onChange: (String) -> Unit,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { focus.moveFocus(FocusDirection.Next) },
            onDone = { focus.clearFocus() },
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TopwayPalette.TextPrimary,
            unfocusedTextColor = TopwayPalette.TextPrimary,
            focusedBorderColor = TopwayPalette.Accent,
            unfocusedBorderColor = TopwayPalette.Border,
            focusedLabelColor = TopwayPalette.AccentLight,
            unfocusedLabelColor = TopwayPalette.TextSecondary,
            focusedPlaceholderColor = TopwayPalette.TextSecondary,
            unfocusedPlaceholderColor = TopwayPalette.TextSecondary,
            cursorColor = TopwayPalette.Accent,
        ),
        modifier = modifier.heightIn(min = TopwayDimens.MinTouchTarget),
    )
}

@Composable
private fun ValueRow(label: String, value: String?, action: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TopwayPalette.TextSecondary)
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodyLarge, color = TopwayPalette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(12.dp))
        action()
    }
}

@Composable
private fun StatusRow(label: StatusLabel, detail: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(label.color)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.settings_status, stringResource(label.text)),
            style = MaterialTheme.typography.bodyMedium,
            color = TopwayPalette.TextPrimary,
        )
        if (detail != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.settings_obd_protocol, detail),
                style = MaterialTheme.typography.bodyMedium,
                color = TopwayPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PillButton(text: String, highlighted: Boolean = false, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (highlighted) TopwayPalette.OnAccent else TopwayPalette.TextPrimary,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (highlighted) TopwayPalette.Accent else TopwayPalette.Control)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = TopwayDimens.MinTouchTarget)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    )
}

@Composable
private fun DevicePickerDialog(
    devices: List<PairedDevice>,
    onSelect: (PairedDevice) -> Unit,
    onBluetoothSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onBluetoothSettings) {
                Text(stringResource(R.string.settings_bt_settings), color = TopwayPalette.AccentLight)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = TopwayPalette.TextSecondary) }
        },
        title = { Text(stringResource(R.string.devices_title)) },
        text = {
            if (devices.isEmpty()) {
                Text(stringResource(R.string.devices_empty))
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    devices.forEach { device ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(TopwayDimens.ControlRadius))
                                .clickable(role = Role.Button, onClickLabel = device.name) { onSelect(device) }
                                .heightIn(min = TopwayDimens.MinTouchTarget)
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                        ) {
                            Text(device.name, style = MaterialTheme.typography.bodyLarge, color = TopwayPalette.TextPrimary)
                            Text(device.address, style = MaterialTheme.typography.labelMedium, color = TopwayPalette.TextSecondary)
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        },
        containerColor = TopwayPalette.PanelRaised,
    )
}
