package eu.kanade.tachiyomi.extension.th.nekopost.model

import kotlinx.serialization.Serializable

@Serializable
data class RawPageItem(
    val fileName: String,
    val height: Int,
    val pageNo: Int,
    val width: Int
)
