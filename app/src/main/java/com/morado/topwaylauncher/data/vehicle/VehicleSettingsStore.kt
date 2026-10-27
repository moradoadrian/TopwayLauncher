package com.morado.topwaylauncher.data.vehicle

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Todo desactivado por defecto: ninguna fuente funciona hasta que el usuario la configura. */
data class VehicleSettings(
    val obdEnabled: Boolean = false,
    val obdAddress: String? = null,
    val obdName: String? = null,
    val pollIntervalMs: Long = DEFAULT_POLL_MS,
    val gpsEnabled: Boolean = false,
    /** Solo tiene efecto en compilaciones de depuración. */
    val demoEnabled: Boolean = false,
    val brand: String = "",
    val model: String = "",
) {
    /** Lo que se muestra en Inicio y en Conducción, o null si el usuario no lo escribió. */
    val vehicleTitle: String? get() = VehicleIdentity.title(brand, model)

    companion object {
        const val DEFAULT_POLL_MS = 1_000L
        val POLL_OPTIONS_MS = listOf(1_000L, 2_000L, 3_000L)
    }
}

/** Preferencias locales del vehículo. No contienen ubicaciones ni datos de trayectos. */
class VehicleSettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    val settings: StateFlow<VehicleSettings> = state.asStateFlow()

    fun update(transform: (VehicleSettings) -> VehicleSettings) {
        state.update(transform)
        val value = state.value
        prefs.edit {
            putBoolean(KEY_OBD_ENABLED, value.obdEnabled)
            putString(KEY_OBD_ADDRESS, value.obdAddress)
            putString(KEY_OBD_NAME, value.obdName)
            putLong(KEY_POLL_MS, value.pollIntervalMs)
            putBoolean(KEY_GPS_ENABLED, value.gpsEnabled)
            putBoolean(KEY_DEMO_ENABLED, value.demoEnabled)
            putString(KEY_BRAND, value.brand)
            putString(KEY_MODEL, value.model)
        }
    }

    private fun read() = VehicleSettings(
        obdEnabled = prefs.getBoolean(KEY_OBD_ENABLED, false),
        obdAddress = prefs.getString(KEY_OBD_ADDRESS, null),
        obdName = prefs.getString(KEY_OBD_NAME, null),
        pollIntervalMs = prefs.getLong(KEY_POLL_MS, VehicleSettings.DEFAULT_POLL_MS)
            .takeIf { it in VehicleSettings.POLL_OPTIONS_MS } ?: VehicleSettings.DEFAULT_POLL_MS,
        gpsEnabled = prefs.getBoolean(KEY_GPS_ENABLED, false),
        demoEnabled = prefs.getBoolean(KEY_DEMO_ENABLED, false),
        brand = VehicleIdentity.sanitize(prefs.getString(KEY_BRAND, null).orEmpty()),
        model = VehicleIdentity.sanitize(prefs.getString(KEY_MODEL, null).orEmpty()),
    )

    private companion object {
        const val PREFS = "vehicle"
        const val KEY_OBD_ENABLED = "obd_enabled"
        const val KEY_OBD_ADDRESS = "obd_address"
        const val KEY_OBD_NAME = "obd_name"
        const val KEY_POLL_MS = "poll_ms"
        const val KEY_GPS_ENABLED = "gps_enabled"
        const val KEY_DEMO_ENABLED = "demo_enabled"
        const val KEY_BRAND = "brand"
        const val KEY_MODEL = "model"
    }
}
