package eu.kanade.tachiyomi.extension.all.everiaclub

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class EveriaClub() : ParsedHttpSource() {
    override val baseUrl = "https://everia.club"
    override val lang = "all"
    override val name = "Everia.club"
    override val supportsLatest = true

    // Latest
    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = element.select("img").attr("abs:src")
        manga.title = element.select(".entry-title").text()
        manga.setUrlWithoutDomain(element.select(".entry-title > a").attr("abs:href"))
        return manga
    }

    override fun latestUpdatesNextPageSelector() = ".next"

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/page/$page/")
    }

    override fun latestUpdatesSelector() = ".posts-wrapper > article"

    // Popular
    override fun popularMangaFromElement(element: Element) = latestUpdatesFromElement(element)
    override fun popularMangaNextPageSelector() = latestUpdatesNextPageSelector()
    override fun popularMangaRequest(page: Int) = latestUpdatesRequest(page)
    override fun popularMangaSelector() = latestUpdatesSelector()

    // Search

    override fun searchMangaFromElement(element: Element) = latestUpdatesFromElement(element)
    override fun searchMangaNextPageSelector() = latestUpdatesNextPageSelector()
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val tagFilter = filters.findInstance<TagFilter>()!!
        val categoryFilter = filters.findInstance<CategoryFilter>()!!
        return when {
            query.isNotEmpty() -> GET("$baseUrl/page/$page/?s=$query")
            categoryFilter.state.isNotEmpty() -> GET("$baseUrl/category/${categoryFilter.state}/page/$page/")
            tagFilter.state.isNotEmpty() -> GET("$baseUrl/tag/${tagFilter.state}/page/$page/")
            else -> latestUpdatesRequest(page)
        }
    }

    override fun searchMangaSelector() = latestUpdatesSelector()

    // Details
    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        manga.title = document.select(".entry-title").text()
        manga.description = document.select(".entry-content").text().trim()
        val genres = mutableListOf<String>()
        document.select(".nv-tags-list > a").forEach {
            genres.add(it.text())
        }
        manga.genre = genres.joinToString(", ")
        return manga
    }

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(element.select("meta[property=\"og:url\"]").attr("abs:content"))
        chapter.chapter_number = 0F
        chapter.name = element.select(".entry-title").text()
        chapter.date_upload = SimpleDateFormat("yyyy-MM-DD", Locale.US).parse(element.select("meta[property=\"article:published_time\"]").attr("abs:content").substringBeforeLast("T").substringAfterLast("/"))?.time ?: 0L
        return chapter
    }

    override fun chapterListSelector() = "html"

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        document.select(".entry-content img").forEachIndexed { i, it ->
            val itUrl = it.attr("abs:src")
            pages.add(Page(i, itUrl, itUrl))
        }
        return pages
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    // Filters
    override fun getFilterList(): FilterList = FilterList(
        Filter.Header("NOTE: Ignored if using text search!"),
        Filter.Header("NOTE: Do not use them together!"),
        Filter.Separator(),
        CategoryFilter(),
        TagFilter(),
    )

    class CategoryFilter : Filter.Text("Category")
    class TagFilter : Filter.Text("Tag")

    private inline fun <reified T> Iterable<*>.findInstance() = find { it is T } as? T
}
