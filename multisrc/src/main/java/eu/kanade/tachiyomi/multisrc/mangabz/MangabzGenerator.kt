package eu.kanade.tachiyomi.multisrc.mangabz

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MangabzGenerator : ThemeSourceGenerator {
    override val themeClass = "MangabzTheme"
    override val themePkg = "mangabz"
    override val baseVersionCode = 1
    override val sources = listOf(
        SingleLang("Mangabz", "https://mangabz.com", "zh", overrideVersionCode = 6),
        SingleLang("vomic", "http://www.vomicmh.com", "zh", className = "Vomic"),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MangabzGenerator().createAll()
        }
    }
}
