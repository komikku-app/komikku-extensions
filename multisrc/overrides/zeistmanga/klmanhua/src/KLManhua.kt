package eu.kanade.tachiyomi.extension.id.klmanhua

import eu.kanade.tachiyomi.multisrc.zeistmanga.Language
import eu.kanade.tachiyomi.multisrc.zeistmanga.ZeistManga

class KLManhua : ZeistManga("KLManhua", "https://klmanhua.blogspot.com", "id") {

    override val hasFilters = true

    override fun getLanguageList(): List<Language> = listOf(
        Language(intl.all, ""),
    )
}
