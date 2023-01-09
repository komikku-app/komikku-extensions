package eu.kanade.tachiyomi.multisrc.mangaraw

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MangaRawGenerator : ThemeSourceGenerator {
    override val themeClass = "MangaRawTheme"

    override val themePkg = "mangaraw"

    override val baseVersionCode = 4

    override val sources = listOf(
        SingleLang("SyoSetu", "https://syosetu.top", "ja"),
        SingleLang("MangaRaw", "https://manga1001.in", "ja", pkgName = "manga9co", overrideVersionCode = 2),
        SingleLang("MangaRawRU", "https://mangaraw.ru", "ja", overrideVersionCode = 1),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MangaRawGenerator().createAll()
        }
    }
}
