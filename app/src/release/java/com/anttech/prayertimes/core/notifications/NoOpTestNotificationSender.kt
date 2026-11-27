package com.anttech.prayertimes.core.notifications

import javax.inject.Inject
import javax.inject.Singleton

/** Release derlemesinde test bildirimi gönderilmez. */
@Singleton
class NoOpTestNotificationSender @Inject constructor() : TestNotificationSender {

    override val isAvailable: Boolean = false

    override fun send() = Unit

    override fun sendSahurTest() = Unit
}
