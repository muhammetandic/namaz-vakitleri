package com.anttech.prayertimes.features.widget

import com.anttech.prayertimes.features.times.domain.CalculateNextPrayerUseCase
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.PrayerType
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/** Widget'da gösterilen tek bir vakit satırı. */
data class PrayerTimeEntry(
    val type: PrayerType,
    val time: LocalTime,
    val isActive: Boolean
)

/** Widget içeriği: sıradaki vakit, aktif vakit ve günün 6 vakti. */
data class WidgetTimesModel(
    val city: String,
    val date: LocalDateTime,
    val activePrayer: PrayerType,
    val nextPrayer: PrayerType,
    val nextPrayerTime: LocalTime,
    val remainingLabel: String,
    val entries: List<PrayerTimeEntry>
)

/**
 * Room'daki günün vakitlerinden widget modelini hesaplar.
 * Sıradaki/aktif vakit hesabı CalculateNextPrayerUseCase ile aynı kuralı izler.
 */
class GetWidgetTimesUseCase @Inject constructor(
    private val calculateNextPrayerUseCase: CalculateNextPrayerUseCase
) {

    constructor() : this(CalculateNextPrayerUseCase())

    operator fun invoke(
        prayerTimes: PrayerTimes?,
        now: LocalDateTime = LocalDateTime.now()
    ): WidgetTimesModel? {
        if (prayerTimes == null) return null

        val countdown = calculateNextPrayerUseCase(prayerTimes, now)
        val nextPrayerTime = when (countdown.nextPrayer) {
            PrayerType.IMSAK ->
                if (countdown.activePrayer == PrayerType.YATSI && now.toLocalTime() > prayerTimes.yatsi) {
                    prayerTimes.tomorrowImsak ?: prayerTimes.imsak
                } else {
                    prayerTimes.imsak
                }

            PrayerType.GUNES -> prayerTimes.gunes
            PrayerType.OGLE -> prayerTimes.ogle
            PrayerType.IKINDI -> prayerTimes.ikindi
            PrayerType.AKSAM -> prayerTimes.aksam
            PrayerType.YATSI -> prayerTimes.yatsi
        }

        val slots = listOf(
            PrayerType.IMSAK to prayerTimes.imsak,
            PrayerType.GUNES to prayerTimes.gunes,
            PrayerType.OGLE to prayerTimes.ogle,
            PrayerType.IKINDI to prayerTimes.ikindi,
            PrayerType.AKSAM to prayerTimes.aksam,
            PrayerType.YATSI to prayerTimes.yatsi
        )

        return WidgetTimesModel(
            city = prayerTimes.city,
            date = now,
            activePrayer = countdown.activePrayer,
            nextPrayer = countdown.nextPrayer,
            nextPrayerTime = nextPrayerTime,
            remainingLabel = remainingLabel(countdown.hours, countdown.minutes),
            entries = slots.map { (type, time) ->
                PrayerTimeEntry(type = type, time = time, isActive = type == countdown.activePrayer)
            }
        )
    }

    private fun remainingLabel(hours: Long, minutes: Long): String {
        return when {
            hours > 0 -> "$hours sa $minutes dk"
            minutes > 0 -> "$minutes dk"
            else -> "az kaldı"
        }
    }
}
