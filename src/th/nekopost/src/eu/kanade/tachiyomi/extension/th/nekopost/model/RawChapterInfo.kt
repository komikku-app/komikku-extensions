package eu.kanade.tachiyomi.extension.th.nekopost.model

import com.google.gson.annotations.SerializedName

data class RawChapterInfo(
    @SerializedName("chapterId")
    val chapterId: Int,
    @SerializedName("chapterNo")
    val chapterNo: String,
    @SerializedName("pageCount")
    val pageCount: Int,
    @SerializedName("pageItem")
    val pageItem: List<RawPageItem>,
    @SerializedName("projectId")
    val projectId: String
)
