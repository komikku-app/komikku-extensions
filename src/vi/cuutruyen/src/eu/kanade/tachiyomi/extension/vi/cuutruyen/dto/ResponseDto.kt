package eu.kanade.tachiyomi.extension.vi.cuutruyen.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResponseDto<T>(
    val data: T,
    @SerialName("_metadata") val metadata: PaginationMetadataDto? = null,
)

@Serializable
data class PaginationMetadataDto(
    @SerialName("total_count") val totalCount: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("current_page") val currentPage: Int,
    @SerialName("per_page") val perPage: Int,
)
