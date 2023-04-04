package eu.kanade.tachiyomi.extension.en.lynxscans

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.Serializable

@Serializable
data class Latest(
    val chapters: LatestChapters,
)

@Serializable
data class LatestChapters(
    val next_page_url: String?,
    val data: List<LatestChaptersData>,
)

@Serializable
data class LatestChaptersData(
    val comic_title: String,
    val comic_thumb: String,
    val comic_titleSlug: String,
) {
    fun toSManga(): SManga = SManga.create().apply {
        title = this@LatestChaptersData.comic_title
        thumbnail_url = this@LatestChaptersData.comic_thumb
        url = "/comics/" + this@LatestChaptersData.comic_titleSlug
    }
}

@Serializable
data class Popular(
    val comics: PopularComics,
)

@Serializable
data class PopularComics(
    val next_page_url: String?,
    val data: List<PopularComicsData>,
)

@Serializable
data class PopularComicsData(
    val title: String,
    val thumb: String,
    val titleSlug: String,
) {
    fun toSManga(): SManga = SManga.create().apply {
        title = this@PopularComicsData.title
        thumbnail_url = this@PopularComicsData.thumb
        url = "/comics/" + this@PopularComicsData.titleSlug
    }
}

@Serializable
data class MangaDetails(
    val comic: MangaDetailsComicData,
)

@Serializable
data class MangaDetailsComicData(
    val title: String,
    val thumb: String,
    val titleSlug: String,
    val artist: String,
    val author: String,
    val description: String,
    val tags: List<MangaDetailsTag>,

    val volumes: List<MangaDetailsVolume>,

) {
    fun toSManga(): SManga = SManga.create().apply {
        title = this@MangaDetailsComicData.title
        thumbnail_url = this@MangaDetailsComicData.thumb
        url = "/comics/" + this@MangaDetailsComicData.titleSlug
        author = if (this@MangaDetailsComicData.author != "blank") this@MangaDetailsComicData.author else null
        artist = if (this@MangaDetailsComicData.artist != "blank") this@MangaDetailsComicData.artist else null
        description = this@MangaDetailsComicData.description
        genre = this@MangaDetailsComicData.tags.joinToString { it.name }
        status = SManga.UNKNOWN
    }
}

@Serializable
data class MangaDetailsTag(
    val name: String,
)

@Serializable
data class MangaDetailsVolume(
    val chapters: List<MangaDetailsChapter>,
    val name: String,
    val number: Int,
)

@Serializable
data class MangaDetailsChapter(
    val name: String,
    val number: Int,
)

@Serializable
data class PageList(
    val pages: List<Page>,
)

@Serializable
data class Page(
    val thumb: String,
)
