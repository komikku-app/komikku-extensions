package eu.kanade.tachiyomi.multisrc.madtheme

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MadThemeGenerator : ThemeSourceGenerator {

    override val themePkg = "madtheme"

    override val themeClass = "MadTheme"

    override val baseVersionCode: Int = 3

    override val sources = listOf(
        SingleLang("BeeHentai", "https://beehentai.com", "en", isNsfw = true),
        SingleLang("BoxManhwa", "https://boxmanhwa.com", "en", isNsfw = true),
        SingleLang("MangaBuddy", "https://mangabuddy.com", "en", isNsfw = true),
        SingleLang("MangaCute", "https://mangacute.com", "en", isNsfw = true),
        SingleLang("MangaFab", "https://mangafab.com", "en", isNsfw = true),
        SingleLang("MangaForest", "https://mangaforest.com", "en", isNsfw = true),
        SingleLang("MangaMad", "https://mangamad.com", "en", isNsfw = true),
        SingleLang("MangaMax", "https://mangamax.net", "en", isNsfw = true),
        SingleLang("MangaSaga", "https://mangasaga.com", "en", isNsfw = true),
        SingleLang("MangaSpin", "https://mangaspin.com", "en", isNsfw = true),
        SingleLang("MangaXYZ", "https://mangaxyz.com", "en", isNsfw = true),
        SingleLang("ManhuaNow", "https://manhuanow.com", "en", isNsfw = true),
        SingleLang("ManhuaSite", "https://manhuasite.com", "en", isNsfw = true),
        SingleLang("TooniClub", "https://tooniclub.com", "en", isNsfw = true),
        SingleLang("TooniFab", "https://toonifab.com", "en", isNsfw = true),
        SingleLang("Toonily.me", "https://toonily.me", "en", isNsfw = true, className = "ToonilyMe"),
        SingleLang("TooniTube", "https://toonitube.com", "en", isNsfw = true),
        SingleLang("TrueManga", "https://truemanga.com", "en", isNsfw = true),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MadThemeGenerator().createAll()
        }
    }
}
