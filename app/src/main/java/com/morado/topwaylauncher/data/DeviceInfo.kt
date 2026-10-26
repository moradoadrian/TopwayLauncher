package com.morado.topwaylauncher.data

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Datos del equipo obtenidos con API públicas y sin permisos. Sirven para confirmar en la
 * propia radio la versión de Android y la resolución cuando no hay ADB disponible.
 */
data class DeviceInfo(
    val androidRelease: String,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String,
    val board: String,
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
) {
    fun summary(): String =
        "Android $androidRelease (API $sdkInt) · ${widthPx}×$heightPx px · $densityDpi dpi · " +
            "$manufacturer $model · $board"

    companion object {
        fun read(context: Context): DeviceInfo {
            val windowManager = context.getSystemService(WindowManager::class.java)
            val (width, height) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.maximumWindowMetrics.bounds
                bounds.width() to bounds.height()
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(metrics)
                metrics.widthPixels to metrics.heightPixels
            }
            return DeviceInfo(
                androidRelease = Build.VERSION.RELEASE,
                sdkInt = Build.VERSION.SDK_INT,
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                board = Build.BOARD,
                widthPx = width,
                heightPx = height,
                densityDpi = context.resources.displayMetrics.densityDpi,
            )
        }
    }
}
