package eu.kanade.tachiyomi.extension.es.datgarscanlation

import eu.kanade.tachiyomi.multisrc.zeistmanga.ZeistManga

class DatGarScanlation : ZeistManga("DatGarScanlation", "https://datgarscanlation.blogspot.com", "es") {

    override val useNewChapterFeed = true
    override val hasFilters = true
    override val hasLanguageFilter = false
}
