package eu.kanade.tachiyomi.extension.th.nekopost.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RawProjectChapter(
    @SerialName("cu_displayname")
    val cuDisplayname: String,
    @SerialName("nc_chapter_id")
    val ncChapterId: String,
    @SerialName("nc_chapter_name")
    val ncChapterName: String,
    @SerialName("nc_chapter_no")
    val ncChapterNo: String,
    @SerialName("nc_created_date")
    val ncCreatedDate: String,
    @SerialName("nc_data_file")
    val ncDataFile: String,
    @SerialName("nc_owner_id")
    val ncOwnerId: String,
    @SerialName("nc_provider")
    val ncProvider: String
)
