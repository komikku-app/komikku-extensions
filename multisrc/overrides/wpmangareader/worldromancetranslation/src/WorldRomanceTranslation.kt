package eu.kanade.tachiyomi.extension.id.worldromancetranslation

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import java.text.SimpleDateFormat
import java.util.Locale

class WorldRomanceTranslation : WPMangaReader("World Romance Translation", "https://wrt.my.id", "id", "/komik", SimpleDateFormat("MMMM dd, yyyy", Locale("id"))) {
    override val projectPageString = "/project-wrt"

    override val hasProjectPage = true
}
