package eu.kanade.tachiyomi.extension.th.nekopost.model

import com.google.gson.annotations.SerializedName

data class RawProjectChapter(
    @SerializedName("cu_displayname")
    val cuDisplayname: String,
    @SerializedName("nc_chapter_id")
    val ncChapterId: String,
    @SerializedName("nc_chapter_name")
    val ncChapterName: String,
    @SerializedName("nc_chapter_no")
    val ncChapterNo: String,
    @SerializedName("nc_created_date")
    val ncCreatedDate: String,
    @SerializedName("nc_data_file")
    val ncDataFile: String,
    @SerializedName("nc_owner_id")
    val ncOwnerId: String,
    @SerializedName("nc_provider")
    val ncProvider: String
)
