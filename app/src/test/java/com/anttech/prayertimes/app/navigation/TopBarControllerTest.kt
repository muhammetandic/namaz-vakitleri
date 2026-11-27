package com.anttech.prayertimes.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TopBarControllerTest {

    @Test
    fun `update stores config and clear removes only the matching owner`() {
        val controller = TopBarController()

        controller.update(
            ownerRoute = "times",
            title = "İstanbul",
            subtitle = "Namaz Vakitleri",
            fontScale = 1.2f,
            actionsKey = false,
            actions = {}
        )

        assertThat(controller.config.value?.title).isEqualTo("İstanbul")
        assertThat(controller.config.value?.fontScale).isEqualTo(1.2f)

        controller.clear("qibla")
        assertThat(controller.config.value?.title).isEqualTo("İstanbul")

        controller.clear("times")
        assertThat(controller.config.value).isNull()
    }

    @Test
    fun `unchanged scalar values keep the same config instance`() {
        val controller = TopBarController()
        controller.update(
            ownerRoute = "times",
            title = "İstanbul",
            subtitle = "Namaz Vakitleri",
            fontScale = 1f,
            actionsKey = null,
            actions = {}
        )
        val first = controller.config.value

        controller.update(
            ownerRoute = "times",
            title = "İstanbul",
            subtitle = "Namaz Vakitleri",
            fontScale = 1f,
            actionsKey = null,
            actions = {}
        )

        assertThat(controller.config.value).isSameInstanceAs(first)
    }
}
