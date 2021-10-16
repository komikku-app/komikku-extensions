package eu.kanade.tachiyomi.extension.en.mangafast

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import uy.kohesive.injekt.injectLazy
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class MangaFast : ParsedHttpSource() {
    override val name = "MangaFast"

    override val baseUrl = "https://mangafast.org"

    override val lang = "en"

    override val supportsLatest = true

    private val json: Json by injectLazy()

    // Popular
    override fun popularMangaRequest(page: Int): Request {
        // Every thing is in a single page.
        return GET("$baseUrl/list-manga", headers)
    }

    override fun popularMangaSelector(): String = "div#animelist li a"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        element.select("a").let { a ->
            setUrlWithoutDomain(a.attr("href"))
            title = a.select("h4").text().trim()
            thumbnail_url = a.select("img").imageFromElement()
        }
    }

    override fun popularMangaNextPageSelector(): String? = null

    // Latest
    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/read", headers)

    override fun latestUpdatesSelector() = "div.ls5"

    override fun latestUpdatesFromElement(element: Element): SManga = SManga.create().apply {
        element.select("a").let { a ->
            setUrlWithoutDomain(a.attr("href"))
            title = a.attr("title")
            thumbnail_url = a.select("img").imageFromElement()
        }
    }

    override fun latestUpdatesNextPageSelector(): String? = null

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val jsonPayload = buildJsonObject {
            put("limit", 30)
            put("q", query)
        }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .add("Accept", ACCEPT_JSON)
            .add("Origin", baseUrl)
            .add("Referer", "$baseUrl/")
            .build()

        return POST(
            "https://search.${baseUrl.substringAfterLast("/")}/comics/ms",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<SearchResultDto>(response.body!!.string())

        val comicList = result.mangaList
            .map(::searchMangaFromObject)

        return MangasPage(comicList, hasNextPage = false)
    }

    private fun searchMangaFromObject(manga: MangaDto): SManga = SManga.create().apply {
        title = manga.title
        thumbnail_url = manga.thumbnail
        url = "/read/${manga.slug}"
    }

    // Manga Details
    override fun mangaDetailsParse(document: Document): SManga {
        val sManga = SManga.create()
        val infoTable = document.select("table.inftable tbody tr")

        sManga.apply {
            title = document.select("h1[itemprop=name]").text().trim()
            description = document.select("h2[id^=Synopsis] + p").text().trim()
            thumbnail_url = document.select("img#Thumbnail").imageFromElement()
        }
        infoTable.forEach { column ->
            val rows = column.select("td")
            when (rows[0].text().trim()) {
                "Title" -> sManga.title = rows[1].text().trim()
                "Genres" -> sManga.genre = rows[1].text().trim().removeSuffix(",")
                "Author" -> sManga.author = rows[1].text().trim()
                "Status" -> sManga.status = rows[1].text().parseStatus()
            }
        }

        return sManga
    }

    private fun String?.parseStatus() = if (this == null) SManga.UNKNOWN else when {
        this.toLowerCase(Locale.ENGLISH).contains("ongoing") -> SManga.ONGOING
        this.toLowerCase(Locale.ENGLISH).contains("completed") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    // Chapter List
    override fun chapterListSelector() =
        "tbody:has(th:contains(Chapter List)) tr[itemprop]:not(:contains(Spoiler & Release Date))"

    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        val link = element.select("a.chapter-link")
        setUrlWithoutDomain(link.attr("href"))
        name = link.text().trim()
        date_upload = element.select("td:last-of-type").text().parseDate()
    }

    private fun String?.parseDate(): Long {
        if (this == null) return 0L
        return try {
            dateFormat.parse(this.trim())?.time ?: 0L
        } catch (pe: ParseException) { // this can happen for spoiler & release date entries
            0L
        }
    }

    // Pages
    override fun pageListParse(document: Document): List<Page> {
        return document.select("div#Read img").mapIndexed { i, element ->
            var url = element.attr("abs:data-src")

            if (url.isEmpty()) {
                url = element.attr("abs:src")
            }

            Page(i, "", url)
        }
    }

    private fun Elements.imageFromElement(): String? {
        return when {
            this.hasAttr("data-src") -> this.attr("abs:data-src")
            this.hasAttr("abs:src") -> this.attr("abs:src")
            else -> null
        }
    }

    // Unused Function
    override fun searchMangaSelector() = throw UnsupportedOperationException("Not Used")

    override fun searchMangaFromElement(element: Element) =
        throw UnsupportedOperationException("Not Used")

    override fun searchMangaNextPageSelector() = throw UnsupportedOperationException("Not Used")

    override fun imageUrlParse(document: Document): String =
        throw UnsupportedOperationException("Not Used")

    override fun getFilterList() = FilterList()

    companion object {
        val dateFormat by lazy {
            SimpleDateFormat("MM/dd/yyyy", Locale.US)
        }
        private val JSON_MEDIA_TYPE = "application/json;charset=UTF-8".toMediaType()
        private const val ACCEPT_JSON = "application/json, text/plain, */*"
    }
}
