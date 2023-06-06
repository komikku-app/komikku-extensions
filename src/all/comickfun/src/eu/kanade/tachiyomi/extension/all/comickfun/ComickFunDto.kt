package eu.kanade.tachiyomi.extension.all.comickfun

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchManga(
    val hid: String,
    val title: String,
    val md_covers: List<MDcovers>,
    val cover_url: String? = null,

) {
    fun toSManga(useScaledCover: Boolean) = SManga.create().apply {
        // appennding # at end as part of migration from slug to hid
        url = "/comic/$hid#"
        title = this@SearchManga.title
        thumbnail_url = parseCover(cover_url, md_covers, useScaledCover)
    }
}

@Serializable
data class Manga(
    val comic: Comic,
    val artists: List<Artist> = emptyList(),
    val authors: List<Author> = emptyList(),
    val genres: List<Genre> = emptyList(),
) {
    fun toSManga(useScaledCover: Boolean) = SManga.create().apply {
        // appennding # at end as part of migration from slug to hid
        url = "/comic/${comic.hid}#"
        title = comic.title
        description = comic.desc.beautifyDescription()
        if (comic.altTitles.isNotEmpty()) {
            description += comic.altTitles.joinToString(
                separator = "\n",
                prefix = "\n\nAlternative Titles:\n",
            ) {
                it.title.toString()
            }
        }
        status = comic.status.parseStatus(comic.translation_completed)
        thumbnail_url = parseCover(comic.cover_url, comic.md_covers, useScaledCover)
        artist = artists.joinToString { it.name.trim() }
        author = authors.joinToString { it.name.trim() }
        genre = genres.joinToString { it.name.trim() }
    }
}

@Serializable
data class Comic(
    val hid: String,
    val title: String,
    @SerialName("md_titles") val altTitles: List<MDtitles>,
    val desc: String = "N/A",
    val status: Int = 0,
    val translation_completed: Boolean = true,
    val md_covers: List<MDcovers>,
    val cover_url: String? = null,
)

@Serializable
data class MDcovers(
    val b2key: String?,
)

@Serializable
data class MDtitles(
    val title: String?,
)

@Serializable
data class Artist(
    val name: String,
    val slug: String,
)

@Serializable
data class Author(
    val name: String,
    val slug: String,
)

@Serializable
data class Genre(
    val slug: String,
    val name: String,
)

@Serializable
data class ChapterList(
    val chapters: MutableList<Chapter>,
    val total: Int,
)

@Serializable
data class Chapter(
    val hid: String,
    val lang: String,
    val title: String = "",
    val created_at: String = "",
    val chap: String = "",
    val vol: String = "",
    val group_name: List<String> = emptyList(),
) {
    fun toSChapter(mangaUrl: String) = SChapter.create().apply {
        url = "$mangaUrl/$hid-chapter-$chap-$lang"
        name = beautifyChapterName(vol, chap, title)
        date_upload = created_at.parseDate()
        scanlator = group_name.joinToString().takeUnless { it.isBlank() } ?: "Unknown"
    }
}

@Serializable
data class PageList(
    val chapter: ChapterPageData,
)

@Serializable
data class ChapterPageData(
    val images: List<Page>,
)

@Serializable
data class Page(
    val url: String? = null,
)
