package com.morado.topwaylauncher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.morado.topwaylauncher.data.AppInfo
import com.morado.topwaylauncher.data.AppRepository
import com.morado.topwaylauncher.data.LauncherApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppsUiState {
    data object Loading : AppsUiState
    data object Empty : AppsUiState
    data object Error : AppsUiState
    data class Success(val apps: List<LauncherApp>) : AppsUiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    private val _appsState = MutableStateFlow<AppsUiState>(AppsUiState.Loading)
    val appsState: StateFlow<AppsUiState> = _appsState.asStateFlow()

    private var loadJob: Job? = null

    /**
     * Recarga la lista. Si ya hay datos se actualiza en segundo plano sin mostrar
     * de nuevo el indicador de carga (evita parpadeos al volver a la pantalla).
     */
    fun refresh() {
        loadJob?.cancel()
        if (_appsState.value !is AppsUiState.Success) {
            _appsState.value = AppsUiState.Loading
        }
        loadJob = viewModelScope.launch {
            _appsState.value = try {
                val apps = repository.loadApps()
                if (apps.isEmpty()) AppsUiState.Empty else AppsUiState.Success(apps)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppsUiState.Error
            }
        }
    }

    fun launch(app: AppInfo): Boolean = repository.launch(app)
}
