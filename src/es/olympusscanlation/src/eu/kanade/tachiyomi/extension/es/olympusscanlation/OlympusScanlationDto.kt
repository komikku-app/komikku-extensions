package eu.kanade.tachiyomi.extension.es.olympusscanlation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChapterDto(
    val id: Int,
    val name: String,
    @SerialName("published_at") val date: String,
)

@Serializable
data class MangaDto(
    val id: Int,
    val name: String,
    val slug: String,
    val cover: String? = null,
    val type: String? = null,
    val summary: String? = null,
)

@Serializable
data class MangaDetailDto(
    var data: MangaDto,
)

@Serializable
data class MetaDto(val total: Int)

@Serializable
data class PageDto(val pages: List<String>)

@Serializable
data class PayloadChapterDto(var data: List<ChapterDto>, val meta: MetaDto)

@Serializable
data class PayloadMangaDto(val data: List<MangaDto>)

@Serializable
data class PayloadPagesDto(val chapter: PageDto)

@Serializable
data class HomeDto(
    val popular_comics: String,
    val new_chapters: List<MangaDto>,
)

@Serializable
data class PayloadHomeDto(
    val data: HomeDto,
)
