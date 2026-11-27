package com.anttech.prayertimes.core.ui

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Tek seferlik olayların Activity katmanına taşınması için ortak kanal. */
sealed interface AppUiEffect {
    data class Toast(val message: String) : AppUiEffect
    data class ShareText(val content: String) : AppUiEffect
}

/** ViewModel'lere gönderilen ve ekranı yeniden çizmeyen UI komutları. */
sealed interface AppUiCommand {
    data object StopPreview : AppUiCommand
}

/**
 * ViewModel'ler kendi [effect] akışlarını birim testler için korur.
 * Bu koordinatör aynı olayı Activity'ye de iletir; olay, kullanıcı başka
 * sekmedeyken üretilse bile kaybolmaz.
 */
@Singleton
class UiEffectsCoordinator @Inject constructor() {

    private val _effects = Channel<AppUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _commands = MutableSharedFlow<AppUiCommand>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val commands: SharedFlow<AppUiCommand> = _commands

    private val _previewActive = MutableStateFlow(false)
    val previewActive: StateFlow<Boolean> = _previewActive.asStateFlow()

    fun emit(effect: AppUiEffect) {
        _effects.trySend(effect)
    }

    fun send(command: AppUiCommand) {
        _commands.tryEmit(command)
    }

    fun setPreviewActive(active: Boolean) {
        _previewActive.value = active
    }
}
