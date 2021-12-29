package eu.kanade.tachiyomi.extension.en.hiperdex

import eu.kanade.tachiyomi.multisrc.madara.Madara

class Hiperdex : Madara("Hiperdex", "https://hiperdex.com", "en") {
    override val useNewChapterEndpoint: Boolean = true
}
