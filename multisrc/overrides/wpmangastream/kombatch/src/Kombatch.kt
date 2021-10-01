package eu.kanade.tachiyomi.extension.id.kombatch

import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import java.text.SimpleDateFormat
import java.util.Locale

class Kombatch : WPMangaStream("Kombatch", "https://kombatch.com", "id", SimpleDateFormat("MMMM dd, yyyy", Locale("id"))) {
    override val hasProjectPage = true
}
