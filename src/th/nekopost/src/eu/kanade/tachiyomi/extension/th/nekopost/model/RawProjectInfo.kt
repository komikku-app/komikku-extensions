package eu.kanade.tachiyomi.extension.th.nekopost.model

import com.google.gson.annotations.SerializedName

data class RawProjectInfo(
    @SerializedName("code")
    val code: String,
    @SerializedName("projectCategoryUsed")
    val projectCategoryUsed: List<RawProjectCategory>,
    @SerializedName("projectChapterList")
    val projectChapterList: List<RawProjectChapter>,
    @SerializedName("projectInfo")
    val projectData: RawProjectInfoData
)
