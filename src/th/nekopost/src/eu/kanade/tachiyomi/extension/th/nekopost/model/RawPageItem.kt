package eu.kanade.tachiyomi.extension.th.nekopost.model

import com.google.gson.annotations.SerializedName

data class RawPageItem(
    @SerializedName("fileName")
    val fileName: String,
    @SerializedName("height")
    val height: Int,
    @SerializedName("pageNo")
    val pageNo: Int,
    @SerializedName("width")
    val width: Int
)
