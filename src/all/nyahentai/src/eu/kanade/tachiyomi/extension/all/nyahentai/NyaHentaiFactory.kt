package eu.kanade.tachiyomi.extension.all.nyahentai

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

class NyaHentaiFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        NyaHentaiEN(),
        NyaHentaiJA(),
        NyaHentaiZH(),
        NyaHentaiALL(),
    )
}

class NyaHentaiEN : NyaHentai("en") {
    override val id = 9170089554867447899
}
class NyaHentaiJA : NyaHentai("ja") {
    override val id = 770924441007081408
}
class NyaHentaiZH : NyaHentai("zh") {
    override val id = 6864783742637475493
}
class NyaHentaiALL : NyaHentai("all") {
    override val id = 7651943104270359588
}
