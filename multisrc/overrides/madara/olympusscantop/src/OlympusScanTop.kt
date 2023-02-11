package eu.kanade.tachiyomi.extension.es.olympusscantop

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class OlympusScanTop : Madara(
    "OlympusScan.top",
    "https://olympusscan.top",
    "es",
    dateFormat = SimpleDateFormat("MMM d, yyy", Locale("es")),
) {
    override val useNewChapterEndpoint = true
}
