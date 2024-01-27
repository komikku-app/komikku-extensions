package eu.kanade.tachiyomi.multisrc.mangamainac

import generator.ThemeSourceData
import generator.ThemeSourceGenerator

class MangaMainacGenerator : ThemeSourceGenerator {

    override val themePkg = "mangamainac"

    override val themeClass = "MangaMainac"

    override val baseVersionCode: Int = 1

    override val sources = listOf<ThemeSourceData>()

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // MangaMainacGenerator().createAll()
        }
    }
}
