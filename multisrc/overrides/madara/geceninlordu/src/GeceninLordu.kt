package eu.kanade.tachiyomi.extension.tr.geceninlordu

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import java.text.SimpleDateFormat
import java.util.Locale

class GeceninLordu : Madara(
    "Gecenin Lordu",
    "https://geceninlordu.com/",
    "tr",
    SimpleDateFormat("dd MMM yyyy", Locale("tr")),
) {

    override val useLoadMoreSearch = false
    override val fetchGenres = false

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$baseUrl/?s=$query&post_type=wp-manga")
}
