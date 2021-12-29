package eu.kanade.tachiyomi.extension.en.manytoon

import eu.kanade.tachiyomi.multisrc.madara.Madara

class ManyToon : Madara("ManyToon", "https://manytoon.com", "en") {

    override val useNewChapterEndpoint: Boolean = true
}
