package eu.kanade.tachiyomi.extension.id.worldromancetranslation

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import okhttp3.Headers
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class WorldRomanceTranslation : WPMangaReader("World Romance Translation", "https://wrt.my.id", "id", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("id"))) {
    override val projectPageString = "/project-wrt"

    override val hasProjectPage = true

    override fun headersBuilder(): Headers.Builder {
        return super.headersBuilder().add("Referer", baseUrl)
    }

    override fun mangaDetailsParse(document: Document) = super.mangaDetailsParse(document).apply {
        thumbnail_url = document.select(seriesThumbnailSelector).attr("abs:data-lazy-src")
    }
}
