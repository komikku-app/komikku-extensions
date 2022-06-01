package eu.kanade.tachiyomi.extension.id.manhwaindo

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Headers
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class ManhwaIndo : WPMangaReader(
    "Manhwa Indo", "https://manhwaindo.id", "id", "/series",
    SimpleDateFormat("MMMM dd, yyyy", Locale("id"))
) {

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", baseUrl)

    // manga details
    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        author = document.select(seriesAuthorSelector).firstOrNull()?.ownText()
        artist = document.select(seriesArtistSelector).firstOrNull()?.ownText()
        genre = document.select(seriesGenreSelector).joinToString { it.text() }
        status = parseStatus(document.select(seriesStatusSelector).text())
        thumbnail_url = document.select(seriesThumbnailSelector).attr("abs:src")
        description = document.select(seriesDescriptionSelector).joinToString("\n") { it.text() }

        // add series type(manga/manhwa/manhua/other) thinggy to genre
        document.select(seriesTypeSelector).firstOrNull()?.ownText()?.let {
            if (it.isEmpty().not() && genre!!.contains(it, true).not()) {
                genre += if (genre!!.isEmpty()) it else ", $it"
            }
        }

        // add alternative name to manga description
        document.select(altNameSelector).firstOrNull()?.ownText()?.let {
            if (it.isEmpty().not()) {
                description += when {
                    description!!.isEmpty() -> altName + it
                    else -> "\n\n$altName" + it
                }
            }
        }
    }

    override val hasProjectPage = true
}
