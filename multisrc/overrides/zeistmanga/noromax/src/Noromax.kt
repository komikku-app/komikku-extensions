package eu.kanade.tachiyomi.extension.id.noromax

import eu.kanade.tachiyomi.multisrc.zeistmanga.ZeistManga

class Noromax : ZeistManga("Noromax", "https://www.noromax.xyz", "id") {

    override val hasFilters = true

    override val imgSelector = "a[href]"
    override val imgSelectorAttr = "href"
}
