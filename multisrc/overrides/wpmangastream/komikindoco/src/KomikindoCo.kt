package eu.kanade.tachiyomi.extension.id.komikindoco

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class KomikindoCo : WPMangaStream("KomikIndo.co", "https://komikindo.co", "id", SimpleDateFormat("MMM dd, yyyy", Locale("id"))) {
    // Formerly "Komikindo.co"
    override val id = 734619124437406170

    private val rateLimitInterceptor = RateLimitInterceptor(4)

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(rateLimitInterceptor)
        .build()

    override val hasProjectPage = true

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            document.select(".seriestucontent").firstOrNull()?.let { infoElement ->
                genre = infoElement.select(".seriestugenre a").joinToString { it.text() }
                status = parseStatus(infoElement.select(".infotable tr:contains(Status) td:last-child").firstOrNull()?.ownText())
                author = infoElement.select(".infotable tr:contains(Author) td:last-child").firstOrNull()?.ownText()
                description = infoElement.select(".entry-content-single[itemprop=\"description\"]").joinToString("\n") { it.text() }
                thumbnail_url = infoElement.select("div.thumb img").imgAttr()

                // add series type(manga/manhwa/manhua/other) thinggy to genre
                document.select(seriesTypeSelector).firstOrNull()?.ownText()?.let {
                    if (it.isEmpty().not() && genre!!.contains(it, true).not()) {
                        genre += if (genre!!.isEmpty()) it else ", $it"
                    }
                }

                // add alternative name to manga description
                document.select(altNameSelector).firstOrNull()?.ownText()?.let {
                    if (it.isBlank().not() && it != "N/A" && it != "-") {
                        description = when {
                            description.isNullOrBlank() -> altName + it
                            else -> description + "\n\n$altName" + it
                        }
                    }
                }
            }
        }
    }

    private val json: Json by injectLazy()

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        document.select(pageSelector)
            .filterNot { it.attr("src").isNullOrEmpty() }
            .mapIndexed { i, img -> pages.add(Page(i, "", img.attr("abs:src"))) }

        // Some sites like mangakita now load pages via javascript
        if (pages.isNotEmpty()) { return pages }

        val docString = document.toString()
        val imageListRegex = Regex("\\\"images.*?:.*?(\\[.*?\\])")
        val imageListJson = imageListRegex.find(docString)!!.destructured.toList()[0]

        val imageList = json.parseToJsonElement(imageListJson).jsonArray

        pages += imageList.mapIndexed { i, jsonEl ->
            Page(i, "", jsonEl.jsonPrimitive.content)
        }

        return pages
    }
}
