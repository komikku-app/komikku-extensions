package eu.kanade.tachiyomi.multisrc.wpcomics

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class WPComicsGenerator : ThemeSourceGenerator {

    override val themePkg = "wpcomics"

    override val themeClass = "WPComics"

    override val baseVersionCode: Int = 2

    override val sources = listOf(
        SingleLang("NetTruyen", "https://www.nettruyenus.com", "vi", overrideVersionCode = 19),
        SingleLang("NhatTruyen", "https://nhattruyenplus.com", "vi", overrideVersionCode = 12),
        SingleLang("TruyenChon", "http://truyenchon.com", "vi", overrideVersionCode = 3),
        SingleLang("XOXO Comics", "https://xoxocomics.net", "en", className = "XoxoComics", overrideVersionCode = 2),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            WPComicsGenerator().createAll()
        }
    }
}
