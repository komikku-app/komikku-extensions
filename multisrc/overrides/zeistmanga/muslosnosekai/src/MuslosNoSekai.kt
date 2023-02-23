package eu.kanade.tachiyomi.extension.es.muslosnosekai

import eu.kanade.tachiyomi.multisrc.zeistmanga.Language
import eu.kanade.tachiyomi.multisrc.zeistmanga.ZeistManga

class MuslosNoSekai : ZeistManga("Muslos No Sekai", "https://muslosnosekai.blogspot.com", "es") {

    override val hasFilters = true

    override fun getLanguageList(): List<Language> = listOf(
        Language(intl.all, ""),
    )
}
