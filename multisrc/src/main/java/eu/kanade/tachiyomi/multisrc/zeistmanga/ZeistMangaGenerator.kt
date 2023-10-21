package eu.kanade.tachiyomi.multisrc.zeistmanga

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class ZeistMangaGenerator : ThemeSourceGenerator {

    override val themePkg = "zeistmanga"

    override val themeClass = "ZeistManga"

    override val baseVersionCode: Int = 7

    override val sources = listOf(
        SingleLang("AiYuManga", "https://www.aiyumanhua.com", "es", overrideVersionCode = 27),
        SingleLang("Asupan Komik", "https://www.asupankomik.my.id", "id", overrideVersionCode = 1),
        SingleLang("Hijala", "https://hijala.blogspot.com", "ar"),
        SingleLang("KLManhua", "https://klmanhua.blogspot.com", "id", isNsfw = true),
        SingleLang("Manga Ai Land", "https://manga-ai-land.blogspot.com", "ar"),
        SingleLang("Muslos No Sekai", "https://muslosnosekai.blogspot.com", "es"),
        SingleLang("ShiyuraSub", "https://shiyurasub.blogspot.com", "id"),
        SingleLang("Tooncubus", "https://www.tooncubus.top", "id", isNsfw = true),
        SingleLang("SobatManKu", "https://www.sobatmanku19.site", "id"),
        SingleLang("KomikRealm", "https://www.komikrealm.my.id", "id"),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ZeistMangaGenerator().createAll()
        }
    }
}
