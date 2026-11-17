package com.anttech.prayertimes.features.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.fillMaxSize
import com.anttech.prayertimes.MainActivity
import com.anttech.prayertimes.R
import java.time.format.DateTimeFormatter

/**
 * 2x2 Minimal Ana Ekran Widget'ı.
 *
 * Konum başlığı ve günün altı vakti alt alta listelenir; aktif vakit hap içinde
 * vurgulanır. İçerik klasik [RemoteViews] ile çizilir ve renkler `@color`
 * kaynaklarından gelir; böylece sistem teması değişince launcher renkleri
 * uygulama sürecine ihtiyaç duymadan anında yeniden çözer.
 * Veri Room'dan gelir ve tetiklerle yenilenir (bkz. [WidgetUpdater]).
 */
class SmallPrayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = WidgetDataLoader.load(context)

        provideContent {
            AndroidRemoteViews(
                remoteViews = buildSmallViews(context, model),
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }
}

private val SmallRowNormal = intArrayOf(
    R.id.row_0_normal, R.id.row_1_normal, R.id.row_2_normal,
    R.id.row_3_normal, R.id.row_4_normal, R.id.row_5_normal
)

private val SmallRowActive = intArrayOf(
    R.id.row_0_active, R.id.row_1_active, R.id.row_2_active,
    R.id.row_3_active, R.id.row_4_active, R.id.row_5_active
)

private val SmallNameNormal = intArrayOf(
    R.id.name_0, R.id.name_1, R.id.name_2, R.id.name_3, R.id.name_4, R.id.name_5
)

private val SmallTimeNormal = intArrayOf(
    R.id.time_0, R.id.time_1, R.id.time_2, R.id.time_3, R.id.time_4, R.id.time_5
)

private val SmallNameActive = intArrayOf(
    R.id.name_0_active, R.id.name_1_active, R.id.name_2_active,
    R.id.name_3_active, R.id.name_4_active, R.id.name_5_active
)

private val SmallTimeActive = intArrayOf(
    R.id.time_0_active, R.id.time_1_active, R.id.time_2_active,
    R.id.time_3_active, R.id.time_4_active, R.id.time_5_active
)

private val SmallSlots = intArrayOf(
    R.id.slot_0, R.id.slot_1, R.id.slot_2, R.id.slot_3, R.id.slot_4, R.id.slot_5
)

internal fun openAppPendingIntent(context: Context): PendingIntent =
    PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

private fun buildSmallViews(context: Context, model: WidgetTimesModel?): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_small_content)
    views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))

    if (model == null) {
        // Veri yoksa "yükleniyor" demek yanıltıcıydı: widget tamamı tıklanabilir,
        // dokunuş uygulamayı açar ve senkronizasyonu tetikler.
        views.setTextViewText(R.id.city, "Vakit verisi yok")
        SmallSlots.forEach { views.setViewVisibility(it, View.GONE) }
        return views
    }

    views.setTextViewText(R.id.city, model.city)

    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    model.entries.forEachIndexed { index, entry ->
        val name = entry.type.turkishName
        val time = entry.time.format(timeFormat)

        views.setTextViewText(SmallNameNormal[index], name)
        views.setTextViewText(SmallTimeNormal[index], time)
        views.setTextViewText(SmallNameActive[index], name)
        views.setTextViewText(SmallTimeActive[index], time)

        if (entry.isActive) {
            views.setViewVisibility(SmallRowNormal[index], View.GONE)
            views.setViewVisibility(SmallRowActive[index], View.VISIBLE)
        } else {
            views.setViewVisibility(SmallRowNormal[index], View.VISIBLE)
            views.setViewVisibility(SmallRowActive[index], View.GONE)
        }
    }

    return views
}

class SmallPrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallPrayerWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        rescheduleWidgetAlarms(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        rescheduleWidgetAlarms(context)
    }
}
