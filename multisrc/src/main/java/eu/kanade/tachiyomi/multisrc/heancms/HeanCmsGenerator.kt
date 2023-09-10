package eu.kanade.tachiyomi.multisrc.heancms

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class HeanCmsGenerator : ThemeSourceGenerator {

    override val themePkg = "heancms"

    override val themeClass = "HeanCms"

    override val baseVersionCode: Int = 18

    override val sources = listOf(
        SingleLang("Glorious Scan", "https://gloriousscan.com", "pt-BR", overrideVersionCode = 17),
        SingleLang("Omega Scans", "https://omegascans.org", "en", isNsfw = true, overrideVersionCode = 17),
        SingleLang("Perf Scan", "https://perf-scan.fr", "fr"),
        SingleLang("Reaper Scans", "https://reaperscans.net", "pt-BR", overrideVersionCode = 36),
        SingleLang("YugenMangas", "https://yugenmangas.net", "es", isNsfw = true, overrideVersionCode = 8),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            HeanCmsGenerator().createAll()
        }
    }
}
