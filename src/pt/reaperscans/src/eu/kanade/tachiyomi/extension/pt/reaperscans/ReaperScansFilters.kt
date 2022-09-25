package eu.kanade.tachiyomi.extension.pt.reaperscans

import eu.kanade.tachiyomi.source.model.Filter

open class EnhancedSelect<T>(name: String, values: Array<T>) : Filter.Select<T>(name, values) {
    val selected: T
        get() = values[state]
}

data class Status(val name: String, val value: String) {
    override fun toString(): String = name
}

class StatusFilter(statuses: List<Status>) : EnhancedSelect<Status>(
    "Status",
    statuses.toTypedArray()
)

data class SortProperty(val name: String, val value: String) {
    override fun toString(): String = name
}

class SortByFilter(private val sortProperties: List<SortProperty>) : Filter.Sort(
    "Ordenar por",
    sortProperties.map { it.name }.toTypedArray(),
    Selection(1, ascending = false)
) {
    val selected: String
        get() = sortProperties[state!!.index].value
}
