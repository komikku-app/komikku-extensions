package eu.kanade.tachiyomi.extension.en.shimadascans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class ShimadaScans : Madara("Shimada Scans", "https://shimadascans.com", "en", dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)) {
    override val versionId = 2
}
