package eu.kanade.tachiyomi.extension.en.allanime

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList

internal class Genre(name: String) : Filter.TriState(name)

internal class CountryFilter(name: String, private val countries: List<Pair<String, String>>) :
    Filter.Select<String>(name, countries.map { it.first }.toTypedArray()) {
    fun getValue() = countries[state].second
}

internal class GenreFilter(title: String, genres: List<Genre>) :
    Filter.Group<Genre>(title, genres)

private val genreList: List<Genre> = listOf(
    Genre("4 Koma"),
    Genre("Action"),
    Genre("Adult"),
    Genre("Adventure"),
    Genre("Cars"),
    Genre("Comedy"),
    Genre("Cooking"),
    Genre("Crossdressing"),
    Genre("Dementia"),
    Genre("Demons"),
    Genre("Doujinshi"),
    Genre("Drama"),
    Genre("Ecchi"),
    Genre("Fantasy"),
    Genre("Game"),
    Genre("Gender Bender"),
    Genre("Gyaru"),
    Genre("Harem"),
    Genre("Historical"),
    Genre("Horror"),
    Genre("Isekai"),
    Genre("Josei"),
    Genre("Kids"),
    Genre("Loli"),
    Genre("Magic"),
    Genre("Manhua"),
    Genre("Manhwa"),
    Genre("Martial Arts"),
    Genre("Mature"),
    Genre("Mecha"),
    Genre("Medical"),
    Genre("Military"),
    Genre("Monster Girls"),
    Genre("Music"),
    Genre("Mystery"),
    Genre("One Shot"),
    Genre("Parody"),
    Genre("Police"),
    Genre("Post Apocalyptic"),
    Genre("Psychological"),
    Genre("Reincarnation"),
    Genre("Reverse Harem"),
    Genre("Romance"),
    Genre("Samurai"),
    Genre("School"),
    Genre("Sci-Fi"),
    Genre("Seinen"),
    Genre("Shota"),
    Genre("Shoujo"),
    Genre("Shoujo Ai"),
    Genre("Shounen"),
    Genre("Shounen Ai"),
    Genre("Slice of Life"),
    Genre("Smut"),
    Genre("Space"),
    Genre("Sports"),
    Genre("Super Power"),
    Genre("Supernatural"),
    Genre("Suspense"),
    Genre("Thriller"),
    Genre("Tragedy"),
    Genre("Unknown"),
    Genre("Vampire"),
    Genre("Webtoons"),
    Genre("Yaoi"),
    Genre("Youkai"),
    Genre("Yuri"),
    Genre("Zombies"),
)

private val countryList: List<Pair<String, String>> = listOf(
    Pair("All", "ALL"),
    Pair("Japan", "JP"),
    Pair("China", "CN"),
    Pair("Korea", "KR"),
)

val filters = FilterList(
    CountryFilter("Countries", countryList),
    GenreFilter("Genres", genreList),
)
