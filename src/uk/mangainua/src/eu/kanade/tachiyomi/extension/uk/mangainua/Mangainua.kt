package eu.kanade.tachiyomi.extension.uk.mangainua

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class Mangainua : ParsedHttpSource() {

    // Info
    override val name = "MANGA/in/UA"
    override val baseUrl = "https://manga.in.ua"
    override val lang = "uk"
    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", baseUrl)

    // Popular
    override fun popularMangaRequest(page: Int): Request {
        return GET(baseUrl)
    }
    override fun popularMangaSelector() = "div.owl-carousel div.card--big"
    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("h3.card__title a").first()!!.let {
                setUrlWithoutDomain(it.attr("href"))
                title = it.text()
            }
            thumbnail_url = element.select("img").attr("abs:src")
        }
    }
    override fun popularMangaNextPageSelector() = "not used"

    // Latest (using for search)
    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/page/$page/")
    }
    override fun latestUpdatesSelector() = "main.main article.item"
    override fun latestUpdatesFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("h3.card__title a").first()!!.let {
                setUrlWithoutDomain(it.attr("href"))
                title = it.text()
            }
            thumbnail_url = element.select("div.card--big img").attr("abs:data-src")
        }
    }
    override fun latestUpdatesNextPageSelector() = "a:contains(Наступна)"

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return if (query.length > 2) {
            POST(
                "$baseUrl/index.php?do=search",
                body = FormBody.Builder()
                    .add("do", "search")
                    .add("subaction", "search")
                    .add("story", query)
                    .add("search_start", page.toString())
                    .build(),
                headers = headers,
            )
        } else {
            throw UnsupportedOperationException("Запит має містити щонайменше 3 символи / The query must contain at least 3 characters")
        }
    }

    override fun searchMangaSelector() = latestUpdatesSelector()
    override fun searchMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("h3.card__title a").first()!!.let {
                setUrlWithoutDomain(it.attr("href"))
                title = it.text()
            }
            thumbnail_url = element.select("div.card--big img").attr("abs:src")
        }
    }
    override fun searchMangaNextPageSelector() = latestUpdatesNextPageSelector()

    // Manga Details
    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            title = document.select("span.UAname").text()
            description = document.select("div.item__full-description").text()
            genre = document.select("div.item__full-sideba--header:eq(4) span").first()!!.select("a").joinToString { it.text() }
            thumbnail_url = document.select("div.item__full-sidebar--poster img").first()!!.attr("abs:src")
        }
    }

    // Chapters
    override fun chapterListSelector() = "div.ltcitems"
    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            element.select("a").let { urlElement ->
                setUrlWithoutDomain(urlElement.attr("href"))
                name = urlElement.text().substringAfter("НОВЕ").trim()
            }
            date_upload = parseDate(element.select("div.ltcright:containsOwn(.)").text())
        }
    }
    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response).reversed()
    }

    // Pages
    override fun pageListParse(document: Document): List<Page> {
        return document.select("ul.loadcomicsimages img").mapIndexed { i, element ->
            Page(i, "", element.attr("abs:data-src"))
        }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    private fun parseDate(dateStr: String): Long {
        return runCatching { DATE_FORMATTER.parse(dateStr)?.time }
            .getOrNull() ?: 0L
    }

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH)
        }
    }
}
