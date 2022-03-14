package eu.kanade.tachiyomi.extension.en.koushoku

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.net.URL

class Koushoku : ParsedHttpSource() {
    companion object {
        const val PREFIX_ID_SEARCH = "id:"

        val archiveRegex = "/archive/(\\d+)".toRegex()
        const val thumbnailSelector = ".thumbnail img"
        const val magazinesSelector = ".metadata .magazines a"
    }

    override val baseUrl = "https://koushoku.org"
    override val name = "Koushoku"
    override val lang = "en"
    override val supportsLatest = false

    private val rateLimitInterceptor = RateLimitInterceptor(5)
    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addNetworkInterceptor(rateLimitInterceptor)
        .build()

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/?page=$page", headers)
    override fun latestUpdatesSelector() = "#archives.feed .entries > .entry"
    override fun latestUpdatesNextPageSelector() = "#archives.feed .pagination .next"

    override fun latestUpdatesFromElement(element: Element) = SManga.create().apply {
        setUrlWithoutDomain(element.select("a").attr("href"))
        title = element.select(".title").text()
        thumbnail_url = element.select(thumbnailSelector).attr("src")
    }

    private fun searchMangaByIdRequest(id: String) = GET("$baseUrl/archive/$id", headers)

    // taken from Tsumino ext
    private fun searchMangaByIdParse(response: Response, id: String): MangasPage {
        val details = mangaDetailsParse(response)
        details.url = "/archive/$id"
        return MangasPage(listOf(details), false)
    }

    // taken from Tsumino ext
    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList
    ): Observable<MangasPage> {
        return if (query.startsWith(PREFIX_ID_SEARCH)) {
            val id = query.removePrefix(PREFIX_ID_SEARCH)
            client.newCall(searchMangaByIdRequest(id)).asObservableSuccess()
                .map { response -> searchMangaByIdParse(response, id) }
        } else {
            super.fetchSearchManga(page, query, filters)
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/search".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("q", query)

        val filterList = if (filters.isEmpty()) getFilterList() else filters
        filterList.findInstance<SortFilter>()?.let {
            url.addQueryParameter("sort", it.toUriPart())
        }
        filterList.findInstance<OrderFilter>()?.let {
            url.addQueryParameter("order", it.toUriPart())
        }

        return GET(url.toString(), headers)
    }

    override fun searchMangaSelector() = latestUpdatesSelector()
    override fun searchMangaNextPageSelector() = latestUpdatesNextPageSelector()
    override fun searchMangaFromElement(element: Element) = latestUpdatesFromElement(element)

    override fun popularMangaRequest(page: Int) = latestUpdatesRequest(page)
    override fun popularMangaSelector() = latestUpdatesSelector()
    override fun popularMangaNextPageSelector() = latestUpdatesNextPageSelector()
    override fun popularMangaFromElement(element: Element) = latestUpdatesFromElement(element)

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        title = document.select(".metadata .title").text()
        thumbnail_url = document.select(thumbnailSelector).attr("src")
        artist = document.select(".metadata .artists a, .metadata .circles a")
            .joinToString { it.text() }
        author = artist
        genre = document.select(".metadata .tags a, $magazinesSelector")
            .ifEmpty { null }?.joinToString { it.text() }
        description = getDesc(document)
        status = SManga.COMPLETED
    }

    override fun chapterListRequest(manga: SManga) = GET("$baseUrl${manga.url}", headers)

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return listOf(
            SChapter.create().apply {
                setUrlWithoutDomain(response.request.url.encodedPath)
                name = "Chapter"
                date_upload = document.select(".metadata .published td:nth-child(2)")
                    .attr("data-unix").toLong() * 1000
            }
        )
    }

    override fun chapterFromElement(element: Element) =
        throw UnsupportedOperationException("Not used")

    override fun chapterListSelector() = throw UnsupportedOperationException("Not used")

    override fun pageListRequest(chapter: SChapter) = GET("$baseUrl${chapter.url}/1")

    override fun pageListParse(document: Document): List<Page> {
        val totalPages = document.selectFirst(".total").text().toInt()
        if (totalPages == 0)
            throw UnsupportedOperationException("Error: Empty pages (try Webview)")

        val id = archiveRegex.find(document.location())?.groups?.get(1)?.value
        if (id.isNullOrEmpty())
            throw UnsupportedOperationException("Error: Unknown archive id")

        val url = URL(document.selectFirst(".page img").attr("src"))
        val origin = "${url.protocol}://${url.host}"

        return (1..totalPages).map {
            Page(it, "", "$origin/data/$id/$it.jpg")
        }
    }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used")

    override fun getFilterList() = FilterList(
        SortFilter(),
        OrderFilter()
    )

    private class SortFilter : UriPartFilter(
        "Sort",
        arrayOf(
            Pair("Created Date", "created_at"),
            Pair("ID", "id"),
            Pair("Title", "title"),
            Pair("Published Date", "published_at")
        )
    )

    private class OrderFilter : UriPartFilter(
        "Order",
        arrayOf(
            Pair("Descending", "desc"),
            Pair("Ascending", "asc"),
        )
    )

    // Taken from nhentai ext
    private open class UriPartFilter(displayName: String, val vals: Array<Pair<String, String>>) :
        Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray()) {
        fun toUriPart() = vals[state].second
    }

    // Taken from nhentai ext
    private inline fun <reified T> Iterable<*>.findInstance() = find { it is T } as? T

    private fun getDesc(document: Document) = buildString {
        val magazines = document.select(magazinesSelector)
        if (magazines.isNotEmpty()) {
            append("Magazines: ")
            append(magazines.joinToString { it.text() })
            append("\n")
        }

        val parodies = document.select(".metadata .parodies a")
        if (parodies.isNotEmpty()) {
            append("Parodies: ")
            append(parodies.joinToString { it.text() })
            append("\n")
        }

        val pages = document.selectFirst(".metadata .pages td:nth-child(2)")
        append("Pages: ").append(pages.text()).append("\n")

        val size = document.selectFirst(".metadata .size td:nth-child(2)")
        append("Size: ").append(size.text())
    }
}
