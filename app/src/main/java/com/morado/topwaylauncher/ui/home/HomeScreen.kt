package com.morado.topwaylauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.data.LauncherApp
import com.morado.topwaylauncher.ui.components.AmbientBackground
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
    var showMediaAccess by rememberSaveable { mutableStateOf(false) }

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
        // Si se sabe qué app está sonando se abre esa; si no, la de música predeterminada.
        val playing = (uiState.nowPlaying as? NowPlayingUiState.Active)?.app
        val music = playing ?: uiState.music
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

    if (showMediaAccess) {
        MediaAccessDialog(
            onConfirm = {
                showMediaAccess = false
                if (!viewModel.openMediaAccess()) showMessage(resources.getString(R.string.media_access_error))
            },
            onDismiss = { showMediaAccess = false },
        )
    }

    HomeScreen(
        state = uiState,
        section = section,
        snackbarHostState = snackbarHostState,
        onHome = { section = HomeSection.Dashboard },
        onNavigation = onNavigation,
        onMusic = onMusic,
        media = MediaActions(
            onOpen = onMusic,
            onEnableAccess = { showMediaAccess = true },
            onPlayPause = viewModel::playPause,
            onPrevious = viewModel::skipPrevious,
            onNext = viewModel::skipNext,
        ),
        onApps = { section = HomeSection.Apps },
        onSettings = {
            if (!viewModel.openSettings()) showMessage(resources.getString(R.string.settings_error))
        },
        onRetry = viewModel::refresh,
        onAppClick = onAppClick,
    )
}

/**
 * Explica qué implica el acceso a notificaciones antes de llevar al usuario a Ajustes, donde
 * Android muestra su propio aviso. Así la decisión se toma sabiendo para qué se usa.
 */
@Composable
private fun MediaAccessDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.media_access_continue), color = TopwayPalette.AccentLight)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.media_access_cancel), color = TopwayPalette.TextSecondary)
            }
        },
        title = { Text(stringResource(R.string.media_access_title)) },
        // Desplazable: en pantallas de coche muy bajas el texto podría no caber.
        text = { Text(stringResource(R.string.media_access_message), Modifier.verticalScroll(rememberScrollState())) },
        containerColor = TopwayPalette.PanelRaised,
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
    media: MediaActions,
    onApps: () -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
    onAppClick: (LauncherApp) -> Unit,
) {
    // Los márgenes del sistema (barras, recorte de cámara) se aplican por zona y no a toda la
    // pantalla: así el fondo de la barra llega hasta el borde y solo su contenido se aparta.
    val safe = WindowInsets.safeDrawing
    val ambientTint = (state.nowPlaying as? NowPlayingUiState.Active)?.track?.ambientColor?.let(::Color)
    AmbientBackground(tint = ambientTint, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
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
                            .background(TopwayPalette.Rail)
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
                                media = media,
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
}
