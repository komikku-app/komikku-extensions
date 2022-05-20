package eu.kanade.tachiyomi.extension.zh.qimiaomh

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class Qimiaomh : ParsedHttpSource() {

    override val name: String = "奇妙漫画"

    override val lang: String = "zh"

    override val baseUrl: String = "https://www.qimiaomh.com"

    override val supportsLatest: Boolean = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()

    private val json: Json by injectLazy()

    // Popular
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/list-1------hits--$page.html", headers)
    }

    override fun popularMangaNextPageSelector(): String? = "a:contains(下一页)"

    override fun popularMangaSelector(): String = "div.classification"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        url = element.select("a").first().attr("href")
        thumbnail_url = element.select("img.lazyload").attr("abs:data-src")
        title = element.select("a").first().text()
    }

    // Latest
    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/list-1------updatetime--$page.html", headers)
    }

    override fun latestUpdatesNextPageSelector(): String? = popularMangaNextPageSelector()

    override fun latestUpdatesSelector(): String = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search/$query/$page.html")
    }

    override fun searchMangaNextPageSelector(): String? = popularMangaNextPageSelector()

    override fun searchMangaSelector(): String = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    // Details

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.selectFirst("h1.title").ownText()
        author = document.select("p.author").first().ownText()
        artist = author
        val glist = document.select("span.labelBox a").map { it.text() }
        genre = glist.joinToString(", ")
        description = document.select("p#worksDesc").text().trim()
        thumbnail_url = document.select("div.ctdbLeft img").attr("src")
        status = when (document.select("a.status").text().substringAfter(":").trim()) {
            "连载中" -> SManga.ONGOING
            "完结" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
    }

    // Chapters

    override fun chapterListSelector(): String = "div.comic-content-list ul.comic-content-c"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        url = element.select("a").first().attr("href")
        name = element.select("li.tit").text().trim()
        date_upload = parseDate(element.selectFirst("li.time").text())
    }

    private fun parseDate(date: String): Long {
        return runCatching { DATE_FORMATTER.parse(date)?.time }
            .getOrNull() ?: 0L
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        return json.decodeFromString<List<String>>(
            document.select("script:containsData(z_img)").html()
                .substringAfter("var z_img='")
                .substringBefore("';")
        ).mapIndexed { i, imageUrl ->
            Page(i, "", imageUrl)
        }
    }

    override fun imageUrlParse(document: Document): String = ""

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        }
    }
}
