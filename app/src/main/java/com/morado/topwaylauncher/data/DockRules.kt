package com.morado.topwaylauncher.data

/** Reglas puras de los accesos rápidos: apps recientes primero, luego las predeterminadas. */
object DockRules {

    /** Máximo de accesos rápidos; la interfaz muestra solo los que caben enteros. */
    const val DOCK_SIZE = 6
    const val RECENT_LIMIT = 8

    /** Coloca [key] al principio, sin duplicados, y limita la lista a [max] elementos. */
    fun pushRecent(current: List<String>, key: String, max: Int = RECENT_LIMIT): List<String> =
        (listOf(key) + current.filter { it != key }).take(max)

    /**
     * Arma el dock: primero las recientes que siguen instaladas y después la primera actividad
     * de cada paquete predeterminado (mapas, música…), sin repetir y hasta [size] elementos.
     */
    fun build(
        apps: List<AppInfo>,
        recentKeys: List<String>,
        defaultPackages: List<String>,
        size: Int = DOCK_SIZE,
    ): List<AppInfo> {
        val byKey = apps.associateBy { it.key }
        val recents = recentKeys.mapNotNull { byKey[it] }
        val defaults = defaultPackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
        return (recents + defaults).distinctBy { it.key }.take(size)
    }
}
