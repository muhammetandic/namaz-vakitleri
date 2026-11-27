package com.anttech.prayertimes.features.calendar.domain

import java.time.LocalDate

/**
 * Domain model for a tracked fasting day.
 * - In Ramadan (hijriMonth == 9): isMarked = true means did NOT fast (makeup/qaza needed)
 * - In other months: isMarked = true means DID fast (nafl/sunnah)
 * - isForbidden = true means fasting is not allowed (Eid al-Fitr, Eid al-Adha, Tashriq days)
 */
data class FastTrackingDay(
    val date: LocalDate,
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriMonthName: String,
    val hijriYear: Int,
    val isMarked: Boolean,
    val isForbidden: Boolean,
    val city: String
) {
    val isRamadan: Boolean
        get() = hijriMonth == 9

    val displayText: String
        get() {
            val dateStr = "%02d %s %d".format(hijriDay, hijriMonthName, hijriYear)
            return if (isForbidden) {
                "$dateStr - Oruç Yasak (${forbiddenReason})"
            } else if (isRamadan) {
                if (isMarked) "$dateStr - Oruç tutulmadı" else "$dateStr - Oruç tutuldu"
            } else {
                if (isMarked) "$dateStr - Oruç TUTULDU (nafile)" else "$dateStr"
            }
        }

    val forbiddenReason: String
        get() {
            return when {
                hijriMonth == 10 && hijriDay == 1 -> "Ramazan Bayramı (1. gün)"
                hijriMonth == 12 && hijriDay == 10 -> "Kurban Bayramı (1. gün)"
                hijriMonth == 12 && hijriDay in 11..13 -> "Teşrik Günleri"
                else -> "Yasak Gün"
            }
        }

    val isNonFastedRamadan: Boolean
        get() = isRamadan && isMarked

    val isFastedNonRamadan: Boolean
        get() = !isRamadan && isMarked && !isForbidden
}