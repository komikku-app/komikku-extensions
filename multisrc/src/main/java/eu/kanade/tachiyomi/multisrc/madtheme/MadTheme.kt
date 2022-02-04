package eu.kanade.tachiyomi.multisrc.madtheme

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

abstract class MadTheme(
    override val name: String,
    override val baseUrl: String,
    override val lang: String,
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("MMM dd, yyy", Locale.US)
) : ParsedHttpSource() {

    override val supportsLatest = true

    override fun headersBuilder() = Headers.Builder().apply {
        add("Referer", "$baseUrl/")
    }

    private val json: Json by injectLazy()

    // Popular
    override fun popularMangaRequest(page: Int): Request =
        searchMangaRequest(page, "", FilterList(OrderFilter(0)))

    override fun popularMangaParse(response: Response): MangasPage =
        searchMangaParse(response)

    override fun popularMangaSelector(): String =
        searchMangaSelector()

    override fun popularMangaFromElement(element: Element): SManga =
        searchMangaFromElement(element)

    override fun popularMangaNextPageSelector(): String? =
        searchMangaNextPageSelector()

    // Latest
    override fun latestUpdatesRequest(page: Int): Request =
        searchMangaRequest(page, "", FilterList(OrderFilter(1)))

    override fun latestUpdatesParse(response: Response): MangasPage =
        searchMangaParse(response)

    override fun latestUpdatesSelector(): String =
        searchMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga =
        searchMangaFromElement(element)

    override fun latestUpdatesNextPageSelector(): String? =
        searchMangaNextPageSelector()

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)

        filters.forEach { filter ->
            when (filter) {
                is GenreFilter -> {
                    filter.state
                        .filter { it.state }
                        .let { list ->
                            if (list.isNotEmpty()) {
                                list.forEach { genre -> url.addQueryParameter("genre[]", genre.id) }
                            }
                        }
                }
                is StatusFilter -> {
                    url.addQueryParameter("status", filter.toUriPart())
                }
                is OrderFilter -> {
                    url.addQueryParameter("sort", filter.toUriPart())
                }
                else -> {}
            }
        }

        return GET(url.toString(), headers)
    }

    override fun searchMangaSelector(): String = ".book-detailed-item"

    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        setUrlWithoutDomain(element.select("a").first()!!.attr("abs:href"))
        title = element.select("a").first()!!.attr("title")
        description = element.select(".summary").first()?.text()
        genre = element.select(".genres > *").joinToString { it.text() }
        thumbnail_url = element.select("img").first()!!.attr("abs:data-src")
    }

    override fun searchMangaNextPageSelector(): String? = ".paginator [rel=next]"

    // Details
    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.select(".detail h1").first()!!.text()
        author = document.select(".detail .meta > p > strong:contains(Authors) ~ a").joinToString { it.text().trim(',', ' ') }
        genre = document.select(".detail .meta > p > strong:contains(Genres) ~ a").joinToString { it.text().trim(',', ' ') }
        thumbnail_url = document.select("#cover img").first()!!.attr("abs:data-src")

        val altNames = document.select(".detail h2").first()?.text()
            ?.split(',', ';')
            ?.mapNotNull { it.trim().takeIf { it != title } }
            ?: listOf()

        description = document.select(".summary .content").first()?.text() +
            (altNames.takeIf { it.isNotEmpty() }?.let { "\n\nAlt name(s): ${it.joinToString()}" } ?: "")

        val statusText = document.select(".detail .meta > p > strong:contains(Status) ~ a").first()!!.text()
        status = when (statusText.toLowerCase(Locale.US)) {
            "ongoing" -> SManga.ONGOING
            "completed" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
    }

    // Chapters
    override fun chapterListRequest(manga: SManga): Request =
        GET("$baseUrl/api/manga${manga.url}/chapters?source=detail", headers)

    override fun searchMangaParse(response: Response): MangasPage {
        if (genresList == null) {
            genresList = parseGenres(response.asJsoup(response.peekBody(Long.MAX_VALUE).string()))
        }
        return super.searchMangaParse(response)
    }

    override fun chapterListSelector(): String = "#chapter-list > li"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        setUrlWithoutDomain(element.select("a").first()!!.attr("abs:href"))
        name = element.select(".chapter-title").first()!!.text()
        date_upload = parseChapterDate(element.select(".chapter-update").first()?.text())
        chapter_number = name.substringAfterLast(' ').toFloatOrNull() ?: -1f
    }

    // Pages
    override fun pageListParse(document: Document): List<Page> {
        val html = document.html()!!

        if (!html.contains("var mainServer = \"")) {
            // No fancy CDN
            return document.select("#chapter-images img").mapIndexed { index, element ->
                Page(index, "", element.attr("abs:data-src"))
            }
        }

        val scheme = baseUrl.toHttpUrl().scheme + "://"

        val mainCdn = html
            .substringAfter("var mainServer = \"")
            .substringBefore("\"")

        val mainCdnHttp = (scheme + mainCdn).toHttpUrl()
        CDN_URL = scheme + mainCdnHttp.host
        CDN_PATH = mainCdnHttp.encodedPath

        val chImages = html
            .substringAfter("var chapImages = '")
            .substringBefore("'")
            .split(',')

        if (html.contains("var multiServers = true")) {
            val altCDNs = json.decodeFromString<List<String>>(
                html
                    .substringAfter("var imageServers = ")
                    .substringBefore("\n")
            )
            CDN_URL_ALT = altCDNs.mapNotNull {
                val url = scheme + it
                if (!(CDN_URL!!).contains(url)) url else null
            }
        }

        val allCDN = listOf(CDN_URL) + CDN_URL_ALT
        return chImages.mapIndexed { index, img ->
            Page(index, "", allCDN.random() + CDN_PATH + img)
        }
    }

    // Image
    override fun imageUrlParse(document: Document): String =
        throw UnsupportedOperationException("Not used.")

    // Date logic lifted from Madara
    private fun parseChapterDate(date: String?): Long {
        date ?: return 0

        fun SimpleDateFormat.tryParse(string: String): Long {
            return try {
                parse(string)?.time ?: 0
            } catch (_: ParseException) {
                0
            }
        }

        return when {
            "ago".endsWith(date) -> {
                parseRelativeDate(date)
            }
            else -> dateFormat.tryParse(date)
        }
    }

    private fun parseRelativeDate(date: String): Long {
        val number = Regex("""(\d+)""").find(date)?.value?.toIntOrNull() ?: return 0
        val cal = Calendar.getInstance()

        return when {
            date.contains("day") -> cal.apply { add(Calendar.DAY_OF_MONTH, -number) }.timeInMillis
            date.contains("hour") -> cal.apply { add(Calendar.HOUR, -number) }.timeInMillis
            date.contains("minute") -> cal.apply { add(Calendar.MINUTE, -number) }.timeInMillis
            date.contains("second") -> cal.apply { add(Calendar.SECOND, -number) }.timeInMillis
            else -> 0
        }
    }

    // Dynamic genres
    private fun parseGenres(document: Document): List<Genre>? {
        return document.select(".checkbox-group.genres").first()?.select("label")?.map {
            Genre(it.select(".radio__label").first()!!.text(), it.select("input").`val`())
        }
    }

    // Filters
    override fun getFilterList() = FilterList(
        GenreFilter(getGenreList()),
        StatusFilter(),
        OrderFilter(),
    )

    private class GenreFilter(genres: List<Genre>) : Filter.Group<Genre>("Genres", genres)
    private class Genre(name: String, val id: String) : Filter.CheckBox(name)
    private var genresList: List<Genre>? = null
    private fun getGenreList(): List<Genre> {
        // Filters are fetched immediately once an extension loads
        // We're only able to get filters after a loading the manga directory, and resetting
        // the filters is the only thing that seems to reinflate the view
        return genresList ?: listOf(Genre("Press reset to attempt to fetch genres", ""))
    }

    class StatusFilter : UriPartFilter(
        "Status",
        arrayOf(
            Pair("All", "all"),
            Pair("Ongoing", "ongoing"),
            Pair("Completed", "completed"),
        )
    )

    class OrderFilter(state: Int = 0) : UriPartFilter(
        "Order By",
        arrayOf(
            Pair("Views", "views"),
            Pair("Updated", "updated_at"),
            Pair("Created", "created_at"),
            Pair("Name A-Z", "name"),
            Pair("Rating", "rating"),
        ),
        state
    )

    open class UriPartFilter(
        displayName: String,
        private val vals: Array<Pair<String, String>>,
        state: Int = 0
    ) :
        Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray(), state) {
        fun toUriPart() = vals[state].second
    }

    companion object {
        private var CDN_URL: String? = null
        private var CDN_URL_ALT: List<String> = listOf()
        private var CDN_PATH: String? = null
    }
}
