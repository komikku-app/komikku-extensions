package eu.kanade.tachiyomi.extension.es.aiyumanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class AiYuManga : Madara(
    "AiYuManga",
    "https://aiyumangascanlation.com",
    "es",
    SimpleDateFormat("MM/dd/yyyy", Locale("es")),
) {
    override val useNewChapterEndpoint = true
    override val chapterUrlSuffix = ""

    override val mangaDetailsSelectorStatus = "div.post-content_item:contains(Status) > div.summary-content"
}
