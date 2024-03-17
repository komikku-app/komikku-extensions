package eu.kanade.tachiyomi.multisrc.masonry

import eu.kanade.tachiyomi.source.model.Filter

abstract class SelectFilter(
    name: String,
    private val options: List<Pair<String, String>>,
) : Filter.Select<String>(
    name,
    options.map { it.first }.toTypedArray(),
) {
    val selected get() = options[state].second
}

class SortFilter : SelectFilter("Sort by", sortFilterOptions) {
    fun getUriPartIfNeeded(part: String) =
        when (part) {
            "updates" -> {
                when (state) {
                    0 -> "" // Trending
                    else -> selected
                }
            }
            // tag & channel
            else -> {
                when (state) {
                    2 -> "" // Popular
                    else -> selected
                }
            }
        }
}

private val sortFilterOptions = listOf(
    Pair("Trending", "trending"),
    Pair("Newest", "newest"),
    Pair("Popular", "popular"),
)

class SearchTypeFilter(options: List<Pair<String, String>>) : SelectFilter("Search query for", options)

class Tag(val name: String, val uriPart: String)

class TagCheckBox(name: String, val uriPart: String) : Filter.CheckBox(name)

class TagsFilter(tags: List<Tag>) :
    Filter.Group<TagCheckBox>("Tags", tags.map { TagCheckBox(it.name, it.uriPart) })
