package eu.kanade.tachiyomi.extension.en.hentaiwebtoon

import eu.kanade.tachiyomi.multisrc.madara.Madara

class HentaiWebtoon : Madara("HentaiWebtoon", "https://hentaiwebtoon.com", "en") {

    // The website does not flag the content, so we just use the old selector.
    override fun popularMangaSelector() = "div.page-item-detail:not(:has(a[href*='bilibilicomics.com']))"
}
