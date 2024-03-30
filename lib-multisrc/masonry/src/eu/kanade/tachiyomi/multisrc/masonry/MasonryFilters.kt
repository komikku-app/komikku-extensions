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
    fun getUriPartIfNeeded(channel: String) =
        when (channel) {
            "updates" -> {
                when (selected) {
                    "trending" -> "" // Trending
                    else -> selected
                }
            }
            // tag & channel
            else -> {
                when (selected) {
                    "popular" -> "" // Popular
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

class SearchTypeFilter(options: List<Pair<String, String>>) : SelectFilter("Browse & Search for", options)

class Tag(val name: String, val uriPart: String)

class TagCheckBox(name: String, val uriPart: String) : Filter.CheckBox(name)

class TagsFilter(tags: List<Tag>) :
    Filter.Group<TagCheckBox>("Tags", tags.map { TagCheckBox(it.name, it.uriPart) })

class ModelTagsFilter(tags: List<Tag>) :
    Filter.Group<TagCheckBox>("Model Tags", tags.map { TagCheckBox(it.name, it.uriPart) })
