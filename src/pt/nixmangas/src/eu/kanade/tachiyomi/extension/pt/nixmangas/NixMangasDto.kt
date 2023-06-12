package eu.kanade.tachiyomi.extension.pt.nixmangas

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
data class NixMangasPaginatedContent<T>(
    @SerialName("current_page") val currentPage: Int,
    val data: List<T> = emptyList(),
    @SerialName("last_page") val lastPage: Int,
) {

    val hasNextPage: Boolean
        get() = currentPage < lastPage
}

@Serializable
data class NixMangasSearchDto(val mangas: NixMangasPaginatedContent<NixMangasWorkDto>)

@Serializable
data class NixMangasDetailsDto(val manga: NixMangasWorkDto)

@Serializable
data class NixMangasReaderDto(val chapter: NixMangasChapterDto)

@Serializable
data class NixMangasWorkDto(
    val id: String,
    val chapters: List<NixMangasChapterDto> = emptyList(),
    val cover: String? = null,
    val genres: List<NixMangasGenreDto> = emptyList(),
    @SerialName("is_adult") val isAdult: Boolean = false,
    val slug: String,
    val status: String? = null,
    @SerialName("synopses") val synopsis: String? = null,
    val thumbnail: String,
    val title: String,
) {

    fun toSManga(): SManga = SManga.create().apply {
        title = this@NixMangasWorkDto.title
        description = synopsis
        genre = genres.joinToString { it.name }
        status = when (this@NixMangasWorkDto.status) {
            "ACTIVE" -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
        thumbnail_url = cover
        url = "/obras/$slug"
    }
}

@Serializable
data class NixMangasGenreDto(val name: String)

@Serializable
data class NixMangasChapterDto(
    val id: String,
    @SerialName("is_published") val isPublished: Boolean,
    val number: Float,
    val pages: List<NixMangasPageDto> = emptyList(),
    val slug: String,
    @SerialName("published_at") val publishedAt: String? = null,
) {

    fun toSChapter(): SChapter = SChapter.create().apply {
        name = "Capítulo ${number.toString().replace(".0", "")}"
        chapter_number = number
        date_upload = runCatching { DATE_FORMATTER.parse(publishedAt!!)?.time }
            .getOrNull() ?: 0L
        url = "/ler/$id"
    }

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
        }
    }
}

@Serializable
data class NixMangasPageDto(@SerialName("page_url") val pageUrl: String)
