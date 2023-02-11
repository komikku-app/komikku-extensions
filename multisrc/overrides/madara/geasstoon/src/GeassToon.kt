package eu.kanade.tachiyomi.extension.tr.geasstoon

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class GeassToon : Madara(
    "GeassToon",
    "https://geasstoon.com",
    "tr",
    dateFormat = SimpleDateFormat("MMM d, yyy", Locale("tr")),
) {
    override val useNewChapterEndpoint = true
}
