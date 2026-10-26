package com.morado.topwaylauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.data.LauncherApp
import com.morado.topwaylauncher.ui.components.ClockHeader
import com.morado.topwaylauncher.ui.components.HomeSection
import com.morado.topwaylauncher.ui.components.NavRail
import com.morado.topwaylauncher.ui.theme.TopwayDimens
import com.morado.topwaylauncher.ui.theme.TopwayPalette
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var section by rememberSaveable { mutableStateOf(HomeSection.Dashboard) }

    // Recarga al volver a primer plano: recoge apps instaladas o desinstaladas mientras tanto.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    // En la lista de apps, Atrás vuelve a Inicio en lugar de cerrar la pantalla.
    BackHandler(enabled = section == HomeSection.Apps) { section = HomeSection.Dashboard }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val onAppClick: (LauncherApp) -> Unit = { app ->
        if (!viewModel.launch(app)) {
            showMessage(resources.getString(R.string.launch_error, app.info.label))
            viewModel.refresh()
        }
    }
    val onNavigation = {
        val navigation = uiState.navigation
        when {
            navigation == null -> showMessage(resources.getString(R.string.maps_unavailable_message))
            !viewModel.open(navigation) -> {
                showMessage(resources.getString(R.string.launch_error, navigation.label))
                viewModel.refresh()
            }
        }
    }
    val onMusic = {
        val music = uiState.music
        when {
            music == null -> {
                // Sin app predeterminada no hay nada que abrir: se lleva al usuario a elegir una.
                section = HomeSection.Apps
                showMessage(resources.getString(R.string.music_unavailable_message))
            }
            !viewModel.open(music) -> {
                showMessage(resources.getString(R.string.launch_error, music.label))
                viewModel.refresh()
            }
        }
    }

    HomeScreen(
        state = uiState,
        section = section,
        snackbarHostState = snackbarHostState,
        onHome = { section = HomeSection.Dashboard },
        onNavigation = onNavigation,
        onMusic = onMusic,
        onApps = { section = HomeSection.Apps },
        onSettings = {
            if (!viewModel.openSettings()) showMessage(resources.getString(R.string.settings_error))
        },
        onRetry = viewModel::refresh,
        onAppClick = onAppClick,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    section: HomeSection,
    snackbarHostState: SnackbarHostState,
    onHome: () -> Unit,
    onNavigation: () -> Unit,
    onMusic: () -> Unit,
    onApps: () -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
    onAppClick: (LauncherApp) -> Unit,
) {
    // Los márgenes del sistema (barras, recorte de cámara) se aplican por zona y no a toda la
    // pantalla: así el fondo de la barra llega hasta el borde y solo su contenido se aparta.
    val safe = WindowInsets.safeDrawing
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val railWidth = (maxWidth * TopwayDimens.RAIL_WIDTH_FRACTION)
                .coerceIn(TopwayDimens.RailMinWidth, TopwayDimens.RailMaxWidth)
            Row(Modifier.fillMaxSize()) {
                NavRail(
                    selected = section,
                    onHome = onHome,
                    onNavigation = onNavigation,
                    onMusic = onMusic,
                    onApps = onApps,
                    onSettings = onSettings,
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(TopwayPalette.Panel)
                        .windowInsetsPadding(safe.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
                        .width(railWidth),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(safe.only(WindowInsetsSides.End + WindowInsetsSides.Vertical))
                        .padding(
                            horizontal = TopwayDimens.ScreenPaddingHorizontal,
                            vertical = TopwayDimens.ScreenPaddingVertical,
                        ),
                    verticalArrangement = Arrangement.spacedBy(TopwayDimens.Gap),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ClockHeader(Modifier.weight(1f, fill = false))
                        if (section == HomeSection.Apps) {
                            Spacer(Modifier.weight(1f))
                            AppsTitle(state.apps)
                        }
                    }
                    val contentModifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                    when (section) {
                        HomeSection.Dashboard -> DashboardContent(
                            state = state,
                            onNavigation = onNavigation,
                            onMusic = onMusic,
                            onAppClick = onAppClick,
                            onShowAllApps = onApps,
                            modifier = contentModifier,
                        )
                        HomeSection.Apps -> AppsContent(
                            state = state.apps,
                            onRetry = onRetry,
                            onAppClick = onAppClick,
                            modifier = contentModifier,
                        )
                    }
                }
            }
        }
    }
}
