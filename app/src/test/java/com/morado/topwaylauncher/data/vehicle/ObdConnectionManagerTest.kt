package com.morado.topwaylauncher.data.vehicle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ObdConnectionManagerTest {

    private var time = 0L

    private fun manager(
        precheck: () -> ObdStatus? = { null },
        connect: suspend (String) -> ObdLink,
    ) = ObdConnectionManager(
        precheck = precheck,
        connect = connect,
        clock = { ++time },
        ioDispatcher = Dispatchers.Unconfined,
        minBackoffMs = 1,
        maxBackoffMs = 4,
        unsupportedRetryMs = 1,
    )

    private fun <T> collect(flow: kotlinx.coroutines.flow.Flow<T>, count: Int): List<T> = runBlocking {
        withTimeout(5_000) { flow.take(count).toList() }
    }

    @Test
    fun `conecta y entrega velocidades reales`() {
        val link = FakeObdLink { command, _ -> FakeObdLink.healthyReply(command, speedHex = "2D") }
        val snapshots = collect(manager { link }.run("AA:BB", pollIntervalMs = 1), 4)
        assertEquals(ObdStatus.Connecting, snapshots[0].status)
        assertEquals(ObdStatus.Connected("ISO 15765-4 (CAN 11/500)"), snapshots[1].status)
        assertEquals(null, snapshots[1].sample)
        assertEquals(45, snapshots[2].sample?.kmh)
        assertEquals(45, snapshots[3].sample?.kmh)
    }

    @Test
    fun `sin poder conectar informa el error y reintenta`() {
        var attempts = 0
        val snapshots = collect(
            manager { attempts++; throw IOException("sin adaptador") }.run("AA:BB", 1),
            4,
        )
        assertEquals(
            listOf(
                ObdStatus.Connecting,
                ObdStatus.Error(ObdError.ConnectFailed),
                ObdStatus.Connecting,
                ObdStatus.Error(ObdError.ConnectFailed),
            ),
            snapshots.map { it.status },
        )
        assertTrue(attempts >= 2)
    }

    @Test
    fun `bluetooth apagado no intenta conectar`() {
        var attempts = 0
        val first = runBlocking {
            manager(precheck = { ObdStatus.BluetoothOff }) { attempts++; throw IOException() }.run("AA:BB", 1).first()
        }
        assertEquals(ObdStatus.BluetoothOff, first.status)
        assertEquals(0, attempts)
    }

    @Test
    fun `timeouts repetidos cierran el canal y vuelven a conectar`() {
        val links = mutableListOf<FakeObdLink>()
        val snapshots = collect(
            manager {
                FakeObdLink { command, _ -> if (command == "010D") null else FakeObdLink.healthyReply(command) }
                    .also { links += it }
            }.run("AA:BB", 1),
            4,
        )
        assertEquals(ObdStatus.Error(ObdError.NoVehicleResponse), snapshots[2].status)
        assertEquals(ObdStatus.Connecting, snapshots[3].status)
        assertTrue(links.first().closed)
        // Tres intentos de velocidad sin respuesta antes de rendirse, no un bucle sin fin.
        assertEquals(ObdConnectionManager.MAX_CONSECUTIVE_FAILURES, links.first().sent.count { it == "010D" })
    }

    @Test
    fun `desconexion durante las lecturas conserva la ultima lectura pero informa el corte`() {
        val snapshots = collect(
            manager {
                FakeObdLink { command, call ->
                    if (command == "010D" && call > 1) IOException("canal cerrado") else FakeObdLink.healthyReply(command)
                }
            }.run("AA:BB", 1),
            4,
        )
        assertEquals(60, snapshots[2].sample?.kmh)
        assertEquals(ObdStatus.Error(ObdError.ConnectionLost), snapshots[3].status)
        // La lectura anterior se conserva con su marca de tiempo: el selector la tratará como antigua.
        assertEquals(snapshots[2].sample, snapshots[3].sample)
    }

    @Test
    fun `respuestas NO DATA no generan velocidad`() {
        val snapshots = collect(
            manager {
                FakeObdLink { command, _ -> if (command == "010D") "NO DATA\r>" else FakeObdLink.healthyReply(command) }
            }.run("AA:BB", 1),
            3,
        )
        assertTrue(snapshots.all { it.sample == null })
        assertEquals(ObdStatus.Error(ObdError.NoVehicleResponse), snapshots[2].status)
    }

    @Test
    fun `vehiculo sin PID de velocidad`() {
        val snapshots = collect(
            manager {
                FakeObdLink { command, _ -> if (command == "0100") "4100BE07A813\r>" else FakeObdLink.healthyReply(command) }
            }.run("AA:BB", 1),
            2,
        )
        assertEquals(ObdStatus.SpeedUnsupported, snapshots[1].status)
    }

    @Test
    fun `adaptador que no responde a la configuracion`() {
        val snapshots = collect(manager { FakeObdLink { _, _ -> null } }.run("AA:BB", 1), 2)
        assertEquals(ObdStatus.Error(ObdError.AdapterNotResponding), snapshots[1].status)
    }

    @Test
    fun `al dejar de recoger el flujo se cierra el canal`() {
        val link = FakeObdLink { command, _ -> FakeObdLink.healthyReply(command) }
        collect(manager { link }.run("AA:BB", 1), 3)
        assertTrue(link.closed)
    }
}
