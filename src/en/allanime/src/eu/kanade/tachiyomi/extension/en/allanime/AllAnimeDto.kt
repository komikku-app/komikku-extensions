package eu.kanade.tachiyomi.extension.en.allanime

import kotlinx.serialization.Serializable

@Serializable
data class ApiPopularResponse(
    val data: PopularResultData,
) {
    @Serializable
    data class PopularResultData(
        val queryPopular: QueryPopularData,
    ) {
        @Serializable
        data class QueryPopularData(
            val recommendations: List<Recommendation>,
        ) {
            @Serializable
            data class Recommendation(
                val anyCard: Manga? = null,
            )
        }
    }
}

@Serializable
data class ApiSearchResponse(
    val data: SearchResultData,
) {
    @Serializable
    data class SearchResultData(
        val mangas: SearchResultMangas,
    ) {
        @Serializable
        data class SearchResultMangas(
            val edges: List<Manga>,
        )
    }
}

@Serializable
data class ApiMangaDetailsResponse(
    val data: MangaDetailsData,
) {
    @Serializable
    data class MangaDetailsData(
        val manga: Manga,
    )
}

@Serializable
data class Manga(
    val _id: String,
    val name: String,
    val thumbnail: String,
    val description: String?,
    val authors: List<String>?,
    val genres: List<String>?,
    val tags: List<String>?,
    val status: String?,
    val altNames: List<String>?,
    val englishName: String? = null,
    val nativeName: String? = null,
)

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
            val availableChaptersDetail: AvailableChapters,
        ) {
            @Serializable
            data class AvailableChapters(
                val sub: List<String>? = null,
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
        val chapterPages: PageList?,
    ) {
        @Serializable
        data class PageList(
            val edges: List<Servers>?,
        ) {
            @Serializable
            data class Servers(
                val pictureUrlHead: String? = null,
                val pictureUrls: List<PageUrl>,
            ) {
                @Serializable
                data class PageUrl(
                    val url: String,
                )
            }
        }
    }
}
