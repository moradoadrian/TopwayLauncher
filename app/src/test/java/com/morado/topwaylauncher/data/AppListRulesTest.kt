package com.morado.topwaylauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AppListRulesTest {

    private val spanish = Locale.forLanguageTag("es-MX")

    private fun app(label: String, pkg: String = "pkg.$label", activity: String = "Main") =
        AppInfo(label = label, packageName = pkg, activityName = activity)

    @Test
    fun `ordena sin distinguir mayusculas ni acentos`() {
        val result = AppListRules.prepare(
            listOf(app("radio"), app("Ájustes"), app("Música"), app("bluetooth")),
            excludePackage = null,
            locale = spanish,
        )
        assertEquals(listOf("Ájustes", "bluetooth", "Música", "radio"), result.map { it.label })
    }

    @Test
    fun `excluye el paquete indicado`() {
        val result = AppListRules.prepare(
            listOf(app("Launcher", pkg = "com.morado.topwaylauncher"), app("Mapas")),
            excludePackage = "com.morado.topwaylauncher",
            locale = spanish,
        )
        assertEquals(listOf("Mapas"), result.map { it.label })
    }

    @Test
    fun `elimina duplicados pero conserva varias actividades del mismo paquete`() {
        val result = AppListRules.prepare(
            listOf(
                app("Radio", pkg = "a", activity = "Fm"),
                app("Radio", pkg = "a", activity = "Fm"),
                app("Radio AM", pkg = "a", activity = "Am"),
            ),
            excludePackage = null,
            locale = spanish,
        )
        assertEquals(2, result.size)
    }

    @Test
    fun `lista vacia devuelve lista vacia`() {
        assertTrue(AppListRules.prepare(emptyList(), excludePackage = null, locale = spanish).isEmpty())
    }
}
