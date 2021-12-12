package eu.kanade.tachiyomi.extension.th.nekopost.model

import kotlinx.serialization.Serializable

@Serializable
data class RawProjectInfo(
    val code: String,
    val projectCategoryUsed: List<RawProjectCategory>,
    val projectChapterList: List<RawProjectChapter>,
    val projectInfo: RawProjectInfoData
)
