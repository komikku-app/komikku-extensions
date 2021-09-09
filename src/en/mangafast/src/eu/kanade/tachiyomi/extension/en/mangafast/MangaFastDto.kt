package eu.kanade.tachiyomi.extension.en.mangafast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResultDto(
    @SerialName("hits") val mangaList: List<MangaDto> = emptyList()
)

@Serializable
data class MangaDto(
    val title: String = "",
    val slug: String = "",
    val thumbnail: String = "",
)
