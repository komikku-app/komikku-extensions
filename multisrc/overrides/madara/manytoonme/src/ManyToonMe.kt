package eu.kanade.tachiyomi.extension.en.manytoonme

import eu.kanade.tachiyomi.multisrc.madara.Madara

class ManyToonMe : Madara("ManyToon.me", "https://manytoon.me", "en") {

    override val useNewChapterEndpoint: Boolean = true

    // The website does not flag the content.
    override val filterNonMangaItems = false
}
