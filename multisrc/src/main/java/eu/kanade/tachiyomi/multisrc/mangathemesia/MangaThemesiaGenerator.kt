package eu.kanade.tachiyomi.multisrc.mangathemesia

import generator.ThemeSourceData.MultiLang
import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MangaThemesiaGenerator : ThemeSourceGenerator {

    override val themePkg = "mangathemesia"

    override val themeClass = "MangaThemesia"

    override val baseVersionCode: Int = 20

    override val sources = listOf(
        MultiLang("Asura Scans", "https://www.asurascans.com", listOf("en", "tr"), className = "AsuraScansFactory", pkgName = "asurascans", overrideVersionCode = 16),
        MultiLang("Flame Scans", "https://flamescans.org", listOf("ar", "en"), className = "FlameScansFactory", pkgName = "flamescans", overrideVersionCode = 1),
        SingleLang("Ace Scans", "https://acescans.xyz", "en", isNsfw = true, overrideVersionCode = 2),
        SingleLang("Alpha Scans", "https://alpha-scans.org", "en", overrideVersionCode = 1),
        SingleLang("Animated Glitched Scans", "https://anigliscans.com", "en"),
        SingleLang("Arcane scan", "https://arcanescan.fr", "fr"),
        SingleLang("Arena Scans", "https://arenascans.net", "en"),
        SingleLang("ARESManga", "https://aresmanga.com", "ar", pkgName = "iimanga", overrideVersionCode = 2),
        SingleLang("Azure Scans", "https://azuremanga.com", "en", overrideVersionCode = 1),
        SingleLang("BeastScans", "https://beastscans.com", "en"),
        SingleLang("Boosei", "https://boosei.com", "id", overrideVersionCode = 1),
        SingleLang("Dojing.net", "https://dojing.net", "id", isNsfw = true, className = "DojingNet"),
        SingleLang("FlameScans.fr", "https://flamescans.fr", "fr", className = "FlameScansFR"),
        SingleLang("Franxx Mangás", "https://franxxmangas.net", "pt-BR", className = "FranxxMangas", isNsfw = true),
        SingleLang("Fusion Scanlation", "https://fusionscanlation.com", "es", className = "FusionScanlation", overrideVersionCode = 2),
        SingleLang("Gabut Scans", "https://gabutscans.com", "id", overrideVersionCode = 1),
        SingleLang("Gecenin Lordu", "https://geceninlordu.com", "tr", overrideVersionCode = 1),
        SingleLang("GoGoManga", "https://gogomanga.fun", "en", overrideVersionCode = 1),
        SingleLang("Imagine Scan", "https://imaginescan.com.br", "pt-BR", isNsfw = true, overrideVersionCode = 1),
        SingleLang("Imperfect Comics", "https://imperfectcomic.com", "en", overrideVersionCode = 8),
        SingleLang("InariManga", "https://inarimanga.com", "es"),
        SingleLang("Infernal Void Scans", "https://void-scans.com", "en", overrideVersionCode = 4),
        SingleLang("Kanzenin", "https://kanzenin.xyz", "id", isNsfw = true),
        SingleLang("Kiryuu", "https://kiryuu.id", "id", overrideVersionCode = 6),
        SingleLang("KlanKomik", "https://klankomik.com", "id", overrideVersionCode = 1),
        SingleLang("Komik AV", "https://komikav.com", "id", overrideVersionCode = 1),
        SingleLang("Komik Cast", "https://komikcast.me", "id", overrideVersionCode = 13),
        SingleLang("Komik Lab", "https://komiklab.com", "id"),
        SingleLang("Komik Station", "https://komikstation.co", "id", overrideVersionCode = 3),
        SingleLang("KomikIndo.co", "https://komikindo.co", "id", className = "KomikindoCo", overrideVersionCode = 3),
        SingleLang("KomikMama", "https://komikmama.co", "id", overrideVersionCode = 1),
        SingleLang("Komiku.com", "https://komiku.com", "id", className = "KomikuCom"),
        SingleLang("Kuma Scans (Kuma Translation)", "https://kumascans.com", "en", className = "KumaScans", overrideVersionCode = 1),
        SingleLang("Legion Scan", "https://legionscans.com", "es"),
        SingleLang("LianScans", "https://www.lianscans.my.id", "id", isNsfw = true),
        SingleLang("Magus Manga", "https://magusmanga.com", "ar"),
        SingleLang("Manga Pro", "https://mangaprotm.com", "ar", pkgName = "mangaproz", overrideVersionCode = 3),
        SingleLang("Manga Raw.org", "https://mangaraw.org", "ja", className = "MangaRawOrg", overrideVersionCode = 1),
        SingleLang("Mangacim", "https://www.mangacim.com", "tr", overrideVersionCode = 1),
        SingleLang("MangaKita", "https://mangakita.net", "id", overrideVersionCode = 1),
        SingleLang("Mangakyo", "https://www.mangakyo.me", "id"),
        SingleLang("Mangasusu", "https://mangasusu.co.in", "id", isNsfw = true, overrideVersionCode = 1),
        SingleLang("MangaTale", "https://mangatale.co", "id"),
        SingleLang("Mangayaro", "https://mangayaro.net", "id"),
        SingleLang("MangKomik", "https://mangkomik.com", "id"),
        SingleLang("Mangás Chan", "https://mangaschan.com", "pt-BR", className = "MangasChan"),
        SingleLang("Manhua Raw", "https://manhuaraw.com", "en"),
        SingleLang("ManhwaDesu", "https://manhwadesu.me", "id", isNsfw = true, overrideVersionCode = 1),
        SingleLang("ManhwaIndo", "https://manhwaindo.id", "id", isNsfw = true, overrideVersionCode = 2),
        SingleLang("ManhwaLand.mom", "https://manhwaland.mom", "id", isNsfw = true, className = "ManhwaLandMom", overrideVersionCode = 1),
        SingleLang("ManhwaList", "https://manhwalist.com", "id", overrideVersionCode = 1),
        SingleLang("Manhwax", "https://manhwax.com", "en", isNsfw = true),
        SingleLang("Mareceh", "https://mareceh.com", "id", isNsfw = true, pkgName = "mangceh", overrideVersionCode = 10),
        SingleLang("Martial Manga", "https://martialmanga.com", "es"),
        SingleLang("MasterKomik", "https://masterkomik.com", "id", overrideVersionCode = 1),
        SingleLang("Miau Scan", "https://miauscan.com", "es"),
        SingleLang("Mihentai", "https://mihentai.com", "all", isNsfw = true, overrideVersionCode = 1),
        SingleLang("Mode Scanlator", "https://modescanlator.com", "pt-BR", overrideVersionCode = 8),
        SingleLang("Nekomik", "https://nekomik.com", "id"),
        SingleLang("Ngomik", "https://ngomik.net", "id", overrideVersionCode = 1),
        SingleLang("Nocturnal Scans", "https://nocturnalscans.com", "en", overrideVersionCode = 1),
        SingleLang("Non-Stop Scans", "https://www.nonstopscans.com", "en", className = "NonStopScans"),
        SingleLang("NoxSubs", "https://noxsubs.com", "tr"),
        SingleLang("Omega Scans", "https://omegascans.org", "en", isNsfw = true),
        SingleLang("Origami Orpheans", "https://origami-orpheans.com.br", "pt-BR", overrideVersionCode = 9),
        SingleLang("Ozul Scans", "https://ozulscans.com", "ar"),
        SingleLang("Patatescans", "https://patatescans.com", "fr", isNsfw = true, overrideVersionCode = 2),
        SingleLang("Phantom Scans", "https://phantomscans.com", "en", overrideVersionCode = 1),
        SingleLang("Phoenix Fansub", "https://phoenixfansub.com", "es", overrideVersionCode = 2),
        SingleLang("PMScans", "http://www.rackusreader.org", "en", overrideVersionCode = 2),
        SingleLang("Random Scans", "https://randomscans.xyz", "en"),
        SingleLang("Rawkuma", "https://rawkuma.com/", "ja"),
        SingleLang("Readkomik", "https://readkomik.com", "en", className = "ReadKomik", overrideVersionCode = 1),
        SingleLang("Realm Scans", "https://realmscans.com", "en", overrideVersionCode = 3),
        SingleLang("Ryukonesia", "https://ryukonesia.net", "id"),
        SingleLang("Sekaikomik", "https://www.sekaikomik.live", "id", isNsfw = true, overrideVersionCode = 9),
        SingleLang("Sekaikomik", "https://www.sekaikomik.site", "id", isNsfw = true, overrideVersionCode = 8),
        SingleLang("Sekte Doujin", "https://sektedoujin.club", "id", isNsfw = true, overrideVersionCode = 3),
        SingleLang("Sekte Komik", "https://sektekomik.com", "id", overrideVersionCode = 4),
        SingleLang("Senpai Ediciones", "http://senpaiediciones.com", "es"),
        SingleLang("Shadow Mangas", "https://shadowmangas.com", "es"),
        SingleLang("Shea Manga", "https://sheakomik.com", "id", overrideVersionCode = 4),
        SingleLang("Shooting Star Scans", "https://shootingstarscans.com", "en", overrideVersionCode = 3),
        SingleLang("Silence Scan", "https://silencescan.com.br", "pt-BR", isNsfw = true, overrideVersionCode = 5),
        SingleLang("Skull Scans", "https://www.skullscans.com", "en", overrideVersionCode = 1),
        SingleLang("Snudae Scans", "https://snudaescans.com", "en", isNsfw = true, className = "BatotoScans", overrideVersionCode = 1),
        SingleLang("Summer Fansub", "https://smmr.in", "pt-BR", isNsfw = true),
        SingleLang("Sushi-Scan", "https://sushiscan.su", "fr", className = "SushiScan"),
        SingleLang("Tarot Scans", "https://www.tarotscans.com", "tr"),
        SingleLang("Tempest Manga", "https://manga.tempestfansub.com", "tr"),
        SingleLang("The Apollo Team", "https://theapollo.team", "en"),
        SingleLang("Tsundoku Traduções", "https://tsundoku.com.br", "pt-BR", className = "TsundokuTraducoes", overrideVersionCode = 9),
        SingleLang("TukangKomik", "https://tukangkomik.com", "id"),
        SingleLang("TurkToon", "https://turktoon.com", "tr"),
        SingleLang("West Manga", "https://westmanga.info", "id", overrideVersionCode = 1),
        SingleLang("White Cloud Pavilion (New)", "https://www.whitecloudpavilion.com", "en", pkgName = "whitecloudpavilionnew", className = "WhiteCloudPavilion"),
        SingleLang("World Romance Translation", "https://wrt.my.id", "id", overrideVersionCode = 10),
        SingleLang("xCaliBR Scans", "https://xcalibrscans.com", "en", overrideVersionCode = 4),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MangaThemesiaGenerator().createAll()
        }
    }
}
