package eu.kanade.tachiyomi.extension.en.mangademon

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class MangaDemon : ParsedHttpSource() {

    override val lang = "en"
    override val supportsLatest = true
    override val name = "Manga Demon"
    override val baseUrl = "https://mangademon.org"

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("referrer", "origin")

    // latest
    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/updates.php?list=$page", headers)
    }

    override fun latestUpdatesNextPageSelector() = ".pagination a:contains(Next)"

    override fun latestUpdatesSelector() = "div.leftside"

    override fun latestUpdatesFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a").apply() {
                title = attr("title").dropLast(4)
                url = attr("href")
            }
            thumbnail_url = element.select("img").attr("abs:src")
        }
    }

    // Popular
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/browse.php?list=$page", headers)
    }

    override fun popularMangaNextPageSelector() = latestUpdatesNextPageSelector()

    override fun popularMangaSelector() = latestUpdatesSelector()

    override fun popularMangaFromElement(element: Element) = latestUpdatesFromElement(element)

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/search.php".toHttpUrl().newBuilder()
            .addQueryParameter("manga", query)
            .build()
        return POST("$url", headers)
    }

    override fun searchMangaSelector() = "a.boxsizing"

    override fun searchMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a").first().let {
                title = element.select("li.boxsizing").text()
                url = (element.attr("href"))
                val urlsorter = title.replace(":", "%20")
                thumbnail_url = ("https://readermc.org/images/thumbnails/$urlsorter.webp")
            }
        }
    }

    override fun searchMangaNextPageSelector() = latestUpdatesNextPageSelector()

    // Manga details
    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select("article")

        return SManga.create().apply {
            title = infoElement.select("h1.novel-title").text()
            author = infoElement.select("div.author").text().drop(7)
            status = parseStatus(infoElement.select("span:has(small:containsOwn(Status))").text())
            genre = infoElement.select("a.property-item").joinToString { it.text() }
            description = infoElement.select("p.description").text()
            thumbnail_url = infoElement.select("img#thumbonail").attr("src")
        }
    }

    private fun parseStatus(status: String?) = when {
        status == null -> SManga.UNKNOWN
        status.contains("Ongoing", ignoreCase = true) -> SManga.ONGOING
        status.contains("Completed", ignoreCase = true) -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListSelector() = "ul.chapter-list li"

    // Get Chapters
    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            element.select("a").let { urlElement ->
                url = (urlElement.attr("href"))
                name = element.select("strong.chapter-title").text()
            }
            val date = element.select("time.chapter-update").text()
            date_upload = parseDate(date)
        }
    }

    private fun parseDate(dateStr: String): Long {
        return runCatching { DATE_FORMATTER.parse(dateStr)?.time }
            .getOrNull() ?: 0L
    }

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        }
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select("img.imgholder")
            .mapIndexed { i, el -> Page(i, "", el.attr("src")) }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")
}
