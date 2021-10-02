package eu.kanade.tachiyomi.extension.all.simplyhentai

import eu.kanade.tachiyomi.annotations.Nsfw
import eu.kanade.tachiyomi.source.SourceFactory
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi @Nsfw
class SimplyHentaiFactory : SourceFactory {
    override fun createSources() = listOf(
        SimplyHentai("en"),
        SimplyHentai("ja"),
        SimplyHentai("zh"),
        SimplyHentai("ko"),
        SimplyHentai("es"),
        SimplyHentai("ru"),
        SimplyHentai("fr"),
        SimplyHentai("de"),
        object : SimplyHentai("pt-BR") {
            // The site uses a Portugal flag for the language,
            // but the contents are in Brazilian Portuguese.
            override val id = 23032005200449651
        },
        SimplyHentai("it"),
        SimplyHentai("pl"),
    )
}
