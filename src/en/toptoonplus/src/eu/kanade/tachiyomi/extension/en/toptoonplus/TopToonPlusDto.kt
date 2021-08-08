package eu.kanade.tachiyomi.extension.en.toptoonplus

import kotlinx.serialization.Serializable

@Serializable
data class TopToonResult<T>(
    val uuid: String? = "",
    val data: T? = null
)

@Serializable
data class TopToonRanking(
    val ranking: List<TopToonComic> = emptyList()
)

@Serializable
data class TopToonDaily(
    val daily: List<TopToonComic> = emptyList()
)

@Serializable
data class TopToonDetails(
    val comic: TopToonComic? = null,
    val episode: List<TopToonEpisode> = emptyList()
)

@Serializable
data class TopToonUsableEpisode(
    val episode: TopToonEpisode? = null,
    val episodePrice: TopToonEpisodePrice? = null,
    val isFree: Boolean = false,
    val needLogin: Boolean = false,
    val purchaseMethod: List<String> = emptyList()
)

@Serializable
data class TopToonEpisodePrice(
    val payType: Int = -1
)

@Serializable
data class TopToonComic(
    val author: List<String> = emptyList(),
    val comicId: Int = -1,
    val information: TopToonComicInfo? = null,
    val thumbnailImage: TopToonComicPoster? = null,
)

@Serializable
data class TopToonComicInfo(
    val description: String = "",
    val mature: Int = 0,
    val title: String = ""
)

@Serializable
data class TopToonComicPoster(
    val jpeg: List<TopToonImage> = emptyList()
)

@Serializable
data class TopToonImage(
    val path: String = ""
)

@Serializable
data class TopToonEpisode(
    val comicId: Int = -1,
    val contentImage: TopToonComicPoster? = null,
    val episodeId: Int = -1,
    val information: TopToonEpisodeInfo? = null,
    val order: Int = -1
)

@Serializable
data class TopToonEpisodeInfo(
    val needLogin: Int = 0,
    val payType: Int = 0,
    val publishedAt: TopToonEpisodeDate? = null,
    val subTitle: String = "",
    val title: String = ""
)

@Serializable
data class TopToonEpisodeDate(
    val date: String = ""
)

@Serializable
data class TopToonAuth(
    val auth: Int = 0,
    val mature: Int = 0,
    val sign: Int = 0,
    val token: String = ""
)
