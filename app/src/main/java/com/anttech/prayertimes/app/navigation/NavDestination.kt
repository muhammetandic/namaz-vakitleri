package com.anttech.prayertimes.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    object Times : NavDestination("times", "Vakitler", Icons.Default.AccessTime, "nav_times")
    object Imsakiye : NavDestination("imsakiye", "İmsakiye", Icons.Default.DateRange, "nav_imsakiye")
    object Qibla : NavDestination("qibla", "Kıble", Icons.Default.Explore, "nav_qibla")
    object Tracker : NavDestination("tracker", "Zikir", Icons.Default.TouchApp, "nav_tracker")
    object Sounds : NavDestination("sound_settings", "Sesler", Icons.Default.MusicNote, "nav_sounds")

    companion object {
        /**
         * 4 core tabs. Sounds lives under Settings now.
         * Getter, not a stored val: prevents a static-init recursion where reading
         * [Times.INSTANCE] before its initializer finishes yields a null entry.
         */
        val primaryTabs: List<NavDestination>
            get() = listOf(Times, Imsakiye, Qibla, Tracker)
    }
}
