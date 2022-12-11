package eu.kanade.tachiyomi.multisrc.zeistmanga

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class ZeistMangaGenerator : ThemeSourceGenerator {

    override val themePkg = "zeistmanga"

    override val themeClass = "ZeistManga"

    override val baseVersionCode: Int = 1

    override val sources = listOf(
        SingleLang("DatGarScanlation", "https://datgarscanlation.blogspot.com", "es"),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ZeistMangaGenerator().createAll()
        }
    }
}
