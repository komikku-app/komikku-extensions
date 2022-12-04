package eu.kanade.tachiyomi.extension.en.trapscans

import eu.kanade.tachiyomi.multisrc.madara.Madara

class TrapScans : Madara("Trap Scans", "https://trapscans.com", "en") {

    override val mangaDetailsSelectorDescription = ".description-summary p"
}
