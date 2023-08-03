package eu.kanade.tachiyomi.extension.all.comickfun

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchManga(
    val hid: String,
    val title: String,
    @SerialName("md_covers") val mdCovers: List<MDcovers> = emptyList(),
    @SerialName("cover_url") val cover: String? = null,

) {
    fun toSManga() = SManga.create().apply {
        // appending # at end as part of migration from slug to hid
        url = "/comic/$hid#"
        title = this@SearchManga.title
        thumbnail_url = parseCover(cover, mdCovers)
    }
}

@Serializable
data class Manga(
    val comic: Comic,
    val artists: List<Name> = emptyList(),
    val authors: List<Name> = emptyList(),
    val genres: List<Name> = emptyList(),
) {
    fun toSManga() = SManga.create().apply {
        // appennding # at end as part of migration from slug to hid
        url = "/comic/${comic.hid}#"
        title = comic.title
        description = comic.desc?.beautifyDescription()
        if (comic.altTitles.isNotEmpty()) {
            if (description.isNullOrEmpty()) {
                description = "Alternative Titles:\n"
            } else {
                description += "\n\nAlternative Titles:\n"
            }

            description += comic.altTitles.mapNotNull { title ->
                title.title?.let { "• $it" }
            }.joinToString("\n")
        }
        status = comic.status.parseStatus(comic.translationComplete)
        thumbnail_url = parseCover(comic.cover, comic.mdCovers)
        artist = artists.joinToString { it.name.trim() }
        author = authors.joinToString { it.name.trim() }
        genre = (listOfNotNull(comic.origination) + genres)
            .joinToString { it.name.trim() }
    }
}

@Serializable
data class Comic(
    val hid: String,
    val title: String,
    val country: String? = null,
    @SerialName("md_titles") val altTitles: List<Title> = emptyList(),
    val desc: String? = null,
    val status: Int? = 0,
    @SerialName("translation_completed") val translationComplete: Boolean? = true,
    @SerialName("md_covers") val mdCovers: List<MDcovers> = emptyList(),
    @SerialName("cover_url") val cover: String? = null,
) {
    val origination = when (country) {
        "jp" -> Name("Manga")
        "kr" -> Name("Manhwa")
        "cn" -> Name("Manhua")
        else -> null
    }
}

@Serializable
data class MDcovers(
    val b2key: String?,
)

@Serializable
data class Title(
    val title: String?,
)

@Serializable
data class Name(
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
    @SerialName("created_at") val createdAt: String = "",
    val chap: String = "",
    val vol: String = "",
    @SerialName("group_name") val groups: List<String> = emptyList(),
) {
    fun toSChapter(mangaUrl: String) = SChapter.create().apply {
        url = "$mangaUrl/$hid-chapter-$chap-$lang"
        name = beautifyChapterName(vol, chap, title)
        date_upload = createdAt.parseDate()
        scanlator = groups.joinToString().takeUnless { it.isBlank() } ?: "Unknown"
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
