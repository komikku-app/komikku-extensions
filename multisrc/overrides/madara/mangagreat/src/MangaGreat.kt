package eu.kanade.tachiyomi.extension.en.mangagreat

import eu.kanade.tachiyomi.multisrc.madara.Madara

class MangaGreat : Madara("MangaGreat", "https://mangagreat.com", "en") {

    // The website does not flag the content.
    override val filterNonMangaItems = false
}
