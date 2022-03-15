package eu.kanade.tachiyomi.extension.en.manhuaus

import eu.kanade.tachiyomi.multisrc.madara.Madara

class ManhuaUS : Madara("ManhuaUS", "https://manhuaus.com", "en") {
    override val useNewChapterEndpoint: Boolean = true

    // The website is incorrectly flagging a lot of their
    // manga content as text instead. To bypass this, we
    // use the old selector that includes all.
    override fun popularMangaSelector() = "div.page-item-detail:not(:has(a[href*='bilibilicomics.com']))"
}
