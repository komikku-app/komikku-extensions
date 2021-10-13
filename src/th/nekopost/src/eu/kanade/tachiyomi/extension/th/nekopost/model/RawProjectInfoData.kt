package eu.kanade.tachiyomi.extension.th.nekopost.model

import com.google.gson.annotations.SerializedName

data class RawProjectInfoData(
    @SerializedName("artist_name")
    val artistName: String,
    @SerializedName("author_name")
    val authorName: String,
    @SerializedName("np_comment")
    val npComment: String,
    @SerializedName("np_created_date")
    val npCreatedDate: String,
    @SerializedName("np_flag_mature")
    val npFlagMature: String,
    @SerializedName("np_info")
    val npInfo: String,
    @SerializedName("np_licenced_by")
    val npLicencedBy: String,
    @SerializedName("np_name")
    val npName: String,
    @SerializedName("np_name_link")
    val npNameLink: String,
    @SerializedName("np_project_id")
    val npProjectId: String,
    @SerializedName("np_status")
    val npStatus: String,
    @SerializedName("np_type")
    val npType: String,
    @SerializedName("np_updated_date")
    val npUpdatedDate: String,
    @SerializedName("np_view")
    val npView: String,
    @SerializedName("np_web")
    val npWeb: String
)
