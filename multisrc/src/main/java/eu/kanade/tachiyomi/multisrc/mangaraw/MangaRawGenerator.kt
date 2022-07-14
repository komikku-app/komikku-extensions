package eu.kanade.tachiyomi.multisrc.mangaraw

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MangaRawGenerator : ThemeSourceGenerator {
    override val themeClass = "MangaRaw"

    override val themePkg = "mangaraw"

    override val baseVersionCode: Int = 1

    override val sources = listOf(
        SingleLang("Manga1001", "https://manga1001.top", "ja", isNsfw = false, overrideVersionCode = 1),
        SingleLang("SyoSetu", "https://syosetu.top", "ja", isNsfw = false, overrideVersionCode = 1),
        SingleLang("Manga9co", "https://manga9.co", "ja", isNsfw = false, overrideVersionCode = 1),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MangaRawGenerator().createAll()
        }
    }
}
