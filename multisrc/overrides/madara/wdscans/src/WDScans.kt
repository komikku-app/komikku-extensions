package eu.kanade.tachiyomi.extension.en.wdscans

import eu.kanade.tachiyomi.multisrc.madara.Madara

class WDScans : Madara("WD Scans (Wicked Dragon Scans)", "https://wdscans.com", "en") {
    override val useNewChapterEndpoint = true
}
