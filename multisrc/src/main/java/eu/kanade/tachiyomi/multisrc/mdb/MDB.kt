package eu.kanade.tachiyomi.multisrc.mdb

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
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
import org.jsoup.select.Evaluator
import org.jsoup.select.QueryParser
import rx.Observable

/** ManhuaDB: https://www.manhuadb.com/ */
abstract class MDB(
    override val name: String,
    override val baseUrl: String,
    override val lang: String = "zh",
) : ParsedHttpSource() {

    override val client = network.client.newBuilder().rateLimit(2).build()

    override fun headersBuilder() = super.headersBuilder().add("Referer", baseUrl)

    protected abstract fun listUrl(params: String): String
    protected abstract fun extractParams(listUrl: String): String
    protected abstract fun searchUrl(page: Int, query: String): String

    override fun popularMangaRequest(page: Int) = GET(listUrl("page-$page"), headers)
    override fun popularMangaSelector() = "div.comic-main-section > div.comic-book-unit"
    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        val link = element.selectFirst(listComicLinkSelector)
        setUrlWithoutDomain(link.attr("href"))
        title = link.text()
        thumbnail_url = element.selectFirst(imgSelector).absUrl("src")
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        parseCategories(document) // parse categories here
        val mangas = document.select(popularMangaSelector()).map { popularMangaFromElement(it) }
        val hasNextPage = popularMangaNextPageSelector()?.let { document.selectFirst(it) } != null
        return MangasPage(mangas, hasNextPage)
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        if (query.isNotEmpty()) {
            val request = GET(searchUrl(page, query), headers)
            client.newCall(request).asObservableSuccess().map { searchMangaParse(it) }
        } else {
            val params = filters.filterIsInstance<CategoryFilter>().map { it.getParam() }
                .filterTo(mutableListOf()) { it.isNotEmpty() }.apply { add("page-$page") }
            val request = GET(listUrl(params.joinToString("-")), headers)
            client.newCall(request).asObservableSuccess().map { popularMangaParse(it) }
        }

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()
    override fun searchMangaSelector() = "div.comic-main-section > div.row > div"
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)
    final override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        throw UnsupportedOperationException("Not used.")

    protected open fun transformTitle(title: String) = title
    protected abstract val authorSelector: Evaluator
    protected open fun transformDescription(description: String) = description

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        title = document.selectFirst(h1Selector).text().let { transformTitle(it) }
        author = document.selectFirst(authorSelector).text()
        description = document.selectFirst(descriptionSelector).text().let { transformDescription(it) }
        genre = parseGenre(document).joinToString(", ")
        status = when (document.selectFirst(statusSelector).text()) {
            "连载中" -> SManga.ONGOING
            "已完结" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        thumbnail_url = document.selectFirst(coverSelector).attr("src")
    }

    protected open fun parseGenre(document: Document): List<String> {
        val list = mutableListOf<String>()
        list.add(document.selectFirst(regionSelector).text())
        list.add(document.selectFirst(audienceSelector).text().removeSuffix("漫画"))
        val tags = document.select(tagSelector)
        for (i in 1 until tags.size) { // skip status
            list.add(tags[i].text())
        }
        return list
    }

    override fun chapterListSelector() = "#comic-book-list li > a"
    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        setUrlWithoutDomain(element.attr("href"))
        name = element.attr("title")
    }

    override fun pageListParse(document: Document): List<Page> {
        val imgData = document.selectFirst(scriptSelector).data()
            .substringAfter("img_data = ").run {
                val endIndex = indexOf(this[0], startIndex = 1) // find end quote
                substring(1, endIndex)
            }
        val readerConfig = document.selectFirst(readerConfigSelector)
        return parseImages(imgData, readerConfig).mapIndexed { i, it ->
            Page(i, imageUrl = it)
        }
    }

    protected abstract fun parseImages(imgData: String, readerConfig: Element): List<String>

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used.")

    protected data class Category(val name: String, val values: Array<String>, val params: List<String>) {
        fun toFilter() = CategoryFilter(name, values, params)
    }

    protected class CategoryFilter(name: String, values: Array<String>, val params: List<String>) :
        Filter.Select<String>(name, values) {
        fun getParam() = params[state]
    }

    private lateinit var categories: List<Category>

    protected open fun parseCategories(document: Document) {
        if (::categories.isInitialized) return
        val filters = document.select(filterSelector)
        val list = ArrayList<Category>(filters.size + 1)
        for (filter in filters) {
            val children = filter.children()
            val filterContainer = children[1]
            if (filterContainer.hasClass("row")) { // Normal filter
                val tags = filterContainer.children()
                val values = ArrayList<String>(tags.size + 1).apply { add("全部") }
                val params = ArrayList<String>(tags.size + 1).apply { add("") }
                for (tag in tags) {
                    val link = tag.child(0).child(0)
                    values.add(link.text())
                    params.add(link.attr("href").let(::extractParams).let(::parseParam))
                }
                list.add(Category(children[0].selectFirst(spanSelector).text(), values.toTypedArray(), params))
            } else if (filterContainer.hasClass("form-row")) { // Dropdown filter
                for (select in filterContainer.select(selectSelector)) {
                    val options = select.children()
                    val values = ArrayList<String>(options.size).apply { add("全部") }
                    val params = ArrayList<String>(options.size).apply { add("") }
                    for (i in 1 until options.size) {
                        values.add(options[i].text())
                        params.add(options[i].attr("value").let(::extractParams).let(::parseParam))
                    }
                    list.add(Category(options[0].text(), values.toTypedArray(), params))
                }
            }
        }
        categories = list
    }

    private fun parseParam(params: String): String {
        val parts = params.split('-')
        for (i in 1 until parts.size step 2) {
            if (parts[i] != "0") return "${parts[i - 1]}-${parts[i]}"
        }
        return ""
    }

    override fun getFilterList() =
        if (::categories.isInitialized) FilterList(
            Filter.Header("如果使用文本搜索，将会忽略分类筛选"),
            *categories.map { it.toFilter() }.toTypedArray()
        ) else FilterList(
            Filter.Header("点击“重置”即可刷新分类，如果失败，"),
            Filter.Header("请尝试重新从图源列表点击进入图源"),
        )

    companion object {
        private val listComicLinkSelector = QueryParser.parse("h2 > a")
        private val imgSelector = Evaluator.Tag("img")
        private val h1Selector = Evaluator.Tag("h1")
        private val coverSelector = QueryParser.parse("td.comic-cover > img")
        private val descriptionSelector = QueryParser.parse("p.comic_story")
        private val tagSelector = QueryParser.parse("ul.tags > li > a")
        private val statusSelector = QueryParser.parse("a.comic-pub-state")
        private val regionSelector = QueryParser.parse("th:contains(地区) + td")
        private val audienceSelector = QueryParser.parse("th:contains(面向读者) + td")
        private val scriptSelector = QueryParser.parse("body > script:containsData(img_data)")
        private val readerConfigSelector = Evaluator.Class("vg-r-data")
        private val filterSelector = QueryParser.parse("div.search_div > div")
        private val spanSelector = Evaluator.Tag("span")
        private val selectSelector = Evaluator.Tag("select")
    }
}
