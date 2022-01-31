package eu.kanade.tachiyomi.extension.all.bilibili

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BilibiliResultDto<T>(
    val code: Int = 0,
    val data: T? = null,
    @SerialName("msg") val message: String = ""
)

@Serializable
data class BilibiliSearchDto(
    val list: List<BilibiliComicDto> = emptyList()
)

@Serializable
data class BilibiliComicDto(
    @SerialName("author_name") val authorName: List<String> = emptyList(),
    @SerialName("classic_lines") val classicLines: String = "",
    @SerialName("comic_id") val comicId: Int = 0,
    @SerialName("ep_list") val episodeList: List<BilibiliEpisodeDto> = emptyList(),
    val id: Int = 0,
    @SerialName("is_finish") val isFinish: Int = 0,
    @SerialName("season_id") val seasonId: Int = 0,
    val styles: List<String> = emptyList(),
    val title: String,
    @SerialName("vertical_cover") val verticalCover: String = ""
) {
    val hasPaidChapters: Boolean
        get() = episodeList.any { episode -> episode.payMode == 1 && episode.payGold > 0 }
}

@Serializable
data class BilibiliEpisodeDto(
    val id: Int,
    @SerialName("pay_gold") val payGold: Int,
    @SerialName("pay_mode") val payMode: Int,
    @SerialName("pub_time") val publicationTime: String,
    @SerialName("short_title") val shortTitle: String,
    val title: String
)

@Serializable
data class BilibiliReader(
    val images: List<BilibiliImageDto> = emptyList()
)

@Serializable
data class BilibiliImageDto(
    val path: String
)

@Serializable
data class BilibiliPageDto(
    val token: String,
    val url: String
)

@Serializable
data class BilibiliAccessTokenCookie(
    val accessToken: String,
    val refreshToken: String,
    val area: String
)

@Serializable
data class BilibiliAccessToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class BilibiliUserEpisodes(
    @SerialName("unlocked_eps") val unlockedEpisodes: List<BilibiliUnlockedEpisode>? = emptyList()
)

@Serializable
data class BilibiliUnlockedEpisode(
    @SerialName("ep_id") val id: Int = 0
)

@Serializable
data class BilibiliGetCredential(
    @SerialName("comic_id") val comicId: Int,
    @SerialName("ep_id") val episodeId: Int,
    val type: Int
)

@Serializable
data class BilibiliCredential(
    val credential: String
)
