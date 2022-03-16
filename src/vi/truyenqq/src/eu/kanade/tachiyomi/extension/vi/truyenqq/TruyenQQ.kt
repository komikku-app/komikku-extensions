package eu.kanade.tachiyomi.extension.vi.truyenqq

import android.net.Uri
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class TruyenQQ : ParsedHttpSource() {

    override val name: String = "TruyenQQ"

    override val lang: String = "vi"

    override val baseUrl: String = "http://truyenqqpro.com"

    override val supportsLatest: Boolean = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()

    override fun headersBuilder(): Headers.Builder {
        return super.headersBuilder().add("Referer", baseUrl)
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    private val floatPattern = Regex("""\d+(?:\.\d+)?""")

    // Popular
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/top-thang/trang-$page.html", headers)
    }
    override fun popularMangaNextPageSelector(): String = ".page_redirect > a:last-child > p:not(.active)"
    override fun popularMangaSelector(): String = "ul.grid > li"
    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        setUrlWithoutDomain(element.select("a").first().attr("abs:href"))
        thumbnail_url = element.select("img.lazy-image").attr("abs:data-src")
        title = element.select("h3 a").text()
    }

    // Latest
    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/truyen-moi-cap-nhat/trang-$page.html", headers)
    }
    override fun latestUpdatesNextPageSelector(): String = popularMangaNextPageSelector()
    override fun latestUpdatesSelector(): String = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val uri = Uri.parse("$baseUrl/tim-kiem/trang-$page.html").buildUpon()
            .appendQueryParameter("q", query)
        return GET(uri.toString(), headers)

        // Todo Filters
    }
    override fun searchMangaNextPageSelector(): String = popularMangaNextPageSelector()
    override fun searchMangaSelector(): String = popularMangaSelector()
    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    // Details

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val info = document.selectFirst(".list-info")
        title = document.select("h1").text()
        author = info.select(".org").joinToString { it.text() }
        artist = author
        val glist = document.select(".list01 li").map { it.text() }
        genre = glist.joinToString()
        description = document.select(".story-detail-info").text()
        thumbnail_url = document.select("img[itemprop=image]").attr("abs:src")
        status = when (info.select(".status > p:last-child").text()) {
            "Đang Cập Nhật" -> SManga.ONGOING
            "Hoàn Thành" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
    }

    // Chapters

    override fun chapterListSelector(): String = "div.works-chapter-list div.works-chapter-item"
    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        setUrlWithoutDomain(element.select("a").attr("abs:href"))
        name = element.select("a").text().trim()
        date_upload = parseDate(element.select(".time-chap").text())
        chapter_number = floatPattern.find(name)?.value?.toFloatOrNull() ?: -1f
    }
    private fun parseDate(date: String): Long {
        return dateFormat.parse(date)?.time ?: 0L
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> = mutableListOf<Page>().apply {
        document.select("img.lazy").forEachIndexed { index, element ->
            add(Page(index, "", element.attr("abs:src")))
        }
    }
    override fun imageUrlParse(document: Document): String {
        throw Exception("Not Used")
    }

    // Not Used
}
