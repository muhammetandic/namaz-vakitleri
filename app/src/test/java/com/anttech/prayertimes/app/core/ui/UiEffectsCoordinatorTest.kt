package com.anttech.prayertimes.core.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UiEffectsCoordinatorTest {

    @Test
    fun `emitted effects reach the activity collector`() = runTest {
        val coordinator = UiEffectsCoordinator()
        val toast = AppUiEffect.Toast("Konum alınamadı")
        val share = AppUiEffect.ShareText("İmsakiye metni")

        coordinator.effects.test {
            coordinator.emit(toast)
            coordinator.emit(share)

            assertThat(awaitItem()).isEqualTo(toast)
            assertThat(awaitItem()).isEqualTo(share)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `preview state is readable without a collector`() {
        val coordinator = UiEffectsCoordinator()

        coordinator.setPreviewActive(true)
        assertThat(coordinator.previewActive.value).isTrue()

        coordinator.setPreviewActive(false)
        assertThat(coordinator.previewActive.value).isFalse()
    }

    @Test
    fun `commands reach the view model collector`() = runTest {
        val coordinator = UiEffectsCoordinator()

        coordinator.commands.test {
            coordinator.send(AppUiCommand.StopPreview)

            assertThat(awaitItem()).isEqualTo(AppUiCommand.StopPreview)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
