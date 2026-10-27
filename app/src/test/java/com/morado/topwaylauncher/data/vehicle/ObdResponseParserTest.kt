package com.morado.topwaylauncher.data.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdResponseParserTest {

    private val speed = ObdResponseParser.PID_VEHICLE_SPEED

    @Test
    fun `velocidad con espacios y prompt`() {
        val reply = ObdResponseParser.parseMode01("41 0D 3C \r\r>", speed)
        assertEquals(ObdReply.Data(listOf(0x3C)), reply)
        assertEquals(60, ObdResponseParser.speedKmh(reply))
    }

    @Test
    fun `velocidad sin espacios (ATS0)`() {
        assertEquals(100, ObdResponseParser.speedKmh(ObdResponseParser.parseMode01("410D64\r\r>", speed)))
    }

    @Test
    fun `velocidad cero es un valor real, no ausencia de datos`() {
        assertEquals(0, ObdResponseParser.speedKmh(ObdResponseParser.parseMode01("410D00\r>", speed)))
    }

    @Test
    fun `valor maximo del PID es 255`() {
        assertEquals(255, ObdResponseParser.speedKmh(ObdResponseParser.parseMode01("410DFF\r>", speed)))
    }

    @Test
    fun `ignora SEARCHING y el eco del comando`() {
        val reply = ObdResponseParser.parseMode01("010D\rSEARCHING...\r410D2A\r\r>", speed)
        assertEquals(42, ObdResponseParser.speedKmh(reply))
    }

    @Test
    fun `con varias ECU toma la primera respuesta valida`() {
        assertEquals(50, ObdResponseParser.speedKmh(ObdResponseParser.parseMode01("410D32\r410D33\r>", speed)))
    }

    @Test
    fun `NO DATA no se convierte en cero`() {
        val reply = ObdResponseParser.parseMode01("NO DATA\r\r>", speed)
        assertEquals(ObdReply.NoData, reply)
        assertNull(ObdResponseParser.speedKmh(reply))
    }

    @Test
    fun `errores del adaptador`() {
        listOf("UNABLE TO CONNECT", "CAN ERROR", "BUS INIT: ...ERROR", "STOPPED", "?").forEach { error ->
            val reply = ObdResponseParser.parseMode01("$error\r\r>", speed)
            assertTrue("$error -> $reply", reply is ObdReply.AdapterError)
            assertNull(ObdResponseParser.speedKmh(reply))
        }
    }

    @Test
    fun `respuesta negativa de la ECU`() {
        assertEquals(ObdReply.Unsupported, ObdResponseParser.parseMode01("7F 01 12\r>", speed))
    }

    @Test
    fun `respuesta a otro PID no se toma como velocidad`() {
        assertEquals(ObdReply.Malformed, ObdResponseParser.parseMode01("410C1AF8\r>", speed))
    }

    @Test
    fun `respuesta sin bytes de datos es invalida`() {
        assertNull(ObdResponseParser.speedKmh(ObdResponseParser.parseMode01("410D\r>", speed)))
    }

    @Test
    fun `basura o texto vacio es invalido`() {
        assertEquals(ObdReply.Malformed, ObdResponseParser.parseMode01("41 0D Z\r>", speed))
        assertEquals(ObdReply.Malformed, ObdResponseParser.parseMode01(">", speed))
        assertEquals(ObdReply.Malformed, ObdResponseParser.parseMode01("410D3\r>", speed))
    }

    @Test
    fun `mapa de PID admitidos (ejemplo SAE J1979 BE 1F A8 13)`() {
        val pids = ObdResponseParser.supportedPids("41 00 BE 1F A8 13\r>")!!
        // BE = 1011 1110 -> 01, 03, 04, 05, 06, 07
        assertTrue(pids.containsAll(listOf(0x01, 0x03, 0x04, 0x05, 0x06, 0x07)))
        assertTrue(0x02 !in pids)
        // 1F = 0001 1111 -> 0C, 0D, 0E, 0F, 10
        assertTrue(pids.containsAll(listOf(0x0C, 0x0D, 0x0E, 0x0F, 0x10)))
        assertTrue(0x0B !in pids)
        // 13 = 0001 0011 -> 1C, 1F, 20
        assertTrue(pids.containsAll(listOf(0x1C, 0x1F, 0x20)))
    }

    @Test
    fun `mapa de PID sin velocidad`() {
        val pids = ObdResponseParser.supportedPids("4100BE07A813\r>")!!
        assertTrue(ObdResponseParser.PID_VEHICLE_SPEED !in pids)
    }

    @Test
    fun `mapas de varias ECU se combinan`() {
        val pids = ObdResponseParser.supportedPids("4100 80000000\r4100 00080000\r>")!!
        assertEquals(setOf(0x01, 0x0D), pids)
    }

    @Test
    fun `sin mapa valido no hay lista`() {
        assertNull(ObdResponseParser.supportedPids("NO DATA\r>"))
        assertNull(ObdResponseParser.supportedPids("UNABLE TO CONNECT\r>"))
        assertNull(ObdResponseParser.supportedPids("4100BE\r>"))
    }

    @Test
    fun `rpm y temperatura del refrigerante`() {
        assertEquals(1726, ObdResponseParser.engineRpm(ObdResponseParser.parseMode01("410C1AF8\r>", 0x0C)))
        assertEquals(90, ObdResponseParser.coolantCelsius(ObdResponseParser.parseMode01("410582\r>", 0x05)))
        assertEquals(-40, ObdResponseParser.coolantCelsius(ObdResponseParser.parseMode01("410500\r>", 0x05)))
        assertNull(ObdResponseParser.engineRpm(ObdResponseParser.parseMode01("410C1A\r>", 0x0C)))
    }
}
