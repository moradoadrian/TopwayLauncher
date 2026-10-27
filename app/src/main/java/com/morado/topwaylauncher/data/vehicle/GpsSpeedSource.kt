package com.morado.topwaylauncher.data.vehicle

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.roundToInt

/**
 * Velocidad que informa el receptor GPS (LocationManager, sin servicios de Google). Solo se
 * usa si el usuario lo activa y concede el permiso. Las ubicaciones no se guardan ni se
 * envían: de cada una solo se toma la velocidad y se descarta.
 */
class GpsSpeedSource(context: Context, private val clock: () -> Long) {

    private val appContext = context.applicationContext

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // Comprobado con hasPermission().
    fun updates(): Flow<GpsSnapshot> = callbackFlow {
        val manager = appContext.getSystemService(LocationManager::class.java)
        when {
            manager == null || LocationManager.GPS_PROVIDER !in manager.allProviders ->
                trySend(GpsSnapshot(GpsStatus.Unavailable))
            !hasPermission() -> trySend(GpsSnapshot(GpsStatus.NeedsPermission))
            else -> {
                var last: SpeedSample? = null
                fun status() = if (LocationManagerCompat.isLocationEnabled(manager) &&
                    manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ) GpsStatus.On else GpsStatus.LocationOff

                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        speedOf(location)?.let { last = SpeedSample(it, clock()) }
                        trySend(GpsSnapshot(status(), last))
                    }

                    override fun onProviderEnabled(provider: String) {
                        trySend(GpsSnapshot(GpsStatus.On, last))
                    }

                    override fun onProviderDisabled(provider: String) {
                        trySend(GpsSnapshot(GpsStatus.LocationOff, last))
                    }

                    // Abstracto antes de Android 10: sin esta implementación fallaría en equipos viejos.
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }
                trySend(GpsSnapshot(status()))
                try {
                    manager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_MS, 0f, listener, Looper.getMainLooper(),
                    )
                } catch (e: SecurityException) {
                    trySend(GpsSnapshot(GpsStatus.NeedsPermission))
                }
                awaitClose { manager.removeUpdates(listener) }
                return@callbackFlow
            }
        }
        awaitClose()
    }

    /** km/h si la ubicación trae una velocidad creíble; null si no la trae o es imprecisa. */
    private fun speedOf(location: Location): Int? {
        if (!location.hasSpeed()) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            location.hasSpeedAccuracy() && location.speedAccuracyMetersPerSecond > MAX_SPEED_ERROR_MS
        ) return null
        val metersPerSecond = location.speed
        if (metersPerSecond < 0f || metersPerSecond > MAX_PLAUSIBLE_MS) return null
        return (metersPerSecond * 3.6f).roundToInt()
    }

    private companion object {
        const val UPDATE_INTERVAL_MS = 1_000L
        const val MAX_SPEED_ERROR_MS = 3f // ±11 km/h
        const val MAX_PLAUSIBLE_MS = 90f // 324 km/h
    }
}
