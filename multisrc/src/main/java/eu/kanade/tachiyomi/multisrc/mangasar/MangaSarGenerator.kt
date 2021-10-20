package eu.kanade.tachiyomi.multisrc.mangasar

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MangaSarGenerator : ThemeSourceGenerator {

    override val themePkg = "mangasar"

    override val themeClass = "MangaSar"

    override val baseVersionCode: Int = 5

    override val sources = listOf(
        SingleLang("MangaTube", "https://mangatube.site", "pt-BR"),
        SingleLang("Meus Mangás", "https://meusmangas.net", "pt-BR", isNsfw = true, className = "MeusMangas", overrideVersionCode = 1)
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MangaSarGenerator().createAll()
        }
    }
}
