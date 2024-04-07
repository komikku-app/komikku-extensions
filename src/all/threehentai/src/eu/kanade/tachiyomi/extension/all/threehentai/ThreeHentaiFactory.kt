package eu.kanade.tachiyomi.extension.all.threehentai

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

class ThreeHentaiFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        ThreeHentai("en", "english"),
        ThreeHentai("ja", "japanese"),
        ThreeHentai("zh", "chinese"),
        ThreeHentai("kr", "korean"),
        ThreeHentai("es", "spanish"),
        ThreeHentai("pt", "portuguese"),
        ThreeHentai("ru", "russian"),
        ThreeHentai("de", "german"),
        ThreeHentai("jv", "javanese"),
        ThreeHentai("fr", "french"),
        ThreeHentai("vi", "vietnamese"),
        ThreeHentai("tr", "turkish"),
        ThreeHentai("pl", "polish"),
        ThreeHentai("ar", "arabic"),
        ThreeHentai("uk", "ukrainian"),
        ThreeHentai("it", "italian"),
        ThreeHentai("my", "burmese"),
        ThreeHentai("th", "thai"),
        ThreeHentai("nl", "dutch"),
        ThreeHentai("id", "indonesian"),
        ThreeHentai("tl", "tagalog"),
        ThreeHentai("ceb", "cebuano"),
        ThreeHentai("hu", "hungarian"),
        ThreeHentai("cs", "czech"),
        ThreeHentai("bg", "bulgarian"),
        ThreeHentai("fi", "finnish"),
        ThreeHentai("la", "latin"),
        ThreeHentai("all", ""),
    )
}
