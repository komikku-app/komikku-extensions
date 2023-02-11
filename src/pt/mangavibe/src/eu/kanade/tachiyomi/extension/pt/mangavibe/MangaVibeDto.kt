package eu.kanade.tachiyomi.extension.pt.mangavibe

import kotlinx.serialization.Serializable

typealias MangaVibePopularDto = MangaVibeResultDto<List<MangaVibeComicDto>>
typealias MangaVibeLatestDto = MangaVibeResultDto<List<MangaVibeLatestChapterDto>>
typealias MangaVibeChapterListDto = MangaVibeResultDto<List<MangaVibeChapterDto>>

@Serializable
data class MangaVibeResultDto<T>(
    val data: T? = null,
)

@Serializable
data class MangaVibeComicDto(
    val description: String? = "",
    val genres: List<String>? = emptyList(),
    val id: Int,
    val status: String? = "",
    val title: Map<String, String?> = emptyMap(),
    val views: Int = -1,
)

@Serializable
data class MangaVibeLatestChapterDto(
    val mediaID: String? = "",
    val title: String? = "",
)

@Serializable
data class MangaVibeChapterDto(
    val datePublished: String? = "",
    val mediaID: Int = -1,
    val number: Float = -1f,
    val pages: Int = -1,
    val title: String? = "",
)
