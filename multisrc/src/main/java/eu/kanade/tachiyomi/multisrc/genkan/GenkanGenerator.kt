package eu.kanade.tachiyomi.multisrc.genkan

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class GenkanGenerator : ThemeSourceGenerator {

    override val themePkg = "genkan"

    override val themeClass = "Genkan"

    override val baseVersionCode: Int = 3

    override val sources = listOf(
        SingleLang("Hunlight Scans", "https://hunlight-scans.info", "en"),
        SingleLang("LynxScans", "https://lynxscans.com", "en", overrideVersionCode = 3),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            GenkanGenerator().createAll()
        }
    }
}
