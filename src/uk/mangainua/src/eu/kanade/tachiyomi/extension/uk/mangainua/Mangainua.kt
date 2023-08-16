package eu.kanade.tachiyomi.extension.uk.mangainua

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.jsoup.select.Evaluator
import java.text.SimpleDateFormat
import java.util.Locale

class Mangainua : ParsedHttpSource() {

    // Info
    override val name = "MANGA/in/UA"
    override val baseUrl = "https://manga.in.ua"
    override val lang = "uk"
    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", baseUrl)

    // Popular
    override fun popularMangaRequest(page: Int): Request {
        return GET(baseUrl)
    }
    override fun popularMangaSelector() = "div.owl-carousel div.card--big"
    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.selectFirst("h3.card__title a")!!.let {
                setUrlWithoutDomain(it.attr("href"))
                title = it.text()
            }
            thumbnail_url = element.selectFirst("img")?.absUrl("src")
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
            element.selectFirst("h3.card__title a")!!.let {
                setUrlWithoutDomain(it.attr("href"))
                title = it.text()
            }
            thumbnail_url = element.selectFirst("div.card--big img")?.absUrl("data-src")
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
            throw Exception("Запит має містити щонайменше 3 символи / The query must contain at least 3 characters")
        }
    }

    override fun searchMangaSelector() = latestUpdatesSelector()
    override fun searchMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.selectFirst("h3.card__title a")!!.let {
                setUrlWithoutDomain(it.attr("href"))
                title = it.text()
            }
            thumbnail_url = element.selectFirst("div.card--big img")?.absUrl("src")
        }
    }
    override fun searchMangaNextPageSelector() = latestUpdatesNextPageSelector()

    // Manga Details
    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            title = document.selectFirst("span.UAname")!!.text()
            description = document.selectFirst("div.item__full-description")!!.text()
            thumbnail_url = document.selectFirst("div.item__full-sidebar--poster img")!!.absUrl("src")
            status = when (document.selectFirst("div.item__full-sideba--header:has(div:containsOwn(Статус перекладу:))")?.selectFirst("span.item__full-sidebar--description")?.text()) {
                "Триває" -> SManga.ONGOING
                "Покинуто" -> SManga.CANCELLED
                "Закінчений" -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
            val type = when (document.selectFirst("div.item__full-sideba--header:has(div:containsOwn(Тип:))")?.selectFirst("span.item__full-sidebar--description")!!.text()) {
                "ВЕБМАНХВА" -> "Manhwa"
                "МАНХВА" -> "Manhwa"
                "МАНЬХВА" -> "Manhua"
                "ВЕБМАНЬХВА" -> "Manhua"
                else -> "Manga"
            }
            genre = document.selectFirst("div.item__full-sideba--header:has(div:containsOwn(Жанри:))")?.selectFirst("span.item__full-sidebar--description")!!.select("a").joinToString { it.text() } + ", " + type
        }
    }

    // Chapters
    override fun chapterListSelector() = throw UnsupportedOperationException()

    override fun chapterFromElement(element: Element): SChapter {
        throw UnsupportedOperationException()
    }

    private fun parseChapterElements(elements: Elements): List<SChapter> {
        var previousChapterName: String? = null
        var previousChapterNumber: Float = 0.0f
        val dateFormat = DATE_FORMATTER
        return elements.map { element ->
            SChapter.create().apply {
                val urlElement = element.selectFirst("a")!!
                setUrlWithoutDomain(urlElement.attr("href"))
                val chapterName = urlElement.text().substringAfter("НОВЕ").trim()
                val chapterNumber = urlElement.text().substringAfter("Розділ").substringBefore("-").trim()
                if (chapterName.contains("Альтернативний переклад")) {
                    name = previousChapterName.toString().substringBefore("-").trim()
                    scanlator = urlElement.text().substringAfter("від:").trim()
                    chapter_number = previousChapterNumber
                } else {
                    name = chapterName
                    previousChapterName = chapterName
                    chapter_number = chapterNumber.toFloat()
                    previousChapterNumber = chapterNumber.toFloat()
                }
                date_upload = dateFormat.parse(element.child(0).ownText())?.time!!
            }
        }
    }
    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val userHash = document.parseUserHash()
        val metaElement = document.selectFirst(Evaluator.Id("linkstocomics"))!!
        val body = FormBody.Builder()
            .addEncoded("action", "show")
            .addEncoded("news_id", metaElement.attr("data-news_id"))
            .addEncoded("news_category", metaElement.attr("data-news_category"))
            .addEncoded("this_link", metaElement.attr("data-this_link"))
            .addEncoded("user_hash", userHash)
            .build()
        val request = POST("$baseUrl/engine/ajax/controller.php?mod=load_chapters", headers, body)
        val chaptersHtml = client.newCall(request).execute().body.string()
        val chaptersDocument = Jsoup.parseBodyFragment(chaptersHtml)
        return parseChapterElements(chaptersDocument.body().children()).asReversed()
    }

    // Pages
    override fun pageListParse(document: Document): List<Page> {
        val userHash = document.parseUserHash()
        val newsId = document.selectFirst(Evaluator.Id("comics"))!!.attr("data-news_id")
        val url = "$baseUrl/engine/ajax/controller.php?mod=load_chapters_image&news_id=$newsId&action=show&user_hash=$userHash"
        val pagesHtml = client.newCall(GET(url, headers)).execute().body.string()
        val pagesDocument = Jsoup.parseBodyFragment(pagesHtml)
        return pagesDocument.getElementsByTag("img").mapIndexed { index, img ->
            Page(index, imageUrl = img.attr("data-src"))
        }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException()

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH)
        }

        private fun Document.parseUserHash(): String {
            val start = "site_login_hash = '"
            for (element in body().children()) {
                if (element.tagName() != "script") continue
                val data = element.data()
                val leftIndex = data.indexOf(start)
                if (leftIndex == -1) continue
                val startIndex = leftIndex + start.length
                val endIndex = data.indexOf('\'', startIndex)
                return data.substring(startIndex, endIndex)
            }
            throw Exception("Couldn't find user hash")
        }
    }
}
