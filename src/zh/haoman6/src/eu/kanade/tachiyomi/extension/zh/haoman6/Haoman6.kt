package eu.kanade.tachiyomi.extension.zh.haoman6

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

class Haoman6 : ParsedHttpSource() {
    override val name: String = "好漫6"
    override val lang: String = "zh"
    override val supportsLatest: Boolean = false
    override val baseUrl: String = "https://www.haoman6.com"

    // Popular

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/custom/hot", headers)
    override fun popularMangaNextPageSelector(): String? = null
    override fun popularMangaSelector(): String = "div.top-list__box > div.top-list__box-item"
    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("p.comic__title > a").text()
        setUrlWithoutDomain(element.select("p.comic__title > a").attr("abs:href"))
        thumbnail_url = element.select("img").attr("abs:data-original")
    }

    // Latest

    override fun latestUpdatesRequest(page: Int) = throw Exception("Not used")
    override fun latestUpdatesNextPageSelector() = throw Exception("Not used")
    override fun latestUpdatesSelector() = throw Exception("Not used")
    override fun latestUpdatesFromElement(element: Element) = throw Exception("Not used")

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search/$query/$page", headers)
    }

    override fun searchMangaNextPageSelector(): String? = "div#Pagination a.next"
    override fun searchMangaSelector(): String = "div.common-comic-item"
    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("p.comic__title > a").text()
        setUrlWithoutDomain(element.select("p.comic__title > a").attr("abs:href"))
        thumbnail_url = element.select("img").attr("abs:data-original")
    }

    // Details

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.select("div.de-info__box > p.comic-title").text()
        thumbnail_url = document.select("div.de-info__cover > img").attr("abs:src")
        author = document.select("div.comic-author > span.name > a").text()
        artist = author
        genre = document.select("div.comic-status > span.text:nth-child(1) a").eachText().joinToString(", ")
        description = document.select("div.comic-intro > p.intro-total").text()
    }

    // Chapters

    override fun chapterListSelector(): String = "ul.chapter__list-box > li"
    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        setUrlWithoutDomain(element.select("a").attr("abs:href"))
        name = element.select("a").text()
    }
    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response).reversed()
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.rd-article__pic > img.lazy-read")
            .mapIndexed { i, el -> Page(i, "", el.attr("abs:echo-pc")) }
    }

    override fun imageUrlParse(document: Document): String = throw Exception("Not Used")
}
