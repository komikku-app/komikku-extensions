package eu.kanade.tachiyomi.extension.pt.saikaiscan

import eu.kanade.tachiyomi.source.model.Filter

class Genre(title: String, val id: Int) : Filter.CheckBox(title)

class GenreFilter(genres: List<Genre>) : Filter.Group<Genre>("Gêneros", genres)

data class Country(val name: String, val id: Int) {
    override fun toString(): String = name
}

open class EnhancedSelect<T>(name: String, values: Array<T>) : Filter.Select<T>(name, values) {
    val selected: T
        get() = values[state]
}

class CountryFilter(countries: List<Country>) : EnhancedSelect<Country>(
    "Nacionalidade",
    countries.toTypedArray()
)

data class Status(val name: String, val id: Int) {
    override fun toString(): String = name
}

class StatusFilter(statuses: List<Status>) : EnhancedSelect<Status>(
    "Status",
    statuses.toTypedArray()
)

data class SortProperty(val name: String, val slug: String) {
    override fun toString(): String = name
}

class SortByFilter(val sortProperties: List<SortProperty>) : Filter.Sort(
    "Ordenar por",
    sortProperties.map { it.name }.toTypedArray(),
    Selection(2, ascending = false)
)
