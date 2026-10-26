package com.morado.topwaylauncher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.morado.topwaylauncher.data.AppRepository
import com.morado.topwaylauncher.data.DockRules
import com.morado.topwaylauncher.data.ExternalShortcut
import com.morado.topwaylauncher.data.LauncherApp
import com.morado.topwaylauncher.data.MediaSessionRepository
import com.morado.topwaylauncher.data.NowPlaying
import com.morado.topwaylauncher.data.RecentAppsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AppsUiState {
    data object Loading : AppsUiState
    data object Empty : AppsUiState
    data object Error : AppsUiState
    data class Success(val apps: List<LauncherApp>) : AppsUiState
}

/** Qué muestra la tarjeta de música. */
sealed interface NowPlayingUiState {
    /** El usuario no concedió el acceso a notificaciones: no se lee qué suena. */
    data object NoAccess : NowPlayingUiState

    /** Hay acceso, pero ninguna app informa de una pista. */
    data object Idle : NowPlayingUiState

    /** [app] es la app que reproduce (nombre, icono y cómo abrirla); null si no se puede abrir. */
    data class Active(val track: NowPlaying, val app: ExternalShortcut?) : NowPlayingUiState
}

data class HomeUiState(
    val apps: AppsUiState = AppsUiState.Loading,
    /** Recientes y predeterminadas, para la fila de accesos rápidos. */
    val quickAccess: List<LauncherApp> = emptyList(),
    /** Google Maps u otra app de mapas predeterminada; null si no hay ninguna. */
    val navigation: ExternalShortcut? = null,
    /** App de música predeterminada; null si el sistema no tiene una elegida. */
    val music: ExternalShortcut? = null,
    val nowPlaying: NowPlayingUiState = NowPlayingUiState.NoAccess,
    /** true si el sistema tiene la pantalla para conceder el acceso a notificaciones. */
    val canRequestMediaAccess: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val recentApps = RecentAppsStore(application)
    private val media = MediaSessionRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var defaultQuickPackages: List<String> = emptyList()
    private var loadJob: Job? = null
    private var mediaJob: Job? = null
    private val mediaApps = mutableMapOf<String, ExternalShortcut?>()

    init {
        updateMediaAccess()
    }

    /**
     * Recarga apps y accesos. Si ya hay datos se actualiza en segundo plano sin mostrar
     * de nuevo el indicador de carga (evita parpadeos al volver a la pantalla).
     */
    fun refresh() {
        updateMediaAccess()
        loadJob?.cancel()
        if (_uiState.value.apps !is AppsUiState.Success) {
            _uiState.update { it.copy(apps = AppsUiState.Loading) }
        }
        loadJob = viewModelScope.launch {
            // Los accesos externos no dependen de la lista: si esta falla, siguen disponibles.
            val navigation = repository.resolveNavigation()
            val music = repository.resolveMusic()
            _uiState.update { it.copy(navigation = navigation, music = music) }

            val result = try {
                repository.loadApps()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            defaultQuickPackages = repository.defaultDockPackages()
            _uiState.update { state ->
                when {
                    result == null -> state.copy(apps = AppsUiState.Error)
                    result.isEmpty() -> state.copy(apps = AppsUiState.Empty, quickAccess = emptyList())
                    else -> state.copy(apps = AppsUiState.Success(result), quickAccess = buildQuickAccess(result))
                }
            }
        }
    }

    /** Abre la app y la registra como reciente. Devuelve false si no se pudo abrir. */
    fun launch(app: LauncherApp): Boolean {
        if (!repository.launch(app.info)) return false
        viewModelScope.launch {
            withContext(Dispatchers.IO) { recentApps.record(app.info.key) }
            val apps = (_uiState.value.apps as? AppsUiState.Success)?.apps ?: return@launch
            val quick = buildQuickAccess(apps)
            _uiState.update { it.copy(quickAccess = quick) }
        }
        return true
    }

    fun open(shortcut: ExternalShortcut): Boolean = repository.open(shortcut)

    fun openSettings(): Boolean = repository.openSettings()

    fun openMediaAccess(): Boolean = repository.openNotificationAccess()

    fun playPause() = media.playPause()

    fun skipNext() = media.skipNext()

    fun skipPrevious() = media.skipPrevious()

    /**
     * El acceso se concede o se retira fuera de la app (en Ajustes), así que se comprueba al
     * volver a la pantalla: se empieza o se deja de leer la sesión según corresponda.
     */
    private fun updateMediaAccess() {
        val canRequest = repository.canOpenNotificationAccess()
        if (!media.hasAccess()) {
            mediaJob?.cancel()
            mediaJob = null
            _uiState.update { it.copy(nowPlaying = NowPlayingUiState.NoAccess, canRequestMediaAccess = canRequest) }
            return
        }
        _uiState.update { it.copy(canRequestMediaAccess = canRequest) }
        // Una lectura que llegó a perder el acceso ya no recibe sesiones: se empieza otra.
        if (mediaJob?.isActive == true && _uiState.value.nowPlaying != NowPlayingUiState.NoAccess) return
        mediaJob?.cancel()
        mediaJob = viewModelScope.launch {
            media.observe()
                .distinctUntilChanged()
                .map<NowPlaying?, NowPlayingUiState> { track ->
                    when {
                        // Si el acceso se retira con la pantalla abierta, Android deja de enviar
                        // sesiones sin avisar: una lista vacía no significa que no suene nada.
                        !media.hasAccess() -> NowPlayingUiState.NoAccess
                        track == null -> NowPlayingUiState.Idle
                        else -> NowPlayingUiState.Active(track, mediaApp(track.packageName))
                    }
                }
                .catch { e -> if (e is SecurityException) emit(NowPlayingUiState.NoAccess) else throw e }
                .collect { nowPlaying -> _uiState.update { it.copy(nowPlaying = nowPlaying) } }
        }
    }

    private suspend fun mediaApp(pkg: String): ExternalShortcut? =
        mediaApps.getOrPut(pkg) { repository.appShortcut(pkg) }

    private suspend fun buildQuickAccess(apps: List<LauncherApp>): List<LauncherApp> {
        val recentKeys = withContext(Dispatchers.IO) { recentApps.load() }
        val byKey = apps.associateBy { it.info.key }
        return DockRules.build(apps.map { it.info }, recentKeys, defaultQuickPackages)
            .mapNotNull { byKey[it.key] }
    }
}
