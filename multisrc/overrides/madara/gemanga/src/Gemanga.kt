package eu.kanade.tachiyomi.extension.ar.gemanga

import eu.kanade.tachiyomi.multisrc.madara.Madara

class Gemanga : Madara("Gemanga", "https://gemanga.com", "ar") {

    // The website does not flag the content, so we just use the old selector.
    override fun popularMangaSelector() = "div.page-item-detail:not(:has(a[href*='bilibilicomics.com']))"
}
