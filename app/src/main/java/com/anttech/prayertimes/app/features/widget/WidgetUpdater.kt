package com.anttech.prayertimes.features.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ana ekran widget'larını veri değiştiğinde yeniden çizer.
 * Tetikler: uygulama açılışı, vakit alarmı, cihaz yeniden başlatma
 * ve sistem 30 dakika güncellemesi.
 * Tema renkleri `@color` kaynaklarından çözüldüğü için tema değişiminde
 * ayrıca yeniden çizim gerekmez.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun updateAll() {
        val manager = GlanceAppWidgetManager(context)

        manager.getGlanceIds(SmallPrayerWidget::class.java).forEach { glanceId ->
            SmallPrayerWidget().update(context, glanceId)
        }
        manager.getGlanceIds(LargePrayerWidget::class.java).forEach { glanceId ->
            LargePrayerWidget().update(context, glanceId)
        }
    }

    /** Ana ekranda en az bir Namazgâh widget'ı var mı? */
    suspend fun hasWidgets(): Boolean {
        val manager = GlanceAppWidgetManager(context)
        return manager.getGlanceIds(SmallPrayerWidget::class.java).isNotEmpty() ||
            manager.getGlanceIds(LargePrayerWidget::class.java).isNotEmpty()
    }
}
