package eu.kanade.tachiyomi.extension.en.koushoku

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
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
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random

class Koushoku : ParsedHttpSource() {
    companion object {
        const val PREFIX_ID_SEARCH = "id:"

        const val thumbnailSelector = "figure img"
        const val magazinesSelector = ".metadata a[href^='/magazines/']"

        private val PATTERN_IMAGES = "(.+/)(\\d+)(.*)".toRegex()
        private val DATE_FORMAT = SimpleDateFormat("E, d MMM yyy HH:mm:ss 'UTC'", Locale.US)
    }

    override val baseUrl = "https://koushoku.org"
    override val name = "Koushoku"
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(KoushokuWebViewInterceptor())
        // Site: 40req per 1 minute
        // Here: 1req per 2 sec -> 30req per 1 minute
        // (somewhat lower due to caching)
        .rateLimitHost("https://koushoku.org".toHttpUrl(), 1, 2)
        .build()

    override fun headersBuilder(): Headers.Builder {
        val chromeStableVersion = listOf("104.0.5112.69", "103.0.5060.71", "103.0.5060.70", "103.0.5060.53", "103.0.5060.129", "102.0.5005.99", "102.0.5005.98", "102.0.5005.78", "102.0.5005.125").random()
        val chromeCanaryVersion = listOf("106.0.5227.0", "106.0.5209.0", "106.0.5206.0", "106.0.5201.2", "106.0.5201.0", "106.0.5200.0", "106.0.5199.0", "106.0.5197.0", "106.0.5196.0", "105.0.5195.2", "105.0.5194.0", "105.0.5193.0", "105.0.5192.0", "105.0.5191.0", "105.0.5190.0", "105.0.5189.0", "105.0.5186.0", "105.0.5185.0", "105.0.5184.0", "105.0.5182.0", "105.0.5180.0", "105.0.5179.3", "105.0.5178.0", "105.0.5177.2", "105.0.5176.0", "105.0.5175.0", "105.0.5174.0", "105.0.5173.0", "105.0.5172.0", "105.0.5171.0").random()
        val chromeVersion = if (Random.nextFloat() > 0.2) chromeStableVersion else chromeCanaryVersion

        val deviceInfo = if (Random.nextFloat() > 0.2) "" else "; " + listOf("SM-S908B", "SM-S908U", "SM-A536B", "SM-A536U", "SM-S901B", "SM-S901U", "SM-A736B", "SM-G973F", "SM-A528B", "SM-G975U", "SM-G990B", "SM-G990U").random()
        val androidVersion = IntRange(if (deviceInfo.isEmpty()) 9 else 11, 12).random()

        return super.headersBuilder()
            .set("User-Agent", "Mozilla/5.0 (Linux; Android $androidVersion$deviceInfo) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Mobile Safari/537.36")
            .add("Referer", "$baseUrl/")
    }

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/?page=$page", headers)
    override fun latestUpdatesSelector() = "#archives.feed .entries > .entry"
    override fun latestUpdatesNextPageSelector() = "footer nav li:has(a.active) + li:not(:last-child) > a"

    override fun latestUpdatesFromElement(element: Element) = SManga.create().apply {
        setUrlWithoutDomain(element.selectFirst("a").attr("href"))
        title = element.selectFirst("[title]").attr("title")
        thumbnail_url = element.selectFirst(thumbnailSelector).absUrl("src")
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
        title = document.selectFirst(".metadata > h1").text()

        // Reuse cover from browse
        thumbnail_url = document.selectFirst(thumbnailSelector).absUrl("src")
            .replace(Regex("/\\d+\\.webp\$"), "/288.webp")

        artist = document.select(".metadata a[href^='/artists/'], .metadata a[href^='/circles/']")
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

                val dateText = document.select("tr > td:first-child:contains(Uploaded Date) + td")
                    .text()
                date_upload = runCatching { DATE_FORMAT.parse(dateText) }
                    .getOrNull()
                    ?.time
                    ?: 0
            }
        )
    }

    override fun chapterFromElement(element: Element) =
        throw UnsupportedOperationException("Not used")

    override fun chapterListSelector() = throw UnsupportedOperationException("Not used")

    override fun pageListRequest(chapter: SChapter) = GET("$baseUrl${chapter.url}/1", headers)

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()

        val totalPages = document.selectFirst(".total")?.text()?.toInt() ?: 0
        if (totalPages == 0)
            throw UnsupportedOperationException("Error: Empty pages (try Webview)")

        val match = PATTERN_IMAGES.find(response.request.url.toString())!!
        val prefix = match.groupValues[1]
        val suffix = match.groupValues[3]

        return (1..totalPages).map {
            Page(it, "$prefix$it$suffix")
        }
    }

    override fun pageListParse(document: Document): List<Page> =
        throw UnsupportedOperationException("Not used")

    override fun imageUrlParse(document: Document): String =
        document.selectFirst(".main img, main img").absUrl("src")

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

        val parodies = document.select(".metadata a[href^='/parodies/']")
        if (parodies.isNotEmpty()) {
            append("Parodies: ")
            append(parodies.joinToString { it.text() })
            append("\n")
        }

        val pages = document.selectFirst("tr > td:first-child:contains(Pages) + td")
        append("Pages: ").append(pages.text()).append("\n")

        val size: Element? = document.selectFirst("tr > td:first-child:contains(Size) + td")
        append("Size: ").append(size?.text() ?: "Unknown")
    }
}
