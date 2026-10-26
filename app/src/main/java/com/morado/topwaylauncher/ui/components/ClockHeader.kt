package com.morado.topwaylauncher.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import com.morado.topwaylauncher.R
import com.morado.topwaylauncher.data.DeviceInfo
import com.morado.topwaylauncher.ui.theme.TopwayPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cabecera con la hora (tamaño moderado) y la fecha en texto secundario. Mantener pulsada
 * la hora muestra la información del equipo, útil para diagnosticar la radio sin ADB.
 */
@Composable
fun ClockHeader(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val now = rememberCurrentTime()
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ROOT
    val is24Hour = DateFormat.is24HourFormat(context)
    var showInfo by rememberSaveable { mutableStateOf(false) }

    val time = remember(now, locale, is24Hour) {
        SimpleDateFormat(if (is24Hour) "H:mm" else "h:mm", locale).format(Date(now))
    }
    val period = remember(now, locale, is24Hour) {
        if (is24Hour) null else SimpleDateFormat("a", locale).format(Date(now))
    }
    val date = remember(now, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
        SimpleDateFormat(pattern, locale).format(Date(now))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    Row(
        modifier = modifier.combinedClickable(
            onClickLabel = stringResource(R.string.device_info_title),
            onClick = {},
            onLongClick = { showInfo = true },
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time, style = MaterialTheme.typography.headlineMedium, color = TopwayPalette.TextPrimary, maxLines = 1)
        if (period != null) {
            Text(
                text = period,
                style = MaterialTheme.typography.labelLarge,
                color = TopwayPalette.TextSecondary,
                modifier = Modifier.padding(start = 6.dp, top = 6.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = date,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = TopwayPalette.TextSecondary,
            maxLines = 1,
        )
    }

    if (showInfo) {
        DeviceInfoDialog(onDismiss = { showInfo = false })
    }
}

@Composable
private fun DeviceInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val info = remember(configuration) { DeviceInfo.read(context) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = TopwayPalette.Selected) }
        },
        title = { Text(stringResource(R.string.device_info_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                InfoLine(R.string.device_info_android, "${info.androidRelease} (API ${info.sdkInt})")
                InfoLine(R.string.device_info_screen, "${info.widthPx} × ${info.heightPx} px · ${info.densityDpi} dpi")
                InfoLine(R.string.device_info_model, "${info.manufacturer} ${info.model}")
                InfoLine(R.string.device_info_board, info.board)
            }
        },
        containerColor = TopwayPalette.PanelRaised,
    )
}

/** Etiqueta encima del valor: los valores largos (modelos, placas) no se parten en columnas. */
@Composable
private fun InfoLine(label: Int, value: String) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = TopwayPalette.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = TopwayPalette.TextPrimary)
    }
}

/**
 * Hora actual que se actualiza con ACTION_TIME_TICK (cada minuto) y cuando cambian la hora
 * o la zona horaria, que es el mecanismo estándar de los launchers; no usa permisos.
 */
@Composable
private fun rememberCurrentTime(): Long {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                now = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        // Las difusiones protegidas del sistema se reciben aunque el receptor no esté exportado.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        now = System.currentTimeMillis()
        onDispose { context.unregisterReceiver(receiver) }
    }
    return now
}
