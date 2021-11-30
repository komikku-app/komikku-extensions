package eu.kanade.tachiyomi.extension.zh.yimmh

import android.net.Uri
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
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Yimmh : ParsedHttpSource() {
    override val name: String = "忆漫"
    override val lang: String = "zh"
    override val supportsLatest: Boolean = true
    override val baseUrl: String = "https://m.yimmh.com"
    override fun headersBuilder() = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Android 11; Mobile; rv:83.0) Gecko/83.0 Firefox/83.0")

    private fun toHttp(url: String): String {
        // Images from https://*.yemancomic.com do not load
        // because of certificate issue, so switch to http.
        return if (url.startsWith("https")) {
            "http" + url.substring(5)
        } else url
    }

    // Popular

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/rank", headers)
    override fun popularMangaNextPageSelector(): String? = null
    override fun popularMangaSelector(): String = "ul.rank-list > a"
    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("p.rank-list-info-right-title").text()
        setUrlWithoutDomain(element.attr("abs:href"))
        thumbnail_url = toHttp(element.select("img").attr("abs:data-original"))
    }

    // Latest

    override fun latestUpdatesParse(response: Response): MangasPage {
        val body = response.body?.string().orEmpty()
        val json = JSONObject(body)
        if (json.getInt("err") != 0) {
            return MangasPage(listOf<SManga>(), false)
        }
        val mangas = arrayListOf<SManga>()
        val books = json.getJSONArray("books")
        for (i in 0 until books.length()) {
            val book = books.getJSONObject(i)
            mangas.add(
                SManga.create().apply {
                    title = book.getString("book_name")
                    url = "/book/${book.getString("unique_id")}"
                    thumbnail_url = toHttp(book.getString("cover_url"))
                }
            )
        }
        return MangasPage(mangas, true)
    }

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/getUpdate?page=${page * 15 - 15}", headers)
    override fun latestUpdatesNextPageSelector(): String? = throw Exception("Not used")
    override fun latestUpdatesSelector() = throw Exception("Not used")
    override fun latestUpdatesFromElement(element: Element) = throw Exception("Not used")

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val uri = Uri.parse(baseUrl).buildUpon()
        if (query.isNotBlank()) {
            uri.appendPath("search")
                .appendQueryParameter("keyword", query)
        } else {
            uri.appendPath("getBooks")
            filters.forEach {
                if (it is CategoryFilter)
                    uri.appendQueryParameter("tag", it.toUri())
                else if (it is StatusFilter)
                    uri.appendQueryParameter("end", it.toUri())
            }
            uri.appendQueryParameter("page", (page * 15 - 15).toString())
        }
        return GET(uri.toString(), headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        return if (response.request.url.toString().startsWith("https://m.yimmh.com/search"))
            super.searchMangaParse(response)
        else
            latestUpdatesParse(response)
    }

    override fun searchMangaNextPageSelector(): String? = null
    override fun searchMangaSelector(): String = "ul.book-list > li"
    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("p.book-list-info-title").text()
        setUrlWithoutDomain(element.select("a").attr("abs:href"))
        thumbnail_url = toHttp(element.select("img").attr("abs:data-original"))
    }

    // Details

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.select("p.detail-main-info-title").text()
        thumbnail_url = toHttp(document.select("div.detail-main-cover > img").attr("abs:data-original"))
        author = document.select("p.detail-main-info-author:contains(作者：) > a").text()
        artist = author
        genre = document.select("p.detail-main-info-class > span").eachText().joinToString(", ")
        description = document.select("p.detail-desc").text()
        status = when (document.select("span.detail-list-title-1").text()) {
            "连载中" -> SManga.ONGOING
            "已完结" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
    }

    // Chapters

    override fun chapterListSelector(): String = "ul#detail-list-select > li"
    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        setUrlWithoutDomain(element.select("a.chapteritem").attr("abs:href"))
        name = element.select("a.chapteritem").text()
    }
    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response).reversed()
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> = mutableListOf<Page>().apply {
        var page = document
        while (true) {
            val images = page.select("div#cp_img > img.lazy")
            images.forEach {
                add(Page(size, "", toHttp(it.attr("abs:data-src"))))
            }
            val nextPage = page.select("a.view-bottom-bar-item:contains(下一页)").attr("href")
            if (nextPage.isNullOrEmpty()) {
                break
            } else {
                page = client.newCall(GET(baseUrl + nextPage, headers))
                    .execute().asJsoup()
            }
        }
    }

    override fun imageUrlParse(document: Document): String = throw Exception("Not Used")

    // Filters

    override fun getFilterList() = FilterList(
        Filter.Header("如果使用文本搜索"),
        Filter.Header("过滤器将被忽略"),
        CategoryFilter(),
        StatusFilter()
    )

    private class CategoryFilter : UriSelectFilterPath(
        "分类",
        arrayOf(
            Pair("全部", "全部"),
            Pair("耽美", "耽美"),
            Pair("热血", "热血"),
            Pair("大女主", "大女主")
        )
    )

    private class StatusFilter : UriSelectFilterPath(
        "进度",
        arrayOf(
            Pair("-1", "全部"),
            Pair("2", "连载中"),
            Pair("1", "已完结")
        )
    )

    /**
     * Class that creates a select filter. Each entry in the dropdown has a name and a display name.
     * If an entry is selected it is appended as a query parameter onto the end of the URI.
     */
    // vals: <name, display>
    private open class UriSelectFilterPath(
        displayName: String,
        val vals: Array<Pair<String, String>>
    ) : Filter.Select<String>(displayName, vals.map { it.second }.toTypedArray()) {
        fun toUri() = vals[state].first
    }
}
