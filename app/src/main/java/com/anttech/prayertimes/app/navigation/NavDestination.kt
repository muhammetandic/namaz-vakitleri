package com.anttech.prayertimes.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector

object AppRoute {
    const val TIMES = "times"
    const val IMSAKIYE = "imsakiye"
    const val QIBLA = "qibla"
    const val TRACKER = "tracker"
    const val SOUNDS = "sound_settings"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS_SETTINGS = "notifications_settings"
    const val PERMISSIONS = "permissions"
    const val CITY_SELECTION = "city_selection"
    const val ABOUT = "about"
}

sealed class NavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    object Times : NavDestination(AppRoute.TIMES, "Vakitler", Icons.Default.AccessTime, "nav_times")
    object Imsakiye : NavDestination(AppRoute.IMSAKIYE, "İmsakiye", Icons.Default.DateRange, "nav_imsakiye")
    object Qibla : NavDestination(AppRoute.QIBLA, "Kıble", Icons.Default.Explore, "nav_qibla")
    object Tracker : NavDestination(AppRoute.TRACKER, "Zikir", Icons.Default.TouchApp, "nav_tracker")
    object Sounds : NavDestination(AppRoute.SOUNDS, "Sesler", Icons.Default.MusicNote, "nav_sounds")

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
