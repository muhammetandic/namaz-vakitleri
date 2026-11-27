package com.anttech.prayertimes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * Üst çubuğu rota sahipleri besler. [ownerRoute], eski bir rotanın
 * temizleme çağrısının yeni rotanın başlığını silmesini engeller.
 */
data class TopBarConfig(
    val ownerRoute: String,
    val title: String,
    val subtitle: String,
    val fontScale: Float,
    val actionsKey: Any? = null,
    val actions: @Composable () -> Unit
)

class TopBarController {

    private val _config: MutableState<TopBarConfig?> = mutableStateOf(null)
    val config: State<TopBarConfig?> = _config

    fun update(
        ownerRoute: String,
        title: String,
        subtitle: String,
        fontScale: Float,
        actionsKey: Any? = null,
        actions: @Composable () -> Unit
    ) {
        val current = _config.value
        if (current != null &&
            current.ownerRoute == ownerRoute &&
            current.title == title &&
            current.subtitle == subtitle &&
            current.fontScale == fontScale &&
            current.actionsKey == actionsKey
        ) {
            return
        }
        _config.value = TopBarConfig(
            ownerRoute = ownerRoute,
            title = title,
            subtitle = subtitle,
            fontScale = fontScale,
            actionsKey = actionsKey,
            actions = actions
        )
    }

    fun clear(ownerRoute: String) {
        if (_config.value?.ownerRoute == ownerRoute) {
            _config.value = null
        }
    }
}
