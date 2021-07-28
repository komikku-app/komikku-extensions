package eu.kanade.tachiyomi.extension.ar.mangaalarab

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class MangaAlarab : ParsedHttpSource() {

    override val name = "MangaAlarab"

    override val baseUrl = "https://mangaalarab.com"

    override val lang = "ar"

    override val supportsLatest = true

    // Popular

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/manga?page=$page")
    }

    override fun popularMangaSelector() = "article"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a").let {
                setUrlWithoutDomain(it.attr("abs:href"))
                title = element.select("h3").text()
                thumbnail_url = element.select("figure img").attr("data-src")
            }
        }
    }

    override fun popularMangaNextPageSelector() = "a[rel=next]"

    // Latest

    override fun latestUpdatesRequest(page: Int): Request {
        return GET(baseUrl)
    }

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun latestUpdatesNextPageSelector(): String? = null

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search?q=$query&page=$page", headers)
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    // Details

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            document.select("article").first().let { info ->

                description = info.select("p.text-sm").text()
            }
        }
    }

    // Chapters

    override fun chapterListSelector() = "div.chapters-container > div > a"

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            name = "${element.text()}"
            setUrlWithoutDomain(element.attr("href"))
        }
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.container > div > div > img").mapIndexed { i, img ->
            Page(i, "", img.attr("abs:src"))
        }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    override fun getFilterList() = FilterList()
}
