package eu.kanade.tachiyomi.extension.en.immortalupdates

import eu.kanade.tachiyomi.multisrc.madara.Madara

class ImmortalUpdates : Madara("Immortal Updates", "https://immortalupdates.com", "en") {
    override val useNewChapterEndpoint: Boolean = true
}
