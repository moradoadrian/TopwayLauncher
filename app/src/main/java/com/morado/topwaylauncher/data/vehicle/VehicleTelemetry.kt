package com.morado.topwaylauncher.data.vehicle

/** De dónde sale la velocidad mostrada. Nunca se mezclan valores de fuentes distintas. */
enum class SpeedSource { Obd, Gps, Demo }

/** Una lectura de velocidad y el instante (reloj monotónico, ms) en que se recibió. */
data class SpeedSample(val kmh: Int, val atMillis: Long)

/** Por qué falló la conexión OBD-II. */
enum class ObdError {
    /** No se pudo abrir el canal Bluetooth con el adaptador (apagado, lejos u ocupado). */
    ConnectFailed,

    /** El adaptador no contestó a los comandos AT: puede no ser un ELM327 compatible. */
    AdapterNotResponding,

    /** El adaptador contesta, pero el vehículo no (contacto apagado o protocolo no detectado). */
    NoVehicleResponse,

    /** Se perdió el canal Bluetooth durante las lecturas. */
    ConnectionLost,
}

sealed interface ObdStatus {
    data object Disabled : ObdStatus
    data object NotConfigured : ObdStatus
    data object NeedsPermission : ObdStatus
    data object BluetoothUnavailable : ObdStatus
    data object BluetoothOff : ObdStatus
    data object Connecting : ObdStatus

    /** Conectado y consultando la velocidad. [protocol] es el que informa el adaptador (ATDP). */
    data class Connected(val protocol: String?) : ObdStatus

    /** El vehículo respondió, pero declara que no admite el PID 0D (velocidad). */
    data object SpeedUnsupported : ObdStatus
    data class Error(val reason: ObdError) : ObdStatus
}

data class ObdSnapshot(val status: ObdStatus, val sample: SpeedSample? = null)

sealed interface GpsStatus {
    data object Disabled : GpsStatus
    data object NeedsPermission : GpsStatus

    /** El equipo no tiene proveedor GPS. */
    data object Unavailable : GpsStatus
    data object LocationOff : GpsStatus

    /** Recibiendo actualizaciones del GPS (puede no haber señal todavía). */
    data object On : GpsStatus
}

data class GpsSnapshot(val status: GpsStatus, val sample: SpeedSample? = null)

/** Estado completo que consume la interfaz. */
data class VehicleState(
    /** null = desconocida. Nunca se sustituye por 0. */
    val speedKmh: Int? = null,
    val source: SpeedSource? = null,
    /** Hubo lecturas, pero ninguna es reciente: no se muestra el último valor como actual. */
    val stale: Boolean = false,
    val obd: ObdStatus = ObdStatus.Disabled,
    val gps: GpsStatus = GpsStatus.Disabled,
    /** El GPS entregó una velocidad reciente (aunque se esté mostrando la de OBD-II). */
    val gpsHasFix: Boolean = false,
)
