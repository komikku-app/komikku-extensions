package eu.kanade.tachiyomi.multisrc.sinmh

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class SinMHGenerator : ThemeSourceGenerator {
    override val themeClass = "SinMH"
    override val themePkg = "sinmh"
    override val baseVersionCode = 3
    override val sources = listOf(
        SingleLang(
            name = "Gufeng Manhua", baseUrl = "https://www.gufengmh9.com", lang = "zh",
            className = "Gufengmh", sourceName = "古风漫画网", overrideVersionCode = 5
        ),
        SingleLang(
            name = "Imitui Manhua", baseUrl = "https://www.imitui.com", lang = "zh",
            className = "Imitui", sourceName = "爱米推漫画", overrideVersionCode = 2
        )
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SinMHGenerator().createAll()
        }
    }
}
