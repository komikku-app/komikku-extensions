package eu.kanade.tachiyomi.extension.en.koushoku

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.HttpUrl
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
    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(KoushokuWebViewInterceptor())
        .rateLimit(1, 4)
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
        .add("Referer", "$baseUrl/")

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

        val filterList = if (filters.isEmpty()) getFilterList() else filters
        filterList.findInstance<SortFilter>()?.addQueryParameter(url)
        url.addQueryParameter("q", buildAdvQuery(query, filterList))
        return GET(url.toString(), headers)
    }

    private fun buildAdvQuery(query: String, filterList: FilterList): String {
        val title = if (query.isNotBlank()) "title*:\"$query\" " else ""
        val filters: List<String> = filterList.filterIsInstance<Filter.Text>().map { filter ->
            if (filter.state.isBlank()) return@map ""
            val included = mutableListOf<String>()
            val excluded = mutableListOf<String>()
            val name = if (filter.name.lowercase().contentEquals("tags")) "tag" else filter.name.lowercase()
            filter.state.split(",").map(String::trim).filterNot(String::isBlank).forEach { entry ->
                if (entry.startsWith("-")) {
                    excluded.add(entry.slice(1 until entry.length))
                } else {
                    included.add(entry)
                }
            }
            buildString {
                if (included.isNotEmpty()) append("$name&*:\"${included.joinToString(",")}\" ")
                if (excluded.isNotEmpty()) append("-$name&*:\"${excluded.joinToString(",")}\"")
            }
        }
        return "$title${
        filters.filterNot(String::isBlank).joinToString(" ", transform = String::trim)
        }"
    }

    override fun searchMangaSelector() = latestUpdatesSelector()
    override fun searchMangaNextPageSelector() = latestUpdatesNextPageSelector()
    override fun searchMangaFromElement(element: Element) = latestUpdatesFromElement(element)

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/popular?page=$page", headers)
    override fun popularMangaSelector() = latestUpdatesSelector()
    override fun popularMangaNextPageSelector() = latestUpdatesNextPageSelector()
    override fun popularMangaFromElement(element: Element) = latestUpdatesFromElement(element)

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return if (!manga.initialized) {
            super.fetchMangaDetails(manga)
        } else {
            Observable.just(manga)
        }
    }

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        title = document.selectFirst(".metadata .title").text()

        // Reuse cover from browse
        thumbnail_url = document.selectFirst(thumbnailSelector).attr("src")
            .replace(Regex("/\\d+\\.webp\$"), "/288.webp")

        artist = document.select(".metadata .artists a, .metadata .circles a")
            .joinToString { it.text() }
        author = artist
        genre = document.select(".metadata .tags a, $magazinesSelector")
            .ifEmpty { null }?.joinToString { it.text() }
        description = getDesc(document)
        status = SManga.COMPLETED
    }

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

    override fun pageListRequest(chapter: SChapter) = GET("$baseUrl${chapter.url}/1", headers)

    override fun pageListParse(document: Document): List<Page> {
        val totalPages = document.selectFirst(".total").text().toInt()
        if (totalPages == 0)
            throw UnsupportedOperationException("Error: Empty pages (try Webview)")

        val id = archiveRegex.find(document.location())?.groups?.get(1)?.value
        if (id.isNullOrEmpty())
            throw UnsupportedOperationException("Error: Unknown archive id")

        val url = URL(document.selectFirst(".main img, main img").attr("src"))
        val origin = "${url.protocol}://${url.host}"

        return (1..totalPages).map {
            Page(it, "", "$origin/data/$id/$it.jpg")
        }
    }

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .add("Origin", baseUrl)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used")

    override fun getFilterList() = FilterList(
        SortFilter(
            "Sort",
            arrayOf(
                Sortable("ID", "id"),
                Sortable("Title", "title"),
                Sortable("Created Date", "created_at"),
                Sortable("Uploaded Date", "published_at"),
                Sortable("Pages", "pages"),
            )
        ),
        Filter.Header("Separate tags with commas (,)"),
        Filter.Header("Prepend with dash (-) to exclude"),
        ArtistFilter(),
        CircleFilter(),
        MagazineFilter(),
        ParodyFilter(),
        TagFilter(),
        PagesFilter()
    )

    // Adapted from Mangadex ext
    class SortFilter(displayName: String, private val sortables: Array<Sortable>) :
        Filter.Sort(
            displayName,
            sortables.map(Sortable::title).toTypedArray(),
            Selection(2, false)
        ) {
        fun addQueryParameter(url: HttpUrl.Builder) {
            if (state != null) {
                val sort = sortables[state!!.index].value
                val order = when (state!!.ascending) {
                    true -> "asc"
                    false -> "desc"
                }

                url.addQueryParameter("sort", sort)
                url.addQueryParameter("order", order)
            }
        }
    }

    data class Sortable(val title: String, val value: String) {
        override fun toString(): String = title
    }

    class ArtistFilter : Filter.Text("Artist")
    class CircleFilter : Filter.Text("Circle")
    class MagazineFilter : Filter.Text("Magazine")
    class ParodyFilter : Filter.Text("Parody")
    class TagFilter : Filter.Text("Tags")
    class PagesFilter : Filter.Text("Pages")

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
