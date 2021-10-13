package eu.kanade.tachiyomi.extension.th.nekopost.model

import com.google.gson.annotations.SerializedName

data class RawProjectSummaryList(
    @SerializedName("code")
    val code: String,
    @SerializedName("listItem")
    val listItem: List<RawProjectSummary>?
)
