package eu.kanade.tachiyomi.multisrc.sinmh

import android.util.Log
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.concurrent.thread

/**
 * 圣樱漫画CMS https://gitee.com/shenl/SinMH-2.0-Guide
 * ref: https://github.com/kanasimi/CeJS/tree/master/application/net/work_crawler/sites
 *      https://github.com/kanasimi/work_crawler/blob/master/document/README.cmn-Hant-TW.md
 */
abstract class SinMH(
    override val name: String,
    _baseUrl: String,
    override val lang: String = "zh",
) : ParsedHttpSource() {
    override val baseUrl = _baseUrl
    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", baseUrl)

    protected open val nextPageSelector = "ul.pagination > li.next:not(.disabled)"
    protected open val comicItemSelector = "#contList > li"
    protected open val comicItemTitleSelector = "p > a"
    protected open fun mangaFromElement(element: Element) =
        SManga.create().apply {
            val titleElement = element.select(comicItemTitleSelector)
            title = titleElement.text()
            setUrlWithoutDomain(titleElement.attr("abs:href"))
            thumbnail_url = element.select("img").attr("abs:src")
        }

    // Popular

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/list/click/?page=$page", headers)
    override fun popularMangaNextPageSelector(): String? = nextPageSelector
    override fun popularMangaSelector() = comicItemSelector
    override fun popularMangaFromElement(element: Element) = mangaFromElement(element)

    // Latest

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/list/update/?page=$page", headers)
    override fun latestUpdatesNextPageSelector(): String? = nextPageSelector
    override fun latestUpdatesSelector() = comicItemSelector
    override fun latestUpdatesFromElement(element: Element) = mangaFromElement(element)

    // Search

    override fun searchMangaNextPageSelector(): String? = nextPageSelector
    override fun searchMangaSelector(): String = comicItemSelector
    override fun searchMangaFromElement(element: Element) = mangaFromElement(element)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        if (query.isNotBlank()) {
            GET("$baseUrl/search/?keywords=$query&page=$page", headers)
        } else {
            val categories = filters.filterIsInstance<UriPartFilter>()
                .joinToString("-", transform = UriPartFilter::toUriPart) + "-"
            GET("$baseUrl/list/$categories/", headers)
        }

    // Details

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        title = document.select(".book-title > h1 > span").text()
        author = document.select(".detail-list strong:contains(作者) + a").text()
        description = document.select("#intro-all").text().trim()
            .removePrefix("漫画简介：").trim()
            .removePrefix("漫画简介：").trim()  // some sources have double prefix
        genre = document.select(".detail-list strong:contains(类型) + a").text() + ", " +
            document.select(".breadcrumb-bar a[href*=/list/]").joinToString(", ") { it.text() }
        status = when (document.select(".detail-list strong:contains(状态) + a").text()) {
            "连载中" -> SManga.ONGOING
            "已完结" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        thumbnail_url = document.select("p.cover > img").attr("abs:src")
        // TODO: can use 更新时间：2022-05-23 to set default upload date
    }

    // Chapters

    override fun chapterListSelector() = ".chapter-body li > a:not([href*=/comic/app/])"
    override fun chapterListParse(response: Response) = super.chapterListParse(response).reversed()
    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        setUrlWithoutDomain(element.attr("abs:href"))
        name = element.text()
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        val script = document.select("script:containsData(chapterImages)").html()
        val images = script.substringAfter("chapterImages = [\"").substringBefore("\"]").split("\",\"")
        val path = script.substringAfter("chapterPath = \"").substringBefore("\";")
        // assume cover images are on the same server
        val server = script.substringAfter("pageImage = \"").substringBefore("/images/cover")
        return images.mapIndexed { i, image -> Page(i, "", "$server/$path/$image") }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not Used.")

    protected class UriPartFilter(displayName: String, values: Array<String>, private val uriParts: Array<String>) :
        Filter.Select<String>(displayName, values) {
        fun toUriPart(): String = uriParts[state]
    }

    protected data class Category(val name: String, val values: Array<String>, val uriParts: Array<String>) {
        fun toUriPartFilter() = UriPartFilter(name, values, uriParts)
    }

    private lateinit var categories: List<Category>
    private var isFetchingCategories = false

    private fun tryFetchCategories() {
        if (isFetchingCategories) return
        isFetchingCategories = true
        thread {
            try {
                fetchCategories()
            } catch (e: Exception) {
                Log.e("SinMH", "Failed to fetch categories ($e)")
            } finally {
                isFetchingCategories = false
            }
        }
    }

    protected open fun fetchCategories() {
        val document = client.newCall(GET("$baseUrl/list/", headers)).execute().asJsoup()
        categories = document.select(".page-main .filter-nav > .filter-item").map { element ->
            val name = element.select("label").text()
            val tags = element.select("a")
            val values = tags.map { it.text() }.toTypedArray()
            val uriParts = tags.map { it.attr("href").removePrefix("/list/").removeSuffix("/") }.toTypedArray()
            Category(name, values, uriParts)
        }
    }

    init {
        tryFetchCategories()
    }

    override fun getFilterList() =
        if (::categories.isInitialized) FilterList(
            Filter.Header("如果使用文本搜索，将会忽略分类筛选"),
            *categories.map(Category::toUriPartFilter).toTypedArray()
        ) else {
            tryFetchCategories()
            FilterList(
                Filter.Header("分类尚未获取，请返回上一页后重试")
            )
        }
}
