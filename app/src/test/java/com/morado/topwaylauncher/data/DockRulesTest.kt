package com.morado.topwaylauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DockRulesTest {

    private val maps = AppInfo("Mapas", "com.maps", "Main")
    private val music = AppInfo("Música", "com.music", "Main")
    private val radio = AppInfo("Radio", "com.radio", "Main")
    private val phone = AppInfo("Teléfono", "com.phone", "Main")
    private val video = AppInfo("Video", "com.video", "Main")
    private val apps = listOf(maps, music, radio, phone, video)

    @Test
    fun `una app abierta pasa al principio sin duplicarse`() {
        val result = DockRules.pushRecent(listOf("a", "b", "c"), "b")
        assertEquals(listOf("b", "a", "c"), result)
    }

    @Test
    fun `las recientes se limitan al maximo`() {
        val result = DockRules.pushRecent(listOf("a", "b", "c"), "d", max = 3)
        assertEquals(listOf("d", "a", "b"), result)
    }

    @Test
    fun `sin recientes el dock usa las apps predeterminadas`() {
        val dock = DockRules.build(apps, recentKeys = emptyList(), defaultPackages = listOf("com.maps", "com.music"))
        assertEquals(listOf(maps, music), dock)
    }

    @Test
    fun `recientes primero, luego predeterminadas, sin repetir y hasta el tamano del dock`() {
        val dock = DockRules.build(
            apps,
            recentKeys = listOf(radio.key, maps.key, phone.key),
            defaultPackages = listOf("com.maps", "com.music", "com.video"),
            size = 4,
        )
        assertEquals(listOf(radio, maps, phone, music), dock)
    }

    @Test
    fun `ignora recientes y predeterminadas que ya no estan instaladas`() {
        val dock = DockRules.build(
            apps,
            recentKeys = listOf("com.removed/Main", music.key),
            defaultPackages = listOf("com.notinstalled"),
        )
        assertEquals(listOf(music), dock)
    }
}
