package com.morado.topwaylauncher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.data.LauncherApp
import com.morado.topwaylauncher.ui.components.AppPager
import com.morado.topwaylauncher.ui.theme.TopwayDimens
import com.morado.topwaylauncher.ui.theme.TopwayPalette

/**
 * Título de la sección: va en la misma línea que la hora para dejar a la cuadrícula toda la
 * altura posible (las pantallas de coche son bajas).
 */
@Composable
fun AppsTitle(state: AppsUiState, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier) {
        Text(
            text = stringResource(R.string.apps_title),
            style = MaterialTheme.typography.titleMedium,
            color = TopwayPalette.TextPrimary,
        )
        if (state is AppsUiState.Success) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = pluralStringResource(R.plurals.apps_count, state.apps.size, state.apps.size),
                style = MaterialTheme.typography.bodyMedium,
                color = TopwayPalette.TextSecondary,
            )
        }
    }
}

/** Lista completa de aplicaciones, en páginas, con sus estados de carga, vacío y error. */
@Composable
fun AppsContent(
    state: AppsUiState,
    onRetry: () -> Unit,
    onAppClick: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        when (state) {
                AppsUiState.Loading -> CenteredMessage(stringResource(R.string.apps_loading)) {
                    CircularProgressIndicator(color = TopwayPalette.TextSecondary)
                }
                AppsUiState.Empty -> CenteredMessage(stringResource(R.string.apps_empty), onRetry)
                AppsUiState.Error -> CenteredMessage(stringResource(R.string.apps_error), onRetry)
                is AppsUiState.Success -> AppPager(
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
                color = TopwayPalette.TextSecondary,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}
