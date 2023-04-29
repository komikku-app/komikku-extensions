package eu.kanade.tachiyomi.extension.es.bokugentranslation

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class BokugenTranslation : Madara(
    "BokugenTranslation",
    "https://bokugents.com",
    "es",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es")),
) {
    override val useNewChapterEndpoint = true
}
