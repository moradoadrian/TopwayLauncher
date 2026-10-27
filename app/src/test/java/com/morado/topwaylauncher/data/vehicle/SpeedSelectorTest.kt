package com.morado.topwaylauncher.data.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedSelectorTest {

    private val now = 100_000L

    @Test
    fun `sin fuentes la velocidad es desconocida, no cero`() {
        val selection = SpeedSelector.select(obd = null, gps = null, demo = null, nowMillis = now)
        assertNull(selection.kmh)
        assertNull(selection.source)
        assertFalse(selection.stale)
    }

    @Test
    fun `OBD reciente tiene prioridad sobre GPS`() {
        val selection = SpeedSelector.select(
            obd = SpeedSample(62, now - 500),
            gps = SpeedSample(60, now - 100),
            demo = null,
            nowMillis = now,
        )
        assertEquals(62, selection.kmh)
        assertEquals(SpeedSource.Obd, selection.source)
    }

    @Test
    fun `OBD antiguo cede al GPS sin mezclar valores`() {
        val selection = SpeedSelector.select(
            obd = SpeedSample(62, now - 10_000),
            gps = SpeedSample(58, now - 1_000),
            demo = null,
            nowMillis = now,
        )
        assertEquals(58, selection.kmh)
        assertEquals(SpeedSource.Gps, selection.source)
    }

    @Test
    fun `una lectura antigua no se muestra como actual`() {
        val selection = SpeedSelector.select(obd = SpeedSample(80, now - 10_000), gps = null, demo = null, nowMillis = now)
        assertNull(selection.kmh)
        assertNull(selection.source)
        assertTrue(selection.stale)
    }

    @Test
    fun `cero reciente es una velocidad valida`() {
        val selection = SpeedSelector.select(obd = SpeedSample(0, now - 200), gps = null, demo = null, nowMillis = now)
        assertEquals(0, selection.kmh)
        assertEquals(SpeedSource.Obd, selection.source)
    }

    @Test
    fun `la demo nunca reemplaza datos reales`() {
        val selection = SpeedSelector.select(
            obd = null,
            gps = SpeedSample(30, now - 500),
            demo = SpeedSample(99, now),
            nowMillis = now,
        )
        assertEquals(SpeedSource.Gps, selection.source)
    }

    @Test
    fun `la demo se identifica como tal`() {
        val selection = SpeedSelector.select(obd = null, gps = null, demo = SpeedSample(40, now), nowMillis = now)
        assertEquals(40, selection.kmh)
        assertEquals(SpeedSource.Demo, selection.source)
    }

    @Test
    fun `el plazo OBD crece con el intervalo de consulta`() {
        assertEquals(3_000L, SpeedSelector.obdMaxAge(1_000))
        assertEquals(9_000L, SpeedSelector.obdMaxAge(3_000))
        val sample = SpeedSample(50, now - 5_000)
        assertNull(SpeedSelector.select(sample, null, null, now, SpeedSelector.obdMaxAge(1_000)).kmh)
        assertEquals(50, SpeedSelector.select(sample, null, null, now, SpeedSelector.obdMaxAge(3_000)).kmh)
    }

    @Test
    fun `lectura con marca de tiempo futura se descarta`() {
        val selection = SpeedSelector.select(obd = SpeedSample(50, now + 5_000), gps = null, demo = null, nowMillis = now)
        assertNull(selection.kmh)
    }
}
