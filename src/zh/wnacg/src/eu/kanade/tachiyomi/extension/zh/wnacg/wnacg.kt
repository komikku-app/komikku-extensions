package eu.kanade.tachiyomi.extension.zh.wnacg

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
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
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable

class wnacg : ParsedHttpSource() {
    override val name = "紳士漫畫"
    override val baseUrl = "https://www.wnacg.org"
    override val lang = "zh"
    override val supportsLatest = false

    override fun popularMangaSelector() = "div.pic_box"
    override fun latestUpdatesSelector() = throw Exception("Not used")
    override fun searchMangaSelector() = popularMangaSelector()
    override fun chapterListSelector() = "div.f_left > a"

    override fun popularMangaNextPageSelector() = "span.thispage + a"
    override fun latestUpdatesNextPageSelector() = throw Exception("Not used")
    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/albums-index-page-$page.html", headers)
    }

    override fun latestUpdatesRequest(page: Int) = throw Exception("Not used")

    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList
    ): Observable<MangasPage> {
        // ps: this web don't support category search and sort
        var req: Request? = null
        if (query.isNotBlank()) {
            req = this.searchMangaRequest(page, query, filters)
        } else if (filters.isNotEmpty()) {
            filters.forEach { filter ->
                if (filter is CategoryFilter) {
                    req = GET("$baseUrl/" + filter.toUriPart().format(page))
                }
            }
        }
        if (req != null) {
            return client.newCall(req!!)
                .asObservableSuccess()
                .map { response -> queryParse(response) }
        }
        return super.fetchSearchManga(page, query, filters)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search/index.php?q=$query&p=$page", headers)
    }

    override fun mangaDetailsRequest(manga: SManga) = GET(baseUrl + manga.url, headers)
    override fun chapterListRequest(manga: SManga) = mangaDetailsRequest(manga)

    override fun pageListRequest(chapter: SChapter) = GET(baseUrl + chapter.url, headers)
    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .set("referer", baseUrl)
        .set("sec-fetch-mode", "no-cors")
        .set("sec-fetch-site", "cross-site")
        .set(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36"
        )

    override fun popularMangaFromElement(element: Element) = mangaFromElement(element)
    override fun latestUpdatesFromElement(element: Element) = throw Exception("Not used")
    override fun searchMangaFromElement(element: Element) = mangaFromElement(element)

    private fun mangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.setUrlWithoutDomain(element.select("a").first().attr("href"))
        manga.title = element.select("a").attr("title").trim().replace(Regex("<[^<>]*>"), "")
        manga.thumbnail_url = "https://" + element.select("img").attr("src").replace("//", "")
        // maybe the local cache cause the old source (url) can not be update. but the image can be update on detailpage.
        // ps. new machine can be load img normal.

        return manga
    }

    private fun queryParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(searchMangaSelector())
            .map { element -> searchMangaFromElement(element) }
        val nextPage = document.select(searchMangaNextPageSelector()).first() != null
        return MangasPage(mangas, nextPage)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chapters = mutableListOf<SChapter>()
        // create one chapter since it is single books
        chapters.add(createChapter("1", document.baseUri()))
        return chapters
    }

    private fun createChapter(pageNumber: String, mangaUrl: String): SChapter {
        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(mangaUrl)
        chapter.name = "Ch. $pageNumber"
        return chapter
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        manga.title = document.select("h2")?.text()?.trim() ?: "Unknown"
        manga.artist = document.select("div.uwuinfo p")?.first()?.text()?.trim() ?: "Unknown"
        manga.author = document.select("div.uwuinfo p")?.first()?.text()?.trim() ?: "Unknown"
        manga.thumbnail_url =
            "https://" + document.select("div.uwthumb img").first().attr("src").replace("//", "")
        manga.description =
            document.select("div.asTBcell p")?.first()?.html()?.replace("<br>", "\n")

        return manga
    }

    override fun pageListParse(document: Document): List<Page> {
        val regex = "\\/\\/\\S*(jpg|png)".toRegex()
        val slideaid = client.newCall(
            GET(
                baseUrl + document.select("a.btn:containsOwn(下拉閱讀)").attr("href"),
                headers
            )
        ).execute().asJsoup()
        val galleryaid =
            client.newCall(GET(baseUrl + slideaid.select("script[src$=html]").attr("src"), headers))
                .execute().asJsoup().toString()
        val matchresult = regex.findAll(galleryaid).map { it.value }.toList()
        val pages = mutableListOf<Page>()
        for (i in matchresult.indices) {
            pages.add(Page(i, "", "https:" + matchresult[i]))
        }
        return pages
    }

    override fun chapterFromElement(element: Element) = throw Exception("Not used")
    override fun imageUrlRequest(page: Page) = throw Exception("Not used")
    override fun imageUrlParse(document: Document) = throw Exception("Not used")

    // >>> Filters >>>

    override fun getFilterList() = FilterList(
        Filter.Header("注意：分类不支持搜索"),
        CategoryFilter()
    )

    private class CategoryFilter : UriPartFilter(
        "分类",
        arrayOf(
            Pair("更新", "albums-index-page-%d.html"),
            Pair("同人志-汉化", "albums-index-page-%d-cate-1.html"),
            Pair("同人志-日语", "albums-index-page-%d-cate-12.html"),
            Pair("同人志-CG书籍", "albums-index-page-%d-cate-2.html"),
            Pair("同人志-Cosplay", "albums-index-page-%d-cate-3.html"),
            Pair("单行本-汉化", "albums-index-page-%d-cate-9.html"),
            Pair("单行本-日语", "albums-index-page-%d-cate-13.html"),
            Pair("杂志&短篇-汉语", "albums-index-page-%d-cate-10.html"),
            Pair("杂志&短篇-日语", "albums-index-page-%d-cate-14.html"),
            Pair("韩漫-汉化", "albums-index-page-%d-cate-20.html"),
            Pair("韩漫-生肉", "albums-index-page-%d-cate-21.html"),
        )
    )

    private open class UriPartFilter(displayName: String, val vals: Array<Pair<String, String>>) :
        Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray()) {
        fun toUriPart() = vals[state].second
    }

    // <<< Filters <<<
}
