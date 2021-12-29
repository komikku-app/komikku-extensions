package eu.kanade.tachiyomi.extension.en.toonily

import eu.kanade.tachiyomi.multisrc.madara.Madara

class Toonily : Madara("Toonily", "https://toonily.com", "en") {
    override val useNewChapterEndpoint: Boolean = true
}
