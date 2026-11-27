package com.anttech.prayertimes.core.notifications

/**
 * Ayarlar ekranındaki "Test bildirimi gönder" butonu için anlık bildirim gönderir.
 *
 * Gerçek uygulama yalnızca debug derlemesinde bulunur; release derlemesinde
 * [isAvailable] false döner ve hiçbir bildirim gönderilmez.
 */
interface TestNotificationSender {

    /** Bu derlemede test bildirimi gönderilebilir mi? */
    val isAvailable: Boolean

    fun send()

    fun sendSahurTest()
}
