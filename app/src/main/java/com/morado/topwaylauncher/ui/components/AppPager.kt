package com.morado.topwaylauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.data.LauncherApp
import com.morado.topwaylauncher.ui.theme.TopwayPalette

/*
 * La cuadrícula se calcula a partir del espacio real y no de un tamaño fijo: así la misma
 * pantalla sirve en el emulador y en la radio, cuya resolución aún no está confirmada.
 * Celda objetivo de ~132 × 112 dp (las pantallas de coche son bajas y anchas); entre 3 y 7
 * columnas y entre 1 y 3 filas por página.
 */
private val TargetCellWidth = 132.dp
private val TargetCellHeight = 112.dp
private val IndicatorHeight = 28.dp
private const val MIN_COLUMNS = 3
private const val MAX_COLUMNS = 7
private const val MAX_ROWS = 3

@Composable
fun AppPager(
    apps: List<LauncherApp>,
    onAppClick: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val gridHeight = maxHeight - IndicatorHeight
        val columns = (maxWidth / TargetCellWidth).toInt().coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        val rows = (gridHeight / TargetCellHeight).toInt().coerceIn(1, MAX_ROWS)
        val cellWidth = maxWidth / columns
        val cellHeight = gridHeight / rows
        // El icono ocupa buena parte de la celda; el resto es aire y la etiqueta (~36 dp).
        val iconSize = (minOf(cellWidth * 0.62f, (cellHeight - 36.dp) * 0.85f)).coerceIn(44.dp, 112.dp)

        val pages = apps.chunked(columns * rows)
        val pagerState = rememberPagerState { pages.size }

        Column(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                key = { pages[it].first().info.key },
            ) { pageIndex ->
                AppPage(
                    apps = pages[pageIndex],
                    columns = columns,
                    rows = rows,
                    iconSize = iconSize,
                    onAppClick = onAppClick,
                )
            }
            PageIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IndicatorHeight),
            )
        }
    }
}

@Composable
private fun AppPage(
    apps: List<LauncherApp>,
    columns: Int,
    rows: Int,
    iconSize: Dp,
    onAppClick: (LauncherApp) -> Unit,
) {
    // Filas y columnas de igual tamaño en todas las páginas: los huecos de la última
    // página quedan vacíos en su sitio, sin estirar ni recolocar los iconos.
    Column(Modifier.fillMaxSize()) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                for (column in 0 until columns) {
                    val app = apps.getOrNull(row * columns + column)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (app != null) AppCell(app, iconSize, onAppClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCell(app: LauncherApp, iconSize: Dp, onAppClick: (LauncherApp) -> Unit) {
    Pressable(
        onClick = { onAppClick(app) },
        onClickLabel = app.info.label,
        modifier = Modifier.padding(4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppIcon(app, iconSize)
            Spacer(Modifier.height(10.dp))
            Text(
                text = app.info.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    if (pageCount <= 1) {
        Spacer(modifier)
        return
    }
    val description = LocalResources.current.getString(R.string.page_indicator, currentPage + 1, pageCount)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        color = if (index == currentPage) TopwayPalette.AccentLight else TopwayPalette.Inactive,
                        shape = CircleShape,
                    ),
            )
        }
    }
}
