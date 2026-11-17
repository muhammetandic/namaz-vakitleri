package com.anttech.prayertimes.features.settings.data

import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.features.settings.domain.SoundOption
import io.mockk.mockk
import org.junit.Test

class DataStoreSoundPreferencesRepositoryTest {

    private val repository = DataStoreSoundPreferencesRepository(mockk(relaxed = true))

    @Test
    fun `malformed builtin values fall back to silent`() {
        assertThat(repository.deserializeSound("BUILTIN")).isEqualTo(SoundOption.Silent)
        assertThat(repository.deserializeSound("BUILTIN|ezan_turkiye")).isEqualTo(SoundOption.Silent)
    }

    @Test
    fun `malformed custom values fall back to silent`() {
        assertThat(repository.deserializeSound("CUSTOM")).isEqualTo(SoundOption.Silent)
        assertThat(repository.deserializeSound("CUSTOM|uri")).isEqualTo(SoundOption.Silent)
    }

    @Test
    fun `valid serialized values still deserialize`() {
        assertThat(repository.deserializeSound("BUILTIN|id|Ezan|ezan1"))
            .isEqualTo(SoundOption.BuiltIn("id", "Ezan", "ezan1"))
        assertThat(repository.deserializeSound("CUSTOM|id|Özel|content://audio/1"))
            .isEqualTo(SoundOption.CustomFile("id", "Özel", "content://audio/1"))
        assertThat(repository.deserializeSound("VIBRATE_ONLY|vibrate_only|Sessiz / Yalnızca Titreşim"))
            .isEqualTo(SoundOption.VibrateOnly)
    }
}
