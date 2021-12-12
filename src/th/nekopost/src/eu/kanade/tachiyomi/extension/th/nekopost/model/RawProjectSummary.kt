package eu.kanade.tachiyomi.extension.th.nekopost.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RawProjectSummary(
    @SerialName("nc_chapter_cover")
    val ncChapterCover: String,
    @SerialName("nc_chapter_id")
    val ncChapterId: String,
    @SerialName("nc_chapter_name")
    val ncChapterName: String,
    @SerialName("nc_chapter_no")
    val ncChapterNo: String,
    @SerialName("nc_created_date")
    val ncCreatedDate: String,
    @SerialName("nc_provider")
    val ncProvider: String,
    @SerialName("no_new_chapter")
    val noNewChapter: String,
    @SerialName("np_group_dir")
    val npGroupDir: String,
    @SerialName("np_name")
    val npName: String,
    @SerialName("np_name_link")
    val npNameLink: String,
    @SerialName("np_project_id")
    val npProjectId: String
)
