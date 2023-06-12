package eu.kanade.tachiyomi.extension.en.allanime

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import java.util.Locale

@Serializable
data class ApiPopularResponse(
    val data: PopularResponseData,
) {
    @Serializable
    data class PopularResponseData(
        @SerialName("queryPopular") val popular: PopularData,
    ) {
        @Serializable
        data class PopularData(
            @SerialName("recommendations") val mangas: List<Popular>,
        ) {
            @Serializable
            data class Popular(
                @SerialName("anyCard") val manga: SearchManga? = null,
            )
        }
    }
}

@Serializable
data class ApiSearchResponse(
    val data: SearchResponseData,
) {
    @Serializable
    data class SearchResponseData(
        val mangas: SearchResultMangas,
    ) {
        @Serializable
        data class SearchResultMangas(
            @SerialName("edges") val mangas: List<SearchManga>,
        )
    }
}

@Serializable
data class SearchManga(
    @SerialName("_id") val id: String,
    val name: String,
    val thumbnail: String? = null,
    val englishName: String? = null,
    val nativeName: String? = null,
) {
    fun toSManga(titleStyle: String?) = SManga.create().apply {
        title = titleStyle.preferedName(name, englishName, nativeName)
        url = "/manga/$id/${name.titleToSlug()}"
        thumbnail_url = thumbnail?.parseThumbnailUrl()
    }
}

@Serializable
data class ApiMangaDetailsResponse(
    val data: MangaDetailsData,
) {
    @Serializable
    data class MangaDetailsData(
        val manga: Manga,
    ) {
        @Serializable
        data class Manga(
            @SerialName("_id") val id: String,
            val name: String,
            val thumbnail: String? = null,
            val description: String? = null,
            val authors: List<String>? = emptyList(),
            val genres: List<String>? = emptyList(),
            val tags: List<String>? = emptyList(),
            val status: String? = null,
            val altNames: List<String>? = emptyList(),
            val englishName: String? = null,
            val nativeName: String? = null,
        ) {
            fun toSManga(titleStyle: String?) = SManga.create().apply {
                title = titleStyle.preferedName(name, englishName, nativeName)
                url = "/manga/$id/${name.titleToSlug()}"
                thumbnail_url = thumbnail?.parseThumbnailUrl()
                description = this@Manga.description?.parseDescription()
                if (!altNames.isNullOrEmpty()) {
                    description += altNames.joinToString(
                        prefix = "\n\nAlternative Names:\n* ",
                        separator = "\n* ",
                    ) { it.trim() }
                }
                if (authors?.isNotEmpty() == true) {
                    author = authors.first().trim()
                    artist = author
                }
                genre = "${genres?.joinToString { it.trim() }}, ${tags?.joinToString { it.trim() }}"
                status = this@Manga.status.parseStatus()
            }
        }
    }
}

@Serializable
data class ApiChapterListResponse(
    val data: ChapterListData,
) {
    @Serializable
    data class ChapterListData(
        val manga: ChapterList,
    ) {
        @Serializable
        data class ChapterList(
            @SerialName("availableChaptersDetail") val chapters: AvailableChapters,
        ) {
            @Serializable
            data class AvailableChapters(
                val sub: List<String>? = null,
            )
        }
    }
}

@Serializable
data class ApiChapterListDetailsResponse(
    val data: ChapterListData,
) {
    @Serializable
    data class ChapterListData(
        @SerialName("episodeInfos") val chapterList: List<ChapterData>? = emptyList(),
    ) {
        @Serializable
        data class ChapterData(
            @SerialName("episodeIdNum") val chapterNum: Float,
            @SerialName("notes") val title: String? = null,
            val uploadDates: DateDto? = null,
        ) {
            @Serializable
            data class DateDto(
                val sub: String? = null,
            )
        }
    }
}

@Serializable
data class ApiPageListResponse(
    val data: PageListData,
) {
    @Serializable
    data class PageListData(
        @SerialName("chapterPages") val pageList: PageList?,
    ) {
        @Serializable
        data class PageList(
            @SerialName("edges") val serverList: List<Servers>?,
        ) {
            @Serializable
            data class Servers(
                @SerialName("pictureUrlHead") val serverUrl: String? = null,
                val pictureUrls: List<PageUrl>?,
            ) {
                @Serializable
                data class PageUrl(
                    val url: String,
                )
            }
        }
    }
}

fun String.parseThumbnailUrl(): String {
    return if (this.matches(AllAnime.urlRegex)) {
        this
    } else {
        "${AllAnime.thumbnail_cdn}$this?w=250"
    }
}

fun String?.parseStatus(): Int {
    if (this == null) {
        return SManga.UNKNOWN
    }

    return when {
        this.contains("releasing", true) -> SManga.ONGOING
        this.contains("finished", true) -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }
}

private fun String.titleToSlug() = this.trim()
    .lowercase(Locale.US)
    .replace(AllAnime.titleSpecialCharactersRegex, "-")

private fun String?.preferedName(name: String, englishName: String?, nativeName: String?): String {
    return when (this) {
        "eng" -> englishName
        "native" -> nativeName
        else -> name
    } ?: name
}

private fun String.parseDescription(): String {
    return Jsoup.parse(
        this.replace("<br>", "br2n"),
    ).text().replace("br2n", "\n")
}
