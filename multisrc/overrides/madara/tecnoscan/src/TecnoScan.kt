package eu.kanade.tachiyomi.extension.es.tecnoscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class TecnoScan : Madara(
    "Tecno Scan",
    "https://tecnoscann.com",
    "es",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es")),
) {
    // Site moved from MangaThemesia to Madara
    override val versionId = 2
    override val useNewChapterEndpoint = true
}
