package eu.kanade.tachiyomi.extension.all.bilibili

import eu.kanade.tachiyomi.source.model.Filter

data class BilibiliTag(val name: String, val id: Int) {
    override fun toString(): String = name
}

class GenreFilter(label: String, genres: Array<BilibiliTag>) :
    Filter.Select<BilibiliTag>(label, genres) {
    val selected: BilibiliTag
        get() = values[state]
}

class AreaFilter(label: String, genres: Array<BilibiliTag>) :
    Filter.Select<BilibiliTag>(label, genres) {
    val selected: BilibiliTag
        get() = values[state]
}

class SortFilter(label: String, options: Array<String>, state: Int = 0) :
    Filter.Select<String>(label, options, state)

class StatusFilter(label: String, statuses: Array<String>) :
    Filter.Select<String>(label, statuses)

class PriceFilter(label: String, prices: Array<String>) :
    Filter.Select<String>(label, prices)
