package eu.kanade.tachiyomi.extension.all.pixiv
import eu.kanade.tachiyomi.source.model.Filter

internal class PixivFilters : MutableList<Filter<*>> by mutableListOf() {
    class Type : Filter.Select<String>("Type", values, 2) {
        companion object {
            private val values: Array<String> =
                arrayOf("All", "Illustrations", "Manga")

            private val searchParams: Array<String?> =
                arrayOf(null, "illust", "manga")
        }

        fun toSearchParameter(): String? = searchParams[state]
    }

    val type = Type().also(::add)

    class Tags : Filter.Text("Tags") {
        fun toPredicate(): ((PixivIllust) -> Boolean)? {
            if (state.isBlank()) return null

            val tags = state.split(' ')
            return { it.tags?.containsAll(tags) == true }
        }
    }

    val tags = Tags().also(::add)

    class Users : Filter.Text("Users") {
        fun toPredicate(): ((PixivIllust) -> Boolean)? {
            if (state.isBlank()) return null
            val regex = Regex(state.split(' ').joinToString("|") { Regex.escape(it) })

            return { it.author_details?.user_name?.contains(regex) == true }
        }
    }

    val users = Users().also(::add)

    class Rating : Filter.Select<String>("Rating", values, 0) {
        companion object {
            private val searchParams: Array<String?> =
                arrayOf(null, "all", "r18")

            private val values: Array<String> =
                arrayOf("All", "All ages", "R-18")

            private val predicates: Array<((PixivIllust) -> Boolean)?> =
                arrayOf(null, { it.x_restrict == "0" }, { it.x_restrict == "1" })
        }

        fun toPredicate(): ((PixivIllust) -> Boolean)? = predicates[state]
        fun toSearchParameter(): String? = searchParams[state]
    }

    val rating = Rating().also(::add)

    init { add(Filter.Header("(the following are ignored when the users filter is in use)")) }

    class Order : Filter.Sort("Order", arrayOf("Date posted")) {
        fun toSearchParameter(): String? = state?.ascending?.let { "date" }
    }

    val order = Order().also(::add)

    class DateBefore : Filter.Text("Posted before")
    val dateBefore = DateBefore().also(::add)

    class DateAfter : Filter.Text("Posted after")
    val dateAfter = DateAfter().also(::add)
}
