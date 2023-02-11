package eu.kanade.tachiyomi.multisrc.mangasar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class MangaSarLatestDto(
    val page: String,
    @JsonNames("lancamentos") val releases: List<MangaSarReleaseDto> = emptyList(),
    @SerialName("total_page") val totalPage: Int? = 0,
)

@Serializable
data class MangaSarReleaseDto(
    val image: String,
    val link: String,
    val name: String,
)

@Serializable
data class MangaSarTitleDto(
    @SerialName("img") val image: String,
    val title: String,
    val url: String,
)

@Serializable
data class MangaSarPaginatedChaptersDto(
    val chapters: List<MangaSarChapterDto>? = emptyList(),
    @SerialName("pagina") val page: Int? = -1,
    @SerialName("total_pags") val totalPages: Int? = -1,
)

@Serializable
data class MangaSarChapterDto(
    @SerialName("date_created") val dateCreated: String,
    val link: String,
    @SerialName("chapter_name") val name: JsonPrimitive,
    val number: JsonPrimitive,
)

@Serializable
data class MangaSarReaderDto(
    val images: List<MangaSarPageDto> = emptyList(),
)

@Serializable
data class MangaSarPageDto(
    val url: String,
)
