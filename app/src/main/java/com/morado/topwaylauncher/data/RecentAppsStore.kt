package com.morado.topwaylauncher.data

import android.content.Context
import androidx.core.content.edit

/** Guarda localmente qué apps se abrieron por última vez. No requiere permisos. */
class RecentAppsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<String> =
        prefs.getString(KEY, null)?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    fun record(key: String) {
        prefs.edit { putString(KEY, DockRules.pushRecent(load(), key).joinToString(SEPARATOR)) }
    }

    private companion object {
        const val PREFS = "recent_apps"
        const val KEY = "keys"
        const val SEPARATOR = "\n"
    }
}
