package eu.kanade.tachiyomi.extension.en.graphitecomics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GraphiteComic(
    val creator: List<GraphitePerson> = emptyList(),
    val description: String = "",
    val genres: List<GraphiteGenre> = emptyList(),
    @SerialName("objectId") val id: String = "",
    val logo: GraphiteComicImage? = null,
    val name: String = "",
    val publisher: GraphitePublisher? = null,
    @SerialName("publisher_slug") val publisherSlug: String = "",
    val slug: String = ""
)

@Serializable
data class GraphiteComicImage(
    val url: String = ""
)

@Serializable
data class GraphitePerson(
    val name: String = ""
)

@Serializable
data class GraphiteGenre(
    @SerialName("genreName") val name: String = ""
)

@Serializable
data class GraphitePublisher(
    val name: String = ""
)

@Serializable
data class GraphiteIssue(
    val accessRule: String? = "",
    val createdAt: String = "",
    val name: String = "",
    val number: Int = -1,
    val pages: List<GraphitePage> = emptyList(),
    @SerialName("publisher_slug") val publisherSlug: String = "",
    val slug: String = "",
    @SerialName("title_slug") val titleSlug: String = "",
    @SerialName("volume_number") val volumeNumber: Int = -1
)

@Serializable
data class GraphitePage(
    @SerialName("objectId") val id: String = "",
    val isEncrypted: Boolean = false
)
