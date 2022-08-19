package eu.kanade.tachiyomi.multisrc.mangaraw

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MangaRawGenerator : ThemeSourceGenerator {
    override val themeClass = "MangaRawTheme"

    override val themePkg = "mangaraw"

    override val baseVersionCode = 2

    override val sources = listOf(
        SingleLang("SyoSetu", "https://syosetu.top", "ja", overrideVersionCode = 1),
        SingleLang("MangaRaw", "https://manga9.co", "ja", pkgName = "manga9co", overrideVersionCode = 1),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MangaRawGenerator().createAll()
        }
    }
}
