package com.morado.topwaylauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NowPlayingRulesTest {

    private data class Session(val name: String, val playing: Boolean, val hasTrack: Boolean)

    private fun pick(vararg sessions: Session) =
        NowPlayingRules.pick(sessions.toList(), isPlaying = { it.playing }, hasTrack = { it.hasTrack })

    @Test
    fun `gana la sesion que esta sonando aunque no sea la primera`() {
        val paused = Session("pausada", playing = false, hasTrack = true)
        val playing = Session("sonando", playing = true, hasTrack = true)
        assertEquals(playing, pick(paused, playing))
    }

    @Test
    fun `si ninguna suena se muestra la primera con pista`() {
        val empty = Session("vacia", playing = false, hasTrack = false)
        val first = Session("primera", playing = false, hasTrack = true)
        val second = Session("segunda", playing = false, hasTrack = true)
        assertEquals(first, pick(empty, first, second))
    }

    @Test
    fun `sesiones sin pista y en pausa no se muestran`() {
        assertNull(pick(Session("vacia", playing = false, hasTrack = false)))
    }

    @Test
    fun `sin sesiones no hay nada que mostrar`() {
        assertNull(pick())
    }
}
