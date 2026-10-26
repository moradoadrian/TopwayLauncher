package com.morado.topwaylauncher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.morado.topwaylauncher.data.AppRepository
import com.morado.topwaylauncher.data.DockRules
import com.morado.topwaylauncher.data.ExternalShortcut
import com.morado.topwaylauncher.data.LauncherApp
import com.morado.topwaylauncher.data.RecentAppsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AppsUiState {
    data object Loading : AppsUiState
    data object Empty : AppsUiState
    data object Error : AppsUiState
    data class Success(val apps: List<LauncherApp>) : AppsUiState
}

data class HomeUiState(
    val apps: AppsUiState = AppsUiState.Loading,
    /** Recientes y predeterminadas, para la fila de accesos rápidos. */
    val quickAccess: List<LauncherApp> = emptyList(),
    /** Google Maps u otra app de mapas predeterminada; null si no hay ninguna. */
    val navigation: ExternalShortcut? = null,
    /** App de música predeterminada; null si el sistema no tiene una elegida. */
    val music: ExternalShortcut? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val recentApps = RecentAppsStore(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var defaultQuickPackages: List<String> = emptyList()
    private var loadJob: Job? = null

    /**
     * Recarga apps y accesos. Si ya hay datos se actualiza en segundo plano sin mostrar
     * de nuevo el indicador de carga (evita parpadeos al volver a la pantalla).
     */
    fun refresh() {
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

    private suspend fun buildQuickAccess(apps: List<LauncherApp>): List<LauncherApp> {
        val recentKeys = withContext(Dispatchers.IO) { recentApps.load() }
        val byKey = apps.associateBy { it.info.key }
        return DockRules.build(apps.map { it.info }, recentKeys, defaultQuickPackages)
            .mapNotNull { byKey[it.key] }
    }
}
