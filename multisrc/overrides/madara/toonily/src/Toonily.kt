package eu.kanade.tachiyomi.extension.en.toonily

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class Toonily : Madara(
    "Toonily",
    "https://toonily.com",
    "en",
    SimpleDateFormat("MMM d, yy", Locale.US)
) {

    // The source customized the Madara theme and broke the filter.
    override val filterNonMangaItems = false

    override val useNewChapterEndpoint: Boolean = true
}
