package eu.kanade.tachiyomi.extension.all.mwmanhwa

import eu.kanade.tachiyomi.multisrc.bakamanga.BakaManga

class MWManhwa : BakaManga(
    "MWManhwa",
    "https://mwmanhwa.net",
    "all",
) {
    override fun getGenreList() = arrayOf(
        Pair("All", ""),
        Pair("Action", "action"),
        Pair("Comedy", "comedy"),
        Pair("Drama", "drama"),
        Pair("Ecchi", "ecchi"),
        Pair("Fantasy", "fantasy"),
        Pair("Gender Bender", "gender-bender"),
        Pair("Harem", "harem"),
        Pair("Mature", "mature"),
        Pair("Psychological", "psychological"),
        Pair("Raw", "adult"),
        Pair("Romance", "romance"),
        Pair("Seinen", "seinen"),
        Pair("Slice of Life", "slice-of-life"),
        Pair("Supernatural", "supernatural"),
    )
}
