package com.morado.topwaylauncher.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Acceso a las aplicaciones instaladas usando solo API públicas de PackageManager.
 * La visibilidad en Android 11+ se resuelve con el bloque <queries> del manifiesto.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    suspend fun loadApps(): List<LauncherApp> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos = queryActivities(launcherIntent).mapNotNull { resolveInfo ->
            val activity = resolveInfo.activityInfo ?: return@mapNotNull null
            AppInfo(
                label = resolveInfo.loadLabel(packageManager).toString().trim()
                    .ifEmpty { activity.packageName },
                packageName = activity.packageName,
                activityName = activity.name,
            )
        }
        AppListRules.prepare(infos, excludePackage = appContext.packageName)
            .map { LauncherApp(info = it, icon = loadIcon(it)) }
    }

    /** Abre la actividad con un intent explícito público. Devuelve false si no se pudo. */
    fun launch(app: AppInfo): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            appContext.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    private fun queryActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

    private fun loadIcon(app: AppInfo) = try {
        packageManager
            .getActivityIcon(ComponentName(app.packageName, app.activityName))
            .toBitmap(ICON_SIZE_PX, ICON_SIZE_PX)
            .asImageBitmap()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    } catch (e: RuntimeException) {
        // Algunos iconos de fabricantes fallan al rasterizarse; se muestra un marcador.
        null
    }

    private companion object {
        const val ICON_SIZE_PX = 144
    }
}
