package com.morado.topwaylauncher.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.data.ExternalShortcut
import com.morado.topwaylauncher.data.LauncherApp
import com.morado.topwaylauncher.data.NowPlaying
import com.morado.topwaylauncher.data.vehicle.VehicleState
import com.morado.topwaylauncher.ui.components.AppIcon
import com.morado.topwaylauncher.ui.components.CARD_PRESSED_SCALE
import com.morado.topwaylauncher.ui.components.Pressable
import com.morado.topwaylauncher.ui.theme.TopwayDimens
import com.morado.topwaylauncher.ui.theme.TopwayPalette
import com.morado.topwaylauncher.ui.vehicle.SpeedCard
import com.morado.topwaylauncher.ui.vehicle.StatusDot

/** Proporciones en horizontal: velocidad protagonista a la izquierda; mapas y música a la derecha. */
private const val SPEED_WEIGHT = 0.38f
private const val SIDE_WEIGHT = 0.62f
private const val NAVIGATION_WEIGHT = 0.42f
private const val MEDIA_WEIGHT = 0.58f

/** Con menos alto, navegación y música van una al lado de la otra en lugar de apiladas. */
private val StackedMinHeight = 260.dp

/** Con menos ancho, la tarjeta de navegación omite el botón (toda la tarjeta ya es pulsable). */
private val NavigationButtonMinWidth = 280.dp

/** Alto mínimo (sin márgenes) para la versión en columna de la tarjeta de navegación. */
private val NavigationTallMinHeight = 170.dp
private val QuickPanelMinHeight = 92.dp
private val QuickPanelMaxHeight = 128.dp
private val QuickLabelSpace = 26.dp

/** Altura a partir de la cual la tarjeta de música muestra la carátula y dos acciones. */
private val MediaRoomyHeight = 172.dp
private val MediaActionMinHeight = 112.dp
private const val DISABLED_ALPHA = 0.3f

/** Tinte de la carátula en la tarjeta de música; el fundido acompaña al del fondo. */
private const val CARD_TINT_ALPHA = 0.16f
private const val TINT_FADE_MS = 900

/** Alto de la fila de controles más su separación, para calcular el tamaño de la carátula. */
private val MediaControlsBlock = 68.dp
private val ArtMinSize = 48.dp
private val ArtMaxSize = 132.dp

/** Acciones de la tarjeta de música. */
class MediaActions(
    /** Abre la app que suena, o la de música predeterminada. */
    val onOpen: () -> Unit,
    /** Explica y lleva al ajuste de acceso a notificaciones. */
    val onEnableAccess: () -> Unit,
    val onPlayPause: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
)

/**
 * Pantalla de inicio: velocidad, navegación, música y accesos rápidos.
 * Todas las medidas salen del espacio disponible (sin posiciones absolutas).
 */
@Composable
fun DashboardContent(
    state: HomeUiState,
    vehicle: State<VehicleState>,
    onSpeedClick: () -> Unit,
    onNavigation: () -> Unit,
    media: MediaActions,
    onAppClick: (LauncherApp) -> Unit,
    onShowAllApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val wide = maxWidth >= maxHeight
        val quickHeight = (maxHeight * 0.26f).coerceIn(QuickPanelMinHeight, QuickPanelMaxHeight)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(TopwayDimens.Gap)) {
            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(TopwayDimens.Gap),
                ) {
                    SpeedCard(vehicle, onSpeedClick, Modifier.weight(SPEED_WEIGHT).fillMaxHeight())
                    BoxWithConstraints(Modifier.weight(SIDE_WEIGHT).fillMaxHeight()) {
                        if (maxHeight >= StackedMinHeight) {
                            Column(verticalArrangement = Arrangement.spacedBy(TopwayDimens.Gap)) {
                                NavigationCard(state.navigation, onNavigation, Modifier.fillMaxWidth().weight(NAVIGATION_WEIGHT))
                                MediaCard(state, media, Modifier.fillMaxWidth().weight(MEDIA_WEIGHT))
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(TopwayDimens.Gap)) {
                                NavigationCard(state.navigation, onNavigation, Modifier.weight(1f).fillMaxHeight())
                                MediaCard(state, media, Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }
                }
            } else {
                SpeedCard(vehicle, onSpeedClick, Modifier.fillMaxWidth().weight(1.2f))
                NavigationCard(state.navigation, onNavigation, Modifier.fillMaxWidth().weight(0.6f))
                MediaCard(state, media, Modifier.fillMaxWidth().weight(1f))
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

/**
 * Superficie de tarjeta pulsable: opaca, con un borde fino. Con [tint], la parte de arriba
 * toma suavemente ese color (la carátula de lo que suena).
 */
@Composable
private fun CardSurface(
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    content: @Composable () -> Unit,
) {
    val animatedTint by animateColorAsState(
        targetValue = tint?.copy(alpha = CARD_TINT_ALPHA) ?: Color.Transparent,
        animationSpec = tween(TINT_FADE_MS),
        label = "cardTint",
    )
    val shape = RoundedCornerShape(TopwayDimens.CardRadius)
    Pressable(onClick = onClick, onClickLabel = onClickLabel, modifier = modifier, pressedScale = CARD_PRESSED_SCALE) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(TopwayPalette.Card)
                .background(Brush.verticalGradient(listOf(animatedTint, Color.Transparent)))
                .border(1.dp, TopwayPalette.CardBorder, shape)
                .padding(TopwayDimens.CardPadding),
        ) {
            content()
        }
    }
}

/**
 * Acceso a Google Maps (o a la app de mapas predeterminada). No dibuja un mapa falso: indica
 * si la app está disponible y, si no lo está, ofrece ir a la lista de aplicaciones.
 */
@Composable
private fun NavigationCard(shortcut: ExternalShortcut?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val subtitle = when {
        shortcut == null -> stringResource(R.string.nav_unavailable)
        shortcut.isGoogleMaps -> stringResource(R.string.nav_open_google_maps)
        else -> stringResource(R.string.nav_open_app, shortcut.label)
    }
    val clickLabel = if (shortcut == null) stringResource(R.string.nav_show_apps) else subtitle
    CardSurface(onClick = onClick, onClickLabel = clickLabel, modifier = modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth < NavigationButtonMinWidth && maxHeight >= NavigationTallMinHeight) {
                // Estrecha y alta (junto a la música en pantallas bajas): todo en columna.
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    ShortcutIcon(shortcut, R.drawable.ic_nav_navigation, 48.dp)
                    NavigationTexts(shortcut, subtitle, subtitleLines = 2)
                    NavigationAction(shortcut)
                }
            } else {
                val iconSize = (maxHeight * 0.55f).coerceIn(40.dp, 72.dp)
                val showAction = maxWidth >= NavigationButtonMinWidth
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    ShortcutIcon(shortcut, R.drawable.ic_nav_navigation, iconSize)
                    Spacer(Modifier.width(16.dp))
                    NavigationTexts(shortcut, subtitle, subtitleLines = 1, Modifier.weight(1f))
                    if (showAction) {
                        Spacer(Modifier.width(12.dp))
                        NavigationAction(shortcut)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationTexts(shortcut: ExternalShortcut?, subtitle: String, subtitleLines: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.nav_title),
            style = MaterialTheme.typography.titleMedium,
            color = TopwayPalette.TextPrimary,
            maxLines = 1,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TopwayPalette.TextSecondary,
            maxLines = subtitleLines,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(if (shortcut != null) TopwayPalette.Connected else TopwayPalette.Disconnected)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(if (shortcut != null) R.string.nav_available else R.string.nav_not_installed),
                style = MaterialTheme.typography.labelMedium,
                color = TopwayPalette.TextSecondary,
                maxLines = 1,
            )
        }
    }
}

/** Con app de mapas, la flecha de abrir; sin ella, el acceso a la lista de aplicaciones. */
@Composable
private fun NavigationAction(shortcut: ExternalShortcut?) {
    if (shortcut != null) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(TopwayPalette.Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = TopwayPalette.OnAccent,
                modifier = Modifier.size(28.dp),
            )
        }
    } else {
        ActionPill(text = stringResource(R.string.nav_show_apps))
    }
}

/**
 * Tarjeta de música. Con el acceso a notificaciones concedido muestra lo que publica la app
 * que suena (título, artista, carátula) y sus controles. Sin acceso, o si no suena nada, es un
 * acceso directo a la app de música predeterminada. Nunca inventa datos de reproducción.
 */
@Composable
private fun MediaCard(state: HomeUiState, actions: MediaActions, modifier: Modifier = Modifier) {
    val nowPlaying = state.nowPlaying
    val openLabel = when {
        nowPlaying is NowPlayingUiState.Active && nowPlaying.app != null ->
            stringResource(R.string.media_open, nowPlaying.app.label)
        state.music != null -> stringResource(R.string.media_open, state.music.label)
        else -> stringResource(R.string.media_choose)
    }
    val tint = (nowPlaying as? NowPlayingUiState.Active)?.track?.ambientColor?.let(::Color)
    CardSurface(onClick = actions.onOpen, onClickLabel = openLabel, modifier = modifier, tint = tint) {
        if (nowPlaying is NowPlayingUiState.Active) {
            ActiveMedia(nowPlaying, actions)
        } else {
            MediaShortcut(
                music = state.music,
                status = stringResource(
                    if (nowPlaying == NowPlayingUiState.Idle) R.string.media_idle else R.string.media_unavailable,
                ),
                openLabel = openLabel,
                onEnableAccess = actions.onEnableAccess
                    .takeIf { nowPlaying == NowPlayingUiState.NoAccess && state.canRequestMediaAccess },
            )
        }
    }
}

/** Lo que suena: carátula (o icono de la app), título, artista y controles. */
@Composable
private fun ActiveMedia(nowPlaying: NowPlayingUiState.Active, actions: MediaActions) {
    val track = nowPlaying.track
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // La carátula ocupa todo el alto que dejan libre los controles: es lo más vistoso.
        val artSize = (maxHeight - MediaControlsBlock).coerceIn(ArtMinSize, ArtMaxSize)
        val roomy = artSize >= 88.dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(track.art ?: nowPlaying.app?.icon, artSize)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    nowPlaying.app?.let { app ->
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = TopwayPalette.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = track.title ?: stringResource(R.string.media_untitled),
                        style = MaterialTheme.typography.titleMedium,
                        color = TopwayPalette.TextPrimary,
                        maxLines = if (roomy) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    track.artist?.let { artist ->
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TopwayPalette.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            MediaControls(track, actions, Modifier.fillMaxWidth())
        }
    }
}

/** Anterior, reproducir/pausar y siguiente. Los saltos se atenúan si la app no los admite. */
@Composable
private fun MediaControls(track: NowPlaying, actions: MediaActions, modifier: Modifier = Modifier) {
    val playPauseLabel = stringResource(if (track.isPlaying) R.string.media_pause else R.string.media_play)
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        ControlButton(R.drawable.ic_media_previous, stringResource(R.string.media_previous), track.canSkipPrevious, actions.onPrevious)
        Pressable(onClick = actions.onPlayPause, onClickLabel = playPauseLabel) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(TopwayPalette.Accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (track.isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play),
                    contentDescription = playPauseLabel,
                    tint = TopwayPalette.OnAccent,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        ControlButton(R.drawable.ic_media_next, stringResource(R.string.media_next), track.canSkipNext, actions.onNext)
    }
}

@Composable
private fun ControlButton(icon: Int, label: String, enabled: Boolean, onClick: () -> Unit) {
    val content = @Composable {
        Box(Modifier.size(TopwayDimens.MinTouchTarget), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(icon),
                contentDescription = label,
                tint = TopwayPalette.TextPrimary,
                modifier = Modifier
                    .size(28.dp)
                    .alpha(if (enabled) 1f else DISABLED_ALPHA),
            )
        }
    }
    if (enabled) Pressable(onClick = onClick, onClickLabel = label) { content() } else content()
}

/**
 * Sin lectura de lo que suena: indica el estado y ofrece abrir la app de música. Si el
 * usuario aún no concedió el acceso, añade la acción para activarlo.
 */
@Composable
private fun MediaShortcut(
    music: ExternalShortcut?,
    status: String,
    openLabel: String,
    onEnableAccess: (() -> Unit)?,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val showAccess = onEnableAccess != null && maxHeight >= MediaActionMinHeight
        // Con poco alto se prioriza activar: abrir la app ya lo hace toda la tarjeta.
        val showOpen = maxHeight >= if (showAccess) MediaRoomyHeight else MediaActionMinHeight
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(image = null, size = 48.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.media_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TopwayPalette.TextPrimary,
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TopwayPalette.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (showOpen) ActionPill(text = openLabel, icon = music?.icon)
                if (showAccess && onEnableAccess != null) {
                    Pressable(onClick = onEnableAccess, onClickLabel = stringResource(R.string.media_enable)) {
                        ActionPill(text = stringResource(R.string.media_enable), highlighted = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPill(text: String, icon: ImageBitmap? = null, highlighted: Boolean = false) {
    Row(
        modifier = Modifier
            .height(TopwayDimens.MinTouchTarget)
            .clip(CircleShape)
            .background(if (highlighted) TopwayPalette.Accent else TopwayPalette.Control)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Image(it, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (highlighted) TopwayPalette.OnAccent else TopwayPalette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Carátula o icono de la app; sin imagen, el pictograma de música. */
@Composable
private fun Artwork(image: ImageBitmap?, size: Dp) {
    val shape = RoundedCornerShape(14.dp)
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(shape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(TopwayPalette.Control),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_nav_music),
                contentDescription = null,
                tint = TopwayPalette.TextSecondary,
                modifier = Modifier.size(size / 2),
            )
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
                .background(TopwayPalette.Control),
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
    val shape = RoundedCornerShape(TopwayDimens.CardRadius)
    BoxWithConstraints(
        modifier
            .clip(shape)
            .background(TopwayPalette.Card)
            .border(1.dp, TopwayPalette.CardBorder, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val fitting = ((maxWidth - TopwayDimens.QuickSlotWidth) / TopwayDimens.QuickSlotWidth).toInt().coerceAtLeast(0)
        val iconSize = (maxHeight - QuickLabelSpace).coerceIn(36.dp, TopwayDimens.QuickIcon)
        // Los iconos (y «Todas») se reparten con el mismo espacio entre ellos para que el panel
        // no quede con un hueco cuando hay pocas apps. Carga y vacío ocupan el espacio con weight.
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                }
            }
            QuickSlot(label = stringResource(R.string.quick_all), onClick = onShowAllApps) {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TopwayPalette.Control),
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
