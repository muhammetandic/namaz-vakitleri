package com.anttech.prayertimes.features.times.domain

enum class PrayerType(val turkishName: String, val shortDescription: String) {
    IMSAK("İmsak", "Sabah namazı vakti girişi ve oruç başlangıcı"),
    GUNES("Güneş", "Güneş doğuşu, sabah namazı çıkışı"),
    OGLE("Öğle", "Öğle namazı vakti"),
    IKINDI("İkindi", "İkindi namazı vakti"),
    AKSAM("Akşam", "Akşam namazı vakti ve iftar"),
    YATSI("Yatsı", "Yatsı namazı vakti ve yatsı ezanı")
}
