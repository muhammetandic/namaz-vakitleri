package com.anttech.prayertimes.core.notifications

import android.content.Context
import android.os.PowerManager

/**
 * Sahur alarmı için tek bir wake lock tutar.
 *
 * Ekranı açma işini [SahurAlarmActivity] `setShowWhenLocked`/`setTurnScreenOn` ile yapar;
 * bu yönetici yalnızca işlem süresince CPU'yu açık tutar. Durdur/ertele aksiyonu
 * aynı kilit örneğini serbest bırakır.
 */
internal object SahurWakeLockManager {
    private const val WAKE_LOCK_TAG = "namazvakitleri:SahurAlarmWake"
    private const val TIMEOUT_MS = 10_000L

    @Volatile
    private var wakeLock: PowerManager.WakeLock? = null

    @Synchronized
    fun acquire(context: Context) {
        releaseLocked()
        val powerManager = context.applicationContext
            .getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).also {
            it.acquire(TIMEOUT_MS)
        }
    }

    @Synchronized
    fun release() {
        releaseLocked()
    }

    private fun releaseLocked() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        wakeLock = null
    }
}
