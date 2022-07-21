package eu.kanade.tachiyomi.extension.en.mangarolls

import eu.kanade.tachiyomi.multisrc.madara.Madara

class MangaRolls : Madara("MangaRolls", "https://mangarolls.com", "en") {
    override val useNewChapterEndpoint = true
}
