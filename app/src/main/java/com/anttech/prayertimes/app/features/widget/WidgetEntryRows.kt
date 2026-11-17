package com.anttech.prayertimes.features.widget

/**
 * Widget vakit listesini sabit sütunlu satırlara böler.
 * Büyük widget 6 vakti iki sıra (3 + 3) halinde gösterir.
 */
fun List<PrayerTimeEntry>.toRows(rowSize: Int): List<List<PrayerTimeEntry>> {
    val size = rowSize.coerceAtLeast(1)
    return chunked(size)
}
