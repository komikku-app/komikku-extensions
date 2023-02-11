package eu.kanade.tachiyomi.multisrc.mangasar

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MangaSarGenerator : ThemeSourceGenerator {

    override val themePkg = "mangasar"

    override val themeClass = "MangaSar"

    override val baseVersionCode: Int = 7

    override val sources = listOf(
        SingleLang("Fire Mangás", "https://firemangas.com", "pt-BR", className = "FireMangas"),
        SingleLang("Mangazim", "https://mangazim.com", "pt-BR"),
        SingleLang("MangásUp", "https://mangasup.net", "pt-BR", className = "MangasUp"),
        SingleLang("Seemangas", "https://seemangas.com", "pt-BR", isNsfw = true),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MangaSarGenerator().createAll()
        }
    }
}
