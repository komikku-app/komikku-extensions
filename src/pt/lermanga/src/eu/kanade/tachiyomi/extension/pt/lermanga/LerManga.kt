package eu.kanade.tachiyomi.extension.pt.lermanga

import android.util.Base64
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class LerManga : ParsedHttpSource() {

    override val name = "Ler Mangá"

    override val baseUrl = "https://lermanga.org"

    override val lang = "pt-BR"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    private val json: Json by injectLazy()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request {
        val path = if (page > 1) "page/$page/" else ""
        return GET("$baseUrl/mangas/$path?orderby=views&order=desc", headers)
    }

    override fun popularMangaSelector(): String = "div.film_list div.flw-item"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("h3.film-name")!!.text()
        thumbnail_url = element.selectFirst("img.film-poster-img")!!.srcAttr()
        setUrlWithoutDomain(element.selectFirst("a.dynamic-name")!!.attr("href"))
    }

    override fun popularMangaNextPageSelector(): String = "div.wp-pagenavi > a:last-child"

    override fun latestUpdatesRequest(page: Int): Request = GET(baseUrl, headers)

    override fun latestUpdatesSelector() = "div.capitulo_recentehome"

    override fun latestUpdatesFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("h3")!!.text()
        thumbnail_url = element.selectFirst("img")!!.absUrl("data-src")
        setUrlWithoutDomain(element.selectFirst("h3 > a")!!.attr("href"))
    }

    override fun latestUpdatesNextPageSelector(): String? = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val path = if (page > 1) "page/$page/" else ""
        val url = "$baseUrl/$path".toHttpUrl().newBuilder()
            .addQueryParameter("s", query)
            .build()

        return GET(url, headers)
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val infoElement = document.selectFirst("div.capitulo_recente")!!

        title = document.select("title").text().substringBeforeLast(" - ")
        genre = infoElement.select("ul.genre-list li a")
            .joinToString { it.text() }
        description = infoElement.selectFirst("div.boxAnimeSobreLast p:last-child")!!.ownText()
        thumbnail_url = infoElement.selectFirst("div.capaMangaInfo img")!!.absUrl("src")
    }

    override fun chapterListSelector() = "div.manga-chapters div.single-chapter"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.selectFirst("a")!!.text()
        date_upload = element.selectFirst("small small")!!.text().toDate()
        setUrlWithoutDomain(element.selectFirst("a")!!.attr("href"))
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.selectFirst("h1.heading-header + script[src^=data]")!!
            .attr("src")
            .substringAfter("base64,")
            .let { Base64.decode(it, Base64.DEFAULT).toString(charset("UTF-8")) }
            .substringAfter("var imagens_cap=")
            .let { json.decodeFromString<List<String>>(it) }
            .mapIndexed { index, imageUrl ->
                Page(index, document.location(), imageUrl)
            }
    }

    override fun imageUrlParse(document: Document) = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private fun Element.srcAttr(): String = when {
        hasAttr("data-src") -> absUrl("data-src")
        else -> absUrl("src")
    }

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(trim())?.time }
            .getOrNull() ?: 0L
    }

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("dd-MM-yyyy", Locale("pt", "BR"))
        }
    }
}
