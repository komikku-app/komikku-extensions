package eu.kanade.tachiyomi.extension.th.nekopost.model

import com.google.gson.annotations.SerializedName

data class RawProjectNameListItem(
    @SerializedName("np_name")
    val npName: String,
    @SerializedName("np_name_link")
    val npNameLink: String,
    @SerializedName("np_no_chapter")
    val npNoChapter: String,
    @SerializedName("np_project_id")
    val npProjectId: String,
    @SerializedName("np_status")
    val npStatus: String,
    @SerializedName("np_type")
    val npType: String
)
