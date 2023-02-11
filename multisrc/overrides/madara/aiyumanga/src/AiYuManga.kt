package eu.kanade.tachiyomi.extension.es.aiyumanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class AiYuManga : Madara(
    "AiYuManga",
    "https://aiyumangascanlation.com",
    "es",
    SimpleDateFormat("d 'de' MMM 'de' yyy", Locale("es")),
) {
    override val useNewChapterEndpoint = true
}
