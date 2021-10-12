package eu.kanade.tachiyomi.extension.en.zinchanmanga

import eu.kanade.tachiyomi.source.model.Filter

class ZinChanGenre(values: Array<String>) : Filter.Select<String>("Genres", values) {
    override fun toString() = (state + 27).toString()

    companion object {
        object Note : Filter.Header("NOTE: can't combine with text search!")

        val genres: Array<String>
            get() = arrayOf(
                "<select>",
                "BL",
                "Manhwa",
                "Smut",
                "Comedy",
                "Romance",
                "Cooking",
                "Korean",
                "Japanese",
                "Manga",
                "Manhua",
                "Webtoon",
            )
    }
}
