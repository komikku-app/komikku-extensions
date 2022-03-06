package eu.kanade.tachiyomi.extension.all.mangadex.dto

import kotlinx.serialization.Serializable

@Serializable
data class ListDto(
    val result: String,
    val response: String,
    val data: ListDataDto,
)

@Serializable
data class ListDataDto(
    val id: String,
    val type: String,
    val attributes: ListAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
data class ListAttributesDto(
    val name: String,
    val visibility: String,
    val version: Int,
)
