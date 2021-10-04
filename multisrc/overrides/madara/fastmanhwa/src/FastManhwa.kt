package eu.kanade.tachiyomi.extension.en.fastmanhwa

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class FastManhwa : Madara("FastManhwa", "https://fastmanhwa.net", "en", dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)) {
    override val useNewChapterEndpoint = true
}
