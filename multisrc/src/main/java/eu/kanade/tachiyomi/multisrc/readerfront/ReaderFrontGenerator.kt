package eu.kanade.tachiyomi.multisrc.readerfront

import generator.ThemeSourceData.MultiLang
import generator.ThemeSourceGenerator

class ReaderFrontGenerator : ThemeSourceGenerator {
    override val themePkg = "readerfront"

    override val themeClass = "ReaderFront"

    override val baseVersionCode = 1

    override val sources = listOf(
        MultiLang("Ravens Scans", "https://ravens-scans.com/", listOf("es", "en"), true),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = ReaderFrontGenerator().createAll()
    }
}
