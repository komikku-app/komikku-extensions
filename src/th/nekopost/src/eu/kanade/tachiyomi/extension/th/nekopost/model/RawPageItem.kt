package eu.kanade.tachiyomi.extension.th.nekopost.model

import kotlinx.serialization.Serializable

@Serializable
data class RawPageItem(
    val id: Int? = null,
    // workaround for https://github.com/Kotlin/kotlinx.serialization/pull/1473
    val fileName: String? = null,
    val pageName: String? = null,
    val height: Int,
    val pageNo: Int,
    val width: Int
)
