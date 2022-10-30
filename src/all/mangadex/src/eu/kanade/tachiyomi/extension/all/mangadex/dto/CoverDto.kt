package eu.kanade.tachiyomi.extension.all.mangadex.dto

import kotlinx.serialization.Serializable

@Serializable
data class CoverListDto(
    val data: List<CoverDto> = emptyList()
)

@Serializable
data class CoverDto(
    val id: String,
    val attributes: CoverAttributesDto? = null,
    val relationships: List<RelationshipDto> = emptyList()
)

@Serializable
data class CoverAttributesDto(
    val name: String? = null,
    val fileName: String? = null,
    val locale: String? = null
)
