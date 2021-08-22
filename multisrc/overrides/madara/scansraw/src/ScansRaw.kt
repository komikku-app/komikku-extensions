package eu.kanade.tachiyomi.extension.en.scansraw

import eu.kanade.tachiyomi.multisrc.madara.Madara

class ScansRaw : Madara("Scans Raw", "https://scansraw.com", "en") {
    override val useNewChapterEndpoint: Boolean = true
}
