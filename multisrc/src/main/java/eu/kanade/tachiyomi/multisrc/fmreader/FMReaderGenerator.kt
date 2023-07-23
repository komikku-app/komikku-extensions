package eu.kanade.tachiyomi.multisrc.fmreader

import generator.ThemeSourceData.MultiLang
import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class FMReaderGenerator : ThemeSourceGenerator {

    override val themePkg = "fmreader"

    override val themeClass = "FMReader"

    override val baseVersionCode: Int = 8

    override val sources = listOf(
        MultiLang("Manhwa18.net", "https://manhwa18.net", listOf("en", "ko"), className = "Manhwa18NetFactory", isNsfw = true),
        SingleLang("Epik Manga", "https://www.epikmanga.com", "tr"),
        SingleLang("KissLove", "https://klz9.com", "ja", isNsfw = true, overrideVersionCode = 4),
        SingleLang("Manga-TR", "https://manga-tr.com", "tr", className = "MangaTR", overrideVersionCode = 1),
        SingleLang("ManhuaRock", "https://manhuarock.net", "vi", overrideVersionCode = 1),
        SingleLang("Manhwa18", "https://manhwa18.com", "en", isNsfw = true, overrideVersionCode = 2),
        SingleLang("Say Truyen", "https://saytruyenvip.com", "vi", overrideVersionCode = 3),
        SingleLang("WeLoveManga", "https://weloma.art", "ja", pkgName = "rawlh", overrideVersionCode = 4),
        SingleLang("Manga1000", "https://manga1000.top", "ja"),
        SingleLang("WeLoveMangaOne", "https://welovemanga.one", "ja", isNsfw = true, overrideVersionCode = 1),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            FMReaderGenerator().createAll()
        }
    }
}
