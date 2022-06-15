package eu.kanade.tachiyomi.multisrc.sinmh

import eu.kanade.tachiyomi.AppInfo
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

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
    protected open val mobileUrl = _baseUrl.replace("www", "m")
    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", baseUrl)

    protected open val nextPageSelector = "ul.pagination > li.next:not(.disabled)"
    protected open val comicItemSelector = "#contList > li"
    protected open val comicItemTitleSelector = "p > a"
    protected open fun mangaFromElement(element: Element) = SManga.create().apply {
        val titleElement = element.selectFirst(comicItemTitleSelector)
        title = titleElement.text()
        setUrlWithoutDomain(titleElement.attr("abs:href"))
        thumbnail_url = element.selectFirst("img").attr("abs:src")
    }

    // Popular

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/list/click/?page=$page", headers)
    override fun popularMangaNextPageSelector(): String? = nextPageSelector
    override fun popularMangaSelector() = comicItemSelector
    override fun popularMangaFromElement(element: Element) = mangaFromElement(element)

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        parseCategories(document)
        val mangas = document.select(popularMangaSelector()).map(::popularMangaFromElement)
        val hasNextPage = popularMangaNextPageSelector()?.let { document.selectFirst(it) } != null
        return MangasPage(mangas, hasNextPage)
    }

    // Latest

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/list/update/?page=$page", headers)
    override fun latestUpdatesNextPageSelector(): String? = nextPageSelector
    override fun latestUpdatesSelector() = comicItemSelector
    override fun latestUpdatesFromElement(element: Element) = mangaFromElement(element)

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        parseCategories(document)
        val mangas = document.select(latestUpdatesSelector()).map(::latestUpdatesFromElement)
        val hasNextPage = latestUpdatesNextPageSelector()?.let { document.selectFirst(it) } != null
        return MangasPage(mangas, hasNextPage)
    }

    // Search

    override fun searchMangaNextPageSelector(): String? = nextPageSelector
    override fun searchMangaSelector(): String = comicItemSelector
    override fun searchMangaFromElement(element: Element) = mangaFromElement(element)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        if (query.isNotBlank()) {
            GET("$baseUrl/search/?keywords=$query&page=$page", headers)
        } else {
            val categories = filters.filterIsInstance<UriPartFilter>().map { it.toUriPart() }
                .filter { it.isNotEmpty() }.joinToString("-") + "-"
            GET("$baseUrl/list/$categories/", headers)
        }

    // Details

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        title = document.selectFirst(".book-title > h1 > span").text()
        author = document.selectFirst(".detail-list strong:contains(作者) + a").text()
        description = document.selectFirst("#intro-all").text().trim()
            .removePrefix("漫画简介：").trim()
            .removePrefix("漫画简介：").trim() // some sources have double prefix
        genre = document.selectFirst(".detail-list strong:contains(类型) + a").text() + ", " +
            document.select(".breadcrumb-bar a[href*=/list/]").joinToString(", ") { it.text() }
        status = when (document.selectFirst(".detail-list strong:contains(状态) + a").text()) {
            "连载中" -> SManga.ONGOING
            "已完结" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        thumbnail_url = document.selectFirst("p.cover > img").attr("abs:src")
    }

    // Chapters

    override fun chapterListRequest(manga: SManga) = GET(mobileUrl + manga.url, headers)

    protected open val dateSelector = ".date"

    protected open fun List<SChapter>.sortedDescending() = this.asReversed()

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).map { chapterFromElement(it) }.sortedDescending().apply {
            if (isNewDateLogic) {
                val date = document.selectFirst(dateSelector).textNodes().last().text()
                this[0].date_upload = DATE_FORMAT.parse(date)?.time ?: 0L
            }
        }
    }

    override fun chapterListSelector() = ".chapter-body li > a:not([href^=/comic/app/])"
    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        setUrlWithoutDomain(element.attr("abs:href"))
        name = element.text()
    }

    // Pages

    override fun pageListRequest(chapter: SChapter) = GET(mobileUrl + chapter.url, headers)

    override fun pageListParse(document: Document): List<Page> {
        val script = document.selectFirst("body > script").html()
        val images = script.substringAfter("chapterImages = [\"").substringBefore("\"]").split("\",\"")
        val path = script.substringAfter("chapterPath = \"").substringBefore("\";")
        // assume cover images are on the page image server
        val server = script.substringAfter("pageImage = \"").substringBefore("/images/cover")
        return images.mapIndexed { i, image ->
            val unescapedImage = image.replace("""\/""", "/")
            val imageUrl = if (unescapedImage.startsWith("/")) {
                "$server$unescapedImage"
            } else {
                "$server/$path$unescapedImage"
            }
            Page(i, imageUrl = imageUrl)
        }
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

    protected open fun parseCategories(document: Document) {
        if (::categories.isInitialized) return
        categories = document.selectFirst(".filter-nav").children().map { element ->
            val name = element.selectFirst("label").text()
            val tags = element.select("a")
            val values = tags.map { it.text() }.toTypedArray()
            val uriParts = tags.map { it.attr("href").removePrefix("/list/").removeSuffix("/") }.toTypedArray()
            Category(name, values, uriParts)
        }
    }

    override fun getFilterList() =
        if (::categories.isInitialized) FilterList(
            Filter.Header("如果使用文本搜索，将会忽略分类筛选"),
            *categories.map(Category::toUriPartFilter).toTypedArray()
        ) else FilterList(
            Filter.Header("点击“重置”即可刷新分类，如果失败，"),
            Filter.Header("请尝试重新从图源列表点击进入图源"),
        )

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    private val isNewDateLogic = run {
        val commitCount = AppInfo.getVersionName().substringAfter('-', "")
        if (commitCount.isNotEmpty()) // Preview
            commitCount.toInt() >= 4442
        else // Stable
            AppInfo.getVersionCode() >= 81
    }
}
