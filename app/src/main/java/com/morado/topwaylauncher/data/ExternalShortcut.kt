package com.morado.topwaylauncher.data

import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Acceso a una app externa (mapas, música) ya verificado: [intent] tiene una actividad que
 * lo atiende en el momento de resolverlo. Se vuelve a comprobar al abrirlo.
 */
data class ExternalShortcut(
    val label: String,
    val icon: ImageBitmap?,
    val intent: Intent,
    /** true si es Google Maps; false si es otra app de mapas predeterminada del sistema. */
    val isGoogleMaps: Boolean = false,
)
