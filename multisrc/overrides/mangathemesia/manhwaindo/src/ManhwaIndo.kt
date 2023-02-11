package eu.kanade.tachiyomi.extension.id.manhwaindo

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import okhttp3.Headers
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class ManhwaIndo : MangaThemesia(
    "Manhwa Indo",
    "https://manhwaindo.id",
    "id",
    "/series",
    SimpleDateFormat("MMMM dd, yyyy", Locale("id")),
) {

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", baseUrl)

    override fun mangaDetailsParse(document: Document) = super.mangaDetailsParse(document).apply {
        thumbnail_url = document.select(seriesThumbnailSelector).attr("abs:src")
    }

    override val hasProjectPage = true
}
