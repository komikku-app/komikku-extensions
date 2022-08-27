package eu.kanade.tachiyomi.multisrc.mymangacms

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MyMangaCMSGenerator : ThemeSourceGenerator {

    override val themePkg = "mymangacms"

    override val themeClass = "MyMangaCMS"

    override val baseVersionCode: Int = 1

    override val sources = listOf(
        SingleLang(
            "TruyenTranhLH",
            "https://truyentranhlh.net",
            "vi",
            overrideVersionCode = 9
        ),
        SingleLang(
            "Phê Manga",
            "https://phemanga.net",
            "vi",
            true,
            "PheManga",
            "phemanga",
        ),
        SingleLang("LKDTT", "https://lkdtt.net", "vi", true, overrideVersionCode = 1)
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MyMangaCMSGenerator().createAll()
        }
    }
}
