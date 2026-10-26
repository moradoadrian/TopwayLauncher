package com.morado.topwaylauncher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.data.ExternalShortcut
import com.morado.topwaylauncher.data.LauncherApp
import com.morado.topwaylauncher.ui.components.AppIcon
import com.morado.topwaylauncher.ui.components.CARD_PRESSED_SCALE
import com.morado.topwaylauncher.ui.components.Pressable
import com.morado.topwaylauncher.ui.theme.TopwayDimens
import com.morado.topwaylauncher.ui.theme.TopwayPalette

/** Proporción de las tarjetas en horizontal: navegación protagonista, música secundaria. */
private const val NAVIGATION_WEIGHT = 0.62f
private const val MEDIA_WEIGHT = 0.38f
private val QuickPanelMinHeight = 92.dp
private val QuickPanelMaxHeight = 128.dp
private val QuickLabelSpace = 26.dp

/**
 * Pantalla de inicio: tarjeta de navegación, tarjeta de música y accesos rápidos.
 * Todas las medidas salen del espacio disponible (sin posiciones absolutas).
 */
@Composable
fun DashboardContent(
    state: HomeUiState,
    onNavigation: () -> Unit,
    onMusic: () -> Unit,
    onAppClick: (LauncherApp) -> Unit,
    onShowAllApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val wide = maxWidth >= maxHeight
        val quickHeight = (maxHeight * 0.3f).coerceIn(QuickPanelMinHeight, QuickPanelMaxHeight)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(TopwayDimens.Gap)) {
            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(TopwayDimens.Gap),
                ) {
                    NavigationCard(state.navigation, onNavigation, Modifier.weight(NAVIGATION_WEIGHT).fillMaxHeight())
                    MediaCard(state.music, onMusic, Modifier.weight(MEDIA_WEIGHT).fillMaxHeight())
                }
            } else {
                NavigationCard(state.navigation, onNavigation, Modifier.fillMaxWidth().weight(1f))
                MediaCard(state.music, onMusic, Modifier.fillMaxWidth().weight(1f))
            }
            QuickAccessPanel(
                appsState = state.apps,
                apps = state.quickAccess,
                onAppClick = onAppClick,
                onShowAllApps = onShowAllApps,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(quickHeight),
            )
        }
    }
}

/** Superficie de tarjeta pulsable: el fondo escala junto con el contenido al tocarla. */
@Composable
private fun CardSurface(
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Pressable(onClick = onClick, onClickLabel = onClickLabel, modifier = modifier, pressedScale = CARD_PRESSED_SCALE) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(TopwayDimens.CardRadius))
                .background(TopwayPalette.Panel)
                .padding(TopwayDimens.CardPadding),
        ) {
            content()
        }
    }
}

/**
 * Tarjeta protagonista: abre Google Maps (o la app de mapas predeterminada). No dibuja un
 * mapa falso: es un acceso directo, y si no hay app de mapas lo dice en el subtítulo.
 */
@Composable
private fun NavigationCard(shortcut: ExternalShortcut?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val subtitle = when {
        shortcut == null -> stringResource(R.string.nav_unavailable)
        shortcut.isGoogleMaps -> stringResource(R.string.nav_open_google_maps)
        else -> stringResource(R.string.nav_open_app, shortcut.label)
    }
    CardSurface(onClick = onClick, onClickLabel = subtitle, modifier = modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val iconSize = (maxHeight * 0.6f).coerceIn(48.dp, 96.dp)
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                ShortcutIcon(shortcut, R.drawable.ic_nav_navigation, iconSize)
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.nav_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TopwayPalette.TextPrimary,
                        maxLines = 1,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TopwayPalette.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (shortcut != null) {
                    Spacer(Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(TopwayPalette.Selected, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = TopwayPalette.OnSelected,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de música. El launcher no lee qué se está reproduciendo (requeriría el permiso
 * especial de acceso a notificaciones), así que no muestra canciones ni carátulas: ofrece
 * abrir la app de música predeterminada, o elegir una si el sistema no tiene ninguna.
 */
@Composable
private fun MediaCard(music: ExternalShortcut?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val action = music?.let { stringResource(R.string.media_open, it.label) } ?: stringResource(R.string.media_choose)
    CardSurface(onClick = onClick, onClickLabel = action, modifier = modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val showAction = maxHeight >= 112.dp
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(TopwayPalette.PanelRaised),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_nav_music),
                            contentDescription = null,
                            tint = TopwayPalette.TextSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.media_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TopwayPalette.TextPrimary,
                        )
                        Text(
                            text = stringResource(R.string.media_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TopwayPalette.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (showAction) {
                    Row(
                        modifier = Modifier
                            .height(TopwayDimens.MinTouchTarget)
                            .clip(CircleShape)
                            .background(TopwayPalette.PanelRaised)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        music?.icon?.let {
                            Image(it, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            text = action,
                            style = MaterialTheme.typography.labelLarge,
                            color = TopwayPalette.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Icono de la app externa, o un pictograma monocromo si no hay app o no tiene icono. */
@Composable
private fun ShortcutIcon(shortcut: ExternalShortcut?, fallback: Int, size: Dp) {
    val icon = shortcut?.icon
    if (icon != null) {
        Image(icon, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(TopwayDimens.ControlRadius))
                .background(TopwayPalette.PanelRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(fallback), contentDescription = null, tint = TopwayPalette.TextSecondary, modifier = Modifier.size(size * 0.45f))
        }
    }
}

/**
 * Accesos rápidos: recientes y predeterminadas, solo las que caben enteras, y siempre un
 * acceso a la lista completa al final.
 */
@Composable
private fun QuickAccessPanel(
    appsState: AppsUiState,
    apps: List<LauncherApp>,
    onAppClick: (LauncherApp) -> Unit,
    onShowAllApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(TopwayDimens.CardRadius))
            .background(TopwayPalette.Panel)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val fitting = ((maxWidth - TopwayDimens.QuickSlotWidth) / TopwayDimens.QuickSlotWidth).toInt().coerceAtLeast(0)
        val iconSize = (maxHeight - QuickLabelSpace).coerceIn(36.dp, TopwayDimens.QuickIcon)
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            when {
                appsState is AppsUiState.Loading -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TopwayPalette.TextSecondary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
                apps.isEmpty() -> Text(
                    text = stringResource(
                        if (appsState is AppsUiState.Error) R.string.apps_error else R.string.quick_empty,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TopwayPalette.TextSecondary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                else -> {
                    apps.take(fitting).forEach { app ->
                        QuickSlot(
                            label = app.info.label,
                            onClick = { onAppClick(app) },
                        ) { AppIcon(app, iconSize) }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
            QuickSlot(label = stringResource(R.string.quick_all), onClick = onShowAllApps) {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TopwayPalette.PanelRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_nav_apps), contentDescription = null, tint = TopwayPalette.TextPrimary, modifier = Modifier.size(iconSize * 0.45f))
                }
            }
        }
    }
}

@Composable
private fun QuickSlot(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Pressable(onClick = onClick, onClickLabel = label, modifier = Modifier.width(TopwayDimens.QuickSlotWidth)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TopwayPalette.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
