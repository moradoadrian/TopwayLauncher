package com.morado.topwaylauncher.data.vehicle

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.math.abs

/**
 * Une las fuentes de velocidad en un solo [VehicleState]. Cada fuente solo trabaja mientras
 * alguien recoge [state] y está activada en los ajustes; al cambiar un ajuste solo se reinicia
 * la fuente afectada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VehicleTelemetryRepository(
    settings: Flow<VehicleSettings>,
    private val obd: ObdConnectionManager,
    private val gps: GpsSpeedSource,
    /** Cambia cuando el usuario concede o retira permisos, para reintentar las fuentes. */
    permissionsVersion: Flow<Int>,
    private val demoAllowed: Boolean,
    private val clock: () -> Long,
) {

    private data class ObdKey(val enabled: Boolean, val address: String?, val pollMs: Long)

    private val obdFlow: Flow<ObdSnapshot> = settings
        .map { ObdKey(it.obdEnabled, it.obdAddress, it.pollIntervalMs) }
        .distinctUntilChanged()
        .flatMapLatest { key ->
            when {
                !key.enabled -> flowOf(ObdSnapshot(ObdStatus.Disabled))
                key.address == null -> flowOf(ObdSnapshot(ObdStatus.NotConfigured))
                else -> obd.run(key.address, key.pollMs)
            }
        }

    private val gpsFlow: Flow<GpsSnapshot> = combine(
        settings.map { it.gpsEnabled }.distinctUntilChanged(),
        permissionsVersion,
    ) { enabled, _ -> enabled }
        .flatMapLatest { enabled -> if (enabled) gps.updates() else flowOf(GpsSnapshot(GpsStatus.Disabled)) }

    private val demoFlow: Flow<SpeedSample?> = settings
        .map { demoAllowed && it.demoEnabled }
        .distinctUntilChanged()
        .flatMapLatest { enabled -> if (enabled) demoSamples() else flowOf(null) }

    /** Reloj de 1 s para detectar lecturas que dejaron de llegar (p. ej. sin señal GPS). */
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(STALE_CHECK_MS)
        }
    }

    val state: Flow<VehicleState> = combine(
        obdFlow,
        gpsFlow,
        demoFlow,
        settings.map { it.pollIntervalMs }.distinctUntilChanged(),
        ticker,
    ) { obd, gps, demo, pollMs, _ ->
        val now = clock()
        val selection = SpeedSelector.select(
            obd = obd.sample,
            gps = gps.sample,
            demo = demo,
            nowMillis = now,
            obdMaxAgeMs = SpeedSelector.obdMaxAge(pollMs),
        )
        VehicleState(
            speedKmh = selection.kmh,
            source = selection.source,
            stale = selection.stale,
            obd = obd.status,
            gps = gps.status,
            gpsHasFix = gps.sample != null && now - gps.sample.atMillis in 0..SpeedSelector.GPS_MAX_AGE_MS,
        )
    }.distinctUntilChanged()

    /**
     * Datos simulados para probar la interfaz en el emulador: rampa de 0 a 120 km/h y vuelta.
     * Se identifican siempre como [SpeedSource.Demo] y nunca tienen prioridad sobre datos reales.
     */
    private fun demoSamples(): Flow<SpeedSample?> = flow {
        var step = 0
        while (true) {
            val phase = step % DEMO_PERIOD_STEPS
            val kmh = DEMO_MAX_KMH - abs(DEMO_MAX_KMH - phase * 2 * DEMO_MAX_KMH / DEMO_PERIOD_STEPS)
            emit(SpeedSample(kmh, clock()))
            step++
            delay(STALE_CHECK_MS)
        }
    }

    private companion object {
        const val STALE_CHECK_MS = 1_000L
        const val DEMO_MAX_KMH = 120
        const val DEMO_PERIOD_STEPS = 40
    }
}
