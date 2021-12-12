package eu.kanade.tachiyomi.extension.th.nekopost.model

import kotlinx.serialization.Serializable

@Serializable
data class RawProjectSummaryList(
    val code: String,
    val listItem: List<RawProjectSummary>?
)
