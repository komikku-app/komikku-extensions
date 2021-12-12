package eu.kanade.tachiyomi.extension.th.nekopost.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RawProjectInfoData(
    @SerialName("artist_name")
    val artistName: String,
    @SerialName("author_name")
    val authorName: String,
    @SerialName("np_comment")
    val npComment: String,
    @SerialName("np_created_date")
    val npCreatedDate: String,
    @SerialName("np_flag_mature")
    val npFlagMature: String,
    @SerialName("np_info")
    val npInfo: String,
    @SerialName("np_licenced_by")
    val npLicencedBy: String,
    @SerialName("np_name")
    val npName: String,
    @SerialName("np_name_link")
    val npNameLink: String,
    @SerialName("np_project_id")
    val npProjectId: String,
    @SerialName("np_status")
    val npStatus: String,
    @SerialName("np_type")
    val npType: String,
    @SerialName("np_updated_date")
    val npUpdatedDate: String,
    @SerialName("np_view")
    val npView: String,
    @SerialName("np_web")
    val npWeb: String
)
