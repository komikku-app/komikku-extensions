package eu.kanade.tachiyomi.extension.id.manhwaindo

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import java.text.SimpleDateFormat
import java.util.Locale

class ManhwaIndo : WPMangaReader(
    "Manhwa Indo", "https://manhwaindo.id", "id",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("id"))
) {

    override val hasProjectPage = true
}
