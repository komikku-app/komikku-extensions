@file:Suppress("CanSealedSubClassBeObject")

package eu.kanade.tachiyomi.extension.all.projectsuki

import eu.kanade.tachiyomi.source.model.Filter
import okhttp3.HttpUrl

@Suppress("NOTHING_TO_INLINE")
object PSFilters {
    internal sealed interface AutoFilter {
        fun applyTo(builder: HttpUrl.Builder)
    }

    private inline fun HttpUrl.Builder.setAdv() = setQueryParameter("adv", "1")

    class Author : Filter.Text("Author"), AutoFilter {

        override fun applyTo(builder: HttpUrl.Builder) {
            when {
                state.isNotBlank() -> builder.setAdv().addQueryParameter("author", state)
            }
        }

        companion object {
            val ownHeader by lazy { Header("Cannot search by multiple authors") }
        }
    }

    class Artist : Filter.Text("Artist"), AutoFilter {

        override fun applyTo(builder: HttpUrl.Builder) {
            when {
                state.isNotBlank() -> builder.setAdv().addQueryParameter("artist", state)
            }
        }

        companion object {
            val ownHeader by lazy { Header("Cannot search by multiple artists") }
        }
    }

    class Status : Filter.Select<Status.Value>("Status", Value.values()), AutoFilter {
        enum class Value(val display: String, val query: String) {
            ANY("Any", ""),
            ONGOING("Ongoing", "ongoing"),
            COMPLETED("Completed", "completed"),
            HIATUS("Hiatus", "hiatus"),
            CANCELLED("Cancelled", "cancelled"),
            ;

            override fun toString(): String = display

            companion object {
                private val values: Array<Value> = values()
                operator fun get(ordinal: Int) = values[ordinal]
            }
        }

        override fun applyTo(builder: HttpUrl.Builder) {
            when (val state = Value[state]) {
                Value.ANY -> {} // default, do nothing
                else -> builder.setAdv().addQueryParameter("status", state.query)
            }
        }
    }

    class Origin : Filter.Select<Origin.Value>("Origin", Value.values()), AutoFilter {
        enum class Value(val display: String, val query: String?) {
            ANY("Any", null),
            KOREA("Korea", "kr"),
            CHINA("China", "cn"),
            JAPAN("Japan", "jp"),
            ;

            override fun toString(): String = display

            companion object {
                private val values: Array<Value> = Value.values()
                operator fun get(ordinal: Int) = values[ordinal]
            }
        }

        override fun applyTo(builder: HttpUrl.Builder) {
            when (val state = Value[state]) {
                Value.ANY -> {} // default, do nothing
                else -> builder.setAdv().addQueryParameter("origin", state.query)
            }
        }
    }
}
