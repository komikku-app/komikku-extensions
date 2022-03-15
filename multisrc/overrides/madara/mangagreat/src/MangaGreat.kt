package eu.kanade.tachiyomi.extension.en.mangagreat

import eu.kanade.tachiyomi.multisrc.madara.Madara

class MangaGreat : Madara("MangaGreat", "https://mangagreat.com", "en") {

    // The website does not flag the content, so we just use the old selector.
    override fun popularMangaSelector() = "div.page-item-detail:not(:has(a[href*='bilibilicomics.com']))"
}
