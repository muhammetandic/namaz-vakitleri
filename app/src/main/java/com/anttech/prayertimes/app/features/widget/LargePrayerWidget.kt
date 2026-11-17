package com.anttech.prayertimes.features.widget

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.fillMaxSize
import com.anttech.prayertimes.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 4x2 Tam Pano Ana Ekran Widget'ı.
 *
 * Üstte konum ve sıradaki vakit, altında sistem saatiyle senkron akan geri
 * sayım ve günün 6 vakti (3 + 3). İçerik klasik [RemoteViews] ile çizilir;
 * renkler `@color` kaynaklarından geldiği için tema değişimi anında uygulanır.
 */
class LargePrayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = WidgetDataLoader.load(context)

        provideContent {
            AndroidRemoteViews(
                remoteViews = buildLargeViews(context, model),
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }
}

private val LargeCellNormal = intArrayOf(
    R.id.cell_0_normal, R.id.cell_1_normal, R.id.cell_2_normal,
    R.id.cell_3_normal, R.id.cell_4_normal, R.id.cell_5_normal
)

private val LargeCellActive = intArrayOf(
    R.id.cell_0_active, R.id.cell_1_active, R.id.cell_2_active,
    R.id.cell_3_active, R.id.cell_4_active, R.id.cell_5_active
)

private val LargeNameNormal = intArrayOf(
    R.id.cname_0, R.id.cname_1, R.id.cname_2, R.id.cname_3, R.id.cname_4, R.id.cname_5
)

private val LargeTimeNormal = intArrayOf(
    R.id.ctime_0, R.id.ctime_1, R.id.ctime_2, R.id.ctime_3, R.id.ctime_4, R.id.ctime_5
)

private val LargeNameActive = intArrayOf(
    R.id.cname_0_active, R.id.cname_1_active, R.id.cname_2_active,
    R.id.cname_3_active, R.id.cname_4_active, R.id.cname_5_active
)

private val LargeTimeActive = intArrayOf(
    R.id.ctime_0_active, R.id.ctime_1_active, R.id.ctime_2_active,
    R.id.ctime_3_active, R.id.ctime_4_active, R.id.ctime_5_active
)

private val LargeSlots = intArrayOf(
    R.id.slot_0, R.id.slot_1, R.id.slot_2, R.id.slot_3, R.id.slot_4, R.id.slot_5
)

private fun buildLargeViews(context: Context, model: WidgetTimesModel?): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_large_content)
    views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))

    if (model == null) {
        // Veri yok: dokunuş uygulamayı açıp senkronizasyonu tetikler.
        views.setTextViewText(R.id.city, "Vakit verisi yok · açmak için dokun")
        views.setTextViewText(R.id.next_prayer, "")
        views.setViewVisibility(R.id.countdown_pill, View.GONE)
        LargeSlots.forEach { views.setViewVisibility(it, View.GONE) }
        return views
    }

    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    views.setTextViewText(R.id.city, model.city)
    views.setTextViewText(
        R.id.next_prayer,
        "SIRADAKİ · ${model.nextPrayer.turkishName.uppercase()} " +
            model.nextPrayerTime.format(timeFormat)
    )

    // Geri sayım: Chronometer sistem saatiyle aynı saniyede akar.
    val remainingMs = WidgetCountdown.remainingMillis(LocalDateTime.now(), model.nextPrayerTime)
    views.setChronometerCountDown(R.id.countdown_value, true)
    views.setChronometer(
        R.id.countdown_value,
        SystemClock.elapsedRealtime() + remainingMs,
        null,
        true
    )

    model.entries.forEachIndexed { index, entry ->
        val name = entry.type.turkishName
        val time = entry.time.format(timeFormat)

        views.setTextViewText(LargeNameNormal[index], name)
        views.setTextViewText(LargeTimeNormal[index], time)
        views.setTextViewText(LargeNameActive[index], name)
        views.setTextViewText(LargeTimeActive[index], time)

        if (entry.isActive) {
            views.setViewVisibility(LargeCellNormal[index], View.GONE)
            views.setViewVisibility(LargeCellActive[index], View.VISIBLE)
        } else {
            views.setViewVisibility(LargeCellNormal[index], View.VISIBLE)
            views.setViewVisibility(LargeCellActive[index], View.GONE)
        }
    }

    return views
}

class LargePrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargePrayerWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        rescheduleWidgetAlarms(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        rescheduleWidgetAlarms(context)
    }
}
