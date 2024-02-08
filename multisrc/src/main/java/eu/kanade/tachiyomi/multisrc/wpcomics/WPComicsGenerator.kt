package eu.kanade.tachiyomi.multisrc.wpcomics

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class WPComicsGenerator : ThemeSourceGenerator {

    override val themePkg = "wpcomics"

    override val themeClass = "WPComics"

    override val baseVersionCode: Int = 3 + 2

    override val sources = listOf(
        SingleLang("NetTruyen", "https://www.nettruyenss.com", "vi", isNsfw = true, overrideVersionCode = 22 + 1),
        SingleLang("NetTruyenX", "https://nettruyenx.com", "vi", isNsfw = true, overrideVersionCode = 1),
        SingleLang("NhatTruyen", "https://nhattruyento.com", "vi", isNsfw = true, overrideVersionCode = 14 + 1),
        SingleLang("NhatTruyenS", "https://nhattruyens.com", "vi", isNsfw = true, overrideVersionCode = 1),
        SingleLang("XOXO Comics", "https://xoxocomic.com", "en", className = "XoxoComics", overrideVersionCode = 3),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            WPComicsGenerator().createAll()
        }
    }
}
