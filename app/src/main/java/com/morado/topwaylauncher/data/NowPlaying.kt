package com.morado.topwaylauncher.data

import androidx.compose.ui.graphics.ImageBitmap

/** Lo que una app de música informa en su sesión multimedia. Solo datos que ella publica. */
data class NowPlaying(
    val packageName: String,
    val title: String?,
    val artist: String?,
    val art: ImageBitmap?,
    val isPlaying: Boolean,
    val canSkipPrevious: Boolean,
    val canSkipNext: Boolean,
)

object NowPlayingRules {

    /**
     * Sesión que muestra la tarjeta: la primera que esté sonando; si ninguna suena, la primera
     * que tenga una pista cargada (el sistema ordena la lista por prioridad). Una sesión sin
     * pista y en pausa no aporta nada que mostrar, así que se ignora.
     */
    fun <T> pick(sessions: List<T>, isPlaying: (T) -> Boolean, hasTrack: (T) -> Boolean): T? =
        sessions.firstOrNull(isPlaying) ?: sessions.firstOrNull(hasTrack)
}
