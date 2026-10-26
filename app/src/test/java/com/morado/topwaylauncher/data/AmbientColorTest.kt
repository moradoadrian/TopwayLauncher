package com.morado.topwaylauncher.data

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientColorTest {

    private fun red(c: Int) = (c shr 16) and 0xFF
    private fun green(c: Int) = (c shr 8) and 0xFF
    private fun blue(c: Int) = c and 0xFF

    @Test
    fun `una caratula gris o negra no tiene color ambiental`() {
        val gray = IntArray(100) { if (it % 2 == 0) 0xFF808080.toInt() else 0xFF000000.toInt() }
        assertNull(AmbientColor.dominant(gray))
    }

    @Test
    fun `gana el matiz que ocupa mas superficie viva`() {
        // 70 % naranja, 20 % azul, 10 % negro.
        val pixels = IntArray(100) {
            when {
                it < 70 -> 0xFFE07A3F.toInt()
                it < 90 -> 0xFF2050C0.toInt()
                else -> 0xFF000000.toInt()
            }
        }
        val color = AmbientColor.dominant(pixels)!!
        assertTrue("debe ser naranja", red(color) > green(color) && green(color) > blue(color))
    }

    @Test
    fun `los pixeles transparentes no cuentan`() {
        val pixels = IntArray(100) { 0x00E07A3F }
        assertNull(AmbientColor.dominant(pixels))
    }

    @Test
    fun `el tono baja un neon y sube un color casi negro`() {
        val neon = AmbientColor.tone(0xFF00FF00.toInt())
        assertTrue("neón atenuado", green(neon) <= (0.75f * 255).toInt() + 1)
        val dark = AmbientColor.tone(0xFF200008.toInt())
        assertTrue("oscuro aclarado", red(dark) >= (0.5f * 255).toInt() - 1)
    }
}
