package eu.kanade.tachiyomi.multisrc.wpcomics

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class WPComicsGenerator : ThemeSourceGenerator {

    override val themePkg = "wpcomics"

    override val themeClass = "WPComics"

    override val baseVersionCode: Int = 2

    override val sources = listOf(
        SingleLang("NetTruyen", "https://www.nettruyenin.com", "vi", overrideVersionCode = 10),
        SingleLang("NhatTruyen", "https://nhattruyenin.com", "vi", overrideVersionCode = 8),
        SingleLang("TruyenChon", "http://truyenchon.com", "vi", overrideVersionCode = 3),
        SingleLang("XOXO Comics", "https://xoxocomics.com", "en", className = "XoxoComics", overrideVersionCode = 1),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            WPComicsGenerator().createAll()
        }
    }
}
