package eu.kanade.tachiyomi.extension.th.nekopost.model

import com.google.gson.annotations.SerializedName

data class RawProjectCategory(
    @SerializedName("npc_name")
    val npcName: String,
    @SerializedName("npc_name_link")
    val npcNameLink: String
)
