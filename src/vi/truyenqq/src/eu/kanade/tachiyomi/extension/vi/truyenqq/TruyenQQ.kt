package eu.kanade.tachiyomi.extension.vi.truyenqq

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class TruyenQQ : ParsedHttpSource() {

    override val name: String = "TruyenQQ"

    override val lang: String = "vi"

    override val baseUrl: String = "http://truyenqqvip.com"

    override val supportsLatest: Boolean = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder().add("Referer", baseUrl)

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    private val floatPattern = Regex("""\d+(?:\.\d+)?""")

    // Selector trả về array các manga (chọn cả ảnh cx được tí nữa parse)
    override fun popularMangaSelector(): String = "ul.grid > li"
    // Selector trả về array các manga update (giống selector ở trên)
    override fun latestUpdatesSelector(): String = popularMangaSelector()
    // Selector của nút trang kế tiếp
    override fun popularMangaNextPageSelector(): String = ".page_redirect > a:nth-last-child(2) > p:not(.active)"
    override fun latestUpdatesNextPageSelector(): String = popularMangaNextPageSelector()

    // Trang html chứa popular
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/truyen-yeu-thich/trang-$page.html", headers)
    }
    // Trang html chứa Latest (các cập nhật mới nhất)
    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/truyen-moi-cap-nhat/trang-$page.html", headers)
    }

    // respond là html của trang popular chứ không phải của element đã select
    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val imgURL = document.select(".book_avatar img").map { it.attr("abs:src") }
        val mangas = document.select(popularMangaSelector()).mapIndexed { index, element -> popularMangaFromElement(element, imgURL[index]) }

        val hasNextPage = popularMangaNextPageSelector().let { selector ->
            document.select(selector).first()
        } != null

        return MangasPage(mangas, hasNextPage)
    }

    // Từ 1 element trong list popular đã select ở trên parse thông tin 1 Manga
    // Trông code bất ổn nhưng t đang cố làm theo blogtruyen vì t không biết gì hết XD
    private fun popularMangaFromElement(element: Element, imgURL: String): SManga {
        val manga = SManga.create()
        element.select(".book_info .book_name h3 a").first().let {
            manga.setUrlWithoutDomain((it.attr("href")))
            manga.title = it.text().trim()
            manga.thumbnail_url = imgURL
        }
        return manga
    }

    // Không dùng bản này của fuction nên throw Exception, dùng function ở trên (có 2 params)
    override fun popularMangaFromElement(element: Element): SManga = throw Exception("Not Used")

    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select(".book_info .book_name h3 a").first().let {
            manga.setUrlWithoutDomain((it.attr("href")))
            manga.title = it.text().trim()
        }
        manga.thumbnail_url = element.select(".book_avatar img").first().attr("abs:src")
        return manga
    }

    // Tìm kiếm
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/tim-kiem/trang-$page.html"
        val uri = url.toHttpUrlOrNull()!!.newBuilder()
        uri.addQueryParameter("q", query)
        return GET(uri.toString(), headers)

        // Todo Filters
    }
    override fun searchMangaNextPageSelector(): String = popularMangaNextPageSelector()
    override fun searchMangaSelector(): String = popularMangaSelector()
    override fun searchMangaFromElement(element: Element): SManga = latestUpdatesFromElement(element)

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
