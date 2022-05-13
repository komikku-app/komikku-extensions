package eu.kanade.tachiyomi.multisrc.mccms

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

// 漫城CMS http://mccms.cn/
abstract class MCCMS(
    override val name: String,
    override val baseUrl: String,
    override val lang: String = "zh",
) : ParsedHttpSource() {
    override val supportsLatest: Boolean = true

    protected open val nextPageSelector = "div#Pagination a.next"
    protected open val comicItemSelector = "div.common-comic-item"
    protected open val comicItemTitleSelector = "p.comic__title > a"

    protected open fun transformTitle(title: String) = title

    // Popular

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/category/order/hits/page/$page", headers)
    override fun popularMangaNextPageSelector() = nextPageSelector
    override fun popularMangaSelector() = comicItemSelector
    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        val titleElement = element.select(comicItemTitleSelector)
        title = transformTitle(titleElement.text().trim())
        setUrlWithoutDomain(titleElement.attr("abs:href"))
        thumbnail_url = element.select("img").attr("abs:data-original")
    }

    // Latest

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/category/order/addtime/page/$page", headers)
    override fun latestUpdatesNextPageSelector() = nextPageSelector
    override fun latestUpdatesSelector() = comicItemSelector
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$baseUrl/search/$query/$page", headers)

    override fun searchMangaNextPageSelector(): String? = nextPageSelector
    override fun searchMangaSelector() = comicItemSelector
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

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
}
