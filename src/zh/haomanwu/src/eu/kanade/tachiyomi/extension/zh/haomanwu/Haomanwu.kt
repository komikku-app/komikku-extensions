package eu.kanade.tachiyomi.extension.zh.haomanwu

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Haomanwu : ParsedHttpSource() {
    override val name: String = "好漫屋"
    override val lang: String = "zh"
    override val supportsLatest: Boolean = true
    override val baseUrl: String = "https://app2.haomanwu.com"

    // Popular

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/category/order/hits/page/$page", headers)
    override fun popularMangaNextPageSelector(): String? = "div#Pagination a.next"
    override fun popularMangaSelector(): String = "div.cate-comic-list > div.common-comic-item"
    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("p.comic__title > a").text()
        url = element.select("p.comic__title > a").attr("href")
        thumbnail_url = element.select("img").attr("data-original")
    }

    // Latest

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/category/order/addtime/page/$page", headers)
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/index.php/search?key=$query", headers)
    }

    override fun searchMangaNextPageSelector(): String? = null
    override fun searchMangaSelector(): String = "li > a"
    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.text()
        url = element.attr("href")
    }

    // Details

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.select("div.de-info__box > p.comic-title").text()
        thumbnail_url = document.select("div.de-info__cover > img").attr("src")
        author = document.select("div.comic-author > span.name > a").text()
        artist = author
        genre = document.select("div.comic-status > span.text:nth-child(1) a").eachText().joinToString(", ")
        description = document.select("p.intro-total").text()
    }

    // Chapters

    override fun chapterListSelector(): String = "ul.chapter__list-box > li"
    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        url = element.select("a").attr("href")
        name = element.select("a").text()
    }
    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response).reversed()
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.rd-article__pic > img.lazy-read")
            .mapIndexed { i, el -> Page(i, "", el.attr("data-original")) }
    }

    override fun imageUrlParse(document: Document): String = throw Exception("Not Used")
}
