package eu.kanade.tachiyomi.extension.all.xinmeitulu

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable

class Xinmeitulu : ParsedHttpSource() {
    override val baseUrl = "https://www.xinmeitulu.com"
    override val lang = "all"
    override val name = "Xinmeitulu"
    override val supportsLatest = false

    // Latest

    override fun latestUpdatesRequest(page: Int) = throw UnsupportedOperationException("Not Used.")
    override fun latestUpdatesNextPageSelector() = throw UnsupportedOperationException("Not Used.")
    override fun latestUpdatesSelector() = throw UnsupportedOperationException("Not Used.")
    override fun latestUpdatesFromElement(element: Element) = throw UnsupportedOperationException("Not Used.")

    // Popular

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/page/$page")
    override fun popularMangaNextPageSelector() = ".next"
    override fun popularMangaSelector() = ".container > .row > div"
    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        setUrlWithoutDomain(element.select("figure > a").attr("abs:href"))
        title = element.select("figcaption").text()
        thumbnail_url = element.select("img").attr("abs:data-original-")
        genre = element.select("a.tag").joinToString(", ") { it.text() }
    }

    // Search

    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)
    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()
    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$baseUrl/page/$page?s=$query", headers)

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return if (query.startsWith("SLUG:")) {
            val slug = query.removePrefix("SLUG:")
            client.newCall(GET("$baseUrl/photo/$slug", headers)).asObservableSuccess()
                .map { response -> MangasPage(listOf(mangaDetailsParse(response.asJsoup())), false) }
        } else super.fetchSearchManga(page, query, filters)
    }

    // Details

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        setUrlWithoutDomain(document.selectFirst("link[rel=canonical]").attr("abs:href"))
        title = document.select(".container > h1").text()
        description = document.select(".container > *:not(div)").text()
        status = SManga.COMPLETED
        thumbnail_url = document.selectFirst("figure img").attr("abs:data-original")
    }

    // Chapters

    override fun chapterListSelector() = "html"

    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        setUrlWithoutDomain(element.selectFirst("link[rel=canonical]").attr("abs:href"))
        name = element.select(".container > h1").text()
    }

    override fun pageListParse(document: Document) =
        document.select(".container > div > figure img").mapIndexed { index, element ->
            Page(index, imageUrl = element.attr("abs:data-original"))
        }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")
}
