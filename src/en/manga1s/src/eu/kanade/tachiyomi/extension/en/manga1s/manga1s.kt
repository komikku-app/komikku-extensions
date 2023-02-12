package eu.kanade.tachiyomi.extension.en.manga1s

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class manga1s : ParsedHttpSource() {

    override val name = "Manga1s"

    override val baseUrl = "https://manga1s.com"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    // Popular
    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/top-search", headers)

    override fun popularMangaSelector() =
        ".novel-wrap"

    override fun popularMangaFromElement(element: Element): SManga =
        SManga.create().apply {
            setUrlWithoutDomain(element.select("h2 > a").attr("href"))
            title = element.select("h2 > a").text()
            thumbnail_url = element.select("img").attr("data-src")
        }

    override fun popularMangaNextPageSelector() =
        "ul.pagination > li:last-child > a"

    // Latest
    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/last-update", headers)

    override fun latestUpdatesSelector() =
        popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga =
        popularMangaFromElement(element)

    override fun latestUpdatesNextPageSelector() =
        popularMangaNextPageSelector()

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("$baseUrl/search?q=$query", headers)

    override fun searchMangaSelector() =
        popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga =
        popularMangaFromElement(element)

    override fun searchMangaNextPageSelector() =
        popularMangaNextPageSelector()

    // Details
    override fun mangaDetailsParse(document: Document) =
        SManga.create().apply {
            title = document.select(".novel-name > h1").text()
            author = document.select(".novel-authors a").text()
            description = document.select("#manga-description").text().trim()
            genre = document.select(".novel-categories > a").joinToString { it.text() }
            status =
                when (
                    document.select(".novel-info i.fa-flag")[0].parent()!!.parent()!!.select("span")
                        .text()
                ) {
                    "On-going" -> SManga.ONGOING
                    "Completed" -> SManga.COMPLETED
                    else -> SManga.UNKNOWN
                }
            thumbnail_url = document.select(".novel-thumbnail > img").attr("data-src")
        }

    // Chapters
    override fun chapterListSelector() =
        ".chapter-name a"

    override fun chapterFromElement(element: Element): SChapter =
        SChapter.create().apply {
            setUrlWithoutDomain(element.attr("href"))
            name = element.text()
            chapter_number = element.text()
                .substringAfter(" ").toFloatOrNull() ?: -1f
        }

    // Pages
    override fun pageListParse(document: Document): List<Page> =
        document.select(".chapter-images > img").mapIndexed { index, element ->
            Page(index, "", element.attr("data-src"))
        }

    override fun imageUrlParse(document: Document) =
        throw UnsupportedOperationException("Not used")
}
