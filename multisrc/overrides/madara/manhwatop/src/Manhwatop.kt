package eu.kanade.tachiyomi.extension.en.manhwatop

import eu.kanade.tachiyomi.multisrc.madara.Madara

class Manhwatop : Madara("Manhwatop", "https://manhwatop.com", "en") {

    // The website does not flag the content, so we just use the old selector.
    override fun popularMangaSelector() = "div.page-item-detail:not(:has(a[href*='bilibilicomics.com']))"
}
