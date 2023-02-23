package eu.kanade.tachiyomi.extension.id.shiyurasub

import eu.kanade.tachiyomi.multisrc.zeistmanga.Language
import eu.kanade.tachiyomi.multisrc.zeistmanga.ZeistManga

class ShiyuraSub : ZeistManga("ShiyuraSub", "https://shiyurasub.blogspot.com", "id") {

    override val hasFilters = true

    override val imgSelector = "a[href]"
    override val imgSelectorAttr = "href"

    override fun getLanguageList(): List<Language> = listOf(
        Language(intl.all, ""),
    )
}
