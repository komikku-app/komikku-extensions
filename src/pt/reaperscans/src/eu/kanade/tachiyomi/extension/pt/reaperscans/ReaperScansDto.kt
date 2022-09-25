package eu.kanade.tachiyomi.extension.pt.reaperscans

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Locale

@Serializable
data class ReaperSeriesDto(
    val id: Int,
    @SerialName("series_slug") val slug: String,
    val author: String? = null,
    val description: String? = null,
    val studio: String? = null,
    val status: String? = null,
    val thumbnail: String,
    val title: String,
    val chapters: List<ReaperChapterDto>? = emptyList()
) {

    fun toSManga(): SManga = SManga.create().apply {
        title = this@ReaperSeriesDto.title
        author = this@ReaperSeriesDto.author
        artist = this@ReaperSeriesDto.studio
        description = this@ReaperSeriesDto.description?.let { Jsoup.parseBodyFragment(it).text() }
        thumbnail_url = "${ReaperScans.API_URL}/cover/$thumbnail"
        status = when (this@ReaperSeriesDto.status) {
            "Ongoing" -> SManga.ONGOING
            "Hiatus" -> SManga.ON_HIATUS
            "Dropped" -> SManga.CANCELLED
            else -> SManga.UNKNOWN
        }
        url = "/series/$slug"
    }
}

@Serializable
data class ReaperChapterDto(
    val id: Int,
    @SerialName("chapter_name") val name: String,
    @SerialName("chapter_slug") val slug: String,
    val index: String,
    @SerialName("created_at") val createdAt: String,
) {

    fun toSChapter(seriesSlug: String): SChapter = SChapter.create().apply {
        name = this@ReaperChapterDto.name
        date_upload = runCatching { DATE_FORMAT.parse(createdAt.substringBefore("."))?.time }
            .getOrNull() ?: 0L
        url = "/series/$seriesSlug/$slug#$id"
    }

    companion object {
        private val DATE_FORMAT by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale("pt", "BR"))
        }
    }
}

@Serializable
data class ReaperReaderDto(
    val content: ReaperReaderContentDto? = null
)

@Serializable
data class ReaperReaderContentDto(
    val images: List<String>? = emptyList()
)
