package eu.kanade.tachiyomi.multisrc.wpmangareader

import generator.ThemeSourceData.MultiLang
import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class WPMangaReaderGenerator : ThemeSourceGenerator {

    override val themePkg = "wpmangareader"

    override val themeClass = "WPMangaReader"

    override val baseVersionCode: Int = 11

    override val sources = listOf(
        MultiLang("Flame Scans", "https://flamescans.org", listOf("ar", "en"), className = "FlameScansFactory", pkgName = "flamescans", overrideVersionCode = 1),
        SingleLang("Anitation Arts", "https://anitationarts.org", "en", overrideVersionCode = 1),
        SingleLang("Alpha Scans", "https://alpha-scans.org", "en", overrideVersionCode = 1),
        SingleLang("BeastScans", "https://beastscans.com", "en"),
        SingleLang("iiMANGA", "https://iimanga.com", "ar"),
        SingleLang("Magus Manga", "https://magusmanga.com", "ar"),
        SingleLang("Kiryuu", "https://kiryuu.id", "id", overrideVersionCode = 6),
        SingleLang("KomikMama", "https://komikmama.net", "id"),
        SingleLang("MangaKita", "https://mangakita.net", "id", overrideVersionCode = 1),
        SingleLang("Gabut Scans", "https://gabutscans.com", "id"),
        SingleLang("Graze Scans", "https://grazescans.com", "en", overrideVersionCode = 2),
        SingleLang("Mangás Chan", "https://mangaschan.com", "pt-BR", className = "MangasChan"),
        SingleLang("Martial Manga", "https://martialmanga.com", "es"),
        SingleLang("Mode Scanlator", "https://modescanlator.com", "pt-BR", overrideVersionCode = 7),
        SingleLang("Ngomik", "https://ngomik.net", "id", overrideVersionCode = 1),
        SingleLang("Sekaikomik", "https://www.sekaikomik.site", "id", isNsfw = true, overrideVersionCode = 8),
        SingleLang("Davey Scans", "https://daveyscans.com", "id", overrideVersionCode = 1),
        SingleLang("Mangasusu", "https://mangasusu.co.in", "id", isNsfw = true, overrideVersionCode = 1),
        SingleLang("Manhua Raw", "https://manhuaraw.com", "en"),
        SingleLang("TurkToon", "https://turktoon.com", "tr"),
        SingleLang("Gecenin Lordu", "https://geceninlordu.com", "tr", overrideVersionCode = 1),
        SingleLang("PMScans", "http://www.rackusreader.org", "en", overrideVersionCode = 2),
        SingleLang("Realm Scans", "https://realmscans.com", "en", overrideVersionCode = 2),
        SingleLang("Skull Scans", "https://www.skullscans.com", "en", overrideVersionCode = 1),
        SingleLang("Shimada Scans", "https://shimadascans.com", "en"),
        SingleLang("Shooting Star Scans", "https://shootingstarscans.com", "en", overrideVersionCode = 3),
        SingleLang("Azure Scans", "https://azuremanga.com", "en", overrideVersionCode = 1),
        SingleLang("ReaperScans.fr (GS)", "https://reaperscans.fr", "fr", className = "ReaperScansFR", pkgName = "gsnation", overrideVersionCode = 2),
        SingleLang("Patatescans", "https://patatescans.com", "fr", isNsfw = true, overrideVersionCode = 2),
        SingleLang("Fusion Scanlation", "https://fusionscanlation.com", "es", className = "FusionScanlation", overrideVersionCode = 2),
        SingleLang("Ace Scans", "https://acescans.xyz", "en", isNsfw = true, overrideVersionCode = 1),
        SingleLang("Silence Scan", "https://silencescan.com.br", "pt-BR", isNsfw = true, overrideVersionCode = 5),
        SingleLang("YANP Fansub", "https://melhorcasal.com", "pt-BR", isNsfw = true, overrideVersionCode = 1),
        SingleLang("World Romance Translation", "https://wrt.my.id", "id", overrideVersionCode = 7),
        SingleLang("Ozul Scans", "https://ozulscans.com", "ar"),
        SingleLang("Tsundoku Traduções", "https://tsundoku.com.br", "pt-BR", className = "TsundokuTraducoes", overrideVersionCode = 9),
        SingleLang("Komik Lab", "https://komiklab.com", "id"),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            WPMangaReaderGenerator().createAll()
        }
    }
}
