package com.morado.topwaylauncher.data

import java.text.Collator
import java.util.Locale

/** Reglas para preparar la lista de aplicaciones antes de mostrarla. */
object AppListRules {

    /**
     * Excluye [excludePackage] (normalmente esta misma app), elimina actividades duplicadas
     * y ordena alfabéticamente sin distinguir mayúsculas ni acentos.
     */
    fun prepare(
        apps: List<AppInfo>,
        excludePackage: String?,
        locale: Locale = Locale.getDefault(),
    ): List<AppInfo> {
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
        return apps
            .asSequence()
            .filter { it.packageName != excludePackage }
            .distinctBy { it.packageName to it.activityName }
            .sortedWith(
                compareBy<AppInfo, String>(collator) { it.label }
                    .thenBy { it.packageName }
                    .thenBy { it.activityName },
            )
            .toList()
    }
}
