package eu.kanade.tachiyomi.multisrc.wpmangareader

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class WPMangaReaderGenerator : ThemeSourceGenerator {

    override val themePkg = "wpmangareader"

    override val themeClass = "WPMangaReader"

    override val baseVersionCode: Int = 11

    override val sources = listOf(
        SingleLang("Anitation Arts", "https://anitationarts.org", "en", overrideVersionCode = 1),
        SingleLang("Alpha Scans", "https://alpha-scans.org", "en"),
        SingleLang("AR FlameScans", "https://ar.flamescans.org", "ar"),
        SingleLang("iiMANGA", "https://iimanga.com", "ar"),
        SingleLang("Magus Manga", "https://magusmanga.com", "ar"),
        SingleLang("Kiryuu", "https://kiryuu.id", "id", overrideVersionCode = 2),
        SingleLang("KomikMama", "https://komikmama.net", "id"),
        SingleLang("MangaKita", "https://mangakita.net", "id", overrideVersionCode = 1),
        SingleLang("Gabut Scans", "https://gabutscans.com", "id"),
        SingleLang("Graze Scans", "https://grazescans.com", "en", overrideVersionCode = 2),
        SingleLang("Martial Manga", "https://martialmanga.com/", "es"),
        SingleLang("Ngomik", "https://ngomik.net", "id", overrideVersionCode = 1),
        SingleLang("Sekaikomik", "https://www.sekaikomik.site", "id", isNsfw = true, overrideVersionCode = 8),
        SingleLang("Davey Scans", "https://daveyscans.com/", "id"),
        SingleLang("Mangasusu", "https://mangasusu.co.in", "id", isNsfw = true),
        SingleLang("Manhua Raw", "https://manhuaraw.com", "en"),
        SingleLang("TurkToon", "https://turktoon.com", "tr"),
        SingleLang("Gecenin Lordu", "https://geceninlordu.com/", "tr", overrideVersionCode = 1),
        SingleLang("A Pair of 2+", "https://pairof2.com", "en", className = "APairOf2"),
        SingleLang("PMScans", "https://reader.pmscans.com", "en"),
        SingleLang("Skull Scans", "https://www.skullscans.com", "en"),
        SingleLang("Luminous Scans", "https://www.luminousscans.com", "en", overrideVersionCode = 1),
        SingleLang("Azure Scans", "https://azuremanga.com", "en"),
        SingleLang("ReaperScans.fr (GS)", "https://reaperscans.fr", "fr", className = "ReaperScansFR", pkgName = "gsnation", overrideVersionCode = 2),
        SingleLang("YugenMangas", "https://yugenmangas.com", "es"),
        SingleLang("DragonTranslation", "https://dragontranslation.com", "es", isNsfw = true, overrideVersionCode = 2),
        SingleLang("Patatescans", "https://patatescans.com", "fr", isNsfw = true, overrideVersionCode = 1),
        SingleLang("Fusion Scanlation", "https://fusionscanlation.com", "es", className = "FusionScanlation", overrideVersionCode = 1),
        SingleLang("Ace Scans", "https://acescans.xyz", "en", isNsfw = true, overrideVersionCode = 1),
        SingleLang("Silence Scan", "https://silencescan.com.br", "pt-BR", isNsfw = true, overrideVersionCode = 5),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            WPMangaReaderGenerator().createAll()
        }
    }
}
