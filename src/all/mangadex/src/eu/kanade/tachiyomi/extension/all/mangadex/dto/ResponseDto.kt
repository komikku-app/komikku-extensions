package eu.kanade.tachiyomi.extension.all.mangadex.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponseDto<T : EntityDto>(
    val result: String,
    val response: String = "",
    val data: List<T> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)

@Serializable
data class ResponseDto<T : EntityDto>(
    val result: String,
    val response: String = "",
    val data: T? = null,
)
