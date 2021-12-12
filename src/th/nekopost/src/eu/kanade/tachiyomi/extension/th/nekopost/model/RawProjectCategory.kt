package eu.kanade.tachiyomi.extension.th.nekopost.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RawProjectCategory(
    @SerialName("npc_name")
    val npcName: String,
    @SerialName("npc_name_link")
    val npcNameLink: String
)
