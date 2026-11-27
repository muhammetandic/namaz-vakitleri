package com.anttech.prayertimes.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * ADB'den sahur test bildirimi tetiklemek için broadcast receiver.
 * Kullanım: adb shell am broadcast -a com.anttech.prayertimes.action.TEST_SAHUR_NOTIFICATION
 */
@AndroidEntryPoint
class TestSahurTriggerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var testNotificationSender: TestNotificationSender

    companion object {
        const val ACTION_TEST_SAHUR = "com.anttech.prayertimes.action.TEST_SAHUR_NOTIFICATION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TEST_SAHUR) {
            testNotificationSender.sendSahurTest()
        }
    }
}