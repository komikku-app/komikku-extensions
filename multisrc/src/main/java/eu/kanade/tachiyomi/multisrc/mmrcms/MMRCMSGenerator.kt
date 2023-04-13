package eu.kanade.tachiyomi.multisrc.mmrcms

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MMRCMSGenerator : ThemeSourceGenerator {

    override val themePkg = "mmrcms"

    override val themeClass = "MMRCMS"

    override val baseVersionCode = 5

    override val sources = listOf(
        SingleLang("مانجا اون لاين", "https://onma.me", "ar", className = "onma"),
        SingleLang("Read Comics Online", "https://readcomicsonline.ru", "en"),
        SingleLang("Fallen Angels", "https://manga.fascans.com", "en", overrideVersionCode = 2),
        SingleLang("Zahard", "https://zahard.xyz", "en", overrideVersionCode = 2),
        SingleLang("Scan FR", "https://www.scan-fr.org", "fr", overrideVersionCode = 2),
        SingleLang("Scan VF", "https://www.scan-vf.net", "fr", overrideVersionCode = 1),
        SingleLang("Scan OP", "https://scan-op.cc", "fr"),
        SingleLang("Komikid", "https://www.komikid.com", "id"),
        SingleLang("Fallen Angels Scans", "https://truyen.fascans.com", "vi"),
        SingleLang("LeoManga", "https://leomanga.me", "es", overrideVersionCode = 1),
        SingleLang("submanga", "https://submanga.io", "es"),
        SingleLang("Mangadoor", "https://mangadoor.com", "es", overrideVersionCode = 1),
        SingleLang("Utsukushii", "https://manga.utsukushii-bg.com", "bg", overrideVersionCode = 1),
        SingleLang("Phoenix-Scans", "https://phoenix-scans.pl", "pl", className = "PhoenixScans", overrideVersionCode = 1),
        SingleLang("Scan-1", "https://wwv.scan-1.com", "fr", className = "ScanOne", overrideVersionCode = 2),
        SingleLang("Lelscan-VF", "https://lelscanvf.com", "fr", className = "LelscanVF", overrideVersionCode = 1),
        SingleLang("AnimaRegia", "https://animaregia.net", "pt-BR", overrideVersionCode = 4),
        SingleLang("MangaID", "https://mangaid.click", "id", overrideVersionCode = 1),
        SingleLang("Jpmangas", "https://jpmangas.cc", "fr", overrideVersionCode = 1),
        SingleLang("Op-VF", "https://www.op-vf.com", "fr", className = "OpVF"),
        SingleLang("FR Scan", "https://frscan.ws", "fr", overrideVersionCode = 2),
        SingleLang("Ama Scans", "https://amascan.com", "pt-BR", isNsfw = true, overrideVersionCode = 2),
        // NOTE: THIS SOURCE CONTAINS A CUSTOM LANGUAGE SYSTEM (which will be ignored)!
        SingleLang("HentaiShark", "https://www.hentaishark.com", "all", isNsfw = true),
    )

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            MMRCMSGenerator().createAll()
        }
    }
}
