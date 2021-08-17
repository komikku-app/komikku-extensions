package eu.kanade.tachiyomi.extension.en.inkr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NextJsWrapper<T>(
    val pageProps: T? = null
)

@Serializable
data class InkrResult<T>(
    val code: Int = -1,
    val data: T? = null
)

@Serializable
data class InkrHome(
    val latestUpdateDetails: List<InkrComic> = emptyList(),
    val topCharts: InkrHomeCharts? = null
)

@Serializable
data class InkrHomeCharts(
    val topTrending: List<InkrComic> = emptyList()
)

@Serializable
data class InkrSearch(
    val title: List<String> = emptyList()
)

@Serializable
data class InkrTitleInfo(
    val titleInfo: InkrComic? = null
)

@Serializable
data class InkrComic(
    val creators: List<InkrPerson> = emptyList(),
    val extras: Map<String, String>? = emptyMap(),
    val firstChapterFirstPublishedDate: String = "",
    val listGenre: JsonElement? = null,
    val name: String = "",
    val oid: String = "",
    val releaseStatus: String = "",
    val summary: List<String> = emptyList(),
    val thumbnailURL: String = "",
    val webPreviewingPages: List<InkrPage> = emptyList()
)

@Serializable
data class InkrPerson(
    val name: String = "",
    val role: String = ""
)

@Serializable
data class InkrPage(
    val url: String = ""
)
