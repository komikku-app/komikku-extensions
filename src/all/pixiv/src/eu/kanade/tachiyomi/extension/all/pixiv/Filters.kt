package eu.kanade.tachiyomi.extension.all.pixiv
import eu.kanade.tachiyomi.source.model.Filter

internal class FilterType : Filter.Select<String>("Type", values, 2) {
    companion object {
        val keys = arrayOf("all", "illust", "manga")
        val values = arrayOf("All", "Illustrations", "Manga")
    }

    val value: String get() = keys[state]
}

internal class FilterRating : Filter.Select<String>("Rating", values, 0) {
    companion object {
        val keys = arrayOf("all", "safe", "r18")
        val values = arrayOf("All", "All ages", "R-18")
    }

    val value: String get() = keys[state]
}

internal class FilterSearchMode : Filter.Select<String>("Mode", values, 1) {
    companion object {
        val keys = arrayOf("s_tag", "s_tag_full", "s_tc")
        val values = arrayOf("Tags (partial)", "Tags (full)", "Title, description")
    }

    val value: String get() = keys[state]
}

internal class FilterOrder : Filter.Sort("Order", arrayOf("Date posted")) {
    val value: String get() = if (state?.ascending == true) "date" else "date_d"
}

internal class FilterDateBefore : Filter.Text("Posted before") {
    val value: String? get() = state.ifEmpty { null }
}

internal class FilterDateAfter : Filter.Text("Posted after") {
    val value: String? get() = state.ifEmpty { null }
}
