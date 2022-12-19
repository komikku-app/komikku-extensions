package eu.kanade.tachiyomi.multisrc.mdb

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MDBGenerator : ThemeSourceGenerator {
    override val themeClass = "MDB"
    override val themePkg = "mdb"
    override val baseVersionCode = 2
    override val sources = listOf(
        SingleLang("ManhuaDB", "https://www.manhuadb.com", "zh", sourceName = "漫画DB", overrideVersionCode = 4),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MDBGenerator().createAll()
        }
    }
}
