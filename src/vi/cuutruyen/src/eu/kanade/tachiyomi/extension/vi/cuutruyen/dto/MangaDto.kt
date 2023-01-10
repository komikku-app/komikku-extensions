package eu.kanade.tachiyomi.extension.vi.cuutruyen.dto

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthorDto(
    val name: String,
)

@Serializable
data class TeamDto(
    val id: Int,
    val name: String,
    val description: String,
)

@Serializable
data class MangaDto(
    val id: Int,
    val name: String,
    @SerialName("cover_url") val coverUrl: String,
    @SerialName("cover_mobile_url") val coverMobileUrl: String,

    val author: AuthorDto? = null,
    val author_name: String? = null,

    val description: String? = null,
    val team: TeamDto? = null,
) {
    fun toSManga(coverQuality: String? = null): SManga = SManga.create().apply {
        val dto = this@MangaDto
        url = "/mangas/${dto.id}"
        title = dto.name
        author = dto.author?.name ?: dto.author_name

        description = ""
        if (dto.team != null) {
            description += "Nhóm dịch: ${dto.team.name}\n\n"
        }
        description += dto.description ?: ""

        thumbnail_url = dto.coverUrl
        if (coverQuality == "cover_url") {
            thumbnail_url = dto.coverUrl
        } else if (coverQuality == "cover_mobile_url") {
            thumbnail_url = dto.coverMobileUrl
        }
    }
}
