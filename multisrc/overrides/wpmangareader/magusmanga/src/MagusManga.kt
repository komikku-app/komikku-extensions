package eu.kanade.tachiyomi.extension.ar.magusmanga

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import java.text.SimpleDateFormat
import java.util.Locale

class MagusManga : WPMangaReader("Magus Manga", "https://magusmanga.com", "ar", dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("ar")))
