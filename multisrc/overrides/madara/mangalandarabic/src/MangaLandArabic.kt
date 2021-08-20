package eu.kanade.tachiyomi.extension.ar.mangalandarabic

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MangaLandArabic : Madara("Manga Land Arabic", "https://mangalandarabic.com", "ar", SimpleDateFormat("yyyy-MM-dd", Locale("ar"))) {
    override val useNewChapterEndpoint: Boolean = true
}
