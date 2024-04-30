package eu.kanade.tachiyomi.extension.all.misskon

import eu.kanade.tachiyomi.source.model.Filter

class TopDays(val name: String, val uri: String)

class TopDaysFilter(displayName: String, private val days: Array<TopDays>) :
    Filter.Select<String>(displayName, days.map { it.name }.toTypedArray()) {
    fun toUriPart() = days[state].uri
}

fun getTopDaysList() = arrayOf(
    TopDays("<Select>", ""),
) + topDaysList()

fun topDaysList() = arrayOf(
    TopDays("Top 3 days", "top3"),
    TopDays("Top week", "top7"),
    TopDays("Top month", "top30"),
    TopDays("Top 2 months", "top60"),
)

class TagsFilter(displayName: String, private val tags: List<Pair<String, String>>) :
    Filter.Select<String>(displayName, tags.map { it.first }.toTypedArray()) {
    fun toUriPart() = tags[state].second
}
