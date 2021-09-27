package eu.kanade.tachiyomi.extension.en.imperfectcomics

import eu.kanade.tachiyomi.multisrc.madara.Madara

class ImperfectComics : Madara("Imperfect Comics", "https://imperfectcomic.com", "en") {
    override val useNewChapterEndpoint: Boolean = true
}
