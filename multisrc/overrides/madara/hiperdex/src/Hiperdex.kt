package eu.kanade.tachiyomi.extension.en.hiperdex

import eu.kanade.tachiyomi.multisrc.madara.Madara

class Hiperdex : Madara("Hiperdex", "https://1sthiperdex.com", "en") {
    override val useNewChapterEndpoint: Boolean = true
}
