package eu.kanade.tachiyomi.extension.es.inarimanga

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import java.text.SimpleDateFormat
import java.util.Locale

class InariManga : WPMangaReader(
    "InariManga",
    "https://inarimanga.com",
    "es",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es"))
)
