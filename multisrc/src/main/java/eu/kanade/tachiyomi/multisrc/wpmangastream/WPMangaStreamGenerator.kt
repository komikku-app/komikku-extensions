package eu.kanade.tachiyomi.multisrc.wpmangastream

import generator.ThemeSourceData.MultiLang
import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class WPMangaStreamGenerator : ThemeSourceGenerator {

    override val themePkg = "wpmangastream"

    override val themeClass = "WPMangaStream"

    override val baseVersionCode: Int = 15

    override val sources = listOf(
        MultiLang("Asura Scans", "https://www.asurascans.com", listOf("en", "tr"), className = "AsuraScansFactory", pkgName = "asurascans", overrideVersionCode = 14),
        SingleLang("Boosei", "https://boosei.com", "id", overrideVersionCode = 1),
        SingleLang("GoGoManga", "https://gogomanga.fun", "en", overrideVersionCode = 1),
        SingleLang("Imagine Scan", "https://imaginescan.com.br", "pt-BR", isNsfw = true, overrideVersionCode = 1),
        SingleLang("Imperfect Comics", "https://imperfectcomic.com", "en", overrideVersionCode = 8),
        SingleLang("Infernal Void Scans", "https://infernalvoidscans.com", "en", overrideVersionCode = 3),
        SingleLang("Kanzenin", "https://kanzenin.xyz", "id", isNsfw = true),
        SingleLang("KlanKomik", "https://klankomik.com", "id", overrideVersionCode = 1),
        SingleLang("Komik AV", "https://komikav.com", "id", overrideVersionCode = 1),
        SingleLang("Komik Cast", "https://komikcast.me", "id", overrideVersionCode = 12),
        SingleLang("Komik Station", "https://komikstation.co", "id", overrideVersionCode = 3),
        SingleLang("KomikIndo.co", "https://komikindo.co", "id", className = "KomikindoCo", overrideVersionCode = 3),
        SingleLang("Kuma Scans (Kuma Translation)", "https://kumascans.com", "en", className = "KumaScans", overrideVersionCode = 1),
        SingleLang("Manga Pro", "https://mangaprotm.com", "ar", pkgName = "mangaproz", overrideVersionCode = 3),
        SingleLang("Manga Raw.org", "https://mangaraw.org", "ja", className = "MangaRawOrg", overrideVersionCode = 1),
        SingleLang("Manhwax", "https://manhwax.com", "en", isNsfw = true),
        SingleLang("MangaSwat", "https://swatmanga.co", "ar", overrideVersionCode = 7),
        SingleLang("Mangakyo", "https://www.mangakyo.me", "id"),
        SingleLang("Mareceh", "https://mareceh.com", "id", isNsfw = true, pkgName = "mangceh", overrideVersionCode = 10),
        SingleLang("MasterKomik", "https://masterkomik.com", "id", overrideVersionCode = 1),
        SingleLang("Mihentai", "https://mihentai.com", "en", isNsfw = true, overrideVersionCode = 1),
        SingleLang("Non-Stop Scans", "https://www.nonstopscans.com", "en", className = "NonStopScans"),
        SingleLang("NoxSubs", "https://noxsubs.com", "tr"),
        SingleLang("Omega Scans", "https://omegascans.org", "en", isNsfw = true),
        SingleLang("Phantom Scans", "https://phantomscans.com", "en", overrideVersionCode = 1),
        SingleLang("Phoenix Fansub", "https://phoenixfansub.com", "es", overrideVersionCode = 2),
        SingleLang("Random Scans", "https://randomscans.xyz", "en"),
        SingleLang("Rawkuma", "https://rawkuma.com/", "ja"),
        SingleLang("Readkomik", "https://readkomik.com", "en", className = "ReadKomik", overrideVersionCode = 1),
        SingleLang("Sekte Doujin", "https://sektedoujin.club", "id", isNsfw = true, overrideVersionCode = 3),
        SingleLang("Sekte Komik", "https://sektekomik.com", "id", overrideVersionCode = 3),
        SingleLang("Shea Manga", "http://sheamanga.my.id", "id", overrideVersionCode = 3),
        SingleLang("Snudae Scans", "https://snudaescans.com", "en", isNsfw = true, className = "BatotoScans", overrideVersionCode = 1),
        SingleLang("Tempest Manga", "https://manga.tempestfansub.com", "tr"),
        SingleLang("The Apollo Team", "https://theapollo.team", "en"),
        SingleLang("TukangKomik", "https://tukangkomik.com", "id"),
        SingleLang("West Manga", "https://westmanga.info", "id", overrideVersionCode = 1),
        SingleLang("xCaliBR Scans", "https://xcalibrscans.com", "en", overrideVersionCode = 3),
        SingleLang("Shadow Mangas", "https://shadowmangas.com", "es"),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            WPMangaStreamGenerator().createAll()
        }
    }
}
