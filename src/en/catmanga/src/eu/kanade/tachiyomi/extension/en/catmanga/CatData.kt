package eu.kanade.tachiyomi.extension.en.catmanga

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.Serializable

@Serializable
data class CatSeries(
    val alt_titles: List<String>,
    val authors: List<String>,
    val genres: List<String>,
    val chapters: List<CatSeriesChapter>? = null,
    val title: String,
    val series_id: String,
    val description: String,
    val status: String,
    val cover_art: CatSeriesCover,
    val all_covers: List<CatSeriesCover>? = null
) {
    fun toSManga() = this.let { series ->
        SManga.create().apply {
            url = "/series/${series.series_id}"
            title = series.title
            thumbnail_url = series.cover_art.source
            author = series.authors.joinToString(", ")
            description = series.description
            genre = series.genres.joinToString(", ")
            status = when (series.status) {
                "ongoing" -> SManga.ONGOING
                "completed" -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }

            if (alt_titles.isNotEmpty()) {
                description += "\n\nAlternative titles:\n"
                alt_titles.forEach {
                    description += "• $it\n"
                }
            }
        }
    }
}

@Serializable
data class CatSeriesChapter(
    val title: String? = null,
    val groups: List<String>,
    val number: Float,
    val display_number: String? = null,
    val volume: Int? = null
)

@Serializable
data class CatSeriesCover(
    val source: String,
    val width: Int,
    val height: Int
)
