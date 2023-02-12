package eu.kanade.tachiyomi.extension.pt.bakai

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class Bakai : ParsedHttpSource() {

    override val name = "Bakai"

    override val baseUrl = "https://bakai.org"

    override val lang = "pt-BR"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimitHost(baseUrl.toHttpUrl(), 1, 2)
        .rateLimitHost(CDN_URL.toHttpUrl(), 1, 1)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", "$baseUrl/")

    // Source doesn't have a popular list, so use latest instead.
    override fun popularMangaRequest(page: Int): Request = latestUpdatesRequest(page)

    override fun popularMangaSelector(): String = latestUpdatesSelector()

    override fun popularMangaFromElement(element: Element): SManga = latestUpdatesFromElement(element)

    override fun popularMangaNextPageSelector(): String = latestUpdatesNextPageSelector()

    override fun latestUpdatesRequest(page: Int): Request {
        val path = if (page > 1) "home/page/$page/" else ""
        return GET("$baseUrl/$path", headers)
    }

    override fun latestUpdatesSelector() = "#elCmsPageWrap ul.ipsGrid article.ipsBox"

    override fun latestUpdatesFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("h2.ipsType_pageTitle a")!!.text().trim()
        thumbnail_url = element.selectFirst("img.ipsImage[alt]")!!.attr("abs:src")
        setUrlWithoutDomain(element.selectFirst("a[title]")!!.attr("href"))
    }

    override fun latestUpdatesNextPageSelector() =
        "#elCmsPageWrap ul.ipsPagination li.ipsPagination_next:not(.ipsPagination_inactive)"

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/search/".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("type", "cms_records1")
            .addQueryParameter("search_in", "titles")
            .addQueryParameter("sortby", "relevancy")
            .toString()

        return GET(url, headers)
    }

    override fun searchMangaSelector() = "#elSearch_main ol.ipsStream li.ipsStreamItem"

    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("h2.ipsStreamItem_title a")!!.text().trim()
        thumbnail_url = element.selectFirst("span.ipsThumb img")!!.attr("abs:src")
        setUrlWithoutDomain(element.selectFirst("h2.ipsStreamItem_title a")!!.attr("href"))
    }

    override fun searchMangaNextPageSelector(): String? = null

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val infoElement = document.selectFirst(chapterListSelector())!!

        author = infoElement.select("p:contains(Artista:) a")
            .joinToString { it.text().trim() }
        genre = infoElement.selectFirst("p:contains(Tags:) span.ipsBadge + span")!!.text()
        status = SManga.COMPLETED
        description = infoElement.select("section.ipsType_richText p")
            .joinToString("\n\n") { it.text().trim() }
        thumbnail_url = infoElement.selectFirst("div.cCmsRecord_image img.ipsImage")!!.attr("abs:src")
    }

    override fun chapterListSelector() = "#ipsLayout_contentWrapper article.ipsContained"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.selectFirst("p:contains(Tipo:) a")!!.text()
        scanlator = element.select("p:contains(Tradução:) a").firstOrNull()
            ?.text()?.trim()
        date_upload = element.ownerDocument()!!.select("div.ipsPageHeader__meta time").firstOrNull()
            ?.attr("datetime")?.toDate() ?: 0L
        setUrlWithoutDomain(element.ownerDocument()!!.location())
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select(chapterListSelector() + " div.ipsGrid img.ipsImage")
            .mapIndexed { i, element ->
                Page(i, document.location(), element.attr("abs:data-src"))
            }
    }

    override fun imageUrlParse(document: Document) = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Accept", ACCEPT_IMAGE)
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(trim())?.time }
            .getOrNull() ?: 0L
    }

    companion object {
        private const val CDN_URL = "https://img.bakai.org"

        private const val ACCEPT_IMAGE = "image/webp,image/apng,image/*,*/*;q=0.8"

        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        }
    }
}
