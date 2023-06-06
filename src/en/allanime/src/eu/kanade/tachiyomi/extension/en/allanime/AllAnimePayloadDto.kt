package eu.kanade.tachiyomi.extension.en.allanime

import kotlinx.serialization.Serializable

@Serializable
data class ApiPopularPayload(
    val variables: ApiPopularVariables,
    val query: String,
) {
    @Serializable
    data class ApiPopularVariables(
        val type: String,
        val size: Int,
        val dateRange: Int,
        val page: Int,
        val allowAdult: Boolean,
        val allowUnknown: Boolean,
    )

    constructor(
        type: String = "manga",
        size: Int,
        dateRange: Int,
        page: Int,
        allowAdult: Boolean = false,
        allowUnknown: Boolean = false,
    ) : this(
        ApiPopularVariables(
            type = type,
            size = size,
            dateRange = dateRange,
            page = page,
            allowAdult = allowAdult,
            allowUnknown = allowUnknown,
        ),
        POPULAR_QUERY,
    )
}

@Serializable
data class ApiSearchPayload(
    val variables: ApiSearchVariables,
    val query: String,
) {
    @Serializable
    data class ApiSearchVariables(
        val search: SearchPayload,
        val limit: Int,
        val page: Int,
        val translationType: String,
        val countryOrigin: String,
    )

    @Serializable
    data class SearchPayload(
        val query: String,
        val genres: List<String>?,
        val excludeGenres: List<String>?,
        val isManga: Boolean,
        val allowAdult: Boolean,
        val allowUnknown: Boolean,
    )

    constructor(
        query: String,
        size: Int,
        page: Int,
        genres: List<String>?,
        excludeGenres: List<String>?,
        translationType: String,
        countryOrigin: String,
        isManga: Boolean = true,
        allowAdult: Boolean = false,
        allowUnknown: Boolean = false,
    ) : this(
        ApiSearchVariables(
            search = SearchPayload(
                query = query,
                genres = genres,
                excludeGenres = excludeGenres,
                isManga = isManga,
                allowAdult = allowAdult,
                allowUnknown = allowUnknown,
            ),
            limit = size,
            page = page,
            translationType = translationType,
            countryOrigin = countryOrigin,
        ),
        SEARCH_QUERY,
    )
}

@Serializable
data class ApiIDPayload(
    val variables: ApiIDVariables,
    val query: String,
) {
    @Serializable
    data class ApiIDVariables(
        val id: String,
    )

    constructor(
        id: String,
        graphqlQuery: String,
    ) : this(
        ApiIDVariables(id),
        graphqlQuery,
    )
}

@Serializable
data class ApiChapterListDetailsPayload(
    val variables: ApiChapterDetailsVariables,
    val query: String,
) {
    @Serializable
    data class ApiChapterDetailsVariables(
        val id: String,
        val chapterNumStart: Float,
        val chapterNumEnd: Float,
    )

    constructor(
        id: String,
        chapterNumStart: Float,
        chapterNumEnd: Float,
    ) : this(
        ApiChapterDetailsVariables(
            id = "manga@$id",
            chapterNumStart = chapterNumStart,
            chapterNumEnd = chapterNumEnd,
        ),
        CHAPTERS_DETAILS_QUERY,
    )
}

@Serializable
data class ApiPageListPayload(
    val variables: ApiPageListVariables,
    val query: String,
) {
    @Serializable
    data class ApiPageListVariables(
        val id: String,
        val chapterNum: String,
        val translationType: String,
    )

    constructor(
        id: String,
        chapterNum: String,
        translationType: String,
    ) : this(
        ApiPageListVariables(
            id = id,
            chapterNum = chapterNum,
            translationType = translationType,
        ),
        PAGE_QUERY,
    )
}
