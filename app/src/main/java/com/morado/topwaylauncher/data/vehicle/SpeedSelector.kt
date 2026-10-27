package com.morado.topwaylauncher.data.vehicle

/**
 * Elige qué velocidad mostrar. Regla fija y sin promedios:
 *
 * 1. OBD-II si su última lectura es reciente (es el sensor del propio vehículo).
 * 2. Si no, GPS si su última lectura es reciente.
 * 3. Si no, la fuente de demostración (solo existe en compilaciones de depuración).
 * 4. Si no, velocidad desconocida. Si alguna fuente llegó a dar datos, se marca como
 *    desactualizada en lugar de seguir mostrando el último valor.
 */
object SpeedSelector {

    const val OBD_MIN_MAX_AGE_MS = 3_000L
    const val GPS_MAX_AGE_MS = 4_000L
    const val DEMO_MAX_AGE_MS = 3_000L

    data class Selection(val kmh: Int?, val source: SpeedSource?, val stale: Boolean)

    /** Antigüedad máxima de una lectura OBD: tres consultas seguidas sin respuesta. */
    fun obdMaxAge(pollIntervalMs: Long): Long = maxOf(OBD_MIN_MAX_AGE_MS, pollIntervalMs * 3)

    fun select(
        obd: SpeedSample?,
        gps: SpeedSample?,
        demo: SpeedSample?,
        nowMillis: Long,
        obdMaxAgeMs: Long = OBD_MIN_MAX_AGE_MS,
    ): Selection {
        fun SpeedSample?.fresh(maxAge: Long) = this != null && nowMillis - atMillis in 0..maxAge
        return when {
            obd.fresh(obdMaxAgeMs) -> Selection(obd!!.kmh, SpeedSource.Obd, stale = false)
            gps.fresh(GPS_MAX_AGE_MS) -> Selection(gps!!.kmh, SpeedSource.Gps, stale = false)
            demo.fresh(DEMO_MAX_AGE_MS) -> Selection(demo!!.kmh, SpeedSource.Demo, stale = false)
            else -> Selection(kmh = null, source = null, stale = obd != null || gps != null)
        }
    }
}
