package eu.kanade.tachiyomi.extension.en.hwtmanga

import kotlinx.serialization.Serializable

@Serializable
data class HWTQuery(
    val cimage: String,
    val postID: String,
    val title: String,
)

@Serializable
data class HWTMangaInfo(
    val cover: String,
    val desc: String,
    val mtag: HWTTag,
    val onames: String,
    val statue: Int,
    val postID: Int,
    val tags: List<HWTTag>,
    val title: String,
)

@Serializable
data class HWTTag(var value: String?)

@Serializable
data class HWTChapterList(
    private val chapterList: List<HWTChapter>,
) : List<HWTChapter> by chapterList

@Serializable
data class HWTChapter(
    val fid: String,
    val pid: String,
    val name: String,
    val cdate: String,
    val is_locked: String,
)

@Serializable
data class HWTPage(val base: String)
