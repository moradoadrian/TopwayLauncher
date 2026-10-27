package com.morado.topwaylauncher.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
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

    /**
     * Paquetes para llenar los accesos rápidos antes de que haya recientes, en este orden:
     * mapas y música predeterminados, YouTube, teléfono y Ajustes. Solo se muestran los que
     * estén instalados (el dock los cruza con la lista real de apps); si hay varias candidatas
     * sin elegir para una categoría, esa se omite.
     */
    suspend fun defaultDockPackages(): List<String> = withContext(Dispatchers.IO) {
        val byCategory = listOf(Intent.CATEGORY_APP_MAPS, Intent.CATEGORY_APP_MUSIC).map { category ->
            resolvePackage(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category))
        }
        (byCategory + YOUTUBE_PACKAGE + resolvePackage(Intent(Intent.ACTION_DIAL)) + resolvePackage(Intent(Settings.ACTION_SETTINGS)))
            .filterNotNull()
            .distinct()
    }

    /**
     * Acceso de navegación. Primero Google Maps con su URL pública de "abrir el mapa"
     * (sin destino, sin iniciar navegación y sin API key); si no está instalado, la app de
     * mapas predeterminada del sistema. null si no hay ninguna.
     */
    suspend fun resolveNavigation(): ExternalShortcut? = withContext(Dispatchers.IO) {
        val googleMaps = Intent(Intent.ACTION_VIEW, GOOGLE_MAPS_OPEN_URI).setPackage(GOOGLE_MAPS_PACKAGE)
        if (resolve(googleMaps) != null) {
            shortcutFor(GOOGLE_MAPS_PACKAGE, googleMaps)?.copy(isGoogleMaps = true)
        } else {
            defaultAppShortcut(Intent.CATEGORY_APP_MAPS)
        }
    }

    /** App de música predeterminada del sistema, o null si no hay una definida. */
    suspend fun resolveMusic(): ExternalShortcut? = withContext(Dispatchers.IO) {
        defaultAppShortcut(Intent.CATEGORY_APP_MUSIC)
    }

    /** Pantalla principal de [pkg] con su nombre e icono, o null si no tiene una abrible. */
    suspend fun appShortcut(pkg: String): ExternalShortcut? = withContext(Dispatchers.IO) {
        packageManager.getLaunchIntentForPackage(pkg)?.let { shortcutFor(pkg, it) }
    }

    /** true si el sistema tiene la pantalla donde se concede el acceso a notificaciones. */
    fun canOpenNotificationAccess(): Boolean = notificationAccessIntent() != null

    /** Abre la pantalla del sistema donde el usuario concede o retira el acceso a notificaciones. */
    fun openNotificationAccess(): Boolean = notificationAccessIntent()?.let(::start) ?: false

    /**
     * En Android 11+ se intenta ir directo al interruptor de esta app; si el fabricante no
     * incluye esa pantalla, a la lista general de acceso a notificaciones.
     */
    private fun notificationAccessIntent(): Intent? {
        val listener = ComponentName(appContext, MediaListenerService::class.java)
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener.flattenToString())
        } else {
            null
        }
        return listOfNotNull(detail, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            .firstOrNull { resolve(it) != null }
    }

    /** Abre la actividad con un intent explícito público. Devuelve false si no se pudo. */
    fun launch(app: AppInfo): Boolean = start(
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
    )

    /** Abre un acceso externo si sigue habiendo una actividad que lo atienda. */
    fun open(shortcut: ExternalShortcut): Boolean =
        resolve(shortcut.intent) != null && start(Intent(shortcut.intent))

    /** Abre los Ajustes del sistema. */
    fun openSettings(): Boolean {
        val settings = Intent(Settings.ACTION_SETTINGS)
        return resolve(settings) != null && start(settings)
    }

    /** Abre los ajustes de Bluetooth del sistema, donde se empareja el adaptador OBD-II. */
    fun openBluetoothSettings(): Boolean {
        val bluetooth = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        return resolve(bluetooth) != null && start(bluetooth)
    }

    private fun start(intent: Intent): Boolean = try {
        appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    /** Pantalla principal de la app predeterminada para [category], si hay una elegida. */
    private fun defaultAppShortcut(category: String): ExternalShortcut? {
        val pkg = resolvePackage(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category)) ?: return null
        val launch = packageManager.getLaunchIntentForPackage(pkg) ?: return null
        return shortcutFor(pkg, launch)
    }

    private fun shortcutFor(pkg: String, intent: Intent): ExternalShortcut? = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(pkg, 0)
        }
        ExternalShortcut(
            label = packageManager.getApplicationLabel(info).toString(),
            icon = rasterize { packageManager.getApplicationIcon(info) },
            intent = intent,
        )
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private fun queryActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

    private fun resolve(intent: Intent): ResolveInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }

    // Con varias candidatas el sistema devuelve su selector ("android"): no hay predeterminada.
    private fun resolvePackage(intent: Intent): String? =
        resolve(intent)?.activityInfo?.packageName?.takeIf { it != "android" }

    private fun loadIcon(app: AppInfo) = try {
        rasterize { packageManager.getActivityIcon(ComponentName(app.packageName, app.activityName)) }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private inline fun rasterize(drawable: () -> Drawable): ImageBitmap? = try {
        drawable().toBitmap(ICON_SIZE_PX, ICON_SIZE_PX).asImageBitmap()
    } catch (e: RuntimeException) {
        // Algunos iconos de fabricantes fallan al rasterizarse; se muestra un marcador.
        null
    }

    private companion object {
        const val ICON_SIZE_PX = 144
        const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"

        // URL pública de Google Maps (Maps URLs): abre el mapa sin destino ni navegación.
        val GOOGLE_MAPS_OPEN_URI: Uri = "https://www.google.com/maps/@?api=1&map_action=map".toUri()
    }
}
