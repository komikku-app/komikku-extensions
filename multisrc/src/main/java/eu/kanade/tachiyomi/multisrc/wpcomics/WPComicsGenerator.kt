package eu.kanade.tachiyomi.multisrc.wpcomics

import generator.ThemeSourceData.MultiLang
import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class WPComicsGenerator : ThemeSourceGenerator {

    override val themePkg = "wpcomics"

    override val themeClass = "WPComics"

    override val baseVersionCode: Int = 2

    override val sources = listOf(
        MultiLang("MangaToro", "https://mangatoro.com", listOf("en", "ja"), overrideVersionCode = 1),
        SingleLang("NetTruyen", "http://www.nettruyengo.com", "vi", overrideVersionCode = 6),
        SingleLang("NhatTruyen", "http://www.nhattruyenvip.com", "vi", overrideVersionCode = 6),
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
