package eu.kanade.tachiyomi.multisrc.wpcomics

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class WPComicsGenerator : ThemeSourceGenerator {

    override val themePkg = "wpcomics"

    override val themeClass = "WPComics"

    override val baseVersionCode: Int = 4

    override val sources = listOf(
        // Original website: https://www.nettruyen.com
        SingleLang("NetTruyen", "https://www.nettruyenclub.com", "vi", isNsfw = true, overrideVersionCode = 21),
        // Original website: https://www.nettruyenx.com
        SingleLang("NetTruyenX", "https://www.nettruyenx.com", "vi", isNsfw = true, overrideVersionCode = 1),
        // Original website: https://www.nhattruyen.com
        SingleLang("NhatTruyen", "https://nhattruyento.com", "vi", isNsfw = true, overrideVersionCode = 15),
        // Original website: https://www.nhattruyens.com
        SingleLang("NhatTruyenS", "https://nhattruyens.com", "vi", isNsfw = true, overrideVersionCode = 1),
        SingleLang("TruyenChon (Dead)", "http://truyenchon.com", "vi", overrideVersionCode = 3, className = "TruyenChon"),
        SingleLang("XOXO Comics", "https://xoxocomic.com", "en", className = "XoxoComics", overrideVersionCode = 3),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            WPComicsGenerator().createAll()
        }
    }
}
