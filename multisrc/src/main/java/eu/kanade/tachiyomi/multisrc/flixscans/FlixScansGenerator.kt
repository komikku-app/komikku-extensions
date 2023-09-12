package eu.kanade.tachiyomi.multisrc.flixscans

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class FlixScansGenerator : ThemeSourceGenerator {

    override val themePkg = "flixscans"

    override val themeClass = "FlixScans"

    override val baseVersionCode: Int = 2

    override val sources = listOf(
        SingleLang("Flix Scans", "https://flixscans.net", "en", className = "FlixScansNet", pkgName = "flixscans"),
        SingleLang("جالاكسي مانجا", "https://flixscans.com", "ar", className = "GalaxyManga", overrideVersionCode = 25),
        SingleLang("مانجا نون", "https://manjanoon.com", "ar", className = "MangaNoon"),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            FlixScansGenerator().createAll()
        }
    }
}
