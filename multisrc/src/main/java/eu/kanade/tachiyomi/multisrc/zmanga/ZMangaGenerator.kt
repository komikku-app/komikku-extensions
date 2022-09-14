package eu.kanade.tachiyomi.multisrc.zmanga

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class ZMangaGenerator : ThemeSourceGenerator {

    override val themePkg = "zmanga"

    override val themeClass = "ZManga"

    override val baseVersionCode: Int = 1

    override val sources = listOf(
        SingleLang("Komikita", "https://komikita.org", "id"),
        SingleLang("KomikPlay", "https://komikplay.com", "id", overrideVersionCode = 1),
        SingleLang("Maid - Manga", "https://www.maid.my.id", "id", overrideVersionCode = 10, className = "MaidManga"),
        SingleLang("Sekte Komik", "https://sektekomik.com", "id", overrideVersionCode = 25),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ZMangaGenerator().createAll()
        }
    }
}
