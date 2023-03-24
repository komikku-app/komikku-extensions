package eu.kanade.tachiyomi.extension.es.inarimanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class InariManga : Madara(
    "InariManga",
    "https://inarimanga.com",
    "es",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es")),
) {
    override val mangaDetailsSelectorDescription = "div.manga-summary"
    override val mangaDetailsSelectorThumbnail = "div.summary_image img.notUsed" // Dimensions of img are not suitable for Tachiyomi

    override val useLoadMoreSearch = false
}
