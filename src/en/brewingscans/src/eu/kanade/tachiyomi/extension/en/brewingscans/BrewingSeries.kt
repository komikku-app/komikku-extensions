package eu.kanade.tachiyomi.extension.en.brewingscans

@kotlinx.serialization.Serializable
data class BrewingSeries(
    val id: String? = null,
    val title: String,
    val description: String,
    val view_count: Int,
    val age: String,
    val chapters: Map<String, String>,
    val author: String? = null,
    val artist: String? = null,
    val genres: List<String>? = null,
) {
    val cover by lazy {
        if (id == null) null else "https://brewingscans.b-cdn.net/covers/${id}_cover.jpg"
    }
}
