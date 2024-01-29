package eu.kanade.tachiyomi.multisrc.wpcomics

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class WPComicsGenerator : ThemeSourceGenerator {

    override val themePkg = "wpcomics"

    override val themeClass = "WPComics"

    override val baseVersionCode: Int = 4

    override val sources = listOf(
        SingleLang("NetTruyen", "https://www.nettruyenss.com", "vi", isNsfw = true, overrideVersionCode = 22),
        SingleLang("NetTruyenX", "https://nettruyenx.com", "vi", isNsfw = true, overrideVersionCode = 1),
        SingleLang("NhatTruyen", "https://nhattruyento.com", "vi", isNsfw = true, overrideVersionCode = 15),
        SingleLang("NhatTruyenS", "https://nhattruyens.com", "vi", isNsfw = true, overrideVersionCode = 1),
        SingleLang("TruyenChon", "http://truyenchon.com", "vi", overrideVersionCode = 3),
        SingleLang("XOXO Comics", "https://xoxocomic.com", "en", className = "XoxoComics", overrideVersionCode = 3),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            WPComicsGenerator().createAll()
        }
    }
}
