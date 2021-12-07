package eu.kanade.tachiyomi.extension.all.asurascans

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class AsuraScansFactory : SourceFactory {
    override fun createSources() = listOf(
        AsuraScansEn(),
        AsuraScansTr()
    )
}

abstract class AsuraScans(
    override val baseUrl: String,
    override val lang: String,
    dateFormat: SimpleDateFormat
) : WPMangaStream("Asura Scans", baseUrl, lang, dateFormat) {
    private val rateLimitInterceptor = RateLimitInterceptor(1, 3, TimeUnit.SECONDS)

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(rateLimitInterceptor)
        .build()
}

class AsuraScansEn : AsuraScans("https://asurascans.com/", "en", SimpleDateFormat("MMM d, yyyy", Locale.US)) {
    override val pageSelector = "div.rdminimal > p > img"

    // Skip scriptPages
    override fun pageListParse(document: Document): List<Page> {
        return document.select(pageSelector)
            .filterNot { it.attr("src").isNullOrEmpty() }
            .mapIndexed { i, img -> Page(i, "", img.attr("src")) }
    }
}

class AsuraScansTr : AsuraScans("https://tr.asurascans.com/", "tr", SimpleDateFormat("MMM d, yyyy", Locale("tr"))) {
    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            document.select("div.bigcontent").firstOrNull()?.let { infoElement ->
                status = parseStatus(infoElement.select(".imptdt:contains(Durum) i").firstOrNull()?.ownText())
                infoElement.select(".fmed b:contains(Yazar)+span").firstOrNull()?.ownText().let {
                    if (it.isNullOrBlank().not() && it != "N/A" && it != "-") {
                        author = it
                    }
                }
                infoElement.select(".fmed b:contains(Çizer)+span").firstOrNull()?.ownText().let {
                    if (it.isNullOrBlank().not() && it != "N/A" && it != "-") {
                        artist = it
                    }
                }
                description = infoElement.select("div.desc p, div.entry-content p").joinToString("\n") { it.text() }
                thumbnail_url = infoElement.select("div.thumb img").imgAttr()

                val genres = infoElement.select(".mgen a")
                    .map { element -> element.text().toLowerCase(Locale.ROOT) }
                    .toMutableSet()

                // add series type(manga/manhwa/manhua/other) thinggy to genre
                document.select(seriesTypeSelector).firstOrNull()?.ownText()?.let {
                    if (it.isEmpty().not() && genres.contains(it).not()) {
                        genres.add(it.toLowerCase(Locale.ROOT))
                    }
                }

                genre = genres.toList().map { it.capitalize(Locale.ROOT) }.joinToString(", ")

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
    override val seriesTypeSelector = ".imptdt:contains(Tür) a"
    override val altName: String = "Alternatif isim: "

    override fun parseStatus(element: String?): Int = when {
        element == null -> SManga.UNKNOWN
        listOf("Devam Ediyor").any { it.contains(element, ignoreCase = true) } -> SManga.ONGOING
        listOf("Tamamlandı").any { it.contains(element, ignoreCase = true) } -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }
}
