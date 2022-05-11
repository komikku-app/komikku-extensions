package eu.kanade.tachiyomi.multisrc.bilibili

import generator.ThemeSourceData.MultiLang
import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class BilibiliGenerator : ThemeSourceGenerator {

    override val themePkg = "bilibili"

    override val themeClass = "Bilibili"

    override val baseVersionCode: Int = 1

    override val sources = listOf(
        MultiLang("BILIBILI COMICS", "https://www.bilibilicomics.com", listOf("en", "zh-Hans", "id"), className = "BilibiliComicsFactory"),
        SingleLang("BILIBILI MANGA", "https://manga.bilibili.com", "zh-Hans", className = "BilibiliManga")
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            BilibiliGenerator().createAll()
        }
    }
}
