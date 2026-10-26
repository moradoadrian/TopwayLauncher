package com.morado.topwaylauncher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.data.LauncherApp
import com.morado.topwaylauncher.ui.components.AppGrid
import com.morado.topwaylauncher.ui.components.ClockPanel
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel()) {
    val appsState by viewModel.appsState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    // Recarga al volver a primer plano: recoge apps instaladas o desinstaladas mientras tanto.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    HomeScreen(
        appsState = appsState,
        snackbarHostState = snackbarHostState,
        onRetry = viewModel::refresh,
        onAppClick = { app ->
            if (!viewModel.launch(app.info)) {
                val message = resources.getString(R.string.launch_error, app.info.label)
                scope.launch { snackbarHostState.showSnackbar(message) }
                viewModel.refresh()
            }
        },
    )
}

@Composable
fun HomeScreen(
    appsState: AppsUiState,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onAppClick: (LauncherApp) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
        ) {
            if (maxWidth >= maxHeight) {
                // Horizontal (caso principal de la radio): reloj a la izquierda, apps a la derecha.
                Row(Modifier.fillMaxSize()) {
                    ClockPanel(
                        modifier = Modifier
                            .weight(0.32f)
                            .fillMaxHeight()
                            .padding(top = 8.dp),
                    )
                    Spacer(Modifier.width(24.dp))
                    AppsSection(
                        state = appsState,
                        onRetry = onRetry,
                        onAppClick = onAppClick,
                        modifier = Modifier.weight(0.68f),
                    )
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    ClockPanel(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                    Spacer(Modifier.height(24.dp))
                    AppsSection(
                        state = appsState,
                        onRetry = onRetry,
                        onAppClick = onAppClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppsSection(
    state: AppsUiState,
    onRetry: () -> Unit,
    onAppClick: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.apps_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        when (state) {
            AppsUiState.Loading -> CenteredMessage(stringResource(R.string.apps_loading)) {
                CircularProgressIndicator()
            }
            AppsUiState.Empty -> CenteredMessage(stringResource(R.string.apps_empty), onRetry)
            AppsUiState.Error -> CenteredMessage(stringResource(R.string.apps_error), onRetry)
            is AppsUiState.Success -> AppGrid(
                apps = state.apps,
                onAppClick = onAppClick,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CenteredMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
    indicator: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            indicator?.invoke()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}
