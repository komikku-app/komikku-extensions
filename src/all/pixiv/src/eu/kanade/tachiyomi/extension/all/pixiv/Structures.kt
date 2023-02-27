package eu.kanade.tachiyomi.extension.all.pixiv

import kotlinx.serialization.Serializable

@Serializable
internal data class PixivApiResponse<T>(
    val error: Boolean,
    val body: T? = null,
    val message: String? = null,
)

@Serializable
internal data class PixivIllust(
    val id: Int? = null,
    val title: String? = null,
    val userName: String? = null,
    val description: String? = null,
    val tags: PixivTags? = null,
    val urls: PixivImageUrls? = null,
    val uploadDate: String? = null,
)

@Serializable
internal data class PixivSearchResult(
    val id: Int? = null,
    val title: String? = null,
    val url: String? = null,
    val isAdContainer: Boolean? = null,
)

@Serializable
internal data class PixivTag(
    val tag: String? = null,
)

@Serializable
internal data class PixivTags(
    val tags: List<PixivTag>? = null,
)

@Serializable
internal data class PixivSearchResults(
    val illustManga: PixivSearchResultsIllusts? = null,
    val illust: PixivSearchResultsIllusts? = null,
    val manga: PixivSearchResultsIllusts? = null,
    val popular: PixivSearchResultsPopular? = null,
)

@Serializable
internal data class PixivSearchResultsIllusts(
    val data: List<PixivSearchResult>? = null,
)

@Serializable
internal data class PixivSearchResultsPopular(
    val permanent: List<PixivSearchResult>? = null,
    val recent: List<PixivSearchResult>? = null,
)

@Serializable
internal data class PixivPage(
    val urls: PixivImageUrls? = null,
)

@Serializable
internal data class PixivImageUrls(
    val original: String? = null,
    val thumb: String? = null,
)
