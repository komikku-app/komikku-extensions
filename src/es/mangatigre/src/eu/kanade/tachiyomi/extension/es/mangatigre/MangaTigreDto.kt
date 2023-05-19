package eu.kanade.tachiyomi.extension.es.mangatigre

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PayloadManga(
    val page: Int,
    @SerialName("_token") val token: String,
)

@Serializable
data class PayloadChapter(
    @SerialName("_token") val token: String,
)

@Serializable
data class PayloadSearch(
    val query: String,
    @SerialName("_token") val token: String,
)

@Serializable
data class MangasDto(
    @SerialName("current_page") val page: Int,
    @SerialName("last_page") val totalPages: Int,
    @SerialName("data") val mangas: List<MangasDataDto>,
)

@Serializable
data class MangasDataDto(
    @SerialName("name") val title: String,
    val slug: String,
    @SerialName("image") val thumbnailFileName: String,
)

@Serializable
data class ChapterDto(
    val manga: ChapterMangaInfoDto,
    val number: Float,
    val images: Map<String, ChapterImagesDto>,
)

@Serializable
data class ChapterMangaInfoDto(
    val slug: String,
)

@Serializable
data class ChapterImagesDto(
    val name: String,
    val format: String,
)

@Serializable
data class SearchDto(
    val result: List<SearchDataDto>,
)

@Serializable
data class SearchDataDto(
    val id: Int,
    @SerialName("name") val title: String,
    val slug: String,
    @SerialName("image")val thumbnailFileName: String,
)

fun String.toStatus(): Int = when (this) {
    "En Marcha" -> SManga.ONGOING
    "Terminado" -> SManga.COMPLETED
    "Detenido" -> SManga.ON_HIATUS
    "Pausado" -> SManga.ON_HIATUS
    else -> SManga.UNKNOWN
}
