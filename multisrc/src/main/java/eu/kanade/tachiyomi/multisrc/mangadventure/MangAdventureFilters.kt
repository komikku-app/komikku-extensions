package eu.kanade.tachiyomi.multisrc.mangadventure

import eu.kanade.tachiyomi.source.model.Filter

/** Filter representing the name of an author. */
internal class Author : Filter.Text("Author") {
    override fun toString() = state
}

/** Filter representing the name of an artist. */
internal class Artist : Filter.Text("Artist") {
    override fun toString() = state
}

/**
 * Filter representing the sort order.
 *
 * @param labels The site's sort order labels.
 */
internal class SortOrder(
    private val labels: Array<String>
) : Filter.Sort("Sort", values, null) {
    override fun toString() = when (state?.ascending) {
        null -> ""
        true -> labels[state!!.index]
        false -> "-" + labels[state!!.index]
    }

    companion object {
        /** The available sort order values. */
        private val values = arrayOf("title", "latest_upload", "chapter_count")
    }
}

/**
 * Filter representing the status of a manga.
 *
 * @param statuses The site's status names.
 */
internal class Status(
    statuses: Array<String>
) : Filter.Select<String>("Status", statuses) {
    override fun toString() = values[state]
}

/**
 * Filter representing a manga category.
 *
 * @param name The display name of the category.
 */
internal class Category(name: String) : Filter.TriState(name)

/**
 * Filter representing the [categories][Category] of a manga.
 *
 * @param categories The site's manga categories.
 */
internal class CategoryList(
    categories: List<String>
) : Filter.Group<Category>("Categories", categories.map(::Category)) {
    override fun toString() = state.filterNot { it.isIgnored() }
        .joinToString(",") { if (it.isIncluded()) it.name else "-" + it.name }
}
