package eu.kanade.tachiyomi.multisrc.genkan

import generator.ThemeSourceData
import generator.ThemeSourceGenerator

class GenkanOriginalGenerator : ThemeSourceGenerator {

    override val themePkg = "genkan"

    override val themeClass = "GenkanOriginal"

    override val baseVersionCode: Int = 1

    override val sources = emptyList<ThemeSourceData>()

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            GenkanOriginalGenerator().createAll()
        }
    }
}
