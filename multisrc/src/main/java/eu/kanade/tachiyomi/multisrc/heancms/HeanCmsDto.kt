package eu.kanade.tachiyomi.multisrc.heancms

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import java.text.SimpleDateFormat

@Serializable
data class HeanCmsQuerySearchDto(
    val data: List<HeanCmsSeriesDto> = emptyList(),
    val meta: HeanCmsQuerySearchMetaDto? = null,
)

@Serializable
data class HeanCmsQuerySearchMetaDto(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("last_page") val lastPage: Int,
) {

    val hasNextPage: Boolean
        get() = currentPage < lastPage
}

@Serializable
data class HeanCmsSearchDto(
    val description: String? = null,
    @SerialName("series_slug") val slug: String,
    @SerialName("series_type") val type: String,
    val title: String,
) {

    fun toSManga(
        apiUrl: String,
        coverPath: String,
        slugMap: Map<String, HeanCms.HeanCmsTitle>,
    ): SManga = SManga.create().apply {
        val slugOnly = slug.replace(HeanCms.TIMESTAMP_REGEX, "")
        val thumbnailFileName = slugMap[slugOnly]?.thumbnailFileName.orEmpty()

        title = this@HeanCmsSearchDto.title
        thumbnail_url = when {
            thumbnailFileName.isNotEmpty() -> "$apiUrl/$coverPath$thumbnailFileName"
            else -> ""
        }
        url = "/series/$slugOnly"
    }
}

@Serializable
data class HeanCmsSeriesDto(
    val id: Int,
    @SerialName("series_slug") val slug: String,
    @SerialName("series_type") val type: String = "Comic",
    val author: String? = null,
    val description: String? = null,
    val studio: String? = null,
    val status: String? = null,
    val thumbnail: String,
    val title: String,
    val tags: List<HeanCmsTagDto>? = emptyList(),
    val chapters: List<HeanCmsChapterDto>? = emptyList(),
) {

    fun toSManga(apiUrl: String, coverPath: String): SManga = SManga.create().apply {
        val descriptionBody = this@HeanCmsSeriesDto.description?.let(Jsoup::parseBodyFragment)

        title = this@HeanCmsSeriesDto.title
        author = this@HeanCmsSeriesDto.author?.trim()
        artist = this@HeanCmsSeriesDto.studio?.trim()
        description = descriptionBody?.select("p")
            ?.joinToString("\n\n") { it.text() }
            ?.ifEmpty { descriptionBody.text().replace("\n", "\n\n") }
        genre = tags.orEmpty()
            .sortedBy(HeanCmsTagDto::name)
            .joinToString { it.name }
        thumbnail_url = "$apiUrl/$coverPath$thumbnail"
        status = this@HeanCmsSeriesDto.status?.toStatus() ?: SManga.UNKNOWN
        url = "/series/${slug.replace(HeanCms.TIMESTAMP_REGEX, "")}"
    }
}

@Serializable
data class HeanCmsTagDto(val name: String)

@Serializable
data class HeanCmsChapterDto(
    val id: Int,
    @SerialName("chapter_name") val name: String,
    @SerialName("chapter_slug") val slug: String,
    val index: String,
    @SerialName("created_at") val createdAt: String,
) {

    fun toSChapter(seriesSlug: String, dateFormat: SimpleDateFormat): SChapter = SChapter.create().apply {
        name = this@HeanCmsChapterDto.name.trim()
        date_upload = runCatching { dateFormat.parse(createdAt)?.time }
            .getOrNull() ?: 0L
        url = "/series/$seriesSlug/$slug#$id"
    }
}

@Serializable
data class HeanCmsReaderDto(
    val content: HeanCmsReaderContentDto? = null,
)

@Serializable
data class HeanCmsReaderContentDto(
    val images: List<String>? = emptyList(),
)

@Serializable
data class HeanCmsQuerySearchPayloadDto(
    val order: String,
    val page: Int,
    @SerialName("order_by") val orderBy: String,
    @SerialName("series_status") val status: String,
    @SerialName("series_type") val type: String,
    @SerialName("tags_ids") val tagIds: List<Int> = emptyList(),
)

@Serializable
data class HeanCmsSearchPayloadDto(val term: String)

fun String.toStatus(): Int = when (this) {
    "Ongoing" -> SManga.ONGOING
    "Hiatus" -> SManga.ON_HIATUS
    "Dropped" -> SManga.CANCELLED
    "Completed", "Finished" -> SManga.COMPLETED
    else -> SManga.UNKNOWN
}
