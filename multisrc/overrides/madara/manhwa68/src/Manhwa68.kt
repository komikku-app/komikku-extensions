package eu.kanade.tachiyomi.extension.en.manhwa68

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class Manhwa68 : Madara(
    "Manhwa68",
    "https://manhwa68.com",
    "en",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
) {

    // The website does not flag the content, so we just use the old selector.
    override fun popularMangaSelector() = "div.page-item-detail:not(:has(a[href*='bilibilicomics.com']))"
}
