package eu.kanade.tachiyomi.extension.tr.evascans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class EvaScans : Madara(
    "EvaScans",
    "https://evascans.com",
    "tr",
    dateFormat = SimpleDateFormat("MMM d, yyy", Locale("tr")),
) {
    override val useNewChapterEndpoint = false
}
