package eu.kanade.tachiyomi.extension.en.manhuaplus

import eu.kanade.tachiyomi.multisrc.madara.Madara

class ManhuaPlus : Madara("Manhua Plus", "https://manhuaplus.com", "en") {

    // The website is incorrectly flagging a lot of their
    // manga content as video instead. To bypass this, we
    // use the old selector that includes all.
    override fun popularMangaSelector() = "div.page-item-detail:not(:has(a[href*='bilibilicomics.com']))"

    override val pageListParseSelector = ".read-container img"
}
