package eu.kanade.tachiyomi.extension.en.kouhaiwork

import kotlinx.serialization.Serializable

@Serializable
data class KouhaiSeries(
    val id: Int,
    val title: String,
    val cover: String,
    val synopsis: String,
    val status: String,
    val artists: List<String>,
    val authors: List<String>,
    val alternative_titles: List<String>,
    val genres: List<String>? = null,
    val themes: List<String>? = null,
    val demographics: List<String>? = null,
    val chapters: List<KouhaiChapter>
)

@Serializable
data class KouhaiChapter(
    val id: Int,
    val group: String,
    val number: Float,
    val updated_at: String,
    val name: String? = null
)

@Serializable
data class KouhaiTag(val id: Int)

@Serializable
data class KouhaiTagList(
    val genres: List<KouhaiTag>,
    val themes: List<KouhaiTag>,
    val demographics: List<KouhaiTag>,
    val status: KouhaiTag?
)
