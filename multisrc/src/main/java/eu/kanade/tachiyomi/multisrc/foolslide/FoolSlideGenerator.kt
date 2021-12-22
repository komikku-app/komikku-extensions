package eu.kanade.tachiyomi.multisrc.foolslide

import generator.ThemeSourceData.MultiLang
import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class FoolSlideGenerator : ThemeSourceGenerator {

    override val themePkg = "foolslide"

    override val themeClass = "FoolSlide"

    override val baseVersionCode: Int = 3

    override val sources = listOf(
        SingleLang("The Cat Scans", "https://reader2.thecatscans.com", "en"),
        SingleLang("Silent Sky", "https://reader.silentsky-scans.net", "en"),
        SingleLang("Death Toll Scans", "https://reader.deathtollscans.net", "en"),
        SingleLang("MangaScouts", "http://onlinereader.mangascouts.org", "de", overrideVersionCode = 1),
        SingleLang("Lilyreader", "https://manga.smuglo.li", "en"),
        SingleLang("Evil Flowers", "https://reader.evilflowers.com", "en", overrideVersionCode = 1),
        SingleLang("Русификация", "https://rusmanga.ru", "ru", className = "Russification"),
        SingleLang("PowerManga", "https://reader.powermanga.org", "it", className = "PowerMangaIT"),
        MultiLang("FoolSlide Customizable", "", listOf("other")),
        SingleLang("Menudo-Fansub", "https://www.menudo-fansub.com", "es", className = "MenudoFansub", overrideVersionCode = 1),
        SingleLang("Sense-Scans", "https://sensescans.com", "en", className = "SenseScans", overrideVersionCode = 2),
        SingleLang("Kirei Cake", "https://reader.kireicake.com", "en"),
        SingleLang("Mangatellers", "https://reader.mangatellers.gr", "en"),
        SingleLang("Iskultrip Scans", "https://maryfaye.net", "en"),
        SingleLang("Anata no Motokare", "https://motokare.xyz", "en", className = "AnataNoMotokare"),
        SingleLang("Yuri-ism", "https://www.yuri-ism.net", "en", className = "YuriIsm"),
        SingleLang("LupiTeam", "https://lupiteam.net", "it", overrideVersionCode = 1),
        SingleLang("Zandy no Fansub", "https://zandynofansub.aishiteru.org", "en"),
        SingleLang("Kirishima Fansub", "https://www.kirishimafansub.net", "es"),
        SingleLang("Baixar Hentai", "https://leitura.baixarhentai.net", "pt-BR", isNsfw = true, overrideVersionCode = 3),
        MultiLang("HNI-Scantrad", "https://hni-scantrad.com", listOf("fr", "en"), className = "HNIScantradFactory", pkgName = "hniscantrad", overrideVersionCode = 1),
        SingleLang("QuegnaReader", "http://pignaquegna.altervista.org", "it", overrideVersionCode = 1),
        SingleLang("NIFTeam", "http://read-nifteam.info", "it"),
        SingleLang("TuttoAnimeManga", "https://tuttoanimemanga.net", "it"),
        SingleLang("Tortuga Ceviri", "http://tortugaceviri.com", "tr", overrideVersionCode = 1),
        SingleLang("Rama", "https://www.ramareader.it", "it"),
        SingleLang("Mabushimajo", "http://mabushimajo.com", "tr"),
        SingleLang("Hyakuro", "https://hyakuro.com", "en"),
        SingleLang("Le Cercle du Scan", "https://lel.lecercleduscan.com", "fr", className = "LeCercleDuScan", overrideVersionCode = 1),
        SingleLang("LetItGo Scans", "https://reader.letitgo.scans.today", "en", overrideVersionCode = 1),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            FoolSlideGenerator().createAll()
        }
    }
}
