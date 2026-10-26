package com.morado.topwaylauncher.data

import androidx.compose.ui.graphics.ImageBitmap

/** Datos puros de una actividad lanzable (sin dependencias de Android, fácil de probar). */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
) {
    /** Identificador estable de la actividad, usado en listas y en las apps recientes. */
    val key: String get() = "$packageName/$activityName"
}

/** Aplicación lista para mostrarse: datos + icono ya convertido (null si no se pudo cargar). */
data class LauncherApp(
    val info: AppInfo,
    val icon: ImageBitmap?,
)
