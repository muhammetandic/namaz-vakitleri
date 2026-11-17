package com.anttech.prayertimes.features.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Widget renkleri launcher tarafında `@color` kaynaklarından çözülür; bu sayede
 * sistem teması değişince uygulama süreci çalışmasa da renkler anında güncellenir.
 * Bu test gündüz/gece kaynaklarının eksiksiz ve birbirinden farklı olduğunu doğrular.
 */
class WidgetColorResourcesTest {

    private val resDir: File = listOf(
        File("src/main/res"),
        File("app/src/main/res")
    ).first { it.exists() }

    private fun colors(path: String): Map<String, Long> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(resDir, path))
        val nodes = document.getElementsByTagName("color")
        val result = mutableMapOf<String, Long>()
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
            val value = node.textContent.trim().removePrefix("#")
            result[name] = value.toLong(16)
        }
        return result
    }

    private fun Map<String, Long>.luminance(key: String): Float {
        val color = this.getValue(key)
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private val day by lazy { colors("values/colors.xml") }
    private val night by lazy { colors("values-night/colors.xml") }

    @Test
    fun `both modes define the same widget colors`() {
        val keys = listOf("widget_text", "widget_secondary", "widget_active_text", "widget_accent")

        assertThat(day.keys).containsAtLeastElementsIn(keys)
        assertThat(night.keys).containsAtLeastElementsIn(keys)
    }

    @Test
    fun `night text is light and day text is dark`() {
        assertThat(night.luminance("widget_text")).isGreaterThan(0.6f)
        assertThat(day.luminance("widget_text")).isLessThan(0.3f)
    }

    @Test
    fun `accent stays orange in both modes`() {
        listOf(day, night).forEach { colors ->
            val accent = colors.getValue("widget_accent")
            val red = accent shr 16 and 0xFF
            val blue = accent and 0xFF
            assertThat(red).isGreaterThan(blue)
        }
    }

    @Test
    fun `active text color differs per mode`() {
        assertThat(day.getValue("widget_active_text"))
            .isNotEqualTo(night.getValue("widget_active_text"))
    }
}
