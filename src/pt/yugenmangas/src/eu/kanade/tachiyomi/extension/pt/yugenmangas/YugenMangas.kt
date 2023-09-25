package eu.kanade.tachiyomi.extension.pt.yugenmangas

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Changed the name from "YugenMangas" to "Yugen Mangás" when
 * the source was updated to handle their CMS changes, so no
 * `versionId` change is needed as the ID should be different to
 * force users to migrate.
 */
class YugenMangas : ParsedHttpSource() {

    override val name = "Yugen Mangás"

    override val baseUrl = "https://yugenmangas.net.br"

    override val lang = "pt-BR"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    private val json: Json by injectLazy()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request = GET(baseUrl, headers)

    override fun popularMangaSelector(): String = "div.popular div.swiper-wrapper a"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("h1")!!.text()
        thumbnail_url = element.selectFirst("img")!!.absUrl("src")
        url = element.attr("href")
    }

    override fun popularMangaNextPageSelector(): String? = null

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/updates/?page=$page", headers)
    }

    override fun latestUpdatesSelector() = "div.container-update-series div.card-series-updates"

    override fun latestUpdatesFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("a.title-serie h1")!!.text()
        thumbnail_url = element.selectFirst("img")!!.absUrl("src")
        url = element.selectFirst("a")!!.attr("href")
    }

    override fun latestUpdatesNextPageSelector() = "div.pagination a:contains(Próxima)"

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/api/series/list".toHttpUrl().newBuilder()
            .addQueryParameter("query", query)
            .build()

        return GET(url, headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<List<SearchResultDto>>(response.body.string())
        val matches = result.map {
            SManga.create().apply {
                title = it.name
                url = "/series/${it.slug}"
            }
        }

        return MangasPage(matches, hasNextPage = false)
    }

    override fun searchMangaSelector() = throw UnsupportedOperationException("Not used")

    override fun searchMangaFromElement(element: Element) = throw UnsupportedOperationException("Not used")

    override fun searchMangaNextPageSelector() = throw UnsupportedOperationException("Not used")

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val infoElement = document.selectFirst("div.main div.resume > div.sinopse")!!

        title = infoElement.selectFirst("div.title-name h1")!!.text()
        author = infoElement.selectFirst("div.author")!!.text()
        genre = infoElement.select("div.genero span").joinToString { it.text() }
        status = infoElement.selectFirst("div.lancamento p")!!.text().toStatus()
        description = infoElement.select("div.sinopse > p").text()
        thumbnail_url = document.selectFirst("div.content div.side div.top-side img")!!.absUrl("src")
    }

    override fun chapterListSelector() = "#listadecapitulos div.chapter a"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.selectFirst("span.chapter-title")!!.text()
        scanlator = element.selectFirst("div.end-chapter span")?.text()
        date_upload = element.selectFirst("span.chapter-lancado")!!.text().toDate()
        url = element.attr("href")
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.chapter-images > img[src]")
            .mapIndexed { index, element ->
                Page(index, document.location(), element.absUrl("src"))
            }
    }

    override fun imageUrlParse(document: Document) = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    @Serializable
    private data class SearchResultDto(val name: String, val slug: String)

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(trim())?.time }
            .getOrNull() ?: 0L
    }

    private fun String.toStatus() = when (this) {
        "ongoing" -> SManga.ONGOING
        "completed", "finished" -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("dd.MM.yyyy", Locale("pt", "BR"))
        }
    }
}
