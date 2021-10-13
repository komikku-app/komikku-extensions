package eu.kanade.tachiyomi.extension.th.nekopost.model

import com.google.gson.annotations.SerializedName

data class RawProjectSummary(
    @SerializedName("nc_chapter_cover")
    val ncChapterCover: String,
    @SerializedName("nc_chapter_id")
    val ncChapterId: String,
    @SerializedName("nc_chapter_name")
    val ncChapterName: String,
    @SerializedName("nc_chapter_no")
    val ncChapterNo: String,
    @SerializedName("nc_created_date")
    val ncCreatedDate: String,
    @SerializedName("nc_provider")
    val ncProvider: String,
    @SerializedName("no_new_chapter")
    val noNewChapter: String,
    @SerializedName("np_group_dir")
    val npGroupDir: String,
    @SerializedName("np_name")
    val npName: String,
    @SerializedName("np_name_link")
    val npNameLink: String,
    @SerializedName("np_project_id")
    val npProjectId: String
)
