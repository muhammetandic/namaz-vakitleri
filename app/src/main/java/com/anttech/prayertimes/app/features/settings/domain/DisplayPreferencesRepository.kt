package com.anttech.prayertimes.features.settings.domain

import kotlinx.coroutines.flow.Flow

/**
 * Görünüm tercihleri: yazı boyutu çarpanı ve sade mod.
 * Her ikisi de kalıcıdır; süreç ölse de korunur.
 */
interface DisplayPreferencesRepository {

    val fontSizeMultiplier: Flow<Float>

    val isSimpleMode: Flow<Boolean>

    suspend fun setFontSizeMultiplier(multiplier: Float)

    suspend fun setSimpleMode(enabled: Boolean)

    companion object {
        const val DEFAULT_FONT_SCALE = 1.0f
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 1.4f
    }
}
