package eu.kanade.tachiyomi.extension.es.vermanhwas

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class VerManhwas : Madara(
    "Ver Manhwas",
    "https://vermanhwa.com",
    "es",
    dateFormat = SimpleDateFormat("MMM d, yyy", Locale("es")),
) {
    override val useNewChapterEndpoint = true
}
