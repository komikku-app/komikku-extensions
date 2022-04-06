package eu.kanade.tachiyomi.extension.ar.gemanga

import eu.kanade.tachiyomi.multisrc.madara.Madara

class Gemanga : Madara("Gemanga", "https://gemanga.com", "ar") {

    // The website does not flag the content.
    override val useLoadMoreSearch = false
    override val filterNonMangaItems = false
}
