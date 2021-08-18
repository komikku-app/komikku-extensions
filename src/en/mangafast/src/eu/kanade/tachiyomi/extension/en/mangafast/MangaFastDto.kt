package eu.kanade.tachiyomi.extension.en.mangafast

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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
