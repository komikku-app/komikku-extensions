package eu.kanade.tachiyomi.multisrc.libgroup

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class LibGenerator : ThemeSourceGenerator {

    override val themePkg = "libgroup"

    override val themeClass = "LibGroup"

    override val baseVersionCode: Int = 11

    override val sources = listOf(
        SingleLang("MangaLib", "https://mangalib.me", "ru", overrideVersionCode = 74),
        SingleLang("HentaiLib", "https://hentailib.me", "ru", isNsfw = true, overrideVersionCode = 19),
        SingleLang("YaoiLib", "https://yaoilib.me", "ru", isNsfw = true, overrideVersionCode = 1)
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LibGenerator().createAll()
        }
    }
}
