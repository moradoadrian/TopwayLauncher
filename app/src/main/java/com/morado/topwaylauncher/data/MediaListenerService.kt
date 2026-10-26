package com.morado.topwaylauncher.data

import android.service.notification.NotificationListenerService

/**
 * Existe solo para que Android permita leer las sesiones multimedia activas
 * (MediaSessionManager.getActiveSessions exige un servicio de escucha con acceso concedido).
 * No sobrescribe ningún método: las notificaciones no se leen, no se guardan ni se envían.
 */
class MediaListenerService : NotificationListenerService()
