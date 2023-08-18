package eu.kanade.tachiyomi.extension.ar.potatomanga

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class PotatoManga : MangaThemesia(
    "PotatoManga",
    "https://potatomanga.xyz",
    "ar",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("ar")),
) {
    override val seriesArtistSelector =
        ".infotable tr:contains(الرسام) td:last-child, ${super.seriesArtistSelector}"
    override val seriesAuthorSelector =
        ".infotable tr:contains(المؤلف) td:last-child, ${super.seriesAuthorSelector}"
    override val seriesStatusSelector =
        ".infotable tr:contains(الحالة) td:last-child, ${super.seriesStatusSelector}"
    override val seriesTypeSelector =
        ".infotable tr:contains(النوع) td:last-child, ${super.seriesTypeSelector}"
    override val seriesAltNameSelector =
        ".infotable tr:contains(الأسماء الثانوية) td:last-child, ${super.seriesAltNameSelector}"
}
