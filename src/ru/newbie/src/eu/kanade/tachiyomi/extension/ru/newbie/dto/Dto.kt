import kotlinx.serialization.Serializable

@Serializable
data class TagsDto(
    val title: TitleDto
)

@Serializable
data class BranchesDto(
    val id: Long,
    val is_default: Boolean
)

@Serializable
data class ImgsDto(
    val large: String,
    val small: String,
)

@Serializable
data class ImgDto(
    val srcset: ImgsDto,
)

@Serializable
data class TitleDto(
    val en: String,
    val ru: String
)

@Serializable
data class AuthorDto(
    val name: String?
)

@Serializable
data class LibraryDto(
    val id: Long,
    val title: TitleDto,
    val image: ImgDto
)

@Serializable
data class MangaDetDto(
    val id: Long,
    val title: TitleDto,
    val author: AuthorDto?,
    val artist: AuthorDto?,
    val description: String,
    val image: ImgDto,
    val genres: List<TagsDto>,
    val type: String,
    val status: String,
    val rating: Float,
    val adult: String?,
    val branches: List<BranchesDto>,
)

@Serializable
data class PageWrapperDto<T>(
    val items: List<T>,
)

@Serializable
data class SeriesWrapperDto<T>(
    val items: T
)

@Serializable
data class BookDto(
    val id: Long,
    val tom: Int?,
    val name: String?,
    val number: Float,
    val created_at: String,
    val translator: String?,
    val is_available: Boolean
)

@Serializable
data class PageDto(
    val id: Int,
    val slices: Int?
)
