package com.morado.topwaylauncher.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.ui.theme.TopwayDimens
import com.morado.topwaylauncher.ui.theme.TopwayPalette

/** Secciones propias del launcher. Mapas, Música y Ajustes abren apps externas. */
enum class HomeSection { Dashboard, Apps }

private const val MAIN_ITEMS = 4
private val DividerBlock = 25.dp

/**
 * Barra lateral: Inicio, Mapas, Música y Apps arriba; Ajustes abajo, separado por un divisor.
 * La altura de cada botón se reparte según el espacio real, de modo que los cinco caben
 * siempre y Ajustes (la salida segura hacia el launcher original) nunca se comprime.
 */
@Composable
fun NavRail(
    selected: HomeSection,
    onHome: () -> Unit,
    onNavigation: () -> Unit,
    onMusic: () -> Unit,
    onApps: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.padding(vertical = 12.dp)) {
        val itemHeight = ((maxHeight - DividerBlock) / (MAIN_ITEMS + 1))
            .coerceIn(TopwayDimens.RailMinItemHeight, TopwayDimens.RailMaxItemHeight)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            RailItem(R.drawable.ic_nav_home, R.string.rail_home, selected == HomeSection.Dashboard, itemHeight, onHome)
            RailItem(R.drawable.ic_nav_navigation, R.string.rail_navigation, false, itemHeight, onNavigation)
            RailItem(R.drawable.ic_nav_music, R.string.rail_music, false, itemHeight, onMusic)
            RailItem(R.drawable.ic_nav_apps, R.string.rail_apps, selected == HomeSection.Apps, itemHeight, onApps)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(32.dp)
                    .height(1.dp)
                    .background(TopwayPalette.Border),
            )
            RailItem(R.drawable.ic_nav_settings, R.string.rail_settings, false, itemHeight, onSettings)
        }
    }
}

@Composable
private fun RailItem(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    selected: Boolean,
    height: Dp,
    onClick: () -> Unit,
) {
    val text = stringResource(label)
    val tint = if (selected) TopwayPalette.TextPrimary else TopwayPalette.TextSecondary
    // Indicador sutil: un paso de tono detrás del botón activo, sin color de acento.
    val background = if (selected) TopwayPalette.PanelRaised else Color.Transparent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(TopwayDimens.ControlRadius))
            .background(background)
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClickLabel = text, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(TopwayDimens.RailIcon))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
