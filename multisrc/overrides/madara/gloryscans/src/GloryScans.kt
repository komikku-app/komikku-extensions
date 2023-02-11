package eu.kanade.tachiyomi.extension.tr.gloryscans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class GloryScans : Madara(
    "Glory Scans",
    "https://gloryscans.com",
    "tr",
    dateFormat = SimpleDateFormat("d MMM yyy", Locale("tr")),
) {
    override val useNewChapterEndpoint = false
}
