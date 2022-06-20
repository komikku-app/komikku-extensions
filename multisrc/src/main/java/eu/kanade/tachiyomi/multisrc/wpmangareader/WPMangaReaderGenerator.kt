package eu.kanade.tachiyomi.multisrc.wpmangareader

import generator.ThemeSourceData.MultiLang
import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class WPMangaReaderGenerator : ThemeSourceGenerator {

    override val themePkg = "wpmangareader"

    override val themeClass = "WPMangaReader"

    override val baseVersionCode: Int = 13

    override val sources = listOf(
        MultiLang("Flame Scans", "https://flamescans.org", listOf("ar", "en"), className = "FlameScansFactory", pkgName = "flamescans", overrideVersionCode = 1),
        SingleLang("Ace Scans", "https://acescans.xyz", "en", isNsfw = true, overrideVersionCode = 1),
        SingleLang("Alpha Scans", "https://alpha-scans.org", "en", overrideVersionCode = 1),
        SingleLang("Anitation Arts", "https://anitationarts.org", "en", overrideVersionCode = 1),
        SingleLang("Arcane scan", "https://arcanescan.fr", "fr"),
        SingleLang("Azure Scans", "https://azuremanga.com", "en", overrideVersionCode = 1),
        SingleLang("BeastScans", "https://beastscans.com", "en"),
        SingleLang("Franxx Mangás", "https://franxxmangas.net", "pt-BR", className = "FranxxMangas", isNsfw = true),
        SingleLang("Fusion Scanlation", "https://fusionscanlation.com", "es", className = "FusionScanlation", overrideVersionCode = 2),
        SingleLang("Gabut Scans", "https://gabutscans.com", "id"),
        SingleLang("Gecenin Lordu", "https://geceninlordu.com", "tr", overrideVersionCode = 1),
        SingleLang("InariManga", "https://inarimanga.com", "es"),
        SingleLang("Kiryuu", "https://kiryuu.id", "id", overrideVersionCode = 6),
        SingleLang("Komik Lab", "https://komiklab.com", "id"),
        SingleLang("KomikMama", "https://komikmama.net", "id"),
        SingleLang("Legion Scan", "https://legionscans.com", "es"),
        SingleLang("Magus Manga", "https://magusmanga.com", "ar"),
        SingleLang("MangKomik", "https://mangkomik.com", "id"),
        SingleLang("MangaKita", "https://mangakita.net", "id", overrideVersionCode = 1),
        SingleLang("Mangasusu", "https://mangasusu.co.in", "id", isNsfw = true, overrideVersionCode = 1),
        SingleLang("Mangás Chan", "https://mangaschan.com", "pt-BR", className = "MangasChan"),
        SingleLang("Manhua Raw", "https://manhuaraw.com", "en"),
        SingleLang("ManhwaIndo", "https://manhwaindo.id", "id", isNsfw = true, overrideVersionCode = 2),
        SingleLang("Martial Manga", "https://martialmanga.com", "es"),
        SingleLang("Miau Scan", "https://miauscan.com", "es"),
        SingleLang("Mode Scanlator", "https://modescanlator.com", "pt-BR", overrideVersionCode = 8),
        SingleLang("Ngomik", "https://ngomik.net", "id", overrideVersionCode = 1),
        SingleLang("Origami Orpheans", "https://origami-orpheans.com.br", "pt-BR", overrideVersionCode = 9),
        SingleLang("Ozul Scans", "https://ozulscans.com", "ar"),
        SingleLang("PMScans", "http://www.rackusreader.org", "en", overrideVersionCode = 2),
        SingleLang("Patatescans", "https://patatescans.com", "fr", isNsfw = true, overrideVersionCode = 2),
        SingleLang("Realm Scans", "https://realmscans.com", "en", overrideVersionCode = 3),
        SingleLang("Sekaikomik", "https://www.sekaikomik.live", "id", isNsfw = true, overrideVersionCode = 9),
        SingleLang("Sekaikomik", "https://www.sekaikomik.site", "id", isNsfw = true, overrideVersionCode = 8),
        SingleLang("Shooting Star Scans", "https://shootingstarscans.com", "en", overrideVersionCode = 3),
        SingleLang("Silence Scan", "https://silencescan.com.br", "pt-BR", isNsfw = true, overrideVersionCode = 5),
        SingleLang("Skull Scans", "https://www.skullscans.com", "en", overrideVersionCode = 1),
        SingleLang("Tsundoku Traduções", "https://tsundoku.com.br", "pt-BR", className = "TsundokuTraducoes", overrideVersionCode = 9),
        SingleLang("TurkToon", "https://turktoon.com", "tr"),
        SingleLang("World Romance Translation", "https://wrt.my.id", "id", overrideVersionCode = 8),
        SingleLang("ARESManga", "https://aresmanga.com", "ar", pkgName = "iimanga", overrideVersionCode = 2),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            WPMangaReaderGenerator().createAll()
        }
    }
}
