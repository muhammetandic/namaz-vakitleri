package com.anttech.prayertimes.core.notifications

import android.content.Intent

/** Ses oynatılamadığında gösterilecek metin bildiriminin verileri. */
internal object AdhanPlaybackFallback {
    const val NOTIFICATION_ID_OFFSET = 3000

    private const val DEFAULT_MESSAGE = "Ses çalınamadı; vakit bilgisi gösteriliyor."

    fun notificationId(intent: Intent, baseId: Int): Int =
        intent.getIntExtra(AdhanPlaybackService.EXTRA_ALARM_ID, baseId) + NOTIFICATION_ID_OFFSET

    fun message(intent: Intent): String =
        intent.getStringExtra(AdhanPlaybackService.EXTRA_MESSAGE) ?: DEFAULT_MESSAGE

    fun vibrationEnabled(intent: Intent): Boolean =
        intent.getBooleanExtra(AdhanPlaybackService.EXTRA_VIBRATION_ENABLED, true)
}
