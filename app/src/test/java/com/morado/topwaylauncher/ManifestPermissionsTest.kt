package com.morado.topwaylauncher

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Cualquier permiso nuevo debe añadirse aquí a propósito, con su justificación en el
 * manifiesto y en el README. Así no entra ninguno por accidente.
 */
class ManifestPermissionsTest {

    private val justified = setOf(
        "android.permission.BLUETOOTH", // Adaptador OBD-II emparejado, Android ≤ 11.
        "android.permission.BLUETOOTH_CONNECT", // Adaptador OBD-II emparejado, Android 12+.
        "android.permission.ACCESS_FINE_LOCATION", // Velocidad GPS opcional.
        "android.permission.ACCESS_COARSE_LOCATION", // Obligatorio junto al anterior en Android 12+.
    )

    @Test
    fun `solo se declaran permisos justificados`() {
        // Las pruebas unitarias se ejecutan con el directorio del módulo como directorio de trabajo.
        val manifest = File("src/main/AndroidManifest.xml")
        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(manifest)
        val nodes = document.getElementsByTagName("uses-permission")
        val declared = (0 until nodes.length).map {
            nodes.item(it).attributes.getNamedItemNS(ANDROID_NS, "name").nodeValue
        }.toSet()
        assertEquals(justified, declared)
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
