package com.morado.topwaylauncher.ui.vehicle

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.morado.topwaylauncher.data.vehicle.BluetoothObd
import com.morado.topwaylauncher.data.vehicle.GpsSpeedSource
import com.morado.topwaylauncher.data.vehicle.ObdConnectionManager
import com.morado.topwaylauncher.data.vehicle.PairedDevice
import com.morado.topwaylauncher.data.vehicle.VehicleSettings
import com.morado.topwaylauncher.data.vehicle.VehicleSettingsStore
import com.morado.topwaylauncher.data.vehicle.VehicleState
import com.morado.topwaylauncher.data.vehicle.VehicleTelemetryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class VehicleViewModel(application: Application) : AndroidViewModel(application) {

    private val clock: () -> Long = SystemClock::elapsedRealtime
    private val store = VehicleSettingsStore(application)
    private val bluetooth = BluetoothObd(application)
    private val gps = GpsSpeedSource(application, clock)
    private val permissionsVersion = MutableStateFlow(0)

    /** La fuente de demostración solo existe en compilaciones de depuración. */
    val demoAvailable: Boolean =
        application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    val settings: StateFlow<VehicleSettings> = store.settings

    /**
     * Las fuentes funcionan mientras la pantalla está visible y 5 s más (para que un giro de
     * pantalla no corte la conexión). Con otra app delante no se consulta el adaptador.
     */
    val vehicle: StateFlow<VehicleState> = VehicleTelemetryRepository(
        settings = store.settings,
        obd = ObdConnectionManager(precheck = bluetooth::blockingStatus, connect = bluetooth::open, clock = clock),
        gps = gps,
        permissionsVersion = permissionsVersion,
        demoAllowed = demoAvailable,
        clock = clock,
    ).state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), VehicleState())

    fun needsBluetoothPermission(): Boolean = !bluetooth.hasPermission()

    fun hasLocationPermission(): Boolean = gps.hasPermission()

    fun pairedDevices(): List<PairedDevice> = bluetooth.pairedDevices()

    fun selectAdapter(device: PairedDevice) =
        store.update { it.copy(obdEnabled = true, obdAddress = device.address, obdName = device.name) }

    fun setObdEnabled(enabled: Boolean) = store.update { it.copy(obdEnabled = enabled) }

    fun setPollInterval(ms: Long) = store.update { it.copy(pollIntervalMs = ms) }

    fun setGpsEnabled(enabled: Boolean) = store.update { it.copy(gpsEnabled = enabled) }

    fun setDemoEnabled(enabled: Boolean) = store.update { it.copy(demoEnabled = enabled) }

    /** Tras conceder o retirar un permiso, las fuentes vuelven a comprobarlo. */
    fun onPermissionsChanged() = permissionsVersion.update { it + 1 }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
