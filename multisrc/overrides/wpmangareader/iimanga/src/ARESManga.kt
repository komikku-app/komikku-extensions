package eu.kanade.tachiyomi.extension.ar.iimanga

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import java.text.SimpleDateFormat
import java.util.Locale

class ARESManga : WPMangaReader("ARESManga", "https://aresmanga.com", "ar", dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("ar"))) {
    // The scanlator changed their name.
    override val id: Long = 230017529540228175
}
