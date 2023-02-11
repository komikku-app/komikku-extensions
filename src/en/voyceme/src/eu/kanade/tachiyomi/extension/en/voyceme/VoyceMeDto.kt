package eu.kanade.tachiyomi.extension.en.voyceme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoyceMeComic(
    val author: VoyceMeAuthor? = null,
    val chapters: List<VoyceMeChapter> = emptyList(),
    val description: String? = "",
    val genres: List<VoyceMeGenreAggregation> = emptyList(),
    val id: Int = -1,
    val slug: String = "",
    val status: String? = "",
    val thumbnail: String = "",
    val title: String = "",
)

@Serializable
data class VoyceMeAuthor(
    val username: String? = "",
)

@Serializable
data class VoyceMeGenreAggregation(
    val genre: VoyceMeGenre? = null,
)

@Serializable
data class VoyceMeGenre(
    val title: String? = "",
)

@Serializable
data class VoyceMeChapter(
    @SerialName("created_at") val createdAt: String = "",
    val id: Int = -1,
    val images: List<VoyceMePage> = emptyList(),
    val title: String = "",
)

@Serializable
data class VoyceMePage(
    val image: String = "",
)
