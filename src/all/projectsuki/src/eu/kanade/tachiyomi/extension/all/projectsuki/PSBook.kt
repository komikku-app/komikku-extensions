package eu.kanade.tachiyomi.extension.all.projectsuki

import org.jsoup.nodes.Element

data class PSBook(
    val imgElement: Element,
    val titleElement: Element,
    val title: String,
    val mangaID: String,
    val url: NormalizedURL,
)
