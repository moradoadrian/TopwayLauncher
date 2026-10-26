package com.morado.topwaylauncher.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.scale
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Lee qué suena en otras apps con la API pública de sesiones multimedia y les reenvía los
 * controles. Requiere que el usuario conceda el acceso a notificaciones a [MediaListenerService];
 * sin él no se lee nada y la tarjeta de música sigue funcionando como acceso directo.
 */
class MediaSessionRepository(context: Context) {

    private val appContext = context.applicationContext
    private val listener = ComponentName(appContext, MediaListenerService::class.java)

    // Sesión mostrada en la tarjeta; solo se usa desde el hilo principal.
    private var active: MediaController? = null

    fun hasAccess(): Boolean =
        appContext.packageName in NotificationManagerCompat.getEnabledListenerPackages(appContext)

    /**
     * Emite la sesión que se muestra (null si no suena nada) cada vez que cambia. Falla con
     * SecurityException si el acceso no está concedido o se retiró.
     */
    fun observe(): Flow<NowPlaying?> = callbackFlow {
        val manager = appContext.getSystemService(MediaSessionManager::class.java)
        if (manager == null) {
            trySend(null)
            awaitClose()
            return@callbackFlow
        }
        val handler = Handler(Looper.getMainLooper())
        var tracked: Map<MediaSession.Token, TrackedSession> = emptyMap()

        fun publish() {
            val selected = NowPlayingRules.pick(
                tracked.values.toList(),
                isPlaying = { it.isPlaying },
                hasTrack = { it.title != null },
            )
            active = selected?.controller
            trySend(selected?.snapshot())
        }

        fun update(controllers: List<MediaController>) {
            val previous = tracked
            tracked = controllers
                .filter { it.packageName != appContext.packageName }
                .associate { controller ->
                    // Se conserva el seguimiento de las sesiones que siguen activas.
                    controller.sessionToken to (previous[controller.sessionToken]
                        ?: TrackedSession(controller, ::publish).also { controller.registerCallback(it, handler) })
                }
            previous.filterKeys { it !in tracked }.values.forEach { it.controller.unregisterCallback(it) }
            publish()
        }

        val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { update(it.orEmpty()) }
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, listener, handler)
            update(manager.getActiveSessions(listener))
        } catch (e: SecurityException) {
            close(e)
        }
        awaitClose {
            manager.removeOnActiveSessionsChangedListener(sessionsListener)
            tracked.values.forEach { it.controller.unregisterCallback(it) }
            active = null
        }
    }

    fun playPause() {
        val controller = active ?: return
        if (isPlaying(controller.playbackState)) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipNext() {
        active?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        active?.transportControls?.skipToPrevious()
    }

    /**
     * Estado de una sesión, actualizado por sus propios avisos. Guardarlo evita pedir los
     * metadatos (y la carátula) al sistema en cada cambio de posición de la reproducción.
     */
    private class TrackedSession(
        val controller: MediaController,
        private val onChange: () -> Unit,
    ) : MediaController.Callback() {
        private var state: PlaybackState? = controller.playbackState
        private var metadata: MediaMetadata? = controller.metadata
        private var art: Artwork? = artOf(metadata)

        val isPlaying: Boolean get() = isPlaying(state)
        val title: String? get() = metadata.text(MediaMetadata.METADATA_KEY_TITLE, MediaMetadata.METADATA_KEY_DISPLAY_TITLE)

        fun snapshot() = NowPlaying(
            packageName = controller.packageName,
            title = title,
            artist = metadata.text(
                MediaMetadata.METADATA_KEY_ARTIST,
                MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
                MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
            ),
            art = art?.image,
            ambientColor = art?.ambientColor,
            isPlaying = isPlaying,
            canSkipPrevious = state.supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS),
            canSkipNext = state.supports(PlaybackState.ACTION_SKIP_TO_NEXT),
        )

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            this.state = state
            onChange()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            this.metadata = metadata
            art = artOf(metadata)
            onChange()
        }
    }

    /** Carátula lista para pintar y su color ambiental, calculados una vez por pista. */
    private class Artwork(val image: ImageBitmap, val ambientColor: Int?)

    private companion object {
        const val ART_MAX_PX = 256

        // Muestra reducida para el color dominante: basta con unos cientos de píxeles.
        const val SAMPLE_PX = 24

        val ACTIVE_STATES = setOf(
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_CONNECTING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM,
        )

        fun isPlaying(state: PlaybackState?) = state != null && state.state in ACTIVE_STATES

        fun PlaybackState?.supports(action: Long) = this != null && actions and action != 0L

        fun MediaMetadata?.text(vararg keys: String): String? =
            this?.let { metadata -> keys.firstNotNullOfOrNull { metadata.getString(it)?.trim()?.ifEmpty { null } } }

        /** Carátula reducida: las apps pueden publicar imágenes grandes y la tarjeta es pequeña. */
        fun artOf(metadata: MediaMetadata?): Artwork? {
            val bitmap = metadata?.run {
                getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            } ?: return null
            return try {
                Artwork(bitmap.fitWithin(ART_MAX_PX).asImageBitmap(), ambientOf(bitmap))
            } catch (e: RuntimeException) {
                null
            }
        }

        fun ambientOf(bitmap: Bitmap): Int? {
            // Se reduce antes de copiar; la copia en ARGB_8888 permite leer píxeles aunque la app
            // haya publicado un bitmap de hardware.
            val sample = bitmap.scale(SAMPLE_PX, SAMPLE_PX).copy(Bitmap.Config.ARGB_8888, false) ?: return null
            val pixels = IntArray(SAMPLE_PX * SAMPLE_PX)
            sample.getPixels(pixels, 0, SAMPLE_PX, 0, 0, SAMPLE_PX, SAMPLE_PX)
            return AmbientColor.dominant(pixels)?.let(AmbientColor::tone)
        }

        fun Bitmap.fitWithin(maxPx: Int): Bitmap {
            val largest = maxOf(width, height)
            if (largest <= maxPx) return this
            val ratio = maxPx.toFloat() / largest
            return scale((width * ratio).toInt().coerceAtLeast(1), (height * ratio).toInt().coerceAtLeast(1))
        }
    }
}
