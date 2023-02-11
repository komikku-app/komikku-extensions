package eu.kanade.tachiyomi.extension.en.mangakio

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MangaKio : Madara(
    "Manga Kio",
    "https://mangakio.com",
    "en",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US),
) {
    override val useNewChapterEndpoint = true
}
