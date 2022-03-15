package eu.kanade.tachiyomi.extension.ar.egymanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale

class EGYManga : Madara(
    "EGY Manga",
    "https://egymanga.net",
    "ar",
    SimpleDateFormat("MMMM dd, yyyy", Locale("ar"))
) {

    override val pageListParseSelector = "div.separator"

    // The website does not flag the content, so we just use the old selector.
    override fun popularMangaSelector() =
        "div.page-item-detail:not(:has(a[href*='bilibilicomics.com']))"

    override fun chapterListParse(response: Response): List<SChapter> =
        super.chapterListParse(response).reversed()
}
