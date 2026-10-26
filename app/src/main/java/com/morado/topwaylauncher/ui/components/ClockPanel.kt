package com.morado.topwaylauncher.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import com.morado.topwaylauncher.data.DeviceInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reloj y fecha. Se actualiza con ACTION_TIME_TICK (cada minuto) y cuando cambian la hora
 * o la zona horaria, que es el mecanismo estándar de los launchers; no usa permisos.
 */
@Composable
fun ClockPanel(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    showDeviceInfo: Boolean = true,
) {
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

    // Locale observable: si cambia el idioma del sistema, el texto se recompone.
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ROOT
    val is24Hour = DateFormat.is24HourFormat(context)
    val timeText = remember(now, locale, is24Hour) {
        formatPattern(now, locale, if (is24Hour) "Hm" else "hm")
    }
    val dateText = remember(now, locale) {
        formatPattern(now, locale, "EEEEdMMMM")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val textAlign = when (horizontalAlignment) {
        Alignment.CenterHorizontally -> TextAlign.Center
        Alignment.End -> TextAlign.End
        else -> TextAlign.Start
    }

    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(
            text = timeText,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            style = MaterialTheme.typography.displayLarge.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Light,
                textAlign = textAlign,
            ),
            autoSize = TextAutoSize.StepBased(minFontSize = 40.sp, maxFontSize = 120.sp),
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
            maxLines = 1,
        )
        if (showDeviceInfo) {
            val configuration = LocalConfiguration.current
            val deviceInfo = remember(configuration) { DeviceInfo.read(context).summary() }
            Spacer(Modifier.height(24.dp))
            Text(
                text = deviceInfo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = textAlign,
            )
        }
    }
}

private fun formatPattern(time: Long, locale: Locale, skeleton: String): String {
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return SimpleDateFormat(pattern, locale).format(Date(time))
}
