package eu.kanade.tachiyomi.extension.en.comikey.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MangaDetailsDto(
    @SerialName("id")
    val id: Int? = null,
    @SerialName("link")
    val link: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("e4pid")
    val e4pid: String? = null,
    @SerialName("uslug")
    val uslug: String? = null,
    @SerialName("alt")
    val alt: String? = null,
    @SerialName("author")
    val author: List<Author?>? = null,
    @SerialName("artist")
    val artist: List<Artist?>? = null,
    @SerialName("adult")
    val adult: Boolean? = null,
    @SerialName("tags")
    val tags: List<Tag?>? = null,
    @SerialName("keywords")
    val keywords: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("excerpt")
    val excerpt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("modified_at")
    val modifiedAt: String? = null,
    @SerialName("publisher")
    val publisher: Publisher? = null,
    @SerialName("color")
    val color: String? = null,
    @SerialName("in_exclusive")
    val inExclusive: Boolean? = null,
    @SerialName("in_hype")
    val inHype: Boolean? = null,
    @SerialName("all_free")
    val allFree: Boolean? = null,
    @SerialName("availability_strategy")
    val availabilityStrategy: AvailabilityStrategy? = null,
    @SerialName("campaigns")
    val campaigns: List<String?>? = null, // unknown list element type, was null
    @SerialName("last_updated")
    val lastUpdated: String? = null,
    @SerialName("chapter_count")
    val chapterCount: Int? = null,
    @SerialName("update_status")
    val updateStatus: Int? = null,
    @SerialName("update_text")
    val updateText: String? = null,
    @SerialName("format")
    val format: Int? = null,
    @SerialName("cover")
    val cover: String? = null,
    @SerialName("logo")
    val logo: String? = null,
    @SerialName("banner")
    val banner: String? = null,
    @SerialName("showcase")
    val showcase: String? = null, // unknown type, was null
    @SerialName("preview")
    val preview: String? = null, // unknown type, was null
    @SerialName("chapter_title")
    val chapterTitle: String? = null,
    @SerialName("geoblocks")
    val geoblocks: String? = null
) {
    @Serializable
    data class Author(
        @SerialName("id")
        val id: Int? = null,
        @SerialName("name")
        val name: String? = null,
        @SerialName("alt")
        val alt: String? = null
    )

    @Serializable
    data class Artist(
        @SerialName("id")
        val id: Int? = null,
        @SerialName("name")
        val name: String? = null,
        @SerialName("alt")
        val alt: String? = null
    )

    @Serializable
    data class Tag(
        @SerialName("name")
        val name: String? = null,
        @SerialName("description")
        val description: String? = null,
        @SerialName("slug")
        val slug: String? = null,
        @SerialName("color")
        val color: String? = null,
        @SerialName("is_primary")
        val isPrimary: Boolean? = null
    )

    @Serializable
    data class Publisher(
        @SerialName("id")
        val id: Int? = null,
        @SerialName("name")
        val name: String? = null,
        @SerialName("language")
        val language: String? = null,
        @SerialName("homepage")
        val homepage: String? = null,
        @SerialName("logo")
        val logo: String? = null,
        @SerialName("geoblocks")
        val geoblocks: String? = null
    )

    @Serializable
    data class AvailabilityStrategy(
        @SerialName("starting_count")
        val startingCount: Int? = null,
        @SerialName("latest_only_free")
        val latestOnlyFree: Boolean? = null,
        @SerialName("catchup_count")
        val catchupCount: Int? = null,
        @SerialName("simulpub")
        val simulpub: Boolean? = null,
        @SerialName("fpf_becomes_paid")
        val fpfBecomesPaid: String? = null,
        @SerialName("fpf_becomes_free")
        val fpfBecomesFree: String? = null,
        @SerialName("fpf_becomes_backlog")
        val fpfBecomesBacklog: String? = null,
        @SerialName("backlog_becomes_backlog")
        val backlogBecomesBacklog: String? = null
    )
}
