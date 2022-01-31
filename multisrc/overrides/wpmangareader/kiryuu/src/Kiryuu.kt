package eu.kanade.tachiyomi.extension.id.kiryuu

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class Kiryuu : WPMangaReader("Kiryuu", "https://kiryuu.id", "id", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("id"))) {
    // Formerly "Kiryuu (WP Manga Stream)"
    override val id = 3639673976007021338

    private val rateLimitInterceptor = RateLimitInterceptor(4)

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(rateLimitInterceptor)
        .build()

    // manga details
    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        author = document.select(".listinfo li:contains(Author), .tsinfo .imptdt:nth-child(4) i, .infotable tr:contains(author) td:last-child")
            .firstOrNull()?.ownText()

        artist = document.select(".infotable tr:contains(artist) td:last-child, .tsinfo .imptdt:contains(artist) i")
            .firstOrNull()?.ownText()

        genre = document.select("div.gnr a, .mgen a, .seriestugenre a").joinToString { it.text() }
        status = parseStatus(
            document.select("div.listinfo li:contains(Status), .tsinfo .imptdt:contains(status), .tsinfo .imptdt:contains(الحالة), .infotable tr:contains(status) td")
                .text()
        )

        title = document.selectFirst(".thumb img").attr("title")
        thumbnail_url = document.select(".thumb img").attr("src")
        description = document.select(".desc, .entry-content[itemprop=description]").joinToString("\n") { it.text() }

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
