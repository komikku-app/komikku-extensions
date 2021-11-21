package eu.kanade.tachiyomi.extension.en.mangachill

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MangaChill : Madara("Manga Chill", "https://mangachill.com", "en", SimpleDateFormat("dd/MM/yyyy", Locale.US)) {
    override val useNewChapterEndpoint: Boolean = true
}
