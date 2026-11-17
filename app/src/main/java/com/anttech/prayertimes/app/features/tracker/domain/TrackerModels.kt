package com.anttech.prayertimes.features.tracker.domain

data class DhikrPreset(
    val id: String,
    val arabic: String,
    val turkishTitle: String,
    val target: Int
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            DhikrPreset("subhanallah", "سُبْحَانَ اللَّهِ", "Sübhanallah", 33),
            DhikrPreset("elhamdulillah", "الْحَمْدُ لِلَّهِ", "Elhamdülillah", 33),
            DhikrPreset("allahuakbar", "اللَّهُ أَكْبَرُ", "Allahu Ekber", 33),
            DhikrPreset("kelimeitevhid", "لَا إِلٰهَ إِلَّا الله", "Lâ ilâhe illallâh", 100),
            DhikrPreset("istigfar", "أَسْتَغْفِرُ اللَّهَ", "Estağfirullah el-Azîm", 100),
            DhikrPreset("salavat", "اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ", "Salavat-ı Şerife", 100),
            DhikrPreset("serbest", "ذِكْرٌ", "Serbest Zikir", 0) // Sınırsız
        )
    }
}
