package eu.kanade.tachiyomi.multisrc.gattsu

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class GattsuGenerator : ThemeSourceGenerator {

    override val themePkg = "gattsu"

    override val themeClass = "Gattsu"

    override val baseVersionCode: Int = 1

    override val sources = listOf(
        SingleLang("Hentai Kai", "https://hentaikai.com", "pt-BR", isNsfw = true),
        SingleLang("Hentai Season", "https://hentaiseason.com", "pt-BR", isNsfw = true),
        SingleLang("Universo Hentai", "https://universohentai.com", "pt-BR", isNsfw = true)
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            GattsuGenerator().createAll()
        }
    }
}
