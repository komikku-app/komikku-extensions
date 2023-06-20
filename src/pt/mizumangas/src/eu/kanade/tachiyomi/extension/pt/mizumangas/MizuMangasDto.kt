package eu.kanade.tachiyomi.extension.pt.mizumangas

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
data class MizuMangasPaginatedContent<T>(
    @SerialName("current_page") val currentPage: Int,
    val data: List<T> = emptyList(),
    @SerialName("last_page") val lastPage: Int,
) {

    val hasNextPage: Boolean
        get() = currentPage < lastPage
}

@Serializable
data class MizuMangasSearchDto(val mangas: List<MizuMangasWorkDto>)

@Serializable
data class MizuMangasWorkDto(
    val id: Int,
    val photo: String? = null,
    val synopsis: String? = null,
    val name: String,
    val status: MizuMangasStatusDto? = null,
    val categories: List<MizuMangasCategoryDto> = emptyList(),
    val people: List<MizuMangasStaffDto> = emptyList(),
) {

    fun toSManga(): SManga = SManga.create().apply {
        title = name
        author = people.joinToString { it.name }
        description = synopsis
        genre = categories.joinToString { it.name }
        status = when (this@MizuMangasWorkDto.status?.name) {
            "Ativo" -> SManga.ONGOING
            "Completo" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        thumbnail_url = "${MizuMangas.CDN_URL}/$photo"
        url = "/manga/$id"
    }
}

@Serializable
data class MizuMangasStatusDto(val name: String)

@Serializable
data class MizuMangasCategoryDto(val name: String)

@Serializable
data class MizuMangasStaffDto(val name: String)

@Serializable
data class MizuMangasChapterDto(
    val id: Int,
    @SerialName("chapter") val number: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("manga_pages") val pages: List<MizuMangasPageDto> = emptyList(),
) {

    fun toSChapter(): SChapter = SChapter.create().apply {
        name = "Capítulo $number"
        chapter_number = number.toFloatOrNull() ?: -1f
        date_upload = runCatching { DATE_FORMATTER.parse(createdAt!!)?.time }
            .getOrNull() ?: 0L
        url = "/manga/reader/$id"
    }

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
        }
    }
}

@Serializable
data class MizuMangasPageDto(val page: String)
