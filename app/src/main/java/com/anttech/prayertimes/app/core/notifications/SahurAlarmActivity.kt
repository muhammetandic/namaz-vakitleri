package com.anttech.prayertimes.core.notifications

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

/** Full-screen sahur alarm controls, shown above the lock screen. */
class SahurAlarmActivity : ComponentActivity() {

    private var alarmId: Int = -1
    private var alarmTitle: String = "Sahur Uyandırma"
    private var alarmMessage: String = "İmsak vakti yaklaştı"
    private var originalTrigger: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        alarmId = intent.getIntExtra(SahurAlarmActionReceiver.EXTRA_ALARM_ID, -1)
        alarmTitle = intent.getStringExtra(SahurAlarmActionReceiver.EXTRA_TITLE) ?: "Sahur Uyandırma"
        alarmMessage = intent.getStringExtra(SahurAlarmActionReceiver.EXTRA_MESSAGE)
            ?: "İmsak vakti yaklaştı"
        originalTrigger = intent.getStringExtra(SahurAlarmActionReceiver.EXTRA_ORIGINAL_TRIGGER)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 48, 32, 48)
            setBackgroundColor(0xFF2B1A13.toInt())
        }
        content.addView(TextView(this).apply {
            text = alarmTitle
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        })
        content.addView(TextView(this).apply {
            text = alarmMessage
            textSize = 20f
            setTextColor(0xFFE8D8CE.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 40)
        })
        content.addView(actionButton("5 dk Ertele") {
            sendAlarmAction(SahurAlarmActionReceiver.ACTION_SNOOZE, alarmId, alarmTitle, alarmMessage, originalTrigger)
        })
        content.addView(actionButton("Durdur") {
            sendAlarmAction(SahurAlarmActionReceiver.ACTION_STOP, alarmId, alarmTitle, alarmMessage, originalTrigger)
        })
        setContentView(content)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            sendAlarmAction(SahurAlarmActionReceiver.ACTION_STOP, alarmId, alarmTitle, alarmMessage, originalTrigger)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun actionButton(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        textSize = 20f
        minHeight = (64 * resources.displayMetrics.density).toInt()
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 16 }
    }

    private fun sendAlarmAction(
        action: String,
        alarmId: Int,
        title: String,
        message: String,
        originalTrigger: String?
    ) {
        if (alarmId >= 0) {
            sendBroadcast(Intent(this, SahurAlarmActionReceiver::class.java).apply {
                this.action = action
                putExtra(SahurAlarmActionReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(SahurAlarmActionReceiver.EXTRA_PRAYER_TYPE, "IMSAK")
                putExtra(SahurAlarmActionReceiver.EXTRA_TITLE, title)
                putExtra(SahurAlarmActionReceiver.EXTRA_MESSAGE, message)
                putExtra(SahurAlarmActionReceiver.EXTRA_ORIGINAL_TRIGGER, originalTrigger)
            })
        }
        finish()
    }
}
