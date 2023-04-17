package eu.kanade.tachiyomi.extension.uk.honeymanga.dtos

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class HoneyMangaDto(
    val id: String,
    val posterId: String,
    val title: String,
    val description: String?,
    val type: String,
)

@Serializable
data class HoneyMangaResponseDto(
    val data: List<HoneyMangaDto>,
)

@Serializable
data class HoneyMangaChapterPagesDto(
    val id: String,
    val resourceIds: JsonObject,
)

@Serializable
data class HoneyMangaChapterDto(
    val id: String,
    val volume: Int,
    val chapterNum: Int,
    val subChapterNum: Int,
    val mangaId: String,
    val lastUpdated: String,
)
