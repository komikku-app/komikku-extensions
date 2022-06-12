package eu.kanade.tachiyomi.multisrc.mccms

import android.util.Log
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.concurrent.thread

/**
 * 漫城CMS http://mccms.cn/
 */
abstract class MCCMS(
    override val name: String,
    override val baseUrl: String,
    override val lang: String = "zh",
) : ParsedHttpSource() {
    override val supportsLatest: Boolean = true

    protected open fun transformTitle(title: String) = title

    // Popular

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/custom/hot", headers)
    override fun popularMangaNextPageSelector(): String? = null
    override fun popularMangaSelector() = ".top-list__box-item"
    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        val titleElement = element.select("p.comic__title > a")
        title = transformTitle(titleElement.text().trim())
        setUrlWithoutDomain(titleElement.attr("abs:href"))
        thumbnail_url = element.select("img").attr("abs:data-original")
    }

    // Latest

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/custom/update", headers)
    override fun latestUpdatesNextPageSelector(): String? = null
    override fun latestUpdatesSelector() = "div.common-comic-item"
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    // Search

    protected open fun textSearchRequest(page: Int, query: String) =
        GET("$baseUrl/search/$query/$page", headers)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        if (query.isNotBlank()) {
            textSearchRequest(page, query)
        } else {
            val categories = filters.filterIsInstance<UriPartFilter>()
                .map { it.toUriPart() }.filter { it.isNotEmpty() }.joinToString("/")
            GET("$baseUrl/category/$categories/page/$page", headers)
        }

    override fun searchMangaNextPageSelector(): String? = "" // empty string means default pagination
    override fun searchMangaSelector() = latestUpdatesSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val isTextSearch = document.location().contains("search")
        val mangas = if (isTextSearch) {
            document.select(searchMangaSelector()).map { searchMangaFromElement(it) }
        } else {
            document.select(latestUpdatesSelector()).map { popularMangaFromElement(it) }
        }
        val hasNextPage = if (isTextSearch && searchMangaNextPageSelector() != "") {
            searchMangaNextPageSelector()?.let { document.selectFirst(it) } != null
        } else { // default pagination
            val buttons = document.select("#Pagination a")
            val count = buttons.size
            // Next page != Last page
            buttons[count - 1].attr("href") != buttons[count - 2].attr("href")
        }
        return MangasPage(mangas, hasNextPage)
    }

    // Details

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        title = transformTitle(document.select("div.de-info__box > p.comic-title").text().trim())
        thumbnail_url = document.select("div.de-info__cover > img").attr("abs:src")
        author = document.select("div.comic-author > span.name > a").text()
        artist = author
        genre = document.select("div.comic-status > span.text:nth-child(1) a").eachText().joinToString(", ")
        description = document.select("div.comic-intro > p.intro-total").text()
    }

    // Chapters

    override fun chapterListSelector() = "ul.chapter__list-box > li"
    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        setUrlWithoutDomain(element.select("a").attr("abs:href"))
        name = element.select("a").text()
    }

    override fun chapterListParse(response: Response) = super.chapterListParse(response).reversed()

    // Pages

    protected open val lazyLoadImageAttr = "data-original"

    override fun pageListParse(document: Document) = document.select("div.rd-article__pic > img")
        .mapIndexed { i, el -> Page(i, "", el.attr("abs:$lazyLoadImageAttr")) }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used.")

    protected class UriPartFilter(name: String, values: Array<String>, private val uriParts: Array<String>) :
        Filter.Select<String>(name, values) {
        fun toUriPart(): String = uriParts[state]
    }

    protected data class Category(val name: String, val values: Array<String>, val uriParts: Array<String>) {
        fun toUriPartFilter() = UriPartFilter(name, values, uriParts)
    }

    private val sortCategory = Category("排序", arrayOf("热门人气", "更新时间"), arrayOf("order/hits", "order/addtime"))
    private lateinit var categories: List<Category>
    private var isFetchingCategories = false

    private fun tryFetchCategories() {
        if (isFetchingCategories) return
        isFetchingCategories = true
        thread {
            try {
                fetchCategories()
            } catch (e: Exception) {
                Log.e("MCCMS/$name", "Failed to fetch categories ($e)")
            } finally {
                isFetchingCategories = false
            }
        }
    }

    protected open fun fetchCategories() {
        val document = client.newCall(GET("$baseUrl/category/", headers)).execute().asJsoup()
        categories = document.select("div.cate-col").map { element ->
            val name = element.select("p.cate-title").text().removeSuffix("：")
            val tags = element.select("li.cate-item > a")
            val values = tags.map { it.text() }.toTypedArray()
            val uriParts = tags.map { it.attr("href").removePrefix("/category/") }.toTypedArray()
            Category(name, values, uriParts)
        }
    }

    override fun getFilterList(): FilterList {
        val result = mutableListOf(
            Filter.Header("如果使用文本搜索，将会忽略分类筛选"),
            sortCategory.toUriPartFilter(),
        )
        if (::categories.isInitialized) {
            categories.forEach { result.add(it.toUriPartFilter()) }
        } else {
            tryFetchCategories()
            result.add(Filter.Header("其他分类正在获取，请返回上一页后重试"))
        }
        return FilterList(result)
    }
}
