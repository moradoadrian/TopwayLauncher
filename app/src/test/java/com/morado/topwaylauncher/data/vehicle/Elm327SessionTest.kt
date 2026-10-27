package com.morado.topwaylauncher.data.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Elm327SessionTest {

    @Test
    fun `inicializa el adaptador y detecta que admite velocidad`() {
        val link = FakeObdLink { command, _ -> FakeObdLink.healthyReply(command) }
        val info = Elm327Session(link).initialize()
        assertTrue(info.speedSupported)
        assertEquals("ISO 15765-4 (CAN 11/500)", info.protocol)
        assertEquals(listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0", "0100", "ATDP"), link.sent)
    }

    @Test
    fun `solo envia configuracion AT y lecturas de modo 01`() {
        val link = FakeObdLink { command, _ -> FakeObdLink.healthyReply(command) }
        val session = Elm327Session(link)
        session.initialize()
        repeat(3) { session.readSpeed() }
        link.sent.forEach { command ->
            assertTrue("Comando no permitido: $command", command.startsWith("AT") || command.startsWith("01"))
        }
    }

    @Test
    fun `vehiculo que no admite el PID 0D`() {
        val link = FakeObdLink { command, _ ->
            if (command == "0100") "4100BE07A813\r>" else FakeObdLink.healthyReply(command)
        }
        assertFalse(Elm327Session(link).initialize().speedSupported)
    }

    @Test(expected = ObdNoVehicleException::class)
    fun `contacto apagado - el vehiculo no contesta`() {
        val link = FakeObdLink { command, _ ->
            if (command == "0100") "SEARCHING...\rUNABLE TO CONNECT\r\r>" else FakeObdLink.healthyReply(command)
        }
        Elm327Session(link).initialize()
    }

    @Test(expected = ObdAdapterException::class)
    fun `adaptador que no acepta comandos AT`() {
        val link = FakeObdLink { command, _ -> if (command == "ATE0") "?\r>" else FakeObdLink.healthyReply(command) }
        Elm327Session(link).initialize()
    }

    @Test(expected = ObdTimeoutException::class)
    fun `adaptador mudo agota el tiempo`() {
        Elm327Session(FakeObdLink { _, _ -> null }).initialize()
    }

    @Test
    fun `protocolo desconocido no impide conectar`() {
        val link = FakeObdLink { command, _ -> if (command == "ATDP") null else FakeObdLink.healthyReply(command) }
        val info = Elm327Session(link).initialize()
        assertTrue(info.speedSupported)
        assertEquals(null, info.protocol)
    }

    @Test
    fun `lee la velocidad`() {
        val link = FakeObdLink { command, _ -> FakeObdLink.healthyReply(command, speedHex = "58") }
        assertEquals(88, ObdResponseParser.speedKmh(Elm327Session(link).readSpeed()))
    }
}
