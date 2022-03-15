package eu.kanade.tachiyomi.extension.en.zinmanga

import eu.kanade.tachiyomi.multisrc.madara.Madara

class Zinmanga : Madara("Zinmanga", "https://zinmanga.com", "en") {

    // The website does not flag the content, so we just use the old selector.
    override fun popularMangaSelector() = "div.page-item-detail:not(:has(a[href*='bilibilicomics.com']))"
}
