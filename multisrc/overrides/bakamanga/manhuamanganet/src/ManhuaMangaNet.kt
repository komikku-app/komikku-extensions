package eu.kanade.tachiyomi.extension.en.manhuamanganet

import eu.kanade.tachiyomi.multisrc.bakamanga.BakaManga

class ManhuaMangaNet : BakaManga(
    "ManhuaManga.net",
    "https://manhuamanga.net",
    "en",
) {
    override fun getGenreList() = arrayOf(
        Pair("All", ""),
        Pair("Action", "action"),
        Pair("Adventure", "adventure"),
        Pair("Based on a Novel", "based-on-a-novel"),
        Pair("Comedy", "comedy"),
        Pair("Comic", "comic"),
        Pair("Cooking", "cooking"),
        Pair("Drama", "drama"),
        Pair("Ecchi", "ecchi"),
        Pair("Fantasy", "fantasy"),
        Pair("Harem", "harem"),
        Pair("Historical", "historical"),
        Pair("Horror", "horror"),
        Pair("Isekai", "isekai"),
        Pair("Josei", "josei"),
        Pair("Magic", "magic"),
        Pair("Manhua", "manhua"),
        Pair("Manhwa", "manhwa"),
        Pair("Martial Arts", "martial-arts"),
        Pair("Mecha", "mecha"),
        Pair("Medical", "medical"),
        Pair("Mystery", "mystery"),
        Pair("Psychological", "psychological"),
        Pair("Reincarnation", "reincarnation"),
        Pair("Romance", "romance"),
        Pair("RPG", "rpg"),
        Pair("School Life", "school-life"),
        Pair("Sci-fi", "sci-fi"),
        Pair("Seinen", "seinen"),
        Pair("Shoujo", "shoujo"),
        Pair("Shounen", "shounen"),
        Pair("Slice of Life", "slice-of-life"),
        Pair("Supernatural", "supernatural"),
        Pair("Tragedy", "tragedy"),
        Pair("Webtoon", "webtoon"),
        Pair("Zombie", "zombie"),
    )
}
