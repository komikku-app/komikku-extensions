package eu.kanade.tachiyomi.extension.th.nekopost.model

import kotlinx.serialization.Serializable

@Serializable
data class RawChapterInfo(
    val chapterId: Int,
    val chapterNo: String,
    val pageCount: Int,
    val pageItem: List<RawPageItem>,
    val projectId: String
)
