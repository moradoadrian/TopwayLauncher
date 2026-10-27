package com.morado.topwaylauncher.data.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleIdentityTest {

    @Test
    fun `marca y modelo se unen en mayusculas`() {
        assertEquals("NISSAN ALTIMA 2007", VehicleIdentity.title("Nissan", "Altima 2007"))
    }

    @Test
    fun `se recortan y colapsan los espacios`() {
        assertEquals("NISSAN ALTIMA 2007", VehicleIdentity.title("  nissan ", " altima   2007 "))
    }

    @Test
    fun `basta con la marca o con el modelo`() {
        assertEquals("NISSAN", VehicleIdentity.title("Nissan", " "))
        assertEquals("ALTIMA", VehicleIdentity.title("", "Altima"))
    }

    @Test
    fun `sin datos no hay titulo`() {
        assertNull(VehicleIdentity.title("", ""))
        assertNull(VehicleIdentity.title("  ", "\t"))
    }

    @Test
    fun `al escribir se quitan saltos de linea y se limita la longitud`() {
        assertEquals("Altima", VehicleIdentity.sanitize("Alti\nma"))
        assertEquals(VehicleIdentity.MAX_LENGTH, VehicleIdentity.sanitize("x".repeat(40)).length)
    }

    @Test
    fun `al escribir se conservan los espacios para poder seguir tecleando`() {
        assertEquals("Altima ", VehicleIdentity.sanitize("Altima "))
    }
}
