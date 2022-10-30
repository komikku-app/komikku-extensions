package eu.kanade.tachiyomi.extension.all.mangadex.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class MangaListDto(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val data: List<MangaDataDto>,
)

@Serializable
data class MangaDto(
    val result: String,
    val data: MangaDataDto,
)

@Serializable
data class RelationshipDto(
    val id: String,
    val type: String,
    val attributes: IncludesAttributesDto? = null,
)

@Serializable
data class IncludesAttributesDto(
    val name: String? = null,
    val fileName: String? = null,
    val username: String? = null
)

@Serializable
data class MangaDataDto(
    val id: String,
    val type: String,
    val attributes: MangaAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
data class MangaAttributesDto(
    val title: JsonElement,
    val altTitles: JsonArray,
    val description: JsonElement,
    val originalLanguage: String?,
    val lastVolume: String?,
    val lastChapter: String?,
    val contentRating: String?,
    val publicationDemographic: String?,
    val status: String?,
    val tags: List<TagDto>,
)

@Serializable
data class TagDto(
    val id: String,
    val attributes: TagAttributesDto
)

@Serializable
data class TagAttributesDto(
    val group: String
)

typealias LocalizedString = Map<String, String>

/**
 * Temporary workaround while Dex API still returns arrays instead of objects
 * in the places that uses [LocalizedString].
 */
fun JsonElement.toLocalizedString(): LocalizedString {
    return (this as? JsonObject)?.entries
        ?.associate { (key, value) -> key to (value.jsonPrimitive.contentOrNull ?: "") }
        .orEmpty()
}
